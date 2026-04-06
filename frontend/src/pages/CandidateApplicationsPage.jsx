import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { Link, useSearchParams } from 'react-router-dom'
import { X, MessageCircle, FileDown, UserX, ClipboardList } from 'lucide-react'

const STEPS = [
  { key: 'APPLIED', label: 'Đã nộp' },
  { key: 'HR_REVIEW', label: 'HR duyệt' },
  { key: 'INTERVIEW', label: 'Phỏng vấn' },
  { key: 'OFFER', label: 'Offer' },
  { key: 'HIRED', label: 'Đã nhận' },
]

function statusBadgeClass(status) {
  const s = String(status || '')
  if (s === 'APPLIED' || s === 'AI_SCREENING' || s === 'AI_PROCESSING' || s === 'AI_QUEUED')
    return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-200'
  if (s === 'HR_REVIEW' || s === 'INTERVIEW') return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-200'
  if (s === 'OFFER' || s === 'HIRED')
    return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-200'
  if (s === 'REJECTED') return 'bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-200'
  if (s === 'WITHDRAWN') return 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200'
  return 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200'
}

function statusText(status) {
  const s = String(status || '')
  const k = normalizeForStepper(s)
  if (s === 'WITHDRAWN') return 'Đã rút đơn'
  if (s === 'REJECTED') return 'Từ chối'
  if (k === 'APPLIED') return s.startsWith('AI_') ? 'Đã nộp (AI đang xử lý)' : 'Đã nộp'
  if (k === 'HR_REVIEW') return 'HR duyệt'
  if (k === 'INTERVIEW') return 'Phỏng vấn'
  if (k === 'OFFER') return 'Offer'
  if (k === 'HIRED') return 'Đã nhận'
  return s.replaceAll('_', ' ')
}

function normalizeForStepper(status) {
  const s = String(status || '')
  if (!s) return 'APPLIED'
  if (s.startsWith('AI_') || s === 'AI_SCREENING' || s === 'AI_PROCESSING' || s === 'AI_QUEUED') return 'APPLIED'
  return s
}

function stepIndex(status) {
  const s = normalizeForStepper(status)
  const idx = STEPS.findIndex((x) => x.key === s)
  if (idx >= 0) return idx
  if (s === 'REJECTED') return 0
  if (s === 'WITHDRAWN') return 0
  return 0
}

