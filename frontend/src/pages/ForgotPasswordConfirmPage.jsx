import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { KeyRound, Mail, ShieldCheck } from 'lucide-react'

export function ForgotPasswordConfirmPage() {
  const [sp] = useSearchParams()
  const nav = useNavigate()

  const [email, setEmail] = useState(sp.get('email') || '')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')
  const [msg, setMsg] = useState('')

  useEffect(() => {
    setEmail(sp.get('email') || email)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sp])

  async function submit(e) {
    e.preventDefault()
    setErr('')
    setMsg('')
    setLoading(true)
    try {
      await api.post('/api/auth/forgot-password/confirm', { email, otp, newPassword })
      setMsg('Đặt lại mật khẩu thành công. Đang chuyển về đăng nhập…')
      setTimeout(() => nav('/login'), 800)
    } catch (e2) {
      setErr(e2?.message || 'OTP không hợp lệ hoặc đã hết hạn.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="max-w-md mx-auto mt-16 p-8 bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Đặt lại mật khẩu</h1>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">Nhập OTP và mật khẩu mới.</p>

      <form onSubmit={submit} className="mt-6 space-y-4">
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
              required
              className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
              placeholder="6 chữ số"
            />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">Mật khẩu mới</label>
          <div className="relative">
            <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              type="password"
              required
              minLength={6}
              className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
              placeholder="Ít nhất 6 ký tự"
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
          {loading ? 'Đang xử lý…' : 'Đặt lại mật khẩu'}
        </button>
      </form>

      <div className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
        <Link to="/forgot-password" className="text-[#2563eb] font-medium hover:underline">
          Gửi lại OTP
        </Link>
        {' · '}
        <Link to="/login" className="text-[#2563eb] font-medium hover:underline">
          Đăng nhập
        </Link>
      </div>
    </section>
  )
}
