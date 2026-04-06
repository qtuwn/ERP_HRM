import { useEffect, useMemo, useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { stripHtmlToText } from '../lib/jobText.js'
import { getUser, normalizeUserRole } from '../lib/storage.js'
import { ArrowLeft, Briefcase, MapPin, Banknote, ExternalLink } from 'lucide-react'

function skillsFrom(value) {
  if (!value) return []
  return String(value)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

function formatMoney(min, max, currency) {
  if (min == null && max == null) return null
  const cur = currency || 'VND'
  const fmt = (v) => {
    try {
      return new Intl.NumberFormat('vi-VN').format(v)
    } catch {
      return String(v)
    }
  }
  if (min != null && max != null) return `${fmt(min)} – ${fmt(max)} ${cur}`
  if (min != null) return `Từ ${fmt(min)} ${cur}`
  return `Đến ${fmt(max)} ${cur}`
}

async function fetchRelatedJobs(currentJob) {
  if (!currentJob?.id) return []

  const tryQueries = []
  if (currentJob.industry && currentJob.city) {
    tryQueries.push({ industry: currentJob.industry, city: currentJob.city })
  }
  if (currentJob.industry) tryQueries.push({ industry: currentJob.industry })
  if (currentJob.city) tryQueries.push({ city: currentJob.city })
  tryQueries.push({})

  for (const q of tryQueries) {
    const p = new URLSearchParams({ page: '0', size: '35', sort: 'createdAt,desc' })
    if (q.industry) p.set('industry', q.industry)
    if (q.city) p.set('city', q.city)
    try {
      const res = await api.get(`/api/jobs?${p.toString()}`)
      const content = Array.isArray(res?.data?.content) ? res.data.content : []
      const filtered = content.filter((j) => j.id !== currentJob.id)
      if (filtered.length > 0) return filtered.slice(0, 24)
    } catch {
      /* thử bộ lọc tiếp theo */
    }
  }
  return []
}

function RelatedJobCard({ j, currentId }) {
  const active = String(j.id) === String(currentId)
  const money = formatMoney(j.salaryMin, j.salaryMax, j.salaryCurrency)
  const skills = skillsFrom(j.requiredSkills).slice(0, 5)
  const snippet = stripHtmlToText(j.description || j.requirements, 100)

  const inner = (
    <>
      <div className="flex items-start justify-between gap-2">
        <h3 className="line-clamp-2 text-sm font-bold leading-snug text-slate-900 dark:text-white">{j.title}</h3>
      </div>
      <p className="mt-1 text-xs font-medium text-slate-600 dark:text-slate-300">{j.companyName || 'Doanh nghiệp'}</p>
      {money ? (
        <p className="mt-1.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400">{money}</p>
      ) : null}
      <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-slate-500 dark:text-slate-400">
        {j.jobType ? <span>{j.jobType}</span> : null}
        {j.city ? (
          <span className="inline-flex items-center gap-0.5">
            <MapPin className="h-3 w-3 shrink-0" />
            {j.city}
          </span>
        ) : null}
      </div>
      {snippet ? <p className="mt-2 line-clamp-2 text-[11px] leading-relaxed text-slate-500 dark:text-slate-400">{snippet}</p> : null}
      {skills.length > 0 ? (
        <div className="mt-2 flex flex-wrap gap-1">
          {skills.map((s) => (
            <span
              key={s}
              className="rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300"
            >
              {s}
            </span>
          ))}
        </div>
      ) : null}
    </>
  )

  const cardClass = [
    'block rounded-xl border p-4 transition-shadow',
    active
      ? 'border-[#2563eb] bg-blue-50/80 shadow-md ring-1 ring-[#2563eb]/25 dark:border-[#2563eb] dark:bg-blue-950/30 dark:ring-[#2563eb]/30'
      : 'border-slate-200 bg-white hover:border-slate-300 hover:shadow-md dark:border-slate-700 dark:bg-slate-900 dark:hover:border-slate-600',
  ].join(' ')

  if (active) {
    return <div className={cardClass}>{inner}</div>
  }

  return (
    <NavLink to={`/jobs/${j.id}`} className={cardClass}>
      {inner}
    </NavLink>
  )
}

export function JobDetailPage() {
  const { id } = useParams()
  const user = useMemo(() => getUser(), [])

  const [loading, setLoading] = useState(true)
  const [job, setJob] = useState(null)
  const [related, setRelated] = useState([])
  const [relatedLoading, setRelatedLoading] = useState(false)

  useEffect(() => {
    let mounted = true
    ;(async () => {
      try {
        setLoading(true)
        const res = await api.get(`/api/jobs/${id}`)
        if (mounted) setJob(res?.data || null)
      } catch {
        if (mounted) setJob(null)
      } finally {
        if (mounted) setLoading(false)
      }
    })()
    return () => {
      mounted = false
    }
  }, [id])

  useEffect(() => {
    if (!job?.id) {
      setRelated([])
      return
    }
    let mounted = true
    setRelatedLoading(true)
    ;(async () => {
      const list = await fetchRelatedJobs(job)
      if (mounted) {
        setRelated(list)
        setRelatedLoading(false)
      }
    })()
    return () => {
      mounted = false
    }
  }, [job])

  const role = normalizeUserRole(user?.role)
  const isCandidate = role === 'CANDIDATE'
  const applyNext = `/jobs/${id}/apply`
  const skills = skillsFrom(job?.requiredSkills)
  const money = job ? formatMoney(job.salaryMin, job.salaryMax, job.salaryCurrency) : null

  if (loading) {
    return (
      <section className="min-h-screen bg-slate-100/90 dark:bg-slate-950">
        <div className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
          <div className="flex flex-col gap-6 lg:flex-row">
            <div className="h-[420px] w-full animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800 lg:w-[380px]" />
            <div className="min-h-[480px] flex-1 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
          </div>
        </div>
      </section>
    )
  }

  if (!job) {
    return (
      <section className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
        <div className="rounded-xl border border-slate-200 bg-white py-16 text-center dark:border-slate-800 dark:bg-slate-900">
          <h2 className="text-2xl font-bold text-slate-800 dark:text-white">Không tìm thấy tin tuyển dụng</h2>
          <p className="mt-2 text-slate-500 dark:text-slate-400">Tin có thể đã đóng hoặc không tồn tại.</p>
          <Link to="/jobs" className="mt-6 inline-block font-semibold text-[#2563eb] hover:underline">
            ← Về danh sách việc làm
          </Link>
        </div>
      </section>
    )
  }

  return (
    <section className="min-h-screen bg-slate-100/90 dark:bg-slate-950">
      <div className="mx-auto max-w-[1400px] px-4 py-6 sm:px-6 lg:py-8">
        <Link
          to="/jobs"
          className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-[#2563eb] dark:text-slate-400 dark:hover:text-blue-400"
        >
          <ArrowLeft className="h-4 w-4" />
          Tất cả việc làm
        </Link>

        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8">
          {/* Cột trái: việc liên quan (ITviec-style) */}
          <aside className="w-full shrink-0 lg:w-[380px] lg:max-h-[calc(100vh-5.5rem)] lg:overflow-y-auto lg:pr-1">
            <h2 className="mb-3 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Việc liên quan
            </h2>
            <div className="flex flex-col gap-3">
              <RelatedJobCard j={job} currentId={id} />
              {relatedLoading ? (
                <div className="space-y-3">
                  {[1, 2, 3].map((k) => (
                    <div key={k} className="h-28 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
                  ))}
                </div>
              ) : (
                related.map((j) => <RelatedJobCard key={j.id} j={j} currentId={id} />)
              )}
              {!relatedLoading && related.length === 0 ? (
                <p className="text-xs text-slate-500 dark:text-slate-400">Chưa có tin liên quan khác trong cùng ngành/địa điểm.</p>
              ) : null}
            </div>
          </aside>

          {/* Cột phải: JD chi tiết */}
          <main className="min-w-0 flex-1">
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="border-b border-slate-100 bg-slate-50 px-6 py-6 dark:border-slate-800 dark:bg-slate-900 sm:px-8">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <div className="mb-2 flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                      {job.industry ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-[#2563eb]/10 px-2.5 py-0.5 font-semibold text-[#2563eb] dark:bg-blue-950/50 dark:text-blue-300">
                          {job.industry}
                        </span>
                      ) : null}
                      {job.level ? <span>{job.level}</span> : null}
                      {job.jobType ? <span>· {job.jobType}</span> : null}
                    </div>
                    <h1 className="text-2xl font-bold leading-tight text-slate-900 dark:text-white sm:text-3xl">{job.title}</h1>
                    <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-600 dark:text-slate-300">
                      <span className="inline-flex items-center gap-1.5 font-medium">
                        <Briefcase className="h-4 w-4 text-slate-400" />
                        {job.companyName || 'Doanh nghiệp'}
                      </span>
                      {job.city ? (
                        <span className="inline-flex items-center gap-1 text-slate-500 dark:text-slate-400">
                          <MapPin className="h-4 w-4" />
                          {job.city}
                        </span>
                      ) : null}
                    </div>
                    {money ? (
                      <p className="mt-3 inline-flex items-center gap-1.5 text-lg font-bold text-emerald-600 dark:text-emerald-400">
                        <Banknote className="h-5 w-5 opacity-80" />
                        {money}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex w-full shrink-0 flex-col gap-2 sm:w-auto sm:min-w-[200px]">
                    {isCandidate ? (
                      <Link
                        to={applyNext}
                        className="inline-flex w-full items-center justify-center rounded-xl bg-[#2563eb] px-6 py-3.5 text-center text-sm font-bold text-white shadow-lg shadow-blue-500/25 transition hover:bg-[#1d4ed8] dark:shadow-blue-900/40"
                      >
                        Ứng tuyển ngay
                      </Link>
                    ) : user ? (
                      <p className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-center text-xs text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                        Chỉ tài khoản <span className="font-semibold">ứng viên</span> nộp hồ sơ trên hệ thống.
                      </p>
                    ) : (
                      <Link
                        to={`/login?next=${encodeURIComponent(applyNext)}`}
                        className="inline-flex w-full items-center justify-center rounded-xl bg-[#2563eb] px-6 py-3.5 text-center text-sm font-bold text-white shadow-lg shadow-blue-500/25 transition hover:bg-[#1d4ed8]"
                      >
                        Đăng nhập để ứng tuyển
                      </Link>
                    )}
                  </div>
                </div>
              </div>

              <div className="space-y-8 px-6 py-8 sm:px-8">
                <div className="flex flex-wrap gap-4 border-b border-slate-100 pb-6 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-300">
                  {job.expiresAt ? (
                    <span>
                      <span className="font-semibold text-slate-800 dark:text-slate-200">Hạn nộp:</span>{' '}
                      {new Date(job.expiresAt).toLocaleDateString('vi-VN')}
                    </span>
                  ) : null}
                  <span>
                    <span className="font-semibold text-slate-800 dark:text-slate-200">Trạng thái tin:</span>{' '}
                    {job.status === 'OPEN' ? (
                      <span className="text-emerald-600 dark:text-emerald-400">Đang tuyển</span>
                    ) : (
                      job.status
                    )}
                  </span>
                  {job.notificationEmail ? (
                    <a href={`mailto:${job.notificationEmail}`} className="inline-flex items-center gap-1 text-[#2563eb] hover:underline">
                      Liên hệ tuyển dụng
                      <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  ) : null}
                </div>

                {skills.length > 0 ? (
                  <div>
                    <h2 className="mb-3 text-lg font-bold text-slate-900 dark:text-white">Kỹ năng</h2>
                    <div className="flex flex-wrap gap-2">
                      {skills.map((s) => (
                        <span
                          key={s}
                          className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm font-medium text-slate-700 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200"
                        >
                          {s}
                        </span>
                      ))}
                    </div>
                  </div>
                ) : null}

                <div>
                  <h2 className="mb-4 text-lg font-bold text-slate-900 dark:text-white">Mô tả công việc</h2>
                  <div
                    className="prose prose-slate max-w-none dark:prose-invert prose-headings:font-semibold prose-p:text-slate-600 dark:prose-p:text-slate-300 prose-li:text-slate-600 dark:prose-li:text-slate-300"
                    dangerouslySetInnerHTML={{ __html: job.description || '<p>Đang cập nhật.</p>' }}
                  />
                </div>

                {job.requirements ? (
                  <div>
                    <h2 className="mb-4 text-lg font-bold text-slate-900 dark:text-white">Yêu cầu ứng viên</h2>
                    <div
                      className="prose prose-slate max-w-none dark:prose-invert prose-headings:font-semibold prose-p:text-slate-600 dark:prose-p:text-slate-300"
                      dangerouslySetInnerHTML={{ __html: job.requirements }}
                    />
                  </div>
                ) : null}

                {job.benefits ? (
                  <div>
                    <h2 className="mb-4 text-lg font-bold text-slate-900 dark:text-white">Quyền lợi</h2>
                    <div
                      className="prose prose-slate max-w-none dark:prose-invert prose-p:text-slate-600 dark:prose-p:text-slate-300"
                      dangerouslySetInnerHTML={{ __html: job.benefits }}
                    />
                  </div>
                ) : null}

                <div className="border-t border-slate-100 pt-8 dark:border-slate-800">
                  {isCandidate ? (
                    <Link
                      to={applyNext}
                      className="inline-flex w-full items-center justify-center rounded-xl bg-[#2563eb] px-6 py-4 text-base font-bold text-white shadow-md transition hover:bg-[#1d4ed8] sm:w-auto sm:min-w-[240px]"
                    >
                      Chuyển đến form ứng tuyển
                    </Link>
                  ) : !user ? (
                    <Link
                      to={`/login?next=${encodeURIComponent(applyNext)}`}
                      className="inline-flex w-full items-center justify-center rounded-xl bg-[#2563eb] px-6 py-4 text-base font-bold text-white shadow-md transition hover:bg-[#1d4ed8] sm:w-auto sm:min-w-[240px]"
                    >
                      Đăng nhập để ứng tuyển
                    </Link>
                  ) : null}
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>
    </section>
  )
}
