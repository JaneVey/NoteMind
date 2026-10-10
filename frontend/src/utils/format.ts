/**
 * 展示层格式化函数。
 *
 * <p>后端接口一律返回 ISO-8601 带时区偏移的字符串（如 `2026-10-08T22:05:20+08:00`），
 * 这是无歧义的值；显示成什么样属于展示职责，放在前端。
 * **不要改回不含时区偏移的格式** —— `new Date()` 会按浏览器本地时区解析，跨时区必然错。
 */

/** 补零到两位 */
function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/**
 * 格式化为 `2026-10-08 22:05`。
 *
 * <p>用 `getFullYear()` 等方法而非直接切片字符串，前者会按用户本地时区换算。
 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 格式化为 `2026-10-08` */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 相对时间：`刚刚` / `5 分钟前` / `3 小时前` / `2 天前`，超过 30 天返回日期 */
export function formatRelativeTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''

  const diffMs = Date.now() - d.getTime()
  const diffMin = Math.floor(diffMs / 60_000)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`

  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`

  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return `${diffDay} 天前`

  return formatDate(iso)
}

/**
 * 文件大小：`856 B` / `12.3 KB` / `1.2 MB`。
 *
 * <p>按 1024 进制换算但显示成 KB/MB —— 显示 `KiB` 反而让人困惑。
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes < 0) return ''
  if (bytes < 1024) return `${bytes} B`

  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes / 1024
  let i = 0
  while (value >= 1024 && i < units.length - 1) {
    value /= 1024
    i++
  }
  // 小于 10 时保留一位小数（1.2 MB），否则取整（123 KB）
  return `${value < 10 ? value.toFixed(1) : Math.round(value)} ${units[i]}`
}

/** 字数：`1.2 万字` / `856 字` */
export function formatWordCount(count: number | null | undefined): string {
  if (!count || count < 0) return '0 字'
  if (count < 10_000) return `${count} 字`
  return `${(count / 10_000).toFixed(1)} 万字`
}

/** 取出可读的短文名，过长时中间省略（保留扩展名）。PDF 的中文名往往很长 */
export function truncateFileName(name: string, max = 24): string {
  if (!name || name.length <= max) return name
  const dot = name.lastIndexOf('.')
  const ext = dot > 0 ? name.slice(dot) : ''
  const base = dot > 0 ? name.slice(0, dot) : name
  const keep = Math.max(4, max - ext.length - 1)
  return `${base.slice(0, keep)}…${ext}`
}

/** 时间 → 时钟字符串 `22:05:31`，用于 `setInterval` 等场景 */
export function formatClock(date: Date = new Date()): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
