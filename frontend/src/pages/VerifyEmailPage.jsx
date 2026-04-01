import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react'

export function VerifyEmailPage() {
  const user = useMemo(() => getUser(), [])
  const [sp] = useSearchParams()
  const token = sp.get('token') || ''

  const [status, setStatus] = useState('loading') // loading | success | error
  const [message, setMessage] = useState('')

  useEffect(() => {
    let mounted = true
    ;(async () => {
      if (!token) {
        if (!mounted) return
        setStatus('error')
        setMessage('Thiếu token xác thực.')
        return
      }
      try {
        await api.get(`/api/auth/verify-email?token=${encodeURIComponent(token)}`)
        if (!mounted) return
        setStatus('success')
        setMessage('Xác thực email thành công. Bạn có thể đăng nhập ngay.')
      } catch (e) {
        if (!mounted) return
        setStatus('error')
        setMessage(e?.message || 'Xác thực thất bại hoặc link đã hết hạn.')
      }
    })()
    return () => {
      mounted = false
    }
  }, [token])

  return (
    <section className="max-w-md mx-auto mt-16 p-8 bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Xác thực email</h1>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">Đang xử lý xác thực tài khoản.</p>

      <div className="mt-6 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/20 p-4">
        {status === 'loading' ? (
          <div className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <Loader2 className="h-4 w-4 animate-spin" />
            Đang xác thực…
          </div>
        ) : status === 'success' ? (
          <div className="flex items-start gap-2 text-sm text-emerald-700 dark:text-emerald-300">
            <CheckCircle2 className="h-5 w-5 mt-0.5" />
            <div>
              <div className="font-semibold">Thành công</div>
              <div className="mt-1 text-slate-700 dark:text-slate-300">{message}</div>
            </div>
          </div>
        ) : (
          <div className="flex items-start gap-2 text-sm text-rose-700 dark:text-rose-300">
            <XCircle className="h-5 w-5 mt-0.5" />
            <div>
              <div className="font-semibold">Thất bại</div>
              <div className="mt-1 text-slate-700 dark:text-slate-300">{message}</div>
            </div>
          </div>
        )}
      </div>

      <div className="mt-6 flex flex-col gap-2">
        <Link
          to="/login"
          className="inline-flex items-center justify-center rounded-lg bg-[#2563eb] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#1d4ed8]"
        >
          Đăng nhập
        </Link>
        {user?.email ? (
          <Link
            to={`/verify-otp?email=${encodeURIComponent(user.email)}`}
            className="inline-flex items-center justify-center rounded-lg border border-slate-300 dark:border-slate-700 px-4 py-2.5 text-sm font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800"
          >
            Xác thực bằng OTP
          </Link>
        ) : null}
      </div>
    </section>
  )
}
