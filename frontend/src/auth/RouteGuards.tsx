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
    const state =
      location.state as {
        from?: unknown
      } | null

    const requestedPath =
      typeof state?.from === 'string'
      && state.from.startsWith('/app')
        ? state.from
        : '/app'

    return (
      <Navigate
        to={requestedPath}
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