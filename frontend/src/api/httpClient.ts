import { ApiError } from './apiError'
import { getCsrfToken } from './csrf'

type HttpMethod =
  | 'GET'
  | 'POST'
  | 'PUT'
  | 'PATCH'
  | 'DELETE'

interface ApiRequestOptions {
  method?: HttpMethod
  headers?: HeadersInit
  body?: BodyInit | null
  signal?: AbortSignal
}

interface JsonRequestOptions {
  method?: HttpMethod
  headers?: HeadersInit
  body?: unknown
  signal?: AbortSignal
}

const MUTATING_METHODS = new Set<HttpMethod>([
  'POST',
  'PUT',
  'PATCH',
  'DELETE',
])

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const method = options.method ?? 'GET'
  const headers = new Headers(options.headers)

  headers.set('Accept', 'application/json')

  if (MUTATING_METHODS.has(method)) {
    const csrf = await getCsrfToken()

    headers.set(
      csrf.headerName,
      csrf.token,
    )
  }

  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers,
    body: options.body,
    signal: options.signal,
  })

  if (!response.ok) {
    throw await ApiError.fromResponse(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type')

  if (!contentType?.includes('application/json')) {
    return undefined as T
  }

  return (await response.json()) as T
}

export async function apiJson<T>(
  path: string,
  options: JsonRequestOptions = {},
): Promise<T> {
  const headers = new Headers(options.headers)

  headers.set(
    'Content-Type',
    'application/json',
  )

  return apiRequest<T>(
    path,
    {
      method: options.method,
      headers,
      body:
        options.body === undefined
          ? undefined
          : JSON.stringify(options.body),
      signal: options.signal,
    },
  )
}