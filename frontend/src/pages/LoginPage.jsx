import { useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { normalizeUserRole, setSession } from '../lib/storage.js'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { FEATURE_PASSWORD_RESET_EMAIL } from '../config/featureFlags.js'

function pickRedirectTarget(searchParams, locationState) {
  const next = searchParams.get('next')
  if (next && next.startsWith('/') && !next.startsWith('//')) return next
  if (locationState?.from && String(locationState.from).startsWith('/')) return locationState.from
  return '/dashboard'
}

/** Admin không dùng /dashboard (Tổng quan) — chỉ HR/Công ty. */
function postLoginPath(requestedPath, userRole) {
  const role = normalizeUserRole(userRole)
  if (role === 'ADMIN' && (requestedPath === '/dashboard' || requestedPath.startsWith('/dashboard/'))) {
    return '/admin/analytics'
  }
  return requestedPath
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const from = useMemo(() => pickRedirectTarget(searchParams, location.state), [searchParams, location.state])

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
      navigate(postLoginPath(from, data?.user?.role), { replace: true })
    } catch (err) {
      setError(err?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="max-w-md mx-auto mt-20 p-8 bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800">
      <div className="text-center mb-8">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Đăng nhập</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-2">Đăng nhập để quản lý và ứng tuyển việc làm</p>
      </div>

      <form className="space-y-6" onSubmit={onSubmit}>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Email</label>
          <input
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-700 dark:text-slate-100"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            autoComplete="email"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Mật khẩu</label>
          <input
            type="password"
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-700 dark:text-slate-100"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            autoComplete="current-password"
            required
          />
          {FEATURE_PASSWORD_RESET_EMAIL ? (
            <div className="mt-2 text-right">
              <Link to="/forgot-password" className="text-sm font-medium text-[#2563eb] hover:underline">
                Quên mật khẩu?
              </Link>
            </div>
          ) : null}
        </div>

        {error ? (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900/40 dark:bg-red-950/20 dark:text-red-300">
            {error}
          </div>
        ) : null}

        <button
          disabled={loading}
          className="w-full bg-[#2563eb] text-white font-medium py-2.5 rounded-lg hover:bg-[#1d4ed8] transition-colors shadow-sm disabled:opacity-50"
        >
          {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
      </form>

      <div className="mt-6 flex items-center justify-between text-sm text-slate-600 dark:text-slate-400">
        <span>
          Chưa có tài khoản?{' '}
          <Link to="/register" className="text-[#2563eb] font-medium hover:underline">
            Đăng ký ngay
          </Link>
        </span>
      </div>
    </section>
  )
}
