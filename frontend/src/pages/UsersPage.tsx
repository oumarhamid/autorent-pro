
export function UsersPage() {
  return (
    <section>
      <p className="text-sm font-medium text-emerald-400">
        Administration
      </p>

      <h1 className="mt-2 text-3xl font-bold tracking-tight text-white">
        Utilisateurs
      </h1>

      <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-400">
        Cette route est accessible uniquement aux comptes
        disposant de la permission USER_READ avec la portée GLOBAL.
      </p>

      <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-6">
        <p className="text-sm font-medium text-white">
          Route d’administration protégée
        </p>

        <p className="mt-2 text-sm leading-6 text-slate-400">
          L’interface complète de gestion des utilisateurs
          sera branchée sur les endpoints d’administration
          existants dans une étape dédiée.
        </p>
      </div>
    </section>
  )
}