import { useEffect, useState } from 'react'

type BackendStatus = 'checking' | 'up' | 'down'

function App() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')

  useEffect(() => {
    fetch('/actuator/health')
      .then((response) => {
        if (!response.ok) {
          throw new Error('Backend health check failed')
        }
        return response.json()
      })
      .then((data: { status?: string }) => {
        setBackendStatus(data.status === 'UP' ? 'up' : 'down')
      })
      .catch(() => setBackendStatus('down'))
  }, [])

  const statusLabel =
    backendStatus === 'checking'
      ? 'Vérification du backend...'
      : backendStatus === 'up'
        ? 'Backend opérationnel'
        : 'Backend indisponible'

  return (
    <main className="min-h-screen bg-slate-950 text-white flex items-center justify-center px-6">
      <section className="max-w-3xl text-center">
        <p className="mb-3 text-sm font-semibold uppercase tracking-[0.3em] text-emerald-400">
          AutoRent Pro
        </p>

        <h1 className="text-4xl font-bold tracking-tight sm:text-6xl">
          Plateforme moderne de location et de gestion de véhicules
        </h1>

        <p className="mt-6 text-lg text-slate-300">
          Fondation frontend opérationnelle.
        </p>

        <div className="mt-8 inline-flex items-center gap-3 rounded-full border border-slate-700 px-5 py-3 text-sm">
          <span
            className={`h-2.5 w-2.5 rounded-full ${
              backendStatus === 'up'
                ? 'bg-emerald-400'
                : backendStatus === 'down'
                  ? 'bg-red-400'
                  : 'bg-amber-400'
            }`}
          />
          {statusLabel}
        </div>
      </section>
    </main>
  )
}

export default App
