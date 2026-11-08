import { AxiosError, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/axios/types'

/** 接口失败：message 为后端返回的原因 */
export class NewsApiError extends Error {}

/**
 * 取出成功响应的 data；业务失败或网络失败时抛出 NewsApiError。
 * toast 为 true 时同时弹出失败提示；需要在界面上就地展示原因的调用传 false。
 */
export async function newsRequestData<T>(
  request: Promise<AxiosResponse<ApiResult<T>> | AxiosError<ApiResult<T>>>,
  toast = true
): Promise<T> {
  const res = await request
  const body = res instanceof AxiosError ? res.response?.data : res.data
  if (body?.code === 200) return body.data
  const message = body?.msg || '网络不给力'
  if (toast) ElMessage({ type: 'error', message, customClass: 'news-admin-message', placement: 'bottom' })
  throw new NewsApiError(message)
}
