import { ApiError } from './apiError'

export interface CsrfToken {
  token: string
  headerName: string
  parameterName?: string
}

let cachedToken: CsrfToken | null = null
let pendingTokenRequest: Promise<CsrfToken> | null = null

export async function getCsrfToken(): Promise<CsrfToken> {
  if (cachedToken) {
    return cachedToken
  }

  if (pendingTokenRequest) {
    return pendingTokenRequest
  }

  pendingTokenRequest = fetchCsrfToken()

  try {
    cachedToken = await pendingTokenRequest

    return cachedToken
  } finally {
    pendingTokenRequest = null
  }
}

export function clearCsrfToken(): void {
  cachedToken = null
  pendingTokenRequest = null
}

async function fetchCsrfToken(): Promise<CsrfToken> {
  const response = await fetch('/api/auth/csrf', {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw await ApiError.fromResponse(response)
  }

  const payload = (await response.json()) as Partial<CsrfToken>

  if (!payload.token || !payload.headerName) {
    throw new Error('The backend returned an invalid CSRF token response.')
  }

  return {
    token: payload.token,
    headerName: payload.headerName,
    parameterName: payload.parameterName,
  }
}