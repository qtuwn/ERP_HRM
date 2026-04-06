import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api } from '../lib/api.js'
import { clearSession, getRefreshToken, getUser } from '../lib/storage.js'

function formatDt(v) {
  if (!v) return '—'
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('vi-VN')
}

export function ProfileSessionsPage() {
  const user = getUser()
  const navigate = useNavigate()
  const [sessions, setSessions] = useState([])
  const [currentId, setCurrentId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/api/users/me/sessions')
      const list = res?.data || []
      setSessions(Array.isArray(list) ? list : [])

      const rt = getRefreshToken()
      if (rt) {
        try {
          const idRes = await api.post('/api/users/me/sessions/identify', { refreshToken: rt })
          setCurrentId(idRes?.data?.sessionId || null)
        } catch {
          setCurrentId(null)
        }
      } else {
        setCurrentId(null)
      }
    } catch (e) {
      setError(e?.message || 'Không tải được danh sách phiên')
      setSessions([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  if (!user) return <Navigate to="/login" replace state={{ from: '/profile/sessions' }} />

  async function revokeOne(id) {
    if (!confirm('Thu hồi phiên đăng nhập này?')) return
    try {
      await api.delete(`/api/users/me/sessions/${id}`)
      await load()
    } catch (e) {
      alert(e?.message || 'Thu hồi thất bại')
    }
  }

  async function revokeAll() {
    if (!confirm('Đăng xuất tất cả thiết bị? Bạn sẽ cần đăng nhập lại trên máy này.')) return
    try {
      await api.post('/api/users/me/sessions/revoke-all', {})
      clearSession()
      navigate('/login', { replace: true })
    } catch (e) {
      alert(e?.message || 'Thao tác thất bại')
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div className="min-w-0 flex-1">
          <h1 className="text-xl font-bold text-slate-900 dark:text-slate-100">Phiên đăng nhập</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Mỗi phiên tương ứng một refresh token đã cấp. Hệ thống chưa lưu chi tiết thiết bị; có thể bổ sung IP / User-Agent
            sau.
          </p>
        </div>
        <Link
          to="/profile"
          className="shrink-0 self-start whitespace-nowrap text-sm font-medium text-[#2563eb] hover:underline sm:pt-1"
        >
          ← Về hồ sơ
        </Link>
      </div>

      {error ? (
        <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200">
          {error}
        </div>
      ) : null}

      <div className="mb-4 flex justify-end">
        <button
          type="button"
          onClick={revokeAll}
          className="rounded-lg border border-rose-300 bg-white px-4 py-2 text-sm font-medium text-rose-700 transition hover:bg-rose-50 dark:border-rose-800 dark:bg-slate-900 dark:text-rose-300 dark:hover:bg-rose-950/40"
        >
          Đăng xuất tất cả thiết bị
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-500">Đang tải…</p>
      ) : (
        <ul className="divide-y divide-slate-200 overflow-hidden rounded-xl border border-slate-200 bg-white dark:divide-slate-800 dark:border-slate-800 dark:bg-slate-900">
          {sessions.map((s) => {
            const isCurrent = currentId && s.id === currentId
            return (
              <li key={s.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-800 dark:text-slate-100">
                    Phiên {isCurrent ? '(thiết bị này)' : ''}
                  </p>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Tạo: {formatDt(s.createdAt)} · Hết hạn: {formatDt(s.expiresAt)}
                    {s.revoked ? ' · Đã thu hồi' : ''}
                    {!s.stillValid && !s.revoked ? ' · Hết hạn' : ''}
                  </p>
                  {s.clientIp || s.userAgent ? (
                    <p className="mt-1 text-[11px] leading-snug text-slate-400 dark:text-slate-500 break-all">
                      {s.clientIp ? `IP: ${s.clientIp}` : null}
                      {s.clientIp && s.userAgent ? ' · ' : null}
                      {s.userAgent ? `UA: ${s.userAgent}` : null}
                    </p>
                  ) : null}
                </div>
                {s.stillValid && !isCurrent ? (
                  <button
                    type="button"
                    onClick={() => revokeOne(s.id)}
                    className="shrink-0 rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
                  >
                    Thu hồi
                  </button>
                ) : null}
              </li>
            )
          })}
          {sessions.length === 0 ? (
            <li className="px-4 py-6 text-center text-sm text-slate-500">Chưa có phiên nào được ghi nhận.</li>
          ) : null}
        </ul>
      )}
    </div>
  )
}
