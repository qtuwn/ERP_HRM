import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { getSockJsUrl } from '../lib/config.js'
import { getAccessToken } from '../lib/storage.js'
import { KanbanApplicationCard } from '../components/KanbanApplicationCard.jsx'
import { Calendar, Eye, Loader2, Radio, SquareCheck, X } from 'lucide-react'

const LANES = [
  { id: 'APPLIED', title: 'Applied' },
  { id: 'HR_REVIEW', title: 'HR Review' },
  { id: 'INTERVIEW', title: 'Interview' },
  { id: 'OFFER', title: 'Offer' },
  { id: 'HIRED', title: 'Hired' },
  { id: 'REJECTED', title: 'Rejected' },
]

function getScoreColor(score) {
  if (score === null || score === undefined) return 'bg-gray-100 text-gray-800'
  if (score >= 80) return 'bg-green-100 text-green-800'
  if (score >= 50) return 'bg-yellow-100 text-yellow-800'
  return 'bg-red-100 text-red-800 border bg-opacity-70 border-red-200'
}

function badgeText(app) {
  const score = app?.aiScore
  if (score === null || score === undefined) return 'AI: N/A'
  if (score >= 80) return `TOP MATCH • ${score}`
  if (score >= 50) return `MATCH • ${score}`
  return `LOW • ${score}`
}

function parseSkills(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch {
      // ignore
    }
    return value
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
  }
  return []
}

/** Trạng thái AI_SCREENING gom vào cột Applied (không còn cột riêng). */
function kanbanLaneForStatus(status) {
  if (status === 'AI_SCREENING') return 'APPLIED'
  return status
}

const KANBAN_WS_EVENTS = new Set(['application:new', 'application:stage_changed'])

/** Hồ sơ còn có thể chuyển sang REJECTED (bulk reject trên pipeline). */
function canBulkRejectStatus(status) {
  const s = String(status || '')
  return s !== 'REJECTED' && s !== 'HIRED' && s !== 'WITHDRAWN'
}

export function KanbanPage() {
  const { jobId } = useParams()
  const navigate = useNavigate()

  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [draggingAppId, setDraggingAppId] = useState(null)
  const [dragOverLane, setDragOverLane] = useState(null)

  const [selectedAppIds, setSelectedAppIds] = useState([])
  const [bulkBusy, setBulkBusy] = useState(false)

  const [showAiModal, setShowAiModal] = useState(false)
  const [aiModalLoading, setAiModalLoading] = useState(false)
  const [aiModalApp, setAiModalApp] = useState(null)
  const [aiModalEval, setAiModalEval] = useState(null)

  const [reviewOpen, setReviewOpen] = useState(false)
  const [reviewLoading, setReviewLoading] = useState(false)
  const [reviewData, setReviewData] = useState(null)
  const [reviewTitle, setReviewTitle] = useState('')
  const [hrNoteDraft, setHrNoteDraft] = useState('')
  const [reviewSaving, setReviewSaving] = useState(false)

  const [wsLive, setWsLive] = useState(false)

  const stompRef = useRef(null)
  const wsDebounceRef = useRef(null)
  const wsMountedRef = useRef(true)

  const fetchApplications = useCallback(
    async (options = {}) => {
      const silent = Boolean(options.silent)
      if (!jobId) return
      if (!silent) setLoading(true)
      try {
        const res = await api.get(`/api/jobs/${jobId}/applications/kanban`)
        setApplications(res?.data || [])
      } finally {
        if (!silent) setLoading(false)
      }
    },
    [jobId]
  )

  useEffect(() => {
    fetchApplications({ silent: false })
  }, [fetchApplications])

  /** Sau khi refetch / realtime: bỏ chọn các id không còn hợp lệ hoặc đã ở trạng thái kết thúc. */
  useEffect(() => {
    setSelectedAppIds((prev) =>
      prev.filter((id) =>
        applications.some((a) => String(a.id) === String(id) && canBulkRejectStatus(a.status))
      )
    )
  }, [applications])

  useEffect(() => {
    wsMountedRef.current = true
    const token = getAccessToken()
    if (!jobId || !token) {
      setWsLive(false)
      return undefined
    }

    let cancelled = false

    function scheduleSilentRefetch() {
      if (wsDebounceRef.current) clearTimeout(wsDebounceRef.current)
      wsDebounceRef.current = setTimeout(() => {
        wsDebounceRef.current = null
        if (wsMountedRef.current) fetchApplications({ silent: true })
      }, 350)
    }

    ;(async () => {
      const [{ default: SockJS }, { Client }] = await Promise.all([
        import('sockjs-client/dist/sockjs'),
        import('@stomp/stompjs'),
      ])
      if (cancelled) return

      const client = new Client({
        webSocketFactory: () => new SockJS(getSockJsUrl()),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 4000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: () => {},
      })

      client.onConnect = () => {
        if (cancelled) return
        setWsLive(true)
        client.subscribe(`/topic/jobs/${jobId}`, (frame) => {
          try {
            const event = JSON.parse(frame.body)
            if (event?.type && KANBAN_WS_EVENTS.has(event.type)) {
              scheduleSilentRefetch()
            }
          } catch {
            // ignore malformed frame
          }
        })
      }

      client.onDisconnect = () => {
        if (wsMountedRef.current) setWsLive(false)
      }

      client.onStompError = () => {
        if (wsMountedRef.current) setWsLive(false)
      }

      client.activate()
      stompRef.current = client
    })()

    return () => {
      cancelled = true
      wsMountedRef.current = false
      if (wsDebounceRef.current) {
        clearTimeout(wsDebounceRef.current)
        wsDebounceRef.current = null
      }
      try {
        stompRef.current?.deactivate()
      } catch {
        // ignore
      } finally {
        stompRef.current = null
      }
      setWsLive(false)
    }
  }, [jobId, fetchApplications])

  const byLane = useMemo(() => {
    const map = new Map()
    LANES.forEach((l) => map.set(l.id, []))
    for (const a of applications) {
      const key = kanbanLaneForStatus(a.status)
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(a)
    }
    // Sort APPLIED by aiScore desc (ưu tiên điểm AI cao)
    const applied = map.get('APPLIED') || []
    map.set(
      'APPLIED',
      applied.slice().sort((x, y) => (y.aiScore ?? -1) - (x.aiScore ?? -1))
    )
    return map
  }, [applications])

  function getLaneCount(id) {
    return (byLane.get(id) || []).length
  }

  const toggleSelection = useCallback((id, app) => {
    if (!canBulkRejectStatus(app?.status)) return
    setSelectedAppIds((prev) =>
      prev.some((x) => String(x) === String(id)) ? prev.filter((x) => String(x) !== String(id)) : [...prev, id]
    )
  }, [])

  function selectAllRejectable() {
    const ids = applications.filter((a) => canBulkRejectStatus(a.status)).map((a) => a.id)
    setSelectedAppIds(ids)
  }

  function clearSelection() {
    setSelectedAppIds([])
  }

  async function bulkReject() {
    if (selectedAppIds.length === 0 || bulkBusy) return
    const ok = window.confirm(`Từ chối ${selectedAppIds.length} hồ sơ đã chọn? Hành động này cập nhật trạng thái sang Từ chối.`)
    if (!ok) return
    setBulkBusy(true)
    try {
      const res = await api.post('/api/applications/bulk-reject', { applicationIds: selectedAppIds })
      const data = res?.data
      const okCount = Array.isArray(data?.succeededIds) ? data.succeededIds.length : 0
      const failed = data?.failed && typeof data.failed === 'object' ? data.failed : {}
      const failCount = Object.keys(failed).length
      let msg = `Đã từ chối thành công: ${okCount}.`
      if (failCount > 0) {
        const first = Object.values(failed)[0]
        msg += ` Không xử lý được: ${failCount} (${first || 'xem chi tiết network'}).`
      }
      window.alert(msg)
    } catch (e) {
      window.alert(e?.message || 'Không thực hiện được từ chối hàng loạt.')
    } finally {
      setBulkBusy(false)
      setSelectedAppIds([])
      await fetchApplications()
    }
  }

  const dragStart = useCallback((app) => {
    setDraggingAppId(app.id)
  }, [])
  const dragEnd = useCallback(() => {
    setDraggingAppId(null)
    setDragOverLane(null)
  }, [])

  async function dropOnLane(newStatus) {
    setDragOverLane(null)
    const app = applications.find((a) => a.id === draggingAppId)
    if (!app || app.status === newStatus) return

    const originalStatus = app.status
    if (newStatus === 'REJECTED') {
      const ok = confirm(`Reject ${app.candidateName}?`)
      if (!ok) return
    }

    // optimistic
    setApplications((prev) => prev.map((x) => (x.id === app.id ? { ...x, status: newStatus } : x)))

    try {
      const res = await api.patch(`/api/applications/${app.id}/status`, {
        status: newStatus,
        note: `Moved to ${newStatus} via Kanban board`,
      })
      if (res?.success === false) {
        throw new Error(res?.message || 'Update failed')
      }
    } catch (e) {
      // rollback
      setApplications((prev) => prev.map((x) => (x.id === app.id ? { ...x, status: originalStatus } : x)))
      alert(e?.message || 'Could not update status')
    }
  }

  const closeReviewModal = useCallback(() => {
    setReviewOpen(false)
    setReviewLoading(false)
    setReviewData(null)
    setHrNoteDraft('')
    setReviewTitle('')
  }, [])

  const openReview = useCallback(
    async (app) => {
      if (!app?.id) return
      setReviewOpen(true)
      setReviewTitle(app.candidateName || 'Ứng viên')
      setReviewLoading(true)
      setReviewData(null)
      setHrNoteDraft('')
      try {
        const res = await api.get(`/api/applications/${app.id}/hr-review`)
        const data = res?.data
        setReviewData(data)
        setHrNoteDraft(data?.application?.hrNote ?? '')
      } catch (e) {
        window.alert(e?.message || 'Không tải được dữ liệu đánh giá.')
        closeReviewModal()
      } finally {
        setReviewLoading(false)
      }
    },
    [closeReviewModal]
  )

  async function saveHrNote() {
    const appId = reviewData?.application?.id
    if (!appId || reviewSaving) return
    setReviewSaving(true)
    try {
      const res = await api.patch(`/api/applications/${appId}/hr-note`, { hrNote: hrNoteDraft })
      const updated = res?.data
      setReviewData((prev) =>
        prev && updated
          ? { ...prev, application: { ...prev.application, ...updated, hrNote: updated.hrNote } }
          : prev
      )
      window.alert('Đã lưu ghi chú HR.')
    } catch (e) {
      window.alert(e?.message || 'Không lưu được ghi chú.')
    } finally {
      setReviewSaving(false)
    }
  }

  function formatInterviewWhen(iso) {
    if (!iso) return '—'
    try {
      return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' })
    } catch {
      return String(iso)
    }
  }

  function prettyFormData(raw) {
    if (raw == null || raw === '') return null
    if (typeof raw !== 'string') return JSON.stringify(raw, null, 2)
    try {
      return JSON.stringify(JSON.parse(raw), null, 2)
    } catch {
      return raw
    }
  }

  const openAiInsights = useCallback(async (app) => {
    setShowAiModal(true)
    setAiModalLoading(true)
    setAiModalApp(app)
    setAiModalEval(null)
    try {
      const res = await api.get(`/api/applications/${app.id}/ai-evaluation`)
      setAiModalEval(res?.data || null)
    } catch (e) {
      alert(e?.message || 'Không thể tải AI đánh giá')
    } finally {
      setAiModalLoading(false)
    }
  }, [])

  const openChatThread = useCallback(
    (applicationId) => {
      const params = new URLSearchParams({
        jobId: String(jobId),
        applicationId: String(applicationId),
      })
      navigate(`/dashboard/messages?${params.toString()}`)
    },
    [jobId, navigate]
  )

  function closeAiModal() {
    setShowAiModal(false)
    setAiModalLoading(false)
    setAiModalApp(null)
    setAiModalEval(null)
  }

  return (
    <section>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6 flex justify-between items-center flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Theo dõi ứng viên (Kanban)</h1>
            <span
              className={[
                'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium',
                wsLive
                  ? 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-200'
                  : 'border-slate-200 bg-slate-50 text-slate-500 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-400',
              ].join(' ')}
              title={wsLive ? 'Đồng bộ STOMP /topic/jobs/{id} đang hoạt động' : 'Chưa kết nối realtime (kiểm tra đăng nhập / WS)'}
            >
              <Radio className={['h-3 w-3', wsLive ? 'text-emerald-600 animate-pulse' : ''].join(' ')} />
              {wsLive ? 'Realtime' : 'Offline'}
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-2 sm:gap-3">
            {applications.some((a) => canBulkRejectStatus(a.status)) ? (
              <button
                type="button"
                onClick={selectAllRejectable}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                <SquareCheck className="h-4 w-4" />
                Chọn tất cả
              </button>
            ) : null}
            {selectedAppIds.length > 0 ? (
              <>
                <button
                  type="button"
                  onClick={clearSelection}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
                >
                  Bỏ chọn
                </button>
                <button
                  type="button"
                  onClick={bulkReject}
                  disabled={bulkBusy}
                  className="inline-flex items-center gap-2 rounded-lg border border-transparent bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {bulkBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                  Từ chối đã chọn ({selectedAppIds.length})
                </button>
              </>
            ) : null}
            <Link
              to="/jobs/management"
              className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-indigo-600 shadow-sm transition hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-indigo-400 dark:hover:bg-slate-700"
            >
              ← Danh sách tin tuyển dụng
            </Link>
          </div>
        </div>

        <p className="mb-4 max-w-3xl text-sm text-slate-600 dark:text-slate-400">
          Tick nhiều hồ sơ rồi dùng <strong className="text-slate-800 dark:text-slate-200">Từ chối đã chọn</strong> để xử lý
          nhanh. Chỉ các hồ sơ chưa ở trạng thái kết thúc (Đã nhận / Từ chối / Đã rút) mới chọn được.
        </p>

        {loading ? (
          <div className="text-sm text-slate-500">Đang tải Kanban...</div>
        ) : (
          <div className="flex space-x-4 overflow-x-auto pb-4" style={{ minHeight: '70vh' }}>
            {LANES.map((lane) => (
              <div
                key={lane.id}
                className="flex-shrink-0 w-80 bg-gray-100 rounded-lg p-4 flex flex-col shadow-sm border border-gray-200 dark:bg-slate-900 dark:border-slate-800"
              >
                <h2 className="text-sm font-semibold text-gray-700 uppercase mb-4 dark:text-slate-200">
                  {lane.title} ({getLaneCount(lane.id)})
                </h2>

                <div
                  className={[
                    'flex-1 space-y-3 overflow-y-auto',
                    dragOverLane === lane.id
                      ? 'bg-indigo-50 border-2 border-dashed border-indigo-300 rounded-md p-2 -m-2'
                      : '',
                  ].join(' ')}
                  onDragOver={(e) => {
                    e.preventDefault()
                    setDragOverLane(lane.id)
                  }}
                  onDragLeave={(e) => {
                    e.preventDefault()
                    setDragOverLane(null)
                  }}
                  onDrop={(e) => {
                    e.preventDefault()
                    dropOnLane(lane.id)
                  }}
                >
                  {(byLane.get(lane.id) || []).map((app) => (
                    <KanbanApplicationCard
                      key={app.id}
                      app={app}
                      selected={selectedAppIds.some((x) => String(x) === String(app.id))}
                      canSelect={canBulkRejectStatus(app.status)}
                      onToggleSelect={toggleSelection}
                      onDragStart={dragStart}
                      onDragEnd={dragEnd}
                      onOpenAiInsights={openAiInsights}
                      onOpenReview={openReview}
                      onOpenChat={openChatThread}
                    />
                  ))}

                  {getLaneCount(lane.id) === 0 ? (
                    <div className="text-sm text-gray-400 text-center py-6 italic border-2 border-dashed border-gray-200 rounded-md dark:border-slate-800">
                      No candidates
                    </div>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {reviewOpen ? (
        <div className="fixed inset-0 z-[60] overflow-y-auto">
          <div className="flex min-h-screen items-center justify-center px-4 py-10 text-center sm:p-0">
            <div className="fixed inset-0 bg-gray-900/60" aria-hidden="true" onClick={closeReviewModal} />
            <div className="relative inline-block w-full max-w-3xl overflow-hidden rounded-xl bg-white text-left align-middle shadow-xl dark:bg-slate-950">
              <div className="flex max-h-[90vh] flex-col">
              <div className="flex items-center justify-between border-b px-6 py-4 dark:border-slate-800">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Đánh giá &amp; ghi chú HR</h3>
                  <p className="text-sm text-gray-500 dark:text-slate-400">{reviewTitle}</p>
                </div>
                <button
                  type="button"
                  className="rounded p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-slate-900"
                  onClick={closeReviewModal}
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto px-6 py-5">
                {reviewLoading ? (
                  <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-slate-400">
                    <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
                    Đang tải hồ sơ…
                  </div>
                ) : !reviewData ? (
                  <p className="text-sm text-gray-500">Không có dữ liệu.</p>
                ) : (
                  <div className="space-y-6">
                    <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900/50">
                      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                        Ứng viên &amp; vị trí
                      </p>
                      <dl className="mt-2 grid gap-2 text-sm text-slate-800 dark:text-slate-200 sm:grid-cols-2">
                        <div>
                          <dt className="text-slate-500 dark:text-slate-400">Họ tên</dt>
                          <dd>{reviewData.candidate?.fullName || '—'}</dd>
                        </div>
                        <div>
                          <dt className="text-slate-500 dark:text-slate-400">Email</dt>
                          <dd className="break-all">{reviewData.candidate?.email || '—'}</dd>
                        </div>
                        <div>
                          <dt className="text-slate-500 dark:text-slate-400">Điện thoại</dt>
                          <dd>{reviewData.candidate?.phone || '—'}</dd>
                        </div>
                        <div>
                          <dt className="text-slate-500 dark:text-slate-400">Tin tuyển dụng</dt>
                          <dd>{reviewData.jobTitle || '—'}</dd>
                        </div>
                        <div className="sm:col-span-2">
                          <dt className="text-slate-500 dark:text-slate-400">Trạng thái hồ sơ</dt>
                          <dd>{reviewData.application?.status || '—'}</dd>
                        </div>
                      </dl>
                    </div>

                    {reviewData.application?.cvUrl ? (
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                          CV
                        </p>
                        <a
                          href={reviewData.application.cvUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="mt-1 inline-flex items-center gap-1 text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400"
                        >
                          <Eye className="h-4 w-4" />
                          Mở CV (tab mới)
                        </a>
                      </div>
                    ) : (
                      <p className="text-sm text-slate-500">Chưa có file CV.</p>
                    )}

                    <div>
                      <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                        <Calendar className="h-4 w-4" />
                        Lịch phỏng vấn
                      </p>
                      {!reviewData.interviews?.length ? (
                        <p className="mt-2 text-sm text-slate-500">Chưa có lịch.</p>
                      ) : (
                        <ul className="mt-2 space-y-2">
                          {reviewData.interviews.map((iv) => (
                            <li
                              key={iv.id}
                              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900"
                            >
                              <div className="font-medium text-slate-900 dark:text-slate-100">
                                {formatInterviewWhen(iv.interviewTime)}
                              </div>
                              <div className="text-slate-600 dark:text-slate-400">
                                {iv.locationOrLink || '—'} · {iv.status || '—'}
                              </div>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>

                    {reviewData.aiEvaluation ? (
                      <div className="rounded-lg border border-indigo-100 bg-indigo-50/80 p-4 dark:border-indigo-900 dark:bg-indigo-950/40">
                        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-700 dark:text-indigo-300">
                          Đánh giá AI (sàng lọc)
                        </p>
                        <div className="mt-2 space-y-2 text-sm text-slate-800 dark:text-slate-200">
                          {reviewData.aiEvaluation.score != null ? (
                            <div>
                              <span className="font-medium">Điểm: </span>
                              {reviewData.aiEvaluation.score}
                            </div>
                          ) : null}
                          {reviewData.aiEvaluation.summary ? (
                            <p className="whitespace-pre-wrap">{reviewData.aiEvaluation.summary}</p>
                          ) : null}
                          {reviewData.aiEvaluation.matchedSkills ? (
                            <p>
                              <span className="font-medium text-slate-600 dark:text-slate-400">
                                Kỹ năng khớp:{' '}
                              </span>
                              {reviewData.aiEvaluation.matchedSkills}
                            </p>
                          ) : null}
                          {reviewData.aiEvaluation.missingSkills ? (
                            <p>
                              <span className="font-medium text-slate-600 dark:text-slate-400">
                                Kỹ năng thiếu:{' '}
                              </span>
                              {reviewData.aiEvaluation.missingSkills}
                            </p>
                          ) : null}
                        </div>
                      </div>
                    ) : null}

                    {prettyFormData(reviewData.application?.formData) ? (
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                          Thông tin form ứng tuyển
                        </p>
                        <pre className="mt-2 max-h-48 overflow-auto rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200">
                          {prettyFormData(reviewData.application.formData)}
                        </pre>
                      </div>
                    ) : null}

                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                        Ghi chú nội bộ HR
                      </p>
                      <p className="mt-1 text-xs text-slate-500 dark:text-slate-500">
                        Chỉ HR/công ty xem được; ứng viên không thấy trên cổng ứng viên.
                      </p>
                      <textarea
                        value={hrNoteDraft}
                        onChange={(e) => setHrNoteDraft(e.target.value)}
                        rows={5}
                        maxLength={8000}
                        placeholder="Nhận xét nội bộ, lý do loại/ưu tiên, nhắc lịch gọi lại…"
                        className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                      />
                      <p className="mt-1 text-right text-xs text-slate-400">{hrNoteDraft.length}/8000</p>
                    </div>
                  </div>
                )}
              </div>

              <div className="flex flex-wrap justify-end gap-2 border-t px-6 py-4 dark:border-slate-800">
                <button
                  type="button"
                  className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
                  onClick={closeReviewModal}
                >
                  Đóng
                </button>
                <button
                  type="button"
                  disabled={reviewLoading || !reviewData || reviewSaving}
                  className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
                  onClick={saveHrNote}
                >
                  {reviewSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                  Lưu ghi chú
                </button>
              </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {showAiModal ? (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4 py-10 text-center sm:p-0">
            <div className="fixed inset-0 bg-gray-900/50" aria-hidden="true" onClick={closeAiModal} />

            <div className="relative inline-block w-full max-w-2xl overflow-hidden rounded-xl bg-white text-left align-middle shadow-xl dark:bg-slate-950">
              <div className="flex items-center justify-between border-b px-6 py-4 dark:border-slate-800">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">AI Insights</h3>
                  <p className="text-sm text-gray-500">{aiModalApp ? aiModalApp.candidateName : ''}</p>
                </div>
                <button
                  className="rounded p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-slate-900"
                  onClick={closeAiModal}
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="px-6 py-5">
                {aiModalLoading ? (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Loader2 className="h-4 w-4 text-indigo-600 animate-spin" />
                    Đang tải đánh giá...
                  </div>
                ) : !aiModalEval ? (
                  <div className="text-sm text-gray-500">Chưa có đánh giá AI.</div>
                ) : (
                  <div className="space-y-5">
                    <div className="flex items-center gap-3">
                      <span
                        className={[
                          'inline-flex items-center rounded-full px-3 py-1 text-sm font-semibold',
                          getScoreColor(aiModalEval.score),
                        ].join(' ')}
                      >
                        {badgeText({ aiScore: aiModalEval.score })}
                      </span>
                      <span className="text-sm text-gray-600">
                        {aiModalEval.discrepancy ? `Suitability: ${aiModalEval.discrepancy}` : ''}
                      </span>
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-gray-800 mb-2 dark:text-slate-200">Matched skills</p>
                      <div className="flex flex-wrap gap-2">
                        {parseSkills(aiModalEval.matchedSkills).map((s) => (
                          <span
                            key={s}
                            className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 border border-emerald-200"
                          >
                            {s}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-gray-800 mb-2 dark:text-slate-200">Missing skills</p>
                      <div className="flex flex-wrap gap-2">
                        {parseSkills(aiModalEval.missingSkills).map((s) => (
                          <span
                            key={s}
                            className="rounded-full bg-rose-50 px-3 py-1 text-xs font-medium text-rose-700 border border-rose-200"
                          >
                            {s}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-gray-800 mb-2 dark:text-slate-200">Summary</p>
                      <p className="text-sm leading-6 text-gray-700 whitespace-pre-line dark:text-slate-300">
                        {aiModalEval.summary || '-'}
                      </p>
                    </div>
                  </div>
                )}
              </div>

              <div className="border-t px-6 py-4 flex justify-end dark:border-slate-800">
                <button
                  className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
                  onClick={closeAiModal}
                >
                  Đóng
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
