export interface ApiErrorPayload {
  status?: number
  code?: string
  message?: string
  path?: string
  timestamp?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string | null
  readonly path: string | null

  constructor(
    status: number,
    message: string,
    code: string | null = null,
    path: string | null = null,
  ) {
    super(message)

    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.path = path
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    const contentType = response.headers.get('content-type')

    if (contentType?.includes('application/json')) {
      try {
        const payload = (await response.json()) as ApiErrorPayload

        return new ApiError(
          response.status,
          payload.message ?? `HTTP request failed with status ${response.status}`,
          payload.code ?? null,
          payload.path ?? null,
        )
      } catch {
        // Fall through to the generic HTTP error.
      }
    }

    return new ApiError(
      response.status,
      `HTTP request failed with status ${response.status}`,
    )
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}