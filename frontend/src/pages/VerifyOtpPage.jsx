import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { Mail, ShieldCheck, RefreshCw } from 'lucide-react'

export function VerifyOtpPage() {
  const [sp] = useSearchParams()
  const nav = useNavigate()

  const [email, setEmail] = useState(sp.get('email') || '')
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [resending, setResending] = useState(false)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  useEffect(() => {
    setEmail(sp.get('email') || email)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sp])

  async function verify(e) {
    e.preventDefault()
    setMsg('')
    setErr('')
    setLoading(true)
    try {
      await api.post('/api/auth/verify-otp', { email, otp })
      setMsg('Xác thực thành công. Bạn có thể đăng nhập.')
      setTimeout(() => nav('/login'), 700)
    } catch (e2) {
      setErr(e2?.message || 'OTP không hợp lệ hoặc đã hết hạn.')
    } finally {
      setLoading(false)
    }
  }

  async function resend() {
    setMsg('')
    setErr('')
    setResending(true)
    try {
      await api.post('/api/auth/resend-otp', { email })
      setMsg('Đã gửi lại OTP. Vui lòng kiểm tra email.')
    } catch (e2) {
      setErr(e2?.message || 'Không thể gửi lại OTP (có thể đang bị cooldown).')
    } finally {
      setResending(false)
    }
  }

  return (
    <section className="max-w-md mx-auto mt-16 p-8 bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Xác thực OTP</h1>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
        Nhập mã OTP đã được gửi về email để kích hoạt tài khoản.
      </p>

      <form onSubmit={verify} className="mt-6 space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">Email</label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              required
              className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
            />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">OTP</label>
          <div className="relative">
            <ShieldCheck className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="6 chữ số"
              required
              className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
            />
          </div>
        </div>

        {err ? (
          <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:border-rose-900/40 dark:bg-rose-950/20 dark:text-rose-300">
            {err}
          </div>
        ) : null}
        {msg ? (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-900/40 dark:bg-emerald-950/20 dark:text-emerald-300">
            {msg}
          </div>
        ) : null}

        <button
          type="submit"
          disabled={loading}
          className="w-full inline-flex items-center justify-center gap-2 rounded-lg bg-[#2563eb] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#1d4ed8] disabled:opacity-50"
        >
          {loading ? 'Đang xác thực…' : 'Xác thực'}
        </button>

        <button
          type="button"
          onClick={resend}
          disabled={resending || !email}
          className="w-full inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 dark:border-slate-700 px-4 py-2.5 text-sm font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-50"
        >
          <RefreshCw className={['h-4 w-4', resending ? 'animate-spin' : ''].join(' ')} />
          Gửi lại OTP
        </button>
      </form>

      <div className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
        <Link to="/login" className="text-[#2563eb] font-medium hover:underline">
          Quay lại đăng nhập
        </Link>
      </div>
    </section>
  )
}
