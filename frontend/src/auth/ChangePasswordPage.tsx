import {
  useState,
  type FormEvent,
} from 'react'
import {
  useLocation,
  useNavigate,
} from 'react-router'

import {
  isApiError,
} from '../api/apiError'
import {
  useAuth,
} from './useAuth'

function passwordErrorMessage(
  error: unknown,
): string {
  if (!isApiError(error)) {
    return 'La modification du mot de passe est momentanément impossible.'
  }

  switch (error.code) {
    case 'CURRENT_PASSWORD_INVALID':
      return 'Le mot de passe actuel est incorrect.'

    case 'PASSWORD_REQUIRED':
      return 'Le nouveau mot de passe est requis.'

    case 'PASSWORD_TOO_SHORT':
      return 'Le nouveau mot de passe doit contenir au moins 15 caractères.'

    case 'PASSWORD_TOO_LONG':
      return 'Le nouveau mot de passe ne doit pas dépasser 128 caractères.'

    case 'PASSWORD_TOO_COMMON':
      return 'Choisissez un mot de passe moins courant.'

    case 'PASSWORD_REUSE_NOT_ALLOWED':
      return 'Le nouveau mot de passe doit être différent du mot de passe actuel.'

    case 'ACCOUNT_UNAVAILABLE':
      return 'Votre compte n’est plus disponible.'

    case 'VALIDATION_FAILED':
      return 'Les informations fournies ne sont pas valides.'

    default:
      return 'La modification du mot de passe est momentanément impossible.'
  }
}

function requestedPathFromState(
  state: unknown,
): string {
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

  return '/app'
}

export function ChangePasswordPage() {
  const {
    user,
    changePassword,
    logout,
  } = useAuth()

  const navigate =
    useNavigate()

  const location =
    useLocation()

  const [currentPassword, setCurrentPassword] =
    useState('')

  const [newPassword, setNewPassword] =
    useState('')

  const [confirmation, setConfirmation] =
    useState('')

  const [isSubmitting, setIsSubmitting] =
    useState(false)

  const [isLoggingOut, setIsLoggingOut] =
    useState(false)

  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)

  const handleSubmit =
    async (
      event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
      event.preventDefault()

      if (isSubmitting || isLoggingOut) {
        return
      }

      if (
        currentPassword.length === 0
        || newPassword.length === 0
        || confirmation.length === 0
      ) {
        setErrorMessage(
          'Veuillez renseigner tous les champs.',
        )

        return
      }

      if (
        Array.from(newPassword).length < 15
      ) {
        setErrorMessage(
          'Le nouveau mot de passe doit contenir au moins 15 caractères.',
        )

        return
      }

      if (
        Array.from(newPassword).length > 128
      ) {
        setErrorMessage(
          'Le nouveau mot de passe ne doit pas dépasser 128 caractères.',
        )

        return
      }

      if (newPassword !== confirmation) {
        setErrorMessage(
          'La confirmation ne correspond pas au nouveau mot de passe.',
        )

        return
      }

      setIsSubmitting(true)
      setErrorMessage(null)

      try {
        await changePassword({
          currentPassword,
          newPassword,
        })

        navigate(
          '/login',
          {
            replace: true,
            state: {
              from:
                requestedPathFromState(
                  location.state,
                ),
            },
          },
        )
      } catch (error) {
        setErrorMessage(
          passwordErrorMessage(error),
        )

        setIsSubmitting(false)
      }
    }

  const handleLogout =
    async (): Promise<void> => {
      if (isSubmitting || isLoggingOut) {
        return
      }

      setIsLoggingOut(true)
      setErrorMessage(null)

      try {
        await logout()
      } catch {
        setErrorMessage(
          'La déconnexion est momentanément impossible.',
        )
        setIsLoggingOut(false)
      }
    }

  return (
    <main className="min-h-screen bg-slate-950 px-6 py-12 text-slate-100">
      <section className="mx-auto max-w-xl">
        <div className="flex items-start justify-between gap-6">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.28em] text-emerald-400">
              AutoRent Pro
            </p>

            <p className="mt-2 text-sm text-slate-500">
              Sécurisation du compte
            </p>
          </div>

          <button
            type="button"
            disabled={
              isSubmitting
              || isLoggingOut
            }
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

        <div className="mt-12 rounded-2xl border border-slate-800 bg-slate-900 p-8">
          <p className="text-sm font-semibold text-amber-400">
            Changement obligatoire
          </p>

          <h1 className="mt-3 text-3xl font-bold tracking-tight text-white">
            Choisissez un nouveau mot de passe
          </h1>

          <p className="mt-4 text-sm leading-6 text-slate-400">
            Le mot de passe actuel est temporaire.
            Vous devez le remplacer avant d’accéder
            aux fonctionnalités d’AutoRent Pro.
          </p>

          {user && (
            <p className="mt-3 text-sm text-slate-500">
              Compte :{' '}
              <span className="font-medium text-slate-300">
                {user.email}
              </span>
            </p>
          )}

          <div className="mt-6 rounded-xl border border-slate-700 bg-slate-950/50 px-4 py-3 text-sm leading-6 text-slate-400">
            Utilisez entre 15 et 128 caractères.
            Après la modification, votre session sera fermée
            et vous devrez vous reconnecter.
          </div>

          {errorMessage && (
            <div
              className="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm leading-6 text-red-200"
              role="alert"
            >
              {errorMessage}
            </div>
          )}

          <form
            className="mt-8 space-y-6"
            onSubmit={(event) => {
              void handleSubmit(event)
            }}
          >
            <div>
              <label
                htmlFor="current-password"
                className="block text-sm font-medium text-slate-200"
              >
                Mot de passe actuel
              </label>

              <input
                id="current-password"
                type="password"
                autoComplete="current-password"
                required
                disabled={
                  isSubmitting
                  || isLoggingOut
                }
                value={currentPassword}
                onChange={(event) => {
                  setCurrentPassword(
                    event.target.value,
                  )
                }}
                className="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-60"
              />
            </div>

            <div>
              <label
                htmlFor="new-password"
                className="block text-sm font-medium text-slate-200"
              >
                Nouveau mot de passe
              </label>

              <input
                id="new-password"
                type="password"
                autoComplete="new-password"
                required
                disabled={
                  isSubmitting
                  || isLoggingOut
                }
                value={newPassword}
                onChange={(event) => {
                  setNewPassword(
                    event.target.value,
                  )
                }}
                className="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-60"
              />
            </div>

            <div>
              <label
                htmlFor="confirm-password"
                className="block text-sm font-medium text-slate-200"
              >
                Confirmer le nouveau mot de passe
              </label>

              <input
                id="confirm-password"
                type="password"
                autoComplete="new-password"
                required
                disabled={
                  isSubmitting
                  || isLoggingOut
                }
                value={confirmation}
                onChange={(event) => {
                  setConfirmation(
                    event.target.value,
                  )
                }}
                className="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-60"
              />
            </div>

            <button
              type="submit"
              disabled={
                isSubmitting
                || isLoggingOut
              }
              className="flex w-full items-center justify-center rounded-xl bg-emerald-500 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting
                ? 'Modification en cours…'
                : 'Modifier le mot de passe'}
            </button>
          </form>
        </div>
      </section>
    </main>
  )
}