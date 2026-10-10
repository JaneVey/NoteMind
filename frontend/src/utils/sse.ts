import { getAccessToken, notifySessionExpired } from '@/utils/authToken'
import { refreshAccessToken } from '@/api/authRefresh'

/**
 * 服务端流式事件类型。
 * 与后端 `com.notemind.ai.dto.ChatStreamChunk` 的 type 取值一一对应。
 */
export type ChatStreamEventType = 'reasoning' | 'content' | 'done' | 'error'

export interface ChatStreamEvent {
  type: ChatStreamEventType
  text?: string | null
}

export interface ChatStreamHandlers {
  /** 思考过程增量（模型思维链） */
  onReasoning?: (text: string) => void
  /** 回答正文增量 */
  onContent?: (text: string) => void
  /** 正常结束（只会被调用一次） */
  onDone?: () => void
  /** 出错（含网络错误与后端 error 事件） */
  onError?: (message: string) => void
}

export interface ChatStreamOptions {
  /** 用于取消生成，例如组件卸载或用户点击「停止」 */
  signal?: AbortSignal
  /** 额外请求头 */
  headers?: Record<string, string>
}

/**
 * 发起流式对话。解析的是 SSE，但按「空行切事件块」处理 ——
 * 一条消息可能被拆到多个网络分片，按行读完再解析会丢掉半行数据。
 *
 * <p>用 fetch + ReadableStream 而不是 EventSource：后者只支持 GET，也无法携带 Authorization 头。
 *
 * <p>结束判定兼容后端 `{"type":"done"}` 与 OpenAI 风格的裸 `data: [DONE]`。
 */
export function streamChat(
  url: string,
  body: unknown,
  handlers: ChatStreamHandlers,
  options: ChatStreamOptions = {},
): void {
  const { signal, headers = {} } = options
  let finished = false

  const finish = (): void => {
    if (finished) return
    finished = true
    handlers.onDone?.()
  }

  const fail = (message: string): void => {
    if (finished) return
    finished = true
    handlers.onError?.(message)
  }

  /**
   * 发起请求，access token 过期时自动刷新一次后重放。
   *
   * <p>axios 那条链路（api/request.ts）已有一致的逻辑，两条链路的行为必须一致。
   *
   * @param allowRetry 只允许重放一次，避免新 token 仍被拒时无限递归
   */
  const doFetch = async (token: string | null, allowRetry: boolean): Promise<Response> => {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: JSON.stringify(body),
      signal,
    })

    if (response.status === 401 && allowRetry) {
      const auth = await refreshAccessToken()
      if (auth) {
        return doFetch(auth.accessToken, false)
      }
      // refresh token 也失效了：会话真的结束
      notifySessionExpired()
    }
    return response
  }

  // 从内存读 token，不读 localStorage —— access token 从不落盘
  doFetch(getAccessToken(), true)
    .then(async (response) => {
      if (!response.ok) {
        // 走到这里的 401：doFetch 已刷新并重放过，仍然失败
        if (response.status === 401) {
          fail('登录已过期，请重新登录')
          return
        }
        // 其他错误：后端响应遵循 Result 结构，尽量把 message 透出去
        const raw = await response.text().catch(() => '')
        let message = `请求失败（HTTP ${response.status}）`
        try {
          const parsed = JSON.parse(raw) as { message?: string }
          if (parsed?.message) message = parsed.message
        } catch {
          /* 非 JSON，保留默认文案 */
        }
        fail(message)
        return
      }
      if (!response.body) {
        fail('响应不包含可读取的内容流')
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const dispatch = (event: ChatStreamEvent): void => {
        switch (event.type) {
          case 'reasoning':
            if (event.text) handlers.onReasoning?.(event.text)
            break
          case 'content':
            if (event.text) handlers.onContent?.(event.text)
            break
          case 'done':
            finish()
            break
          case 'error':
            fail(event.text || '生成失败')
            break
          default:
            break
        }
      }

      const handleBlock = (block: string): void => {
        const dataLines: string[] = []
        for (const line of block.split('\n')) {
          if (!line || line.startsWith(':')) continue // 空行与注释行
          const colon = line.indexOf(':')
          const field = colon === -1 ? line : line.slice(0, colon)
          let value = colon === -1 ? '' : line.slice(colon + 1)
          if (value.startsWith(' ')) value = value.slice(1)
          if (field === 'data') dataLines.push(value)
        }
        if (dataLines.length === 0) return

        // 规范允许同一事件有多条 data 行，按换行拼接
        const data = dataLines.join('\n')
        if (data === '[DONE]') {
          finish()
          return
        }
        try {
          dispatch(JSON.parse(data) as ChatStreamEvent)
        } catch {
          // 不是 JSON：按纯文本正文处理，保证不丢内容
          handlers.onContent?.(data)
        }
      }

      for (;;) {
        const { done, value } = await reader.read()
        if (done) break

        // 统一换行符，并按 SSE 规范以「空行」切分事件块
        buffer += decoder.decode(value, { stream: true })
        buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

        let boundary = buffer.indexOf('\n\n')
        while (boundary >= 0) {
          const block = buffer.slice(0, boundary)
          buffer = buffer.slice(boundary + 2)
          if (block.trim()) handleBlock(block)
          boundary = buffer.indexOf('\n\n')
        }

        if (finished) {
          await reader.cancel().catch(() => undefined)
          break
        }
      }

      // 流已结束：处理残留内容，并确保 onDone 被调用
      if (buffer.trim()) handleBlock(buffer)
      finish()
    })
    .catch((error: unknown) => {
      if (error instanceof DOMException && error.name === 'AbortError') {
        // 用户主动取消：按正常结束处理，不报错
        finish()
        return
      }
      fail(error instanceof Error ? error.message : '网络请求失败')
    })
}
