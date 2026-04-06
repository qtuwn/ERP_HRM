import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Bell, Check, CheckCheck, ExternalLink, RefreshCw } from 'lucide-react'
import { api } from '../lib/api.js'

function fmtTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
}

export function NotificationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  async function load() {
    setLoading(true)
    setErr('')
    try {
      const res = await api.get('/api/users/me/notifications?size=50')
      setItems(res?.data?.content || [])
    } catch (e) {
      setErr(e?.message || 'Không tải được thông báo')
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function markRead(id) {
    if (!id) return
    setItems((prev) => prev.map((x) => (String(x.id) === String(id) ? { ...x, readAt: x.readAt || new Date().toISOString() } : x)))
    try {
      await api.patch(`/api/users/me/notifications/${id}/read`, {})
    } catch {
      // ignore (UI optimistic)
    }
  }

  async function markAllRead() {
    setItems((prev) => prev.map((x) => ({ ...x, readAt: x.readAt || new Date().toISOString() })))
    try {
      await api.post('/api/users/me/notifications/mark-all-read', {})
    } catch {
      // ignore
    }
  }

  function openLink(n) {
    const link = n?.link
    if (!link) return
    markRead(n.id)
    if (link.startsWith('/')) {
      setSearchParams({ go: link }, { replace: true })
      window.history.replaceState({}, '', link)
      window.location.assign(link)
    } else {
      window.open(link, '_blank', 'noopener,noreferrer')
    }
  }

  const unread = items.filter((x) => !x.readAt).length

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#2563eb]/10 text-[#2563eb] dark:bg-[#2563eb]/20 dark:text-blue-200">
            <Bell className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Trung tâm thông báo</h1>
            {unread > 0 ? <p className="text-xs text-slate-500 dark:text-slate-400">{unread} chưa đọc</p> : null}
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={load}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            <RefreshCw className="h-4 w-4" />
            Làm mới
          </button>
          <button
            type="button"
            onClick={markAllRead}
            className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-3 py-2 text-sm font-semibold text-white hover:bg-[#1d4ed8]"
          >
            <CheckCheck className="h-4 w-4" />
            Đánh dấu đã đọc
          </button>
        </div>
      </div>

      {err ? (
        <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/30 dark:text-rose-200">
          {err}
        </div>
      ) : null}

      <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {loading ? (
          <div className="p-6 text-sm text-slate-500">Đang tải…</div>
        ) : items.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-500 dark:text-slate-400">
            Chưa có thông báo nào.
            <div className="mt-3">
              <Link to="/candidate/applications" className="text-[#2563eb] hover:underline">
                Xem hồ sơ ứng tuyển
              </Link>
            </div>
          </div>
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {items.map((n) => {
              const unreadRow = !n.readAt
              return (
                <li key={n.id} className={unreadRow ? 'bg-[#2563eb]/[0.03] dark:bg-[#2563eb]/10' : ''}>
                  <div className="flex items-start gap-3 p-4 sm:p-5">
                    <div
                      className={[
                        'mt-1 h-2.5 w-2.5 shrink-0 rounded-full',
                        unreadRow ? 'bg-[#2563eb]' : 'bg-slate-300 dark:bg-slate-700',
                      ].join(' ')}
                      title={unreadRow ? 'Chưa đọc' : 'Đã đọc'}
                    />

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-slate-900 dark:text-white">{n.title}</p>
                          {n.body ? (
                            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{n.body}</p>
                          ) : null}
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400">{fmtTime(n.createdAt)}</div>
                      </div>

                      <div className="mt-3 flex flex-wrap gap-2">
                        {n.link ? (
                          <button
                            type="button"
                            onClick={() => openLink(n)}
                            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
                          >
                            <ExternalLink className="h-3.5 w-3.5" />
                            Mở
                          </button>
                        ) : null}
                        {unreadRow ? (
                          <button
                            type="button"
                            onClick={() => markRead(n.id)}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-[#2563eb] px-3 py-1.5 text-xs font-semibold text-white hover:bg-[#1d4ed8]"
                          >
                            <Check className="h-3.5 w-3.5" />
                            Đã đọc
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}
