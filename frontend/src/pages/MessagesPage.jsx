import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { ApplicationChatPanel } from '../components/ApplicationChatPanel.jsx'
import { ArrowLeft, MessageCircle, Search } from 'lucide-react'

function statusShort(s) {
  const x = String(s || '')
  if (x === 'REJECTED') return 'Từ chối'
  if (x === 'HIRED') return 'Đã nhận'
  if (x === 'OFFER') return 'Offer'
  return 'Đang xử lý'
}

export function MessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const applicationIdFromUrl = searchParams.get('applicationId')

  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [selectedId, setSelectedId] = useState(null)
  const [listQuery, setListQuery] = useState('')
  const [focusChat, setFocusChat] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      try {
        const res = await api.get('/api/users/me/applications?size=100')
        const list = res?.data?.content || []
        if (!cancelled) setApplications(list)
      } catch {
        if (!cancelled) setApplications([])
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!applications.length) {
      setSelectedId(null)
      return
    }
    if (applicationIdFromUrl && applications.some((a) => String(a.id) === applicationIdFromUrl)) {
      setSelectedId(applicationIdFromUrl)
      return
    }
    setSelectedId((prev) => {
      if (prev != null && applications.some((a) => String(a.id) === String(prev))) return prev
      return applications[0]?.id ?? null
    })
  }, [applications, applicationIdFromUrl])

  useEffect(() => {
    if (loading || !selectedId) return
    if (applicationIdFromUrl !== String(selectedId)) {
      setSearchParams({ applicationId: selectedId }, { replace: true })
    }
  }, [loading, selectedId, applicationIdFromUrl, setSearchParams])

  function openThread(id) {
    if (!id) return
    setSelectedId(id)
    setSearchParams({ applicationId: id }, { replace: true })
    setFocusChat(true)
  }

  function backToList() {
    setFocusChat(false)
  }

  const filtered = useMemo(() => {
    const q = listQuery.trim().toLowerCase()
    if (!q) return applications
    return applications.filter((a) => {
      const title = (a.jobTitle || '').toLowerCase()
      return title.includes(q)
    })
  }, [applications, listQuery])

  const selected = applications.find((a) => String(a.id) === String(selectedId))

  return (
    <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-[1600px] flex-col gap-0 px-4 sm:px-6 lg:flex-row lg:px-8">
      {/* Cột 1: danh sách hội thoại */}
      <aside
        className={[
          'flex w-full shrink-0 flex-col border-b border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900',
          'lg:w-80 lg:border-b-0 lg:border-r',
          focusChat ? 'hidden lg:flex' : 'flex',
        ].join(' ')}
      >
        <div className="border-b border-slate-200 px-4 py-4 dark:border-slate-800">
          <h1 className="text-lg font-bold text-slate-900 dark:text-white">Tin nhắn</h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">Chọn đơn ứng tuyển để chat với nhà tuyển dụng</p>
          {!loading && applications.length > 0 ? (
            <div className="relative mt-3">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                value={listQuery}
                onChange={(e) => setListQuery(e.target.value)}
                placeholder="Tìm theo tên vị trí…"
                className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm text-slate-900 outline-none focus:border-[#2563eb] focus:ring-1 focus:ring-[#2563eb] dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
              />
            </div>
          ) : null}
        </div>
        {loading ? (
          <div className="p-4 text-sm text-slate-500">Đang tải…</div>
        ) : applications.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 p-6 text-center text-sm text-slate-500">
            <MessageCircle className="h-10 w-10 text-slate-300" />
            <p>Chưa có đơn ứng tuyển nào.</p>
            <Link to="/jobs" className="font-medium text-[#2563eb] hover:underline">
              Tìm việc ngay
            </Link>
          </div>
        ) : (
          <ul className="max-h-[50vh] overflow-y-auto lg:max-h-none lg:flex-1">
            {filtered.length === 0 ? (
              <li className="px-4 py-6 text-center text-sm text-slate-500">Không khớp từ khóa.</li>
            ) : (
              filtered.map((app) => {
                const active = String(app.id) === String(selectedId)
                return (
                  <li key={app.id}>
                    <button
                      type="button"
                      onClick={() => openThread(app.id)}
                      className={[
                        'flex w-full flex-col gap-0.5 border-b border-slate-100 px-4 py-3 text-left text-sm transition dark:border-slate-800',
                        active ? 'bg-blue-50 dark:bg-blue-950/40' : 'hover:bg-slate-50 dark:hover:bg-slate-800/80',
                      ].join(' ')}
                    >
                      <span className="font-medium text-slate-900 line-clamp-2 dark:text-white">
                        {app.jobTitle || 'Vị trí ứng tuyển'}
                      </span>
                      <span className="text-xs text-slate-500 dark:text-slate-400">{statusShort(app.status)}</span>
                    </button>
                  </li>
                )
              })
            )}
          </ul>
        )}
      </aside>

      {/* Cột 2: chat */}
      <section
        className={[
          'flex min-h-[420px] min-w-0 flex-1 flex-col bg-slate-50 dark:bg-slate-950/50',
          focusChat ? 'flex' : 'hidden lg:flex',
        ].join(' ')}
      >
        <div className="flex items-center gap-2 border-b border-slate-200 bg-white px-3 py-2 dark:border-slate-800 dark:bg-slate-900 lg:hidden">
          <button
            type="button"
            onClick={backToList}
            className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            <ArrowLeft className="h-4 w-4" />
            Danh sách
          </button>
        </div>
        <ApplicationChatPanel
          applicationId={selectedId}
          applicationTitle={selected ? `${selected.jobTitle || 'Ứng tuyển'}` : ''}
          className="min-h-0 min-h-[360px] flex-1"
        />
      </section>

      {/* Cột 3: đơn đã ứng tuyển — CTA mở chat */}
      <aside className="hidden w-72 shrink-0 flex-col border-l border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900 xl:flex">
        <div className="border-b border-slate-200 px-4 py-4 dark:border-slate-800">
          <h2 className="text-sm font-bold text-slate-900 dark:text-white">Đơn đã ứng tuyển</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Mở nhanh cuộc trò chuyện theo từng vị trí</p>
        </div>
        {loading ? (
          <div className="p-4 text-sm text-slate-500">Đang tải…</div>
        ) : applications.length === 0 ? (
          <div className="p-4 text-sm text-slate-500">Chưa có đơn.</div>
        ) : (
          <ul className="flex-1 overflow-y-auto">
            {applications.map((app) => {
              const active = String(app.id) === String(selectedId)
              return (
                <li key={`rail-${app.id}`} className="border-b border-slate-100 dark:border-slate-800">
                  <div className="flex flex-col gap-2 p-3">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-slate-900 line-clamp-2 dark:text-white">{app.jobTitle}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">{statusShort(app.status)}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => openThread(app.id)}
                      className={[
                        'rounded-lg px-3 py-1.5 text-center text-xs font-semibold transition',
                        active
                          ? 'bg-[#2563eb] text-white'
                          : 'border border-slate-200 text-[#2563eb] hover:bg-[#2563eb]/10 dark:border-slate-600',
                      ].join(' ')}
                    >
                      {active ? 'Đang mở' : 'Nhắn tin'}
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </aside>
    </div>
  )
}
