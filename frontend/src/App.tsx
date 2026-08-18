import {
  LoginPage,
} from './auth/LoginPage'
import {
  useAuth,
} from './auth/useAuth'

function App() {
  const {
    user,
    status,
  } = useAuth()

  if (status === 'loading') {
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

  if (!user) {
    return <LoginPage />
  }

  return (
    <main className="min-h-screen bg-slate-950 px-6 py-16 text-white">
      <section className="mx-auto max-w-4xl">
        <p className="text-sm font-bold uppercase tracking-[0.3em] text-emerald-400">
          AutoRent Pro
        </p>

        <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-2xl shadow-black/20">
          <p className="text-sm font-medium text-emerald-400">
            Session authentifiée
          </p>

          <h1 className="mt-3 text-3xl font-bold tracking-tight">
            Bienvenue dans AutoRent Pro
          </h1>

          <p className="mt-4 text-slate-400">
            Vous êtes connecté avec le compte :
          </p>

          <p className="mt-2 font-semibold text-white">
            {user.email}
          </p>

          <div className="mt-8">
            <p className="text-sm font-medium text-slate-300">
              Rôles actifs
            </p>

            <div className="mt-3 flex flex-wrap gap-2">
              {user.roles.length > 0 ? (
                user.roles.map((role) => (
                  <span
                    key={role}
                    className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300"
                  >
                    {role}
                  </span>
                ))
              ) : (
                <span className="text-sm text-slate-500">
                  Aucun rôle attribué.
                </span>
              )}
            </div>
          </div>

          <p className="mt-8 text-sm leading-6 text-slate-500">
            Cet écran est temporaire. Les routes et espaces métier
            protégés seront introduits à l’étape suivante.
          </p>
        </div>
      </section>
    </main>
  )
}

export default App