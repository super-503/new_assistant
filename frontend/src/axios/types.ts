export interface ApiResult<T> {
  code: number
  ret: number
  msg: string
  data: T
  errors?: string[] | null
}
