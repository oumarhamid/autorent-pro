import {
  apiJson,
} from './httpClient'
import {
  clearCsrfToken,
} from './csrf'
import type {
  ChangePasswordRequest,
} from '../auth/auth.types'

export async function changePassword(
  request: ChangePasswordRequest,
): Promise<void> {
  await apiJson<void>(
    '/api/account/change-password',
    {
      method: 'POST',
      body: request,
    },
  )

  clearCsrfToken()
}