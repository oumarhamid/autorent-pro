
import {
  useAuth,
} from '../auth/useAuth'

export function DashboardPage() {
  const {
    user,
  } = useAuth()

  return (
    <section>
      <p className="text-sm font-medium text-emerald-400">
        Espace sécurisé
      </p>

      <h1 className="mt-2 text-3xl font-bold tracking-tight text-white">
        Tableau de bord
      </h1>

      <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-400">
        Votre session AutoRent Pro est active.
        Les modules métier seront intégrés progressivement
        dans cet espace protégé.
      </p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <article className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
          <p className="text-sm text-slate-500">
            Compte connecté
          </p>

          <p className="mt-2 font-semibold text-white">
            {user?.email}
          </p>
        </article>

        <article className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
          <p className="text-sm text-slate-500">
            Rôles actifs
          </p>

          <div className="mt-3 flex flex-wrap gap-2">
            {user?.roles.map((role) => (
              <span
                key={role}
                className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300"
              >
                {role}
              </span>
            ))}
          </div>
        </article>
      </div>
    </section>
  )
}