import {
  useState,
} from 'react'
import {
  NavLink,
  Outlet,
} from 'react-router'

import {
  hasPermission,
} from '../auth/authorization'
import {
  useAuth,
} from '../auth/useAuth'

export function AppShell() {
  const {
    user,
    logout,
  } = useAuth()

  const [isLoggingOut, setIsLoggingOut] =
    useState(false)

  const [logoutError, setLogoutError] =
    useState<string | null>(null)

  if (!user) {
    return null
  }

  const canReadUsers =
    hasPermission(
      user,
      'USER_READ',
      'GLOBAL',
    )

  const navigationClassName =
    ({
      isActive,
    }: {
      isActive: boolean
    }) =>
      [
        'block rounded-lg px-3 py-2 text-sm font-medium transition',
        isActive
          ? 'bg-emerald-500/10 text-emerald-300'
          : 'text-slate-400 hover:bg-slate-800 hover:text-white',
      ].join(' ')

  const handleLogout =
    async (): Promise<void> => {
      if (isLoggingOut) {
        return
      }

      setIsLoggingOut(true)
      setLogoutError(null)

      try {
        await logout()
      } catch {
        setLogoutError(
          'La déconnexion a échoué. Veuillez réessayer.',
        )
        setIsLoggingOut(false)
      }
    }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-950/95">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-6 px-6 py-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.28em] text-emerald-400">
              AutoRent Pro
            </p>

            <p className="mt-1 text-xs text-slate-500">
              Plateforme de gestion automobile
            </p>
          </div>

          <div className="flex items-center gap-5">
            <div className="text-right">
              <p className="text-sm font-medium text-white">
                {user.email}
              </p>

              <p className="mt-1 text-xs text-slate-500">
                {user.roles.join(', ')}
              </p>
            </div>

            <button
              type="button"
              disabled={isLoggingOut}
              onClick={() => {
                void handleLogout()
              }}
              className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-medium text-slate-300 transition hover:border-slate-600 hover:bg-slate-900 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoggingOut
                ? 'Déconnexion…'
                : 'Se déconnecter'}
            </button>
          </div>
        </div>

        {logoutError && (
          <div className="mx-auto max-w-7xl px-6 pb-4">
            <p
              className="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-2 text-sm text-red-200"
              role="alert"
            >
              {logoutError}
            </p>
          </div>
        )}
      </header>

      <div className="mx-auto grid max-w-7xl md:grid-cols-[220px_1fr]">
        <aside className="border-b border-slate-800 px-4 py-5 md:min-h-[calc(100vh-81px)] md:border-b-0 md:border-r">
          <nav
            className="space-y-1"
            aria-label="Navigation principale"
          >
            <NavLink
              to="/app"
              end
              className={navigationClassName}
            >
              Tableau de bord
            </NavLink>

            {canReadUsers && (
              <NavLink
                to="/app/users"
                className={navigationClassName}
              >
                Utilisateurs
              </NavLink>
            )}
          </nav>
        </aside>

        <div className="min-w-0 px-6 py-8 md:px-10">
          <Outlet />
        </div>
      </div>
    </div>
  )
}