import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { getSockJsUrl } from '../lib/config.js'
import { getAccessToken } from '../lib/storage.js'
import { Eye, Info, Loader2, Mail, MessageSquare, Radio, X } from 'lucide-react'

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

export function KanbanPage() {
  const { jobId } = useParams()

  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [draggingAppId, setDraggingAppId] = useState(null)
  const [dragOverLane, setDragOverLane] = useState(null)

  const [selectedAppIds, setSelectedAppIds] = useState([])

  const [showAiModal, setShowAiModal] = useState(false)
  const [aiModalLoading, setAiModalLoading] = useState(false)
  const [aiModalApp, setAiModalApp] = useState(null)
  const [aiModalEval, setAiModalEval] = useState(null)

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

  function toggleSelection(id) {
    setSelectedAppIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  async function bulkReject() {
    if (selectedAppIds.length === 0) return
    const ok = confirm(`Reject ${selectedAppIds.length} candidate(s)?`)
    if (!ok) return
    await api.post('/api/applications/bulk-reject', { applicationIds: selectedAppIds })
    setSelectedAppIds([])
    await fetchApplications()
  }

  function dragStart(app) {
    setDraggingAppId(app.id)
  }
  function dragEnd() {
    setDraggingAppId(null)
    setDragOverLane(null)
  }

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

  async function openAiInsights(app) {
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
  }

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
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Job Applications Kanban</h1>
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
          <div className="flex items-center space-x-3">
            {selectedAppIds.length > 0 ? (
              <button
                onClick={bulkReject}
                className="bg-red-600 border border-transparent text-white hover:bg-red-700 px-4 py-2 rounded shadow-sm hover:shadow transition text-sm font-medium"
              >
                Reject Selected ({selectedAppIds.length})
              </button>
            ) : null}
            <Link
              to="/jobs/management"
              className="text-indigo-600 bg-white hover:bg-gray-50 border px-4 py-2 rounded shadow-sm hover:shadow transition"
            >
              Back to Jobs
            </Link>
          </div>
        </div>

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
                    <div
                      key={app.id}
                      className="bg-white p-4 rounded-md shadow border border-gray-200 cursor-move hover:border-indigo-400 hover:shadow-md transition duration-150 dark:bg-slate-950 dark:border-slate-800"
                      draggable
                      onDragStart={() => dragStart(app)}
                      onDragEnd={dragEnd}
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex items-center gap-2 overflow-hidden">
                          <input
                            type="checkbox"
                            checked={selectedAppIds.includes(app.id)}
                            onChange={() => toggleSelection(app.id)}
                            onClick={(e) => e.stopPropagation()}
                            className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded flex-shrink-0 cursor-pointer"
                          />
                          <h3 className="font-medium text-gray-900 text-sm truncate pr-2 dark:text-white">
                            {app.candidateName}
                          </h3>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          {app.aiStatus === 'AI_PROCESSING' || app.aiStatus === 'AI_QUEUED' ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold bg-indigo-50 text-indigo-700">
                              <Loader2 className="h-3 w-3 animate-spin" />
                              AI Screening...
                            </span>
                          ) : (
                            <span
                              className={[
                                'inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold',
                                getScoreColor(app.aiScore),
                              ].join(' ')}
                            >
                              {badgeText(app)}
                            </span>
                          )}
                        </div>
                      </div>

                      <a
                        href={`mailto:${app.candidateEmail}`}
                        className="text-xs text-gray-500 hover:text-indigo-600 hover:underline truncate block mb-1"
                      >
                        <span className="inline-flex items-center gap-1">
                          <Mail className="h-3 w-3" />
                          {app.candidateEmail}
                        </span>
                      </a>

                      <div className="mt-3 flex gap-3 items-center">
                        {app.cvUrl ? (
                          <a
                            href={app.cvUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center"
                          >
                            <Eye className="h-3 w-3 mr-1" />
                            CV
                          </a>
                        ) : null}

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation()
                            openAiInsights(app)
                          }}
                          className="text-xs text-slate-700 hover:text-indigo-700 font-medium flex items-center bg-slate-50 hover:bg-indigo-50 px-2 py-1 rounded border border-slate-200 dark:bg-slate-900 dark:border-slate-700"
                        >
                          <Info className="h-3 w-3 mr-1" />
                          AI Insights
                        </button>

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation()
                            window.dispatchEvent(
                              new CustomEvent('open-chat', {
                                detail: {
                                  applicationId: app.id,
                                  applicationTitle: `Chat — ${app.candidateName || 'Ứng viên'}`.trim(),
                                },
                              })
                            )
                          }}
                          className="text-xs text-blue-600 hover:text-blue-800 font-medium flex items-center ml-auto bg-blue-50 px-2 py-1 rounded dark:bg-blue-950/40 dark:text-blue-300"
                          title="Mở chat với ứng viên"
                        >
                          <MessageSquare className="h-3 w-3 mr-1" />
                          Chat
                        </button>
                      </div>
                    </div>
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