export function CandidateApplicationsPage() {
  const user = useMemo(() => getUser(), [])
  const [searchParams, setSearchParams] = useSearchParams()
  const openFromQueryOnceRef = useRef(false)

  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)

  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState(null)
  const [stageHistory, setStageHistory] = useState([])
  const [withdrawing, setWithdrawing] = useState(false)

  async function fetchList() {
    setLoading(true)
    try {
      const res = await api.get('/api/users/me/applications?size=50')
      setApplications(res?.data?.content || [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [])

  useEffect(() => {
    if (openFromQueryOnceRef.current) return
    const appId = searchParams.get('applicationId')
    if (!appId) return
    if (!applications || applications.length === 0) return
    if (!applications.some((a) => String(a.id) === String(appId))) return
    openFromQueryOnceRef.current = true
    openDetail(appId)
    const next = new URLSearchParams(searchParams)
    next.delete('applicationId')
    setSearchParams(next, { replace: true })
  }, [applications, searchParams, setSearchParams])

  async function openDetail(appId) {
    setDetailOpen(true)
    setDetailLoading(true)
    setDetail(null)
    setStageHistory([])
    try {
      const [res, histRes] = await Promise.all([
        api.get(`/api/users/me/applications/${appId}`),
        api.get(`/api/users/me/applications/${appId}/stage-history`),
      ])
      setDetail(res?.data || null)
      setStageHistory(Array.isArray(histRes?.data) ? histRes.data : [])
    } finally {
      setDetailLoading(false)
    }
  }

  function openChat(app) {
    if (String(app?.status || '') === 'WITHDRAWN') return
    window.dispatchEvent(
      new CustomEvent('open-chat', {
        detail: {
          applicationId: app.id,
          applicationTitle: `Chat w/ HR: ${app.jobTitle || ''}`.trim(),
        },
      })
    )
  }

  async function withdrawApplication(appId) {
    if (!appId) return
    if (
      !confirm(
        'Bạn có chắc muốn rút đơn ứng tuyển này? Sau khi rút, bạn có thể ứng tuyển lại cùng tin nếu tin vẫn còn mở và trong hạn.'
      )
    ) {
      return
    }
    setWithdrawing(true)
    try {
      await api.post(`/api/users/me/applications/${appId}/withdraw`, {})
      setDetailOpen(false)
      setDetail(null)
      await fetchList()
    } catch (e) {
      alert(e?.message || 'Không thể rút đơn.')
      await fetchList()
      if (detailOpen) {
        try {
          await openDetail(appId)
        } catch {
          /* bỏ qua — tránh kẹt UI khi session lỗi */
        }
      }
    } finally {
      setWithdrawing(false)
    }
  }

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex items-center justify-between gap-4 flex-wrap">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Đơn ứng tuyển của tôi</h1>
        <Link to="/jobs" className="text-sm font-medium text-[#2563eb] hover:underline">
          Tìm việc
        </Link>
      </div>

      <div className="bg-white dark:bg-slate-900 shadow-sm border border-slate-200 dark:border-slate-800 overflow-hidden rounded-xl">
        {loading ? (
          <div className="px-6 py-6 text-sm text-slate-500 dark:text-slate-400">Đang tải...</div>
        ) : (
          <ul role="list" className="divide-y divide-slate-200 dark:divide-slate-800">
            {applications.map((app) => (
              <li key={app.id}>
                <div className="px-4 py-4 flex items-center sm:px-6 justify-between hover:bg-slate-50 dark:hover:bg-slate-800/40 transition border-l-4 border-transparent hover:border-[#2563eb]">
                  <div className="flex flex-col">
                    <div className="flex items-center gap-3">
                      <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">{app.jobTitle}</p>
                      <span
                        className={[
                          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                          statusBadgeClass(app.status),
                        ].join(' ')}
                      >
                        {statusText(app.status)}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                      Applied: {app.createdAt ? new Date(app.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </p>
                  </div>
                  <div className="ml-4 flex-shrink-0 flex gap-2">
                    <button
                      type="button"
                      onClick={() => openDetail(app.id)}
                      className="inline-flex items-center px-3 py-2 border border-slate-300 dark:border-slate-700 text-sm font-medium rounded-md shadow-sm bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 transition"
                    >
                      Chi tiết
                    </button>
                    <button
                      type="button"
                      onClick={() => openChat(app)}
                      disabled={String(app.status || '') === 'WITHDRAWN'}
                      className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-[#2563eb] bg-blue-50 hover:bg-blue-100 transition disabled:opacity-60 disabled:cursor-not-allowed dark:bg-blue-950/30 dark:hover:bg-blue-950/40"
                    >
                      <MessageCircle className="h-4 w-4 mr-1.5" />
                      Mở chat
                    </button>
                  </div>
                </div>
              </li>
            ))}
            {applications.length === 0 ? (
              <li>
                <div className="px-6 py-10 text-center text-slate-500 dark:text-slate-400">
                  Chưa có đơn ứng tuyển nào.
                </div>
              </li>
            ) : null}
          </ul>
        )}
      </div>

      {detailOpen ? (
        <div className="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-black/40 backdrop-blur-[1px]" onClick={() => setDetailOpen(false)} />
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">
              &#8203;
            </span>
            <div className="inline-block align-bottom bg-white dark:bg-slate-900 rounded-2xl px-4 pt-5 pb-4 text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full sm:p-6 w-full border border-slate-200 dark:border-slate-800 relative">
              <button
                onClick={() => setDetailOpen(false)}
                className="absolute right-4 top-4 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                aria-label="Close"
              >
                <X className="h-5 w-5" />
              </button>

              {detailLoading ? (
                <div className="p-8 text-center text-slate-500 dark:text-slate-400 animate-pulse">
                  Loading Application Details...
                </div>
              ) : detail ? (
                <div>
                  <div className="flex justify-between items-start mb-4 border-b border-slate-200 dark:border-slate-800 pb-4 pr-8">
                    <div>
                      <h3 className="text-lg leading-6 font-semibold text-slate-900 dark:text-slate-100">
                        {detail?.jobTitle}
                      </h3>
                      <p className="text-sm text-slate-500 dark:text-slate-400">
                        Applied on:{' '}
                        {detail?.application?.createdAt
                          ? new Date(detail.application.createdAt).toLocaleDateString('vi-VN')
                          : '—'}
                      </p>
                    </div>
                  </div>

                  <div className="space-y-6">
                    <div>
                      <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-100 mb-2">Tiến trình</h4>
                      <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/30 p-4">
                        {String(detail?.application?.status || '') === 'WITHDRAWN' ? (
                          <div className="text-sm font-semibold text-slate-600 dark:text-slate-300">
                            Bạn đã rút đơn ứng tuyển này.
                          </div>
                        ) : normalizeForStepper(detail?.application?.status) === 'REJECTED' ? (
                          <div className="text-sm font-semibold text-rose-700 dark:text-rose-300">
                            Hồ sơ đã bị từ chối
                          </div>
                        ) : (
                          <div className="flex flex-wrap items-center gap-2">
                            {STEPS.map((s, idx) => {
                              const active = idx <= stepIndex(detail?.application?.status)
                              return (
                                <div key={s.key} className="flex items-center gap-2">
                                  <div
                                    className={[
                                      'h-8 px-3 rounded-full text-xs font-semibold flex items-center',
                                      active
                                        ? 'bg-[#2563eb] text-white'
                                        : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-slate-500 dark:text-slate-300',
                                    ].join(' ')}
                                  >
                                    {s.label}
                                  </div>
                                  {idx < STEPS.length - 1 ? (
                                    <div className={['h-0.5 w-6', active ? 'bg-[#2563eb]' : 'bg-slate-200 dark:bg-slate-700'].join(' ')} />
                                  ) : null}
                                </div>
                              )
                            })}
                          </div>
                        )}
                      </div>
                    </div>

                    {detail?.withdrawalEligibility?.allowed ? (
                      <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-4 dark:border-amber-900/50 dark:bg-amber-950/20">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                          Bạn có thể rút đơn khi tin còn mở, chưa hết hạn nộp và đơn đang ở giai đoạn đầu (chưa phỏng
                          vấn / offer).
                        </p>
                        <button
                          type="button"
                          disabled={withdrawing}
                          onClick={() => withdrawApplication(detail.application?.id)}
                          className="mt-3 inline-flex items-center gap-2 rounded-lg border border-rose-300 bg-white px-4 py-2 text-sm font-semibold text-rose-700 shadow-sm transition hover:bg-rose-50 disabled:opacity-60 dark:border-rose-800 dark:bg-slate-900 dark:text-rose-300 dark:hover:bg-rose-950/40"
                        >
                          <UserX className="h-4 w-4" />
                          {withdrawing ? 'Đang xử lý…' : 'Rút đơn ứng tuyển'}
                        </button>
                      </div>
                    ) : detail?.withdrawalEligibility?.reason &&
                      String(detail?.application?.status || '') !== 'WITHDRAWN' ? (
                      <p className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600 dark:border-slate-700 dark:bg-slate-800/50 dark:text-slate-300">
                        <span className="font-semibold">Rút đơn:</span> {detail.withdrawalEligibility.reason}
                      </p>
                    ) : null}

                    {stageHistory.length > 0 ? (
                      <div>
                        <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-100 mb-2">
                          Lịch sử trạng thái
                        </h4>
                        <ol className="space-y-2 border-l-2 border-slate-200 dark:border-slate-700 pl-4">
                          {stageHistory.map((h) => (
                            <li key={h.id} className="text-sm text-slate-600 dark:text-slate-300">
                              <span className="font-medium text-slate-800 dark:text-slate-100">
                                {statusText(h.fromStage)} → {statusText(h.toStage)}
                              </span>
                              {h.createdAt ? (
                                <span className="block text-xs text-slate-500 mt-0.5">
                                  {new Date(h.createdAt).toLocaleString('vi-VN')}
                                </span>
                              ) : null}
                              {h.note ? (
                                <span className="block text-xs text-slate-500 mt-0.5">{h.note}</span>
                              ) : null}
                            </li>
                          ))}
                        </ol>
                      </div>
                    ) : null}

                    {String(detail?.application?.status || '') !== 'WITHDRAWN' ? (
                      <Link
                        to={`/candidate/applications/${detail.application?.id}/tasks`}
                        className="inline-flex items-center gap-2 rounded-xl border border-[#2563eb]/40 bg-blue-50/80 px-4 py-2.5 text-sm font-semibold text-[#2563eb] hover:bg-blue-100 dark:bg-blue-950/30 dark:hover:bg-blue-950/50"
                      >
                        <ClipboardList className="h-4 w-4" />
                        Nhiệm vụ &amp; tài liệu từ HR
                      </Link>
                    ) : null}

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div className="bg-slate-50 dark:bg-slate-800/50 p-3 rounded">
                        <span className="block text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
                          Current Stage
                        </span>
                        <span className="block mt-1 text-sm font-bold capitalize text-slate-800 dark:text-slate-100">
                          {statusText(detail?.application?.status)}
                        </span>
                      </div>
                      <div className="bg-slate-50 dark:bg-slate-800/50 p-3 rounded">
                        <span className="block text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
                          CV Reference
                        </span>
                        {detail?.application?.cvUrl ? (
                          <a
                            href={detail.application.cvUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-1 inline-flex items-center gap-2 text-sm font-medium text-[#2563eb] hover:underline"
                          >
                            <FileDown className="h-4 w-4" />
                            Download / View CV
                          </a>
                        ) : (
                          <span className="block mt-1 text-sm text-slate-600 dark:text-slate-300">—</span>
                        )}
                      </div>
                    </div>

                    {detail?.aiEvaluation ? (
                      <div className="bg-blue-50/50 dark:bg-blue-950/20 border border-blue-100 dark:border-blue-900/40 rounded p-4">
                        <h4 className="text-sm font-semibold text-blue-800 dark:text-blue-200 mb-2">
                          AI Screening Summary
                        </h4>
                        <div className="text-sm text-slate-700 dark:text-slate-200 whitespace-pre-wrap">
                          {detail.aiEvaluation.summary}
                        </div>
                      </div>
                    ) : null}

                    {detail?.interviews && detail.interviews.length > 0 ? (
                      <div>
                        <h4 className="text-md font-semibold text-slate-800 dark:text-slate-100 mb-2 border-b border-slate-200 dark:border-slate-800 pb-2">
                          Interview History
                        </h4>
                        <ul className="space-y-3">
                          {detail.interviews.map((it) => (
                            <li
                              key={it.id}
                              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded p-3 flex justify-between items-center shadow-sm"
                            >
                              <div>
                                <p className="text-sm font-medium text-slate-800 dark:text-slate-100">
                                  Scheduled:{' '}
                                  {it.interviewTime ? new Date(it.interviewTime).toLocaleString('vi-VN') : '—'}
                                </p>
                                <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                                  {it.locationOrLink ? (
                                    it.locationOrLink.startsWith('http') ? (
                                      <a
                                        href={it.locationOrLink}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="text-[#2563eb] hover:underline"
                                      >
                                        Join Meeting
                                      </a>
                                    ) : (
                                      <span>{it.locationOrLink}</span>
                                    )
                                  ) : (
                                    <span>—</span>
                                  )}
                                </div>
                              </div>
                              <span
                                className={[
                                  'px-2 py-1 text-xs font-semibold rounded-full',
                                  it.status === 'SCHEDULED'
                                    ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-200'
                                    : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200',
                                ].join(' ')}
                              >
                                {it.status}
                              </span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    ) : (
                      <div className="text-sm text-slate-500 dark:text-slate-400 italic p-4 text-center border-t border-slate-200 dark:border-slate-800">
                        No interviews scheduled yet.
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="p-8 text-center text-slate-500 dark:text-slate-400">Không tải được chi tiết.</div>
              )}
            </div>
          </div>
        </div>
      ) : null}

      {!user ? (
        <div className="mt-6 text-sm text-slate-500 dark:text-slate-400">
          Bạn chưa đăng nhập. Vui lòng{' '}
          <Link to="/login" className="text-[#2563eb] font-medium hover:underline">
            đăng nhập
          </Link>
          .
        </div>
      ) : null}
    </section>
  )
}
