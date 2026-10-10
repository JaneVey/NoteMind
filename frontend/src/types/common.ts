/**
 * 通用类型。
 *
 * <p>后端所有接口返回统一响应壳 {@link ApiResponse}，由 axios 拦截器拆壳后只拿到 `data`。
 */

/** 主键。后端为 BIGINT（Java Long），但 JSON 里可能以字符串传输以避免精度丢失 */
export type Id = string | number

/**
 * 时间字符串，一律是 ISO-8601 带时区偏移，如 `2026-10-08T22:05:20+08:00`。
 *
 * <p>后端响应移除了 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")` —— 该格式不含时区偏移，
 * `new Date()` 会按浏览器本地时区解析，跨时区必然错。要展示成 `2026-10-08 22:05`
 * 请用 `@/utils/format` 里的格式化函数，不要改接口的返回格式。
 */
export type IsoDateTime = string

/** 后端统一响应壳 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 分页结果（后端 PageResult） */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 分页查询参数 */
export interface PageQuery {
  /** 页码，从 1 开始 */
  page?: number
  /** 每页条数，后端限制 1..100 */
  size?: number
}
