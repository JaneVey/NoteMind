import { redirectToLogin, TOKEN_KEY } from '@/api/request'

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
 * 发起流式对话。
 *
 * <h3>为什么用 fetch + ReadableStream 而不是 EventSource</h3>
 * `EventSource` 只支持 GET，且**无法携带 `Authorization` 头**。
 * 本项目使用 JWT 鉴权、请求体为 JSON，因此必须用 fetch 手动读取响应流。
 * 这一点原实现是对的，此处保留该方式并补全解析。
 *
 * <h3>本次修正的三处缺陷</h3>
 * 原实现按 `\n` 切分、只识别以 `data: ` 开头的行：
 * <ol>
 *   <li><b>未按 SSE 规范以空行切分事件块</b> —— 一条消息被拆到多个网络分片时，
 *       半行数据会被当成完整数据解析，导致解析失败或内容丢失；</li>
 *   <li><b>未处理多行 data</b> —— 规范允许同一事件有多条 `data:` 行，需按换行拼接；</li>
 *   <li><b>无法区分思考过程与正文</b> —— 两者混在一起渲染。</li>
 * </ol>
 *
 * <h3>关于 `[DONE]`</h3>
 * 后端当前发送的是 `{"type":"done"}` 事件；此处同时兼容 OpenAI 风格的裸
 * `data: [DONE]`，便于将来切换到其他兼容服务时无需改动前端。
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

  const token = localStorage.getItem(TOKEN_KEY)

  fetch(url, {
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
    .then(async (response) => {
      if (!response.ok) {
        // 401：与 axios 链路（api/request.ts）保持一致 —— 清除本地登录状态并跳登录页。
        // 否则用户只会看到一句"未登录或登录已过期"，却不知道要去登录。
        if (response.status === 401) {
          redirectToLogin()
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
