import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { clearSession, getUser } from '../lib/storage.js'
import { useMemo } from 'react'

function cn({ isActive }) {
  return [
    'px-3 py-2 rounded-lg text-sm font-medium transition',
    isActive ? 'bg-blue-50 text-blue-700' : 'text-slate-700 hover:bg-slate-100',
  ].join(' ')
}

export function AppShell() {
  const navigate = useNavigate()
  const user = useMemo(() => getUser(), [])

  return (
    <div className="min-h-full">
      <header className="sticky top-0 z-10 border-b bg-white/80 backdrop-blur">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex h-14 items-center justify-between">
            <Link to="/jobs" className="flex items-center gap-2">
              <div className="h-8 w-8 rounded-xl bg-gradient-to-br from-blue-600 to-blue-800" />
              <div className="font-semibold tracking-tight">VTHR</div>
            </Link>

            <nav className="flex items-center gap-1">
              <NavLink to="/jobs" className={cn}>
                Jobs
              </NavLink>
              <NavLink to="/dashboard" className={cn}>
                Dashboard
              </NavLink>
            </nav>

            <div className="flex items-center gap-2">
              {user ? (
                <>
                  <div className="hidden text-sm text-slate-600 md:block">
                    {user?.fullName || user?.email} ({user?.role})
                  </div>
                  <button
                    className="rounded-lg border px-3 py-2 text-sm font-medium hover:bg-slate-50"
                    onClick={() => {
                      clearSession()
                      navigate('/login')
                    }}
                  >
                    Logout
                  </button>
                </>
              ) : (
                <Link
                  className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
                  to="/login"
                >
                  Login
                </Link>
              )}
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

