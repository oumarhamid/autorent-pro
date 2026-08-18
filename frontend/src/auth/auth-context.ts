import {
  createContext,
} from 'react'

import type {
  CurrentUser,
  LoginRequest,
} from './auth.types'

export type AuthenticationStatus =
  | 'loading'
  | 'authenticated'
  | 'unauthenticated'

export interface AuthContextValue {
  user: CurrentUser | null
  status: AuthenticationStatus
  isAuthenticated: boolean
  sessionError: string | null

  login: (
    credentials: LoginRequest,
  ) => Promise<CurrentUser>

  logout: () => Promise<void>

  refreshUser: () => Promise<CurrentUser | null>
}

export const AuthContext =
  createContext<AuthContextValue | undefined>(
    undefined,
  )