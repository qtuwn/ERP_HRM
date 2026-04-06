import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { ArrowRight, Banknote, Briefcase, Building2, MapPin, Search } from 'lucide-react'
import { api } from '../lib/api.js'
import { stripHtmlToText } from '../lib/jobText.js'
import { getUser } from '../lib/storage.js'

const PAGE_SIZE = 10
const SECTION_SIZE = 6

function formatSalaryLabel(min, max, currency) {
  if (min == null && max == null) return null
  const cur = (currency || 'VND').toUpperCase() === 'VND' ? 'vnđ' : currency || ''
  const fmt = (v) => {
    try {
      return new Intl.NumberFormat('vi-VN').format(v)
    } catch {
      return String(v)
    }
  }
  if (min != null && max != null) return `${fmt(min)} – ${fmt(max)} ${cur}`
  if (max != null) return `Lên đến ${fmt(max)} ${cur}`
  return `Từ ${fmt(min)} ${cur}`
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

async function fetchJobFeedKeyset(searchString) {
  const res = await api.get(`/api/jobs/feed?${searchString}`)
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

function jobAccentBadge(job) {
  const created = job.createdAt ? new Date(job.createdAt) : null
  const days = created && !Number.isNaN(created.getTime()) ? (Date.now() - created.getTime()) / 86_400_000 : 999
  if (days <= 5) {
    return {
      text: 'Mới',
      className:
        'border border-amber-200/80 bg-amber-50 text-amber-800 dark:border-amber-800/50 dark:bg-amber-950/50 dark:text-amber-200',
    }
  }
  if (job.salaryMax != null && job.salaryMax >= 25_000_000) {
    return {
      text: 'HOT',
      className:
        'border border-[#2563eb]/30 bg-[#2563eb]/10 text-[#1d4ed8] dark:border-blue-500/40 dark:bg-blue-950/60 dark:text-blue-300',
    }
  }
  return null
}

function CompanyAvatar({ name, logoUrl }) {
  const initial = (name || 'C').trim().charAt(0).toUpperCase() || 'C'
  const shell =
    'shrink-0 overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-md shadow-slate-200/40 ring-2 ring-white dark:border-slate-600 dark:bg-slate-800 dark:shadow-none dark:ring-slate-900 sm:h-16 sm:w-16 h-14 w-14'
  if (logoUrl && /^https?:\/\//i.test(logoUrl)) {
    return (
      <div className={`relative ${shell}`}>
        <img src={logoUrl} alt="" className="h-full w-full object-contain p-1.5" loading="lazy" />
      </div>
    )
  }
  return (
    <div
      className={`flex items-center justify-center text-lg font-bold text-[#2563eb] dark:text-blue-300 sm:text-xl ${shell} border-[#2563eb]/20 bg-[#2563eb]/10 dark:border-blue-500/25 dark:bg-blue-950/40`}
      aria-hidden
    >
      {initial}
    </div>
  )
}

function MetaChip({ icon: Icon, children }) {
  if (!children) return null
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-slate-200/90 bg-slate-50 px-2.5 py-1 text-[11px] font-medium text-slate-600 dark:border-slate-600 dark:bg-slate-800/80 dark:text-slate-300">
      {Icon ? <Icon className="h-3 w-3 shrink-0 text-[#2563eb] opacity-90 dark:text-blue-400" /> : null}
      {children}
    </span>
  )
}

function JobCard({ job }) {
  const salary = formatSalaryLabel(job.salaryMin, job.salaryMax, job.salaryCurrency)
  const badge = jobAccentBadge(job)
  const teaser =
    stripHtmlToText(job.description)?.slice(0, 110) ||
    stripHtmlToText(job.requirements)?.slice(0, 110) ||
    null

  return (
    <article className="group relative overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-[#2563eb]/35 hover:shadow-lg hover:shadow-blue-500/10 dark:border-slate-700/90 dark:bg-slate-900 dark:hover:border-[#2563eb]/40 dark:hover:shadow-blue-900/20">
      <div
        className="pointer-events-none absolute inset-y-0 left-0 w-[3px] bg-[#2563eb] opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        aria-hidden
      />
      <div className="flex flex-col gap-4 p-4 sm:flex-row sm:items-stretch sm:gap-5 sm:p-5">
        <div className="flex min-w-0 flex-1 gap-4">
          <CompanyAvatar name={job.companyName} logoUrl={job.companyLogo} />

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-start gap-2">
              <Link
                to={`/jobs/${job.id}`}
                className="line-clamp-2 min-w-0 flex-1 text-base font-bold leading-snug text-slate-900 transition-colors hover:text-[#2563eb] sm:text-lg dark:text-white dark:hover:text-blue-400"
              >
                {job.title}
              </Link>
              {badge ? (
                <span
                  className={`relative z-10 shrink-0 rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${badge.className}`}
                >
                  {badge.text}
                </span>
              ) : null}
            </div>

            <div className="relative z-10 mt-1.5 flex items-center gap-1.5 text-sm text-slate-500 dark:text-slate-400">
              <Building2 className="h-3.5 w-3.5 shrink-0 text-slate-400 dark:text-slate-500" />
              <span className="truncate font-medium text-slate-700 dark:text-slate-300">{job.companyName || 'Doanh nghiệp'}</span>
            </div>

            <div className="relative z-10 mt-3 flex flex-wrap gap-2">
              <MetaChip icon={MapPin}>{job.city}</MetaChip>
              <MetaChip icon={Briefcase}>{job.jobType}</MetaChip>
              {job.level ? <MetaChip>{job.level}</MetaChip> : null}
            </div>

            {salary ? (
              <div className="relative z-10 mt-3 inline-flex items-center gap-1.5 rounded-lg border border-emerald-200/70 bg-emerald-50/80 px-2.5 py-1 text-sm font-semibold text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
                <Banknote className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
                {salary}
              </div>
            ) : null}

            {teaser ? (
              <p className="relative z-10 mt-2 line-clamp-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400">{teaser}…</p>
            ) : null}
          </div>
        </div>

        <div className="relative z-10 flex shrink-0 flex-col justify-end gap-2 border-t border-slate-100 pt-4 sm:w-[9.5rem] sm:border-l sm:border-t-0 sm:pl-5 sm:pt-0 dark:border-slate-700">
          <Link
            to={`/jobs/${job.id}`}
            className="inline-flex h-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-xs font-semibold text-slate-700 transition hover:border-[#2563eb]/45 hover:text-[#2563eb] dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:border-blue-500/50 dark:hover:text-blue-300"
            onClick={(e) => e.stopPropagation()}
          >
            Chi tiết
          </Link>
          <Link
            to={`/jobs/${job.id}/apply`}
            className="inline-flex h-10 items-center justify-center gap-1 rounded-xl bg-[#2563eb] text-xs font-bold text-white shadow-sm transition hover:bg-[#1d4ed8] active:scale-[0.98]"
            onClick={(e) => e.stopPropagation()}
          >
            Ứng tuyển
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </div>
    </article>
  )
}

function SectionBlock({ title, subtitle, jobs, loading }) {
  return (
    <section className="space-y-5">
      <div>
        <h2 className="text-xl font-bold text-[#2563eb] dark:text-blue-400 sm:text-2xl">{title}</h2>
        {subtitle ? <p className="mt-1.5 text-sm text-slate-600 dark:text-slate-400">{subtitle}</p> : null}
      </div>
      {loading ? (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="h-40 animate-pulse rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
      ) : jobs.length === 0 ? (
        <p className="text-sm text-slate-500 dark:text-slate-400">Chưa có tin phù hợp mục này.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
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

  const [searchInput, setSearchInput] = useState(qUrl)

  useEffect(() => {
    setSearchInput(qUrl)
  }, [qUrl])

  const [debouncedQ, setDebouncedQ] = useState(qUrl)
  useEffect(() => {
    const t = setTimeout(() => {
      const next = searchInput.trim()
      setDebouncedQ(next)
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

  const mainFeed = useInfiniteQuery({
    queryKey: ['public-jobs-feed', filterKey, PAGE_SIZE],
    queryFn: async ({ pageParam }) => {
      const p = new URLSearchParams(commonBase)
      p.set('size', String(PAGE_SIZE))
      if (pageParam?.afterCreatedAt && pageParam?.afterId) {
        p.set('afterCreatedAt', pageParam.afterCreatedAt)
        p.set('afterId', pageParam.afterId)
      }
      return fetchJobFeedKeyset(p.toString())
    },
    initialPageParam: null,
    getNextPageParam: (last) => {
      if (!last?.hasNext || !last?.nextAfterCreatedAt || !last?.nextAfterId) return undefined
      return { afterCreatedAt: last.nextAfterCreatedAt, afterId: last.nextAfterId }
    },
  })

  const jobs = mainFeed.data?.pages.flatMap((pg) => pg?.content || []) ?? []
  const mainLoading = mainFeed.isPending
  const mainFetching = mainFeed.isFetching || mainFeed.isFetchingNextPage
  const mainError = mainFeed.isError
  const mainErr = mainFeed.error

  const user = getUser()
  const displayName = user?.fullName?.trim() || user?.email?.split('@')[0] || ''

  function setParam(key, value) {
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
    setSearchInput('')
    setDebouncedQ('')
    setSearchParams({}, { replace: true })
  }

  const loadMoreBlock =
    mainFeed.hasNextPage || mainFeed.isFetchingNextPage ? (
      <div className="flex flex-wrap items-center justify-center gap-2 pt-6">
        <button
          type="button"
          disabled={mainFeed.isFetchingNextPage || mainFetching}
          onClick={() => mainFeed.fetchNextPage()}
          className="rounded-xl border border-[#2563eb]/40 bg-[#2563eb] px-6 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#1d4ed8] disabled:cursor-not-allowed disabled:opacity-50 dark:border-blue-600"
        >
          {mainFeed.isFetchingNextPage ? 'Đang tải…' : 'Tải thêm việc làm'}
        </button>
      </div>
    ) : null

  const hero = (
    <div className="overflow-hidden rounded-[1.75rem] border border-[#1d4ed8] bg-[#2563eb] text-white shadow-lg shadow-[#2563eb]/20">
      <div className="px-6 py-10 sm:px-10 sm:py-12 lg:px-14 lg:py-14">
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-white/85">VTHR Careers Hub</p>
        <h1 className="mt-3 text-3xl font-bold leading-tight tracking-tight sm:text-4xl lg:text-[2.75rem]">
          {displayName
            ? `Việc làm dành cho bạn, ${displayName}`
            : 'Khám phá việc làm phù hợp kỹ năng và mục tiêu của bạn'}
        </h1>
        <p className="mt-4 max-w-2xl text-sm leading-relaxed text-white/90 sm:text-base">
          Tìm theo từ khóa, địa điểm, ngành và loại hình — bộ lọc kết hợp giúp bạn nhanh chóng thu hẹp danh sách việc làm
          phù hợp.
        </p>

        <div className="mt-8 flex flex-col gap-3 sm:gap-4">
          <div className="flex flex-col gap-2 rounded-2xl bg-white p-2 shadow-lg shadow-black/10 ring-1 ring-black/5 dark:bg-slate-100 sm:flex-row sm:items-stretch sm:rounded-full sm:p-2 sm:pl-3">
            <div className="flex min-h-[48px] shrink-0 items-center gap-2 border-b border-slate-200 px-3 py-2 sm:border-b-0 sm:border-r sm:py-0 dark:border-slate-200">
              <MapPin className="h-5 w-5 shrink-0 text-[#2563eb]" />
              <select
                value={city}
                onChange={(e) => setParam('city', e.target.value)}
                className="min-w-0 flex-1 bg-transparent text-sm font-medium text-slate-900 outline-none dark:text-slate-900"
                aria-label="Địa điểm"
              >
                <option value="">Mọi địa điểm</option>
                {cities.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
            <div className="min-w-0 flex-1 px-2 sm:px-1">
              <label htmlFor="job-search" className="sr-only">
                Tìm việc
              </label>
              <input
                id="job-search"
                type="search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Kỹ năng, chức danh, công ty…"
                className="h-12 w-full rounded-xl border-0 bg-transparent px-3 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-0 dark:text-slate-900 sm:h-12"
              />
            </div>
            <button
              type="button"
              onClick={() => setDebouncedQ(searchInput.trim())}
              className="inline-flex h-12 shrink-0 items-center justify-center gap-2 rounded-xl bg-[#2563eb] px-8 text-sm font-bold text-white shadow-md shadow-blue-600/25 transition hover:bg-[#1d4ed8] sm:rounded-full"
            >
              <Search className="h-4 w-4" />
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
    </div>
  )

  const filterBar = (
    <div
      id="job-browse-filters"
      className="scroll-mt-24 rounded-[1.25rem] border border-slate-200/90 bg-white p-5 shadow-sm dark:border-slate-700/90 dark:bg-slate-900 sm:p-6"
    >
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-12 xl:items-end">
        <label className="flex flex-col gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400 xl:col-span-3">
          Ngành / danh mục
          <select
            value={industry}
            onChange={(e) => setParam('industry', e.target.value)}
            className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50/80 px-4 text-sm font-medium text-slate-900 outline-none transition focus:border-[#2563eb]/50 focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-600 dark:bg-slate-950 dark:text-slate-100"
          >
            <option value="">Tất cả</option>
            {industries.map((ind) => (
              <option key={ind} value={ind}>
                {ind}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400 xl:col-span-2">
          Loại hình
          <select
            value={jobType}
            onChange={(e) => setParam('jobType', e.target.value)}
            className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50/80 px-4 text-sm font-medium text-slate-900 outline-none transition focus:border-[#2563eb]/50 focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-600 dark:bg-slate-950 dark:text-slate-100"
          >
            <option value="">Tất cả</option>
            {jobTypes.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400 xl:col-span-2">
          Cấp bậc
          <select
            value={level}
            onChange={(e) => setParam('level', e.target.value)}
            className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50/80 px-4 text-sm font-medium text-slate-900 outline-none transition focus:border-[#2563eb]/50 focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-600 dark:bg-slate-950 dark:text-slate-100"
          >
            <option value="">Tất cả</option>
            {levels.map((lv) => (
              <option key={lv} value={lv}>
                {lv}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400 xl:col-span-3">
          Kỹ năng
          <input
            type="text"
            value={skill}
            onChange={(e) => setParam('skill', e.target.value)}
            placeholder="VD: Java, React…"
            className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50/80 px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-[#2563eb]/50 focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-600 dark:bg-slate-950 dark:text-slate-100"
          />
        </label>
        <div className="flex xl:col-span-2 xl:justify-end">
          <button
            type="button"
            onClick={clearFilters}
            className="h-12 w-full rounded-xl border border-slate-200 px-5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800 xl:w-auto"
          >
            Xóa bộ lọc
          </button>
        </div>
      </div>
    </div>
  )

  const headerBlock = (
    <div className="space-y-6 py-8 lg:space-y-8 lg:py-12">
      {hero}
      {filterBar}
    </div>
  )

  if (mainLoading) {
    return (
      <div className="mx-auto max-w-6xl space-y-8 px-4 sm:px-6 lg:max-w-7xl">
        {headerBlock}
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div
              key={idx}
              className="h-44 animate-pulse rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
      </div>
    )
  }

  if (mainError) {
    return (
      <div className="mx-auto max-w-6xl space-y-6 px-4 sm:px-6 lg:max-w-7xl">
        {headerBlock}
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-200">
          {mainErr?.message || 'Không tải được danh sách việc làm.'}
        </div>
      </div>
    )
  }

  const countFmt = new Intl.NumberFormat('vi-VN').format(jobs.length)

  return (
    <div className="mx-auto max-w-6xl space-y-14 px-4 pb-20 sm:px-6 lg:max-w-7xl">
      {headerBlock}

      <SectionBlock
        title="Việc làm mới nhất"
        subtitle="Đăng tải gần đây, sắp xếp theo thời gian tạo."
        jobs={latestQuery.data?.content || []}
        loading={latestQuery.isLoading}
      />

      <SectionBlock
        title="Việc làm phù hợp"
        subtitle="Gợi ý theo từ khóa tìm kiếm và ngành bạn chọn."
        jobs={suitableSourceQuery.data?.content || []}
        loading={suitableSourceQuery.isLoading}
      />

      <SectionBlock
        title="Việc làm hấp dẫn"
        subtitle="Ưu tiên mức lương tối đa theo dữ liệu tin đăng."
        jobs={attractiveQuery.data?.content || []}
        loading={attractiveQuery.isLoading}
      />

      <section className="space-y-5">
        <div className="flex flex-col gap-2 border-b border-slate-200 pb-4 dark:border-slate-700 sm:flex-row sm:items-end sm:justify-between">
          <h2 className="text-2xl font-bold text-[#2563eb] dark:text-blue-400 sm:text-3xl">
            Đang hiển thị {countFmt} việc làm
          </h2>
          <p className="text-sm text-slate-600 dark:text-slate-400">
            {mainFeed.hasNextPage ? (
              <span>Còn tin phía dưới — nhấn Tải thêm (phân trang keyset, tránh OFFSET sâu).</span>
            ) : jobs.length > 0 ? (
              <span>Đã hết danh sách theo bộ lọc hiện tại.</span>
            ) : null}
            {mainFetching ? <span className="ml-2 text-slate-400">· Đang cập nhật…</span> : null}
          </p>
        </div>

        {jobs.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-slate-200 bg-white py-16 text-center text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-400">
            Không có tin tuyển dụng phù hợp bộ lọc.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            {jobs.map((j) => (
              <JobCard key={j.id} job={j} />
            ))}
          </div>
        )}
        {loadMoreBlock}
      </section>
    </div>
  )
}
