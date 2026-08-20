import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import {
  changePassword as changePasswordRequest,
} from '../api/accountApi'
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
} from '../api/authApi'
import {
  isApiError,
} from '../api/apiError'
import {
  AuthContext,
  type AuthenticationStatus,
  type AuthContextValue,
} from './auth-context'
import type {
  ChangePasswordRequest,
  CurrentUser,
  LoginRequest,
} from './auth.types'

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({
  children,
}: AuthProviderProps) {
  const [user, setUser] =
    useState<CurrentUser | null>(null)

  const [status, setStatus] =
    useState<AuthenticationStatus>(
      'loading',
    )

  const [sessionError, setSessionError] =
    useState<string | null>(null)

  const clearAuthentication =
    useCallback(() => {
      setUser(null)
      setStatus('unauthenticated')
      setSessionError(null)
    }, [])

  const refreshUser =
    useCallback(
      async (): Promise<CurrentUser | null> => {
        try {
          const currentUser =
            await getCurrentUser()

          setUser(currentUser)
          setStatus('authenticated')
          setSessionError(null)

          return currentUser
        } catch (error) {
          if (
            isApiError(error)
            && error.status === 401
          ) {
            clearAuthentication()
            return null
          }

          setUser(null)
          setStatus('unauthenticated')
          setSessionError(
            'Impossible de vérifier la session actuelle.',
          )

          return null
        }
      },
      [
        clearAuthentication,
      ],
    )

  useEffect(() => {
    let active = true

    const restoreSession =
      async (): Promise<void> => {
        try {
          const currentUser =
            await getCurrentUser()

          if (!active) {
            return
          }

          setUser(currentUser)
          setStatus('authenticated')
          setSessionError(null)
        } catch (error) {
          if (!active) {
            return
          }

          setUser(null)
          setStatus('unauthenticated')

          if (
            isApiError(error)
            && error.status === 401
          ) {
            setSessionError(null)
            return
          }

          setSessionError(
            'Impossible de vérifier la session actuelle.',
          )
        }
      }

    void restoreSession()

    return () => {
      active = false
    }
  }, [])

  const login =
    useCallback(
      async (
        credentials: LoginRequest,
      ): Promise<CurrentUser> => {
        const authenticatedUser =
          await loginRequest(credentials)

        setUser(authenticatedUser)
        setStatus('authenticated')
        setSessionError(null)

        return authenticatedUser
      },
      [],
    )

  const logout =
    useCallback(
      async (): Promise<void> => {
        try {
          await logoutRequest()
        } catch (error) {
          if (
            !isApiError(error)
            || error.status !== 401
          ) {
            throw error
          }
        }

        clearAuthentication()
      },
      [
        clearAuthentication,
      ],
    )

  const changePassword =
    useCallback(
      async (
        request: ChangePasswordRequest,
      ): Promise<void> => {
        try {
          await changePasswordRequest(
            request,
          )
        } catch (error) {
          if (
            isApiError(error)
            && error.status === 401
          ) {
            clearAuthentication()
          }

          throw error
        }

        clearAuthentication()
      },
      [
        clearAuthentication,
      ],
    )

  const value =
    useMemo<AuthContextValue>(
      () => ({
        user,
        status,

        isAuthenticated:
          status === 'authenticated'
          && user !== null,

        sessionError,

        login,
        logout,
        changePassword,
        refreshUser,
      }),
      [
        user,
        status,
        sessionError,
        login,
        logout,
        changePassword,
        refreshUser,
      ],
    )

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}