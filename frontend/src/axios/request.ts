import axios, { type AxiosError, type AxiosResponse } from 'axios'
import qs from 'qs'
import type { ApiResult } from './types'

type ApiResponse<T> = AxiosResponse<ApiResult<T>> | AxiosError<ApiResult<T>>

const client = axios.create({
  paramsSerializer: { serialize: (params) => qs.stringify(params, { arrayFormat: 'comma' }) }
})

let redirectingToLogin = false

async function resolveResponse<T>(request: Promise<AxiosResponse<ApiResult<T>>>): Promise<ApiResponse<T>> {
  try {
    return await request
  } catch (error) {
    if (!axios.isAxiosError<ApiResult<T>>(error)) throw error
    if (error.response?.status === 401 && !redirectingToLogin) {
      redirectingToLogin = true
      window.location.assign('/login')
    }
    return error
  }
}

export const get = <T>(url: string, params?: object) =>
  resolveResponse(client.get<ApiResult<T>>(url, { params }))

export const post = <T>(url: string, data?: object) =>
  resolveResponse(client.post<ApiResult<T>>(url, data))

export const putRequest = <T>(url: string, data?: object) =>
  resolveResponse(client.put<ApiResult<T>>(url, data))

export const deleteRequest = <T>(url: string, params?: object) =>
  resolveResponse(client.delete<ApiResult<T>>(url, { params }))
