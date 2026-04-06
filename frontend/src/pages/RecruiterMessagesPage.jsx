import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { useDebouncedValue } from '../lib/useDebouncedValue.js'
import { ApplicationChatPanel } from '../components/ApplicationChatPanel.jsx'
import { ArrowLeft, Briefcase, MessageCircle, Search } from 'lucide-react'

const ALL_JOBS = 'ALL'

function statusShort(s) {
  const x = String(s || '')
  if (x === 'REJECTED') return 'Từ chối'
  if (x === 'HIRED') return 'Đã nhận'
  if (x === 'OFFER') return 'Offer'
  if (x === 'INTERVIEW') return 'Phỏng vấn'
  if (x === 'HR_REVIEW') return 'HR duyệt'
  return x === 'APPLIED' ? 'Đã nộp' : x || '—'
}

function mapInboxRows(content) {
  if (!Array.isArray(content)) return []
  return content.map((t) => ({
    id: t.applicationId,
    jobId: t.jobId,
    jobTitle: t.jobTitle,
    candidateName: t.candidateName,
    candidateEmail: t.candidateEmail,
    status: t.status,
    lastMessagePreview: t.lastMessagePreview,
    lastMessageAt: t.lastMessageAt,
  }))
}

function formatShortTime(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return ''
    return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

export function RecruiterMessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const jobIdFromUrl = searchParams.get('jobId')
  const applicationIdFromUrl = searchParams.get('applicationId')

  const [jobs, setJobs] = useState([])
  const [jobsLoading, setJobsLoading] = useState(true)
  const [inboxAllJobs, setInboxAllJobs] = useState(false)
  const [selectedJobId, setSelectedJobId] = useState(null)

  const [applications, setApplications] = useState([])
  const [appsLoading, setAppsLoading] = useState(false)
  const [selectedApplicationId, setSelectedApplicationId] = useState(null)
  const [listQuery, setListQuery] = useState('')
  const debouncedListQuery = useDebouncedValue(listQuery, 280)
  const [focusChat, setFocusChat] = useState(false)

  const selectedJob = useMemo(
    () => jobs.find((j) => String(j.id) === String(selectedJobId)),
    [jobs, selectedJobId]
  )

  const fetchJobs = useCallback(async () => {
    setJobsLoading(true)
    try {
      const params = new URLSearchParams({
        page: '0',
        size: '100',
        sort: 'createdAt,desc',
      })
      const res = await api.get(`/api/jobs/department?${params.toString()}`)
      const list = res?.data?.content || []
      setJobs(list)
    } catch {
      setJobs([])
    } finally {
      setJobsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchJobs()
  }, [fetchJobs])

  useEffect(() => {
    if (!jobs.length) {
      setSelectedJobId(null)
      return
    }
    if (jobIdFromUrl && jobs.some((j) => String(j.id) === jobIdFromUrl)) {
      setInboxAllJobs(false)
      setSelectedJobId(jobIdFromUrl)
      return
    }
    if (!jobIdFromUrl && applicationIdFromUrl) {
      setInboxAllJobs(true)
      setSelectedJobId(null)
      return
    }
    setInboxAllJobs(false)
    setSelectedJobId((prev) => {
      if (prev && jobs.some((j) => String(j.id) === String(prev))) return prev
      return jobs[0]?.id ?? null
    })
  }, [jobs, jobIdFromUrl, applicationIdFromUrl])

  const fetchInboxThreads = useCallback(async () => {
    if (!inboxAllJobs && !selectedJobId) {
      setApplications([])
      return
    }
    setAppsLoading(true)
    try {
      const params = new URLSearchParams({ page: '0', size: '500' })
      if (!inboxAllJobs && selectedJobId) params.set('jobId', String(selectedJobId))
      const res = await api.get(`/api/inbox/recruiter/threads?${params.toString()}`)
      const page = res?.data
      const content = page?.content ?? []
      setApplications(mapInboxRows(content))
    } catch {
      setApplications([])
    } finally {
      setAppsLoading(false)
    }
  }, [inboxAllJobs, selectedJobId])

  useEffect(() => {
    fetchInboxThreads()
  }, [fetchInboxThreads])

  useEffect(() => {
    if (!applications.length) {
      setSelectedApplicationId(null)
      return
    }
    if (applicationIdFromUrl && applications.some((a) => String(a.id) === applicationIdFromUrl)) {
      setSelectedApplicationId(applicationIdFromUrl)
      return
    }
    setSelectedApplicationId((prev) => {
      if (prev && applications.some((a) => String(a.id) === String(prev))) return prev
      return applications[0]?.id ?? null
    })
  }, [applications, applicationIdFromUrl])

  useEffect(() => {
    const a = selectedApplicationId ? String(selectedApplicationId) : ''
    setSearchParams((prev) => {
      if (inboxAllJobs) {
        const n = new URLSearchParams()
        if (a) n.set('applicationId', a)
        return prev.toString() === n.toString() ? prev : n
      }
      if (!selectedJobId) return prev
      const j = String(selectedJobId)
      const n = new URLSearchParams()
      n.set('jobId', j)
      if (a) n.set('applicationId', a)
      return prev.toString() === n.toString() ? prev : n
    }, { replace: true })
  }, [inboxAllJobs, selectedJobId, selectedApplicationId, setSearchParams])

  function openThread(appId) {
    if (!appId) return
    setSelectedApplicationId(appId)
    setFocusChat(true)
  }

  function backToList() {
    setFocusChat(false)
  }

  function onJobChange(value) {
    if (value === ALL_JOBS) {
      setInboxAllJobs(true)
      setSelectedJobId(null)
    } else {
      setInboxAllJobs(false)
      setSelectedJobId(value || null)
    }
    setSelectedApplicationId(null)
    setListQuery('')
    setFocusChat(false)
  }

  const filtered = useMemo(() => {
    const q = debouncedListQuery.trim().toLowerCase()
    if (!q) return applications
    return applications.filter((a) => {
      const name = (a.candidateName || '').toLowerCase()
      const email = (a.candidateEmail || '').toLowerCase()
      const job = (a.jobTitle || '').toLowerCase()
      return name.includes(q) || email.includes(q) || job.includes(q)
    })
  }, [applications, debouncedListQuery])

  const selectedApp = applications.find((a) => String(a.id) === String(selectedApplicationId))

  const chatTitle = selectedApp
    ? `${selectedApp.candidateName || 'Ứng viên'} · ${selectedJob?.title || selectedApp.jobTitle || 'Tin tuyển dụng'}`
    : selectedJob?.title || 'Chat'

  const kanbanJobId = selectedApp?.jobId || selectedJobId

  return (
    <div className="flex h-[calc(100vh-3.5rem)] min-w-0 flex-col gap-0 overflow-hidden px-4 py-4 sm:px-6 lg:flex-row lg:px-8">
      <aside
        className={[
          'flex w-full max-w-full shrink-0 min-h-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900',
          'lg:w-80',
          focusChat ? 'hidden lg:flex' : 'flex',
        ].join(' ')}
      >
        <div className="border-b border-slate-200 p-4 dark:border-slate-800">
          <h1 className="text-lg font-bold text-slate-900 dark:text-white">Tin nhắn (HR)</h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Inbox theo tin tuyển dụng hoặc tất cả tin được phép — có xem trước tin nhắn cuối
          </p>
          <div className="mt-3">
            <label className="sr-only" htmlFor="hr-msg-job">
              Tin tuyển dụng
            </label>
            <div className="relative">
              <Briefcase className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <select
                id="hr-msg-job"
                value={inboxAllJobs ? ALL_JOBS : selectedJobId || ''}
                disabled={jobsLoading || !jobs.length}
                onChange={(e) => onJobChange(e.target.value)}
                className="w-full appearance-none rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
              >
                {jobsLoading ? (
                  <option>Đang tải tin...</option>
                ) : jobs.length === 0 ? (
                  <option value="">Chưa có tin tuyển dụng</option>
                ) : (
                  <>
                    <option value={ALL_JOBS}>Tất cả tin (được phép)</option>
                    {jobs.map((j) => (
                      <option key={j.id} value={j.id}>
                        {j.title || j.id}
                      </option>
                    ))}
                  </>
                )}
              </select>
            </div>
            {!inboxAllJobs && selectedJobId ? (
              <Link
                to={`/jobs/${selectedJobId}/kanban`}
                className="mt-2 inline-block text-xs font-medium text-[#2563eb] hover:underline"
              >
                Mở Kanban tin này
              </Link>
            ) : null}
          </div>
          {!appsLoading && applications.length > 0 ? (
            <div className="relative mt-3">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                value={listQuery}
                onChange={(e) => setListQuery(e.target.value)}
                placeholder="Tìm ứng viên hoặc tin…"
                className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-[#2563eb] dark:border-slate-700 dark:bg-slate-950"
              />
            </div>
          ) : null}
        </div>
        {appsLoading ? (
          <div className="p-4 text-sm text-slate-500">Đang tải hội thoại…</div>
        ) : !inboxAllJobs && !selectedJobId ? (
          <div className="p-4 text-sm text-slate-500">Chọn một tin tuyển dụng.</div>
        ) : applications.length === 0 ? (
          <div className="flex flex-1 flex-col items-center gap-2 p-6 text-center text-sm text-slate-500">
            <MessageCircle className="h-8 w-8 text-slate-300" />
            <p>Chưa có hồ sơ ứng tuyển trong phạm vi này.</p>
          </div>
        ) : (
          <ul className="min-h-0 flex-1 overflow-y-auto">
            {filtered.length === 0 ? (
              <li className="px-4 py-6 text-center text-sm text-slate-500">Không khớp từ khóa.</li>
            ) : (
              filtered.map((app) => {
                const active = String(app.id) === String(selectedApplicationId)
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
                      <div className="flex items-start justify-between gap-2">
                        <span className="font-medium text-slate-900 dark:text-white">
                          {app.candidateName || 'Ứng viên'}
                        </span>
                        {app.lastMessageAt ? (
                          <span className="shrink-0 text-[10px] text-slate-400">{formatShortTime(app.lastMessageAt)}</span>
                        ) : null}
                      </div>
                      {inboxAllJobs && app.jobTitle ? (
                        <span className="text-xs font-medium text-slate-600 dark:text-slate-300">{app.jobTitle}</span>
                      ) : null}
                      <span className="text-xs text-slate-500 dark:text-slate-400">{app.candidateEmail || '—'}</span>
                      <span className="text-xs text-slate-500 dark:text-slate-400">{statusShort(app.status)}</span>
                      {app.lastMessagePreview ? (
                        <span className="line-clamp-2 text-xs text-slate-500 dark:text-slate-400">{app.lastMessagePreview}</span>
                      ) : null}
                    </button>
                  </li>
                )
              })
            )}
          </ul>
        )}
      </aside>

      <section
        className={[
          'mt-4 flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-950/50',
          'lg:mt-0',
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
          {kanbanJobId ? (
            <Link
              to={`/jobs/${kanbanJobId}/kanban`}
              className="ml-auto text-xs font-medium text-[#2563eb] hover:underline"
            >
              Kanban
            </Link>
          ) : null}
        </div>
        <div className="hidden items-center justify-end border-b border-slate-200 bg-white px-3 py-1.5 dark:border-slate-800 dark:bg-slate-900 lg:flex">
          {kanbanJobId ? (
            <Link
              to={`/jobs/${kanbanJobId}/kanban`}
              className="text-xs font-medium text-[#2563eb] hover:underline"
            >
              Mở Kanban tin này
            </Link>
          ) : null}
        </div>
        <ApplicationChatPanel
          applicationId={selectedApplicationId}
          applicationTitle={chatTitle}
          className="min-h-0 flex-1 rounded-b-xl"
        />
      </section>
    </div>
  )
}
