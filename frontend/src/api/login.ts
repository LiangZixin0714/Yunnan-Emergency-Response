import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types/user'

export function login(params: LoginParams): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/login', params)
}

export function register(params: RegisterParams): Promise<ApiResponse<UserInfo>> {
  return request.post('/auth/register', params)
}
