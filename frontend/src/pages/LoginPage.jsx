import { useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { setSession } from '../lib/storage.js'
import { useLocation, useNavigate } from 'react-router-dom'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = useMemo(() => location.state?.from || '/dashboard', [location.state])

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await api.post('/api/auth/login', { email, password })
      const data = res?.data
      setSession({
        accessToken: data?.accessToken,
        refreshToken: data?.refreshToken,
        user: data?.user,
      })
      navigate(from, { replace: true })
    } catch (err) {
      setError(err?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto grid max-w-4xl grid-cols-1 gap-6 md:grid-cols-2">
      <div className="rounded-2xl border bg-white p-6">
        <h1 className="text-xl font-semibold tracking-tight">Đăng nhập</h1>
        <p className="mt-1 text-sm text-slate-600">
          Dùng API hiện tại của Spring Boot: <code className="rounded bg-slate-100 px-1">/api/auth/login</code>
        </p>

        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <div>
            <label className="text-sm font-medium text-slate-700">Email</label>
            <input
              className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
              required
            />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Password</label>
            <input
              type="password"
              className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              required
            />
          </div>

          {error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <button
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Đang đăng nhập…' : 'Login'}
          </button>
        </form>
      </div>

      <div className="rounded-2xl border bg-gradient-to-br from-blue-600 to-blue-800 p-6 text-white">
        <div className="text-sm/6 opacity-90">Demo scope cho ngày mai</div>
        <div className="mt-2 text-2xl font-semibold tracking-tight">React + Vite + Tailwind</div>
        <ul className="mt-4 space-y-2 text-sm text-white/90">
          <li>- A: Jobs list (public) từ <code className="rounded bg-white/10 px-1">GET /api/jobs</code></li>
          <li>- B: Dashboard stats từ <code className="rounded bg-white/10 px-1">GET /api/dashboard/stats</code></li>
          <li>- JWT lưu localStorage, proxy <code className="rounded bg-white/10 px-1">/api</code> → 8080</li>
        </ul>
      </div>
    </div>
  )
}

