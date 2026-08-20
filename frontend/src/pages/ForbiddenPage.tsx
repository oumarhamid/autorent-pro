
import {
  Link,
} from 'react-router'

export function ForbiddenPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 px-6 text-slate-100">
      <section className="w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900 p-8 text-center">
        <p className="text-sm font-bold uppercase tracking-[0.28em] text-amber-400">
          Accès refusé
        </p>

        <h1 className="mt-4 text-3xl font-bold text-white">
          Autorisation insuffisante
        </h1>

        <p className="mt-4 text-sm leading-6 text-slate-400">
          Votre compte est authentifié, mais il ne dispose
          pas des droits requis pour accéder à cette page.
        </p>

        <Link
          to="/app"
          className="mt-7 inline-flex rounded-xl bg-emerald-500 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400"
        >
          Retour au tableau de bord
        </Link>
      </section>
    </main>
  )
}