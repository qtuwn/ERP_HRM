import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { ApplicationChatPanel } from '../components/ApplicationChatPanel.jsx'
import { ArrowLeft, Briefcase, MessageCircle, Search } from 'lucide-react'

function statusShort(s) {
  const x = String(s || '')
  if (x === 'REJECTED') return 'Từ chối'
  if (x === 'HIRED') return 'Đã nhận'
  if (x === 'OFFER') return 'Offer'
  if (x === 'INTERVIEW') return 'Phỏng vấn'
  if (x === 'HR_REVIEW') return 'HR duyệt'
  return x === 'APPLIED' ? 'Đã nộp' : x || '—'
}

export function RecruiterMessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const jobIdFromUrl = searchParams.get('jobId')
  const applicationIdFromUrl = searchParams.get('applicationId')

  const [jobs, setJobs] = useState([])
  const [jobsLoading, setJobsLoading] = useState(true)
  const [selectedJobId, setSelectedJobId] = useState(null)

  const [applications, setApplications] = useState([])
  const [appsLoading, setAppsLoading] = useState(false)
  const [selectedApplicationId, setSelectedApplicationId] = useState(null)
  const [listQuery, setListQuery] = useState('')
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
      setSelectedJobId(jobIdFromUrl)
      return
    }
    setSelectedJobId((prev) => {
      if (prev && jobs.some((j) => String(j.id) === String(prev))) return prev
      return jobs[0]?.id ?? null
    })
  }, [jobs, jobIdFromUrl])

  const fetchKanban = useCallback(async (jobId) => {
    if (!jobId) {
      setApplications([])
      return
    }
    setAppsLoading(true)
    try {
      const res = await api.get(`/api/jobs/${jobId}/applications/kanban`)
      setApplications(Array.isArray(res?.data) ? res.data : [])
    } catch {
      setApplications([])
    } finally {
      setAppsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!selectedJobId) {
      setApplications([])
      setSelectedApplicationId(null)
      return
    }
    fetchKanban(selectedJobId)
  }, [selectedJobId, fetchKanban])

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
    if (!selectedJobId) return
    const j = String(selectedJobId)
    const a = selectedApplicationId ? String(selectedApplicationId) : ''
    setSearchParams(
      (prev) => {
        if (prev.get('jobId') === j && (a ? prev.get('applicationId') === a : !prev.get('applicationId'))) {
          return prev
        }
        const n = new URLSearchParams()
        n.set('jobId', j)
        if (a) n.set('applicationId', a)
        return n
      },
      { replace: true }
    )
  }, [selectedJobId, selectedApplicationId, setSearchParams])

  function openThread(appId) {
    if (!appId) return
    setSelectedApplicationId(appId)
    setFocusChat(true)
  }

  function backToList() {
    setFocusChat(false)
  }

  function onJobChange(jobId) {
    setSelectedJobId(jobId)
    setSelectedApplicationId(null)
    setListQuery('')
    setFocusChat(false)
  }

  const filtered = useMemo(() => {
    const q = listQuery.trim().toLowerCase()
    if (!q) return applications
    return applications.filter((a) => {
      const name = (a.candidateName || '').toLowerCase()
      const email = (a.candidateEmail || '').toLowerCase()
      return name.includes(q) || email.includes(q)
    })
  }, [applications, listQuery])

  const selectedApp = applications.find((a) => String(a.id) === String(selectedApplicationId))

  const chatTitle = selectedApp
    ? `${selectedApp.candidateName || 'Ứng viên'} · ${selectedJob?.title || 'Tin tuyển dụng'}`
    : selectedJob?.title || 'Chat'

  return (
    <div className="flex min-h-[calc(100vh-3.5rem)] flex-col gap-0 px-4 py-4 sm:px-6 lg:flex-row lg:px-8">
      <aside
        className={[
          'flex w-full max-w-full shrink-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900',
          'lg:w-80',
          focusChat ? 'hidden lg:flex' : 'flex',
        ].join(' ')}
      >
        <div className="border-b border-slate-200 p-4 dark:border-slate-800">
          <h1 className="text-lg font-bold text-slate-900 dark:text-white">Tin nhắn (HR)</h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">Chọn tin tuyển dụng, sau đó chọn ứng viên</p>
          <div className="mt-3">
            <label className="sr-only" htmlFor="hr-msg-job">
              Tin tuyển dụng
            </label>
            <div className="relative">
              <Briefcase className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <select
                id="hr-msg-job"
                value={selectedJobId || ''}
                disabled={jobsLoading || !jobs.length}
                onChange={(e) => onJobChange(e.target.value || null)}
                className="w-full appearance-none rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
              >
                {jobsLoading ? (
                  <option>Đang tải tin...</option>
                ) : jobs.length === 0 ? (
                  <option value="">Chưa có tin tuyển dụng</option>
                ) : (
                  jobs.map((j) => (
                    <option key={j.id} value={j.id}>
                      {j.title || j.id}
                    </option>
                  ))
                )}
              </select>
            </div>
            {selectedJobId ? (
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
                placeholder="Tìm ứng viên…"
                className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-[#2563eb] dark:border-slate-700 dark:bg-slate-950"
              />
            </div>
          ) : null}
        </div>
        {appsLoading ? (
          <div className="p-4 text-sm text-slate-500">Đang tải ứng viên…</div>
        ) : !selectedJobId ? (
          <div className="p-4 text-sm text-slate-500">Chọn một tin tuyển dụng.</div>
        ) : applications.length === 0 ? (
          <div className="flex flex-1 flex-col items-center gap-2 p-6 text-center text-sm text-slate-500">
            <MessageCircle className="h-8 w-8 text-slate-300" />
            <p>Chưa có hồ sơ ứng tuyển.</p>
          </div>
        ) : (
          <ul className="max-h-[55vh] overflow-y-auto lg:max-h-none lg:flex-1">
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
                      <span className="font-medium text-slate-900 dark:text-white">
                        {app.candidateName || 'Ứng viên'}
                      </span>
                      <span className="text-xs text-slate-500 dark:text-slate-400">{app.candidateEmail || '—'}</span>
                      <span className="text-xs text-slate-500 dark:text-slate-400">{statusShort(app.status)}</span>
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
          'mt-4 flex min-h-[420px] min-w-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-950/50',
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
        </div>
        <ApplicationChatPanel
          applicationId={selectedApplicationId}
          applicationTitle={chatTitle}
          className="min-h-0 min-h-[360px] flex-1 rounded-b-xl"
        />
      </section>
    </div>
  )
}
