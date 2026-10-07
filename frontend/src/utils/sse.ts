/**
 * SSE 流式接收工具
 * 用于 AI 对话的逐字输出
 */
export interface SseConnectionOptions {
  method?: string
  body?: unknown
  onMessage?: (data: unknown) => void
  onError?: (error: unknown) => void
  onDone?: () => void
  signal?: AbortSignal | null
}

export function createSseConnection(url: string, options: SseConnectionOptions = {}) {
  const {
    method = 'POST',
    body = null,
    onMessage = () => {},
    onError = () => {},
    onDone = () => {},
    signal = null,
  } = options

  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : null,
    signal,
  })
    .then(async (response) => {
      if (!response.body) throw new Error('SSE 响应不包含可读取的内容')
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          onDone()
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (data === '[DONE]') {
              onDone()
              return
            }
            try {
              onMessage(JSON.parse(data))
            } catch {
              onMessage(data)
            }
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err)
      }
    })
}
