import {
  useState,
  type FormEvent,
} from 'react'

import {
  isApiError,
} from '../api/apiError'
import {
  useAuth,
} from './useAuth'

export function LoginPage() {
  const {
    login,
    sessionError,
  } = useAuth()

  const [email, setEmail] =
    useState('')

  const [password, setPassword] =
    useState('')

  const [isSubmitting, setIsSubmitting] =
    useState(false)

  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)

  const handleSubmit =
    async (
      event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
      event.preventDefault()

      if (isSubmitting) {
        return
      }

      const normalizedEmail =
        email.trim()

      if (
        normalizedEmail.length === 0
        || password.length === 0
      ) {
        setErrorMessage(
          'Veuillez renseigner votre adresse e-mail et votre mot de passe.',
        )

        return
      }

      setIsSubmitting(true)
      setErrorMessage(null)

      try {
        await login({
          email: normalizedEmail,
          password,
        })
      } catch (error) {
        if (
          isApiError(error)
          && error.status === 401
        ) {
          setErrorMessage(
            'Adresse e-mail ou mot de passe incorrect.',
          )
        } else {
          setErrorMessage(
            'La connexion est momentanément impossible. Veuillez réessayer.',
          )
        }
      } finally {
        setIsSubmitting(false)
      }
    }

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto grid min-h-screen max-w-7xl lg:grid-cols-2">
        <section className="hidden border-r border-slate-800 lg:flex lg:flex-col lg:justify-between lg:px-16 lg:py-14">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.32em] text-emerald-400">
              AutoRent Pro
            </p>

            <div className="mt-24 max-w-xl">
              <p className="text-sm font-medium text-emerald-400">
                Gestion automobile professionnelle
              </p>

              <h1 className="mt-5 text-5xl font-bold leading-tight tracking-tight text-white">
                Gérez vos locations et votre flotte depuis un espace sécurisé.
              </h1>

              <p className="mt-7 max-w-lg text-lg leading-8 text-slate-400">
                Une plateforme conçue pour centraliser les agences,
                les véhicules, les réservations, les contrats et les
                opérations quotidiennes.
              </p>
            </div>
          </div>

          <p className="text-sm text-slate-500">
            Accès réservé aux utilisateurs autorisés.
          </p>
        </section>

        <section className="flex items-center justify-center px-6 py-12 sm:px-10 lg:px-16">
          <div className="w-full max-w-md">
            <div className="mb-10 lg:hidden">
              <p className="text-sm font-bold uppercase tracking-[0.3em] text-emerald-400">
                AutoRent Pro
              </p>
            </div>

            <div>
              <p className="text-sm font-semibold text-emerald-400">
                Espace sécurisé
              </p>

              <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">
                Connexion
              </h2>

              <p className="mt-3 text-sm leading-6 text-slate-400">
                Utilisez votre compte AutoRent Pro pour accéder à votre espace.
              </p>
            </div>

            {sessionError && (
              <div
                className="mt-7 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm leading-6 text-amber-200"
                role="status"
              >
                {sessionError}
              </div>
            )}

            {errorMessage && (
              <div
                className="mt-7 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm leading-6 text-red-200"
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
                  htmlFor="email"
                  className="block text-sm font-medium text-slate-200"
                >
                  Adresse e-mail
                </label>

                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="username"
                  required
                  value={email}
                  disabled={isSubmitting}
                  onChange={(event) => {
                    setEmail(event.target.value)
                  }}
                  className="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="nom@entreprise.com"
                />
              </div>

              <div>
                <label
                  htmlFor="password"
                  className="block text-sm font-medium text-slate-200"
                >
                  Mot de passe
                </label>

                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  disabled={isSubmitting}
                  onChange={(event) => {
                    setPassword(event.target.value)
                  }}
                  className="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="Votre mot de passe"
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="flex w-full items-center justify-center rounded-xl bg-emerald-500 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400 focus:outline-none focus:ring-2 focus:ring-emerald-400 focus:ring-offset-2 focus:ring-offset-slate-950 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting
                  ? 'Connexion en cours…'
                  : 'Se connecter'}
              </button>
            </form>

            <div className="mt-8 border-t border-slate-800 pt-6">
              <p className="text-xs leading-5 text-slate-500">
                Votre session est gérée de manière sécurisée par le serveur.
                Aucun mot de passe n’est conservé dans le navigateur.
              </p>
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}