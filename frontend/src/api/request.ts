import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'

export interface ApiResponse<T> { code: number; message: string; data: T }
type RequestConfig = AxiosRequestConfig
interface TypedRequest {
  get<T = unknown>(url: string, config?: RequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: RequestConfig): Promise<T>
  <T = unknown>(config: RequestConfig): Promise<T>
}

const client = axios.create({ baseURL: '/api', timeout: 30000 })
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('notemind_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
client.interceptors.response.use(
  <T>(response: AxiosResponse<ApiResponse<T>>): T => {
    const result = response.data
    if (result.code !== 200) throw new Error(result.message)
    return result.data
  },
  (error: unknown) => Promise.reject(error),
)
export default client as unknown as TypedRequest
