import {
  Navigate,
  Outlet,
  useLocation,
} from 'react-router'

import {
  hasPermission,
} from './authorization'
import type {
  PermissionCode,
  PermissionScope,
} from './auth.types'
import {
  useAuth,
} from './useAuth'

function AuthLoadingScreen() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 px-6 text-white">
      <div className="text-center">
        <div
          className="mx-auto h-9 w-9 animate-spin rounded-full border-2 border-slate-700 border-t-emerald-400"
          aria-hidden="true"
        />

        <p className="mt-5 text-sm text-slate-400">
          Vérification de votre session…
        </p>
      </div>
    </main>
  )
}

function getRequestedAppPath(
  state: unknown,
): string | null {
  const routeState =
    state as {
      from?: unknown
    } | null

  if (
    typeof routeState?.from === 'string'
    && routeState.from.startsWith('/app')
  ) {
    return routeState.from
  }

  return null
}

export function ProtectedRoute() {
  const {
    user,
    status,
  } = useAuth()

  const location =
    useLocation()

  if (status === 'loading') {
    return <AuthLoadingScreen />
  }

  if (
    status !== 'authenticated'
    || !user
  ) {
    const requestedPath =
      `${location.pathname}${location.search}${location.hash}`

    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: requestedPath,
        }}
      />
    )
  }

  if (user.mustChangePassword) {
    const requestedPath =
      `${location.pathname}${location.search}${location.hash}`

    return (
      <Navigate
        to="/change-password"
        replace
        state={{
          from: requestedPath,
        }}
      />
    )
  }

  return <Outlet />
}

export function GuestRoute() {
  const {
    user,
    status,
  } = useAuth()

  const location =
    useLocation()

  if (status === 'loading') {
    return <AuthLoadingScreen />
  }

  if (
    status === 'authenticated'
    && user
  ) {
    const requestedPath =
      getRequestedAppPath(
        location.state,
      )

    if (user.mustChangePassword) {
      return (
        <Navigate
          to="/change-password"
          replace
          state={{
            from:
              requestedPath
              ?? '/app',
          }}
        />
      )
    }

    return (
      <Navigate
        to={
          requestedPath
          ?? '/app'
        }
        replace
      />
    )
  }

  return <Outlet />
}

export function PasswordChangeRoute() {
  const {
    user,
    status,
  } = useAuth()

  const location =
    useLocation()

  if (status === 'loading') {
    return <AuthLoadingScreen />
  }

  if (
    status !== 'authenticated'
    || !user
  ) {
    return (
      <Navigate
        to="/login"
        replace
      />
    )
  }

  if (!user.mustChangePassword) {
    return (
      <Navigate
        to={
          getRequestedAppPath(
            location.state,
          )
          ?? '/app'
        }
        replace
      />
    )
  }

  return <Outlet />
}

interface PermissionRouteProps {
  permission: PermissionCode
  scope: PermissionScope
}

export function PermissionRoute({
  permission,
  scope,
}: PermissionRouteProps) {
  const {
    user,
    status,
  } = useAuth()

  if (status === 'loading') {
    return <AuthLoadingScreen />
  }

  if (
    status !== 'authenticated'
    || !user
  ) {
    return (
      <Navigate
        to="/login"
        replace
      />
    )
  }

  if (
    !hasPermission(
      user,
      permission,
      scope,
    )
  ) {
    return (
      <Navigate
        to="/forbidden"
        replace
      />
    )
  }

  return <Outlet />
}