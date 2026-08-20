import type {
  CurrentUser,
  LoginRequest,
} from '../auth/auth.types'
import { clearCsrfToken } from './csrf'
import {
  apiJson,
  apiRequest,
} from './httpClient'

export async function login(
  credentials: LoginRequest,
): Promise<CurrentUser> {
  const user = await apiJson<CurrentUser>(
    '/api/auth/login',
    {
      method: 'POST',
      body: credentials,
    },
  )

  /*
   * Spring Security may rotate the HTTP session during login.
   * A CSRF token obtained before authentication must therefore
   * not be reused blindly afterwards.
   */
  clearCsrfToken()

  return user
}

export async function getCurrentUser(): Promise<CurrentUser> {
  return apiRequest<CurrentUser>(
    '/api/auth/me',
  )
}

export async function logout(): Promise<void> {
  await apiRequest<void>(
    '/api/auth/logout',
    {
      method: 'POST',
    },
  )

  clearCsrfToken()
}