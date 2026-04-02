import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { MapPin, Search } from 'lucide-react'
import { api } from '../lib/api.js'
import { stripHtmlToText } from '../lib/jobText.js'
import { getUser } from '../lib/storage.js'

const PAGE_SIZE = 9
const SECTION_SIZE = 6

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
  if (min != null && max != null) return `${fmt(min)} - ${fmt(max)} ${cur}`
  if (min != null) return `Từ ${fmt(min)} ${cur}`
  return `Đến ${fmt(max)} ${cur}`
}

function buildFilterParams(q, city, industry, jobType, level, skill) {
  const p = new URLSearchParams()
  if (q) p.set('q', q)
  if (city) p.set('city', city)
  if (industry) p.set('industry', industry)
  if (jobType) p.set('jobType', jobType)
  if (level) p.set('level', level)
  if (skill) p.set('skill', skill)
  return p
}

async function fetchJobPage(searchString) {
  const res = await api.get(`/api/jobs?${searchString}`)
  return res?.data ?? null
}

function scoreJob(j, q, industry) {
  let s = 0
  const blob = `${j.title || ''} ${j.description || ''} ${j.requirements || ''} ${j.requiredSkills || ''}`.toLowerCase()
  const iq = (q || '').trim().toLowerCase()
  if (iq) {
    for (const part of iq.split(/\s+/).filter(Boolean)) {
      if (part.length > 1 && blob.includes(part)) s += 2
    }
  }
  if (industry && String(j.industry || '').toLowerCase() === industry.toLowerCase()) s += 3
  if (j.city) s += 0.05
  return s
}

function pickSuitable(jobs, q, industry) {
  if (!jobs?.length) return []
  return [...jobs]
    .sort((a, b) => scoreJob(b, q, industry) - scoreJob(a, q, industry))
    .slice(0, SECTION_SIZE)
}

function JobCard({ job }) {
  const money = formatMoney(job.salaryMin, job.salaryMax, job.salaryCurrency)
  const desc =
    stripHtmlToText(job.description) ||
    stripHtmlToText(job.requirements) ||
    'Mô tả công việc đang được cập nhật.'
  const tag = job.industry || job.department || 'Việc làm'

  return (
    <article className="flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-6 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-sm text-slate-600 dark:text-slate-300">{job.companyName || 'Doanh nghiệp'}</div>
          <div className="mt-1 font-semibold leading-snug text-slate-900 dark:text-white">{job.title}</div>
        </div>
        <span className="inline-flex shrink-0 rounded-full bg-[#2563eb]/12 px-2.5 py-1 text-xs font-semibold text-[#2563eb]">
          {tag}
        </span>
      </div>

      <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-700 dark:text-slate-200">{desc}</p>

      <div className="mt-4 flex flex-wrap gap-2">
        {money ? (
          <span className="inline-flex items-center rounded-lg bg-[#2563eb]/10 px-2.5 py-1 text-xs font-semibold text-[#2563eb]">
            {money}
          </span>
        ) : null}
        {job.city ? <div className="text-xs font-medium text-slate-500 dark:text-slate-400">📍 {job.city}</div> : null}
        {job.jobType ? <div className="text-xs font-medium text-slate-500 dark:text-slate-400">💼 {job.jobType}</div> : null}
      </div>

      <div className="mt-auto space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800">
        {job.level ? <div className="text-xs text-slate-500 dark:text-slate-400">Cấp: {job.level}</div> : null}
        <div className="flex items-center justify-between gap-2">
          <Link
            to={`/jobs/${job.id}`}
            className="inline-flex items-center justify-center rounded-lg border border-[#2563eb] px-3 py-2 text-sm font-semibold text-[#2563eb] transition-colors hover:bg-[#2563eb]/10"
          >
            Chi tiết
          </Link>
          <Link
            to={`/jobs/${job.id}/apply`}
            className="inline-flex items-center justify-center rounded-lg bg-[#2563eb] px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#1d4ed8]"
          >
            Ứng tuyển nhanh
          </Link>
        </div>
      </div>
    </article>
  )
}

function SectionBlock({ title, subtitle, jobs, loading }) {
  return (
    <section className="space-y-4">
      <div>
        <h2 className="text-lg font-bold text-slate-900 dark:text-white sm:text-xl">{title}</h2>
        {subtitle ? <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{subtitle}</p> : null}
      </div>
      {loading ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="h-48 animate-pulse rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
      ) : jobs.length === 0 ? (
        <p className="text-sm text-slate-500 dark:text-slate-400">Chưa có tin phù hợp mục này.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {jobs.map((j) => (
            <JobCard key={j.id} job={j} />
          ))}
        </div>
      )}
    </section>
  )
}

export function JobsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const qUrl = searchParams.get('q') || ''
  const city = searchParams.get('city') || ''
  const industry = searchParams.get('industry') || ''
  const jobType = searchParams.get('jobType') || ''
  const level = searchParams.get('level') || ''
  const skill = searchParams.get('skill') || ''

  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState(qUrl)

  useEffect(() => {
    setSearchInput(qUrl)
  }, [qUrl])

  const [debouncedQ, setDebouncedQ] = useState(qUrl)
  useEffect(() => {
    const t = setTimeout(() => {
      const next = searchInput.trim()
      setDebouncedQ((prev) => {
        if (prev !== next) queueMicrotask(() => setPage(0))
        return next
      })
    }, 350)
    return () => clearTimeout(t)
  }, [searchInput])

  useEffect(() => {
    setSearchParams(
      (prev) => {
        const n = new URLSearchParams(prev)
        if (debouncedQ) n.set('q', debouncedQ)
        else n.delete('q')
        return n
      },
      { replace: true }
    )
  }, [debouncedQ, setSearchParams])

  const filterKey = useMemo(
    () => ({ debouncedQ, city, industry, jobType, level, skill }),
    [debouncedQ, city, industry, jobType, level, skill]
  )

  const commonBase = useMemo(
    () => buildFilterParams(debouncedQ, city, industry, jobType, level, skill),
    [debouncedQ, city, industry, jobType, level, skill]
  )

  const { data: filterOpts } = useQuery({
    queryKey: ['job-filter-options'],
    queryFn: async () => {
      const j = await api.get('/api/jobs/filter-options')
      return j?.data ?? {}
    },
    staleTime: 5 * 60_000,
  })

  const cities = Array.isArray(filterOpts?.cities) ? filterOpts.cities.filter(Boolean) : []
  const industries = Array.isArray(filterOpts?.industries) ? filterOpts.industries.filter(Boolean) : []
  const jobTypes = Array.isArray(filterOpts?.jobTypes) ? filterOpts.jobTypes.filter(Boolean) : []
  const levels = Array.isArray(filterOpts?.levels) ? filterOpts.levels.filter(Boolean) : []

  const latestQuery = useQuery({
    queryKey: ['public-jobs-latest', filterKey],
    queryFn: () => {
      const p = new URLSearchParams(commonBase)
      p.set('page', '0')
      p.set('size', String(SECTION_SIZE))
      p.set('sort', 'createdAt,desc')
      return fetchJobPage(p.toString())
    },
  })

  const attractiveQuery = useQuery({
    queryKey: ['public-jobs-attractive', filterKey],
    queryFn: () => {
      const p = new URLSearchParams(commonBase)
      p.set('page', '0')
      p.set('size', String(SECTION_SIZE))
      p.set('sort', 'salaryMax,desc')
      return fetchJobPage(p.toString())
    },
  })

  const suitableSourceQuery = useQuery({
    queryKey: ['public-jobs-suitable-src', filterKey],
    queryFn: () => {
      const p = new URLSearchParams(commonBase)
      p.set('page', '0')
      p.set('size', '36')
      p.set('sort', 'createdAt,desc')
      return fetchJobPage(p.toString())
    },
    select: (data) => {
      if (!data?.content) return { ...data, content: [] }
      return { ...data, content: pickSuitable(data.content, debouncedQ, industry) }
    },
  })

  const {
    data: pageData,
    isLoading: mainLoading,
    isFetching: mainFetching,
    isError: mainError,
    error: mainErr,
  } = useQuery({
    queryKey: ['public-jobs-main', page, filterKey, PAGE_SIZE],
    queryFn: () => {
      const p = new URLSearchParams(commonBase)
      p.set('page', String(page))
      p.set('size', String(PAGE_SIZE))
      p.set('sort', 'createdAt,desc')
      return fetchJobPage(p.toString())
    },
  })

  const jobs = pageData?.content || []
  const totalPages = typeof pageData?.totalPages === 'number' ? pageData.totalPages : 0
  const totalElements = typeof pageData?.totalElements === 'number' ? pageData.totalElements : jobs.length

  const user = getUser()
  const displayName = user?.fullName?.trim() || user?.email?.split('@')[0] || ''

  function setParam(key, value) {
    setPage(0)
    setSearchParams(
      (prev) => {
        const n = new URLSearchParams(prev)
        if (value) n.set(key, value)
        else n.delete(key)
        return n
      },
      { replace: true }
    )
  }

  function clearFilters() {
    setPage(0)
    setSearchInput('')
    setDebouncedQ('')
    setSearchParams({}, { replace: true })
  }

  const pagination = (
    <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
      <button
        type="button"
        disabled={page <= 0 || mainFetching}
        onClick={() => setPage(0)}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Đầu
      </button>
      <button
        type="button"
        disabled={page <= 0 || mainFetching}
        onClick={() => setPage((p) => Math.max(0, p - 1))}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Trước
      </button>
      <span className="px-2 text-sm text-slate-600 dark:text-slate-400">
        {page + 1} / {Math.max(totalPages, 1)}
      </span>
      <button
        type="button"
        disabled={mainFetching || totalPages === 0 || page >= totalPages - 1}
        onClick={() => setPage((p) => p + 1)}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Sau
      </button>
      <button
        type="button"
        disabled={mainFetching || totalPages === 0 || page >= totalPages - 1}
        onClick={() => setPage(Math.max(0, totalPages - 1))}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Cuối
      </button>
    </div>
  )

  const hero = (
    <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-br from-slate-900 via-[#1e3a5f] to-[#2563eb] text-white shadow-xl">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_80%_20%,rgba(255,255,255,0.12),transparent_50%),radial-gradient(circle_at_20%_80%,rgba(255,255,255,0.08),transparent_45%)]" />
      <div className="relative p-8 sm:p-10 lg:p-12">
        <div className="mb-3 text-xs font-semibold uppercase tracking-[0.18em] text-white/80">VTHR Careers Hub</div>
        <h1 className="text-3xl font-bold leading-tight sm:text-4xl lg:text-5xl">
          {displayName
            ? `Việc làm dành cho bạn, ${displayName}`
            : 'Khám phá việc làm phù hợp kỹ năng và mục tiêu của bạn'}
        </h1>
        <p className="mt-4 max-w-3xl text-sm leading-relaxed text-white/90 sm:text-base">
          Tìm theo từ khóa, địa điểm, ngành và loại hình — lọc kết hợp nhiều tiêu chí (backend dùng truy vấn JPQL/SQL; gợi ý
          &quot;Phù hợp&quot; trên trang tính điểm nhẹ theo từ khóa và ngành bạn chọn).
        </p>

        <div className="mt-8 flex flex-col gap-3 rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur sm:flex-row sm:items-stretch">
          <div className="flex min-w-0 shrink-0 items-center gap-2 rounded-xl border border-white/20 bg-white/5 px-3 py-2 sm:max-w-[200px]">
            <MapPin className="h-4 w-4 shrink-0 text-white/70" />
            <select
              value={city}
              onChange={(e) => setParam('city', e.target.value)}
              className="w-full min-w-0 bg-transparent text-sm text-white outline-none *:bg-slate-900 *:text-white"
            >
              <option value="">Mọi địa điểm</option>
              {cities.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="min-w-0 flex-1">
            <label htmlFor="job-search" className="sr-only">
              Tìm việc
            </label>
            <input
              id="job-search"
              type="search"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Kỹ năng, chức danh, công ty…"
              className="h-full w-full rounded-xl border border-white/20 bg-white/10 px-4 py-3 text-sm text-white placeholder:text-white/55 focus:border-white/40 focus:outline-none focus:ring-2 focus:ring-white/25"
            />
          </div>
          <button
            type="button"
            onClick={() => setDebouncedQ(searchInput.trim())}
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-[#ef4444] px-6 py-3 text-sm font-bold text-white shadow-lg shadow-red-900/30 transition hover:bg-[#dc2626]"
          >
            <Search className="h-4 w-4" />
            Tìm kiếm
          </button>
        </div>
      </div>
    </div>
  )

  const filterBar = (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-6">
      <div className="flex flex-wrap items-end gap-3">
        <label className="flex min-w-[140px] flex-1 flex-col gap-1 text-xs font-medium text-slate-600 dark:text-slate-400">
          Ngành / danh mục
          <select
            value={industry}
            onChange={(e) => setParam('industry', e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
          >
            <option value="">Tất cả</option>
            {industries.map((ind) => (
              <option key={ind} value={ind}>
                {ind}
              </option>
            ))}
          </select>
        </label>
        <label className="flex min-w-[140px] flex-1 flex-col gap-1 text-xs font-medium text-slate-600 dark:text-slate-400">
          Loại hình
          <select
            value={jobType}
            onChange={(e) => setParam('jobType', e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
          >
            <option value="">Tất cả</option>
            {jobTypes.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label className="flex min-w-[140px] flex-1 flex-col gap-1 text-xs font-medium text-slate-600 dark:text-slate-400">
          Cấp bậc
          <select
            value={level}
            onChange={(e) => setParam('level', e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
          >
            <option value="">Tất cả</option>
            {levels.map((lv) => (
              <option key={lv} value={lv}>
                {lv}
              </option>
            ))}
          </select>
        </label>
        <label className="flex min-w-[160px] flex-[2] flex-col gap-1 text-xs font-medium text-slate-600 dark:text-slate-400">
          Kỹ năng (chuỗi con trong yêu cầu)
          <input
            type="text"
            value={skill}
            onChange={(e) => setParam('skill', e.target.value)}
            placeholder="VD: Java, React…"
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
          />
        </label>
        <button
          type="button"
          onClick={clearFilters}
          className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
        >
          Xóa bộ lọc
        </button>
      </div>
    </div>
  )

  const headerBlock = (
    <div className="space-y-6 py-10 lg:space-y-8 lg:py-14">
      {hero}
      {filterBar}
    </div>
  )

  if (mainLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-8 px-4 sm:px-6">
        {headerBlock}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div
              key={idx}
              className="h-40 animate-pulse rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
      </div>
    )
  }

  if (mainError) {
    return (
      <div className="mx-auto max-w-7xl space-y-6 px-4 sm:px-6">
        {headerBlock}
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-200">
          {mainErr?.message || 'Không tải được danh sách việc làm.'}
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl space-y-12 px-4 pb-16 sm:px-6">
      {headerBlock}

      <SectionBlock
        title="Việc làm mới nhất"
        subtitle="Đăng tải gần đây, sắp xếp theo thời gian tạo."
        jobs={latestQuery.data?.content || []}
        loading={latestQuery.isLoading}
      />

      <SectionBlock
        title="Việc làm phù hợp"
        subtitle="Gợi ý theo từ khóa tìm kiếm và ngành bạn chọn (điểm số trên tập tin gần đây)."
        jobs={suitableSourceQuery.data?.content || []}
        loading={suitableSourceQuery.isLoading}
      />

      <SectionBlock
        title="Việc làm hấp dẫn"
        subtitle="Ưu tiên mức lương tối đa (theo dữ liệu tin đăng)."
        jobs={attractiveQuery.data?.content || []}
        loading={attractiveQuery.isLoading}
      />

      <section className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white sm:text-2xl">Tất cả việc làm</h2>
          <p className="text-sm text-slate-600 dark:text-slate-300">
            <span className="font-semibold">{totalElements}</span> vị trí
            {totalPages > 1 ? (
              <span className="text-slate-500 dark:text-slate-400">
                {' '}
                · Trang {page + 1}/{Math.max(totalPages, 1)}
              </span>
            ) : null}
            {mainFetching ? <span className="ml-2 text-xs font-normal text-slate-400">· Đang cập nhật…</span> : null}
          </p>
        </div>
      </section>

      {jobs.length === 0 ? (
        <p className="text-center text-sm text-slate-500 dark:text-slate-400">Không có tin tuyển dụng phù hợp bộ lọc.</p>
      ) : (
        <div className="grid grid-cols-1 gap-7 md:grid-cols-2 xl:grid-cols-3">
          {jobs.map((j) => (
            <JobCard key={j.id} job={j} />
          ))}
        </div>
      )}
      {totalPages > 1 ? pagination : null}
    </div>
  )
}
