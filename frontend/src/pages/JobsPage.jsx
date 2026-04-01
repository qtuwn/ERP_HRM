import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api.js'

const PAGE_SIZE = 9

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

export function JobsPage() {
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [debouncedQ, setDebouncedQ] = useState('')

  useEffect(() => {
    const t = setTimeout(() => {
      const next = searchInput.trim()
      setDebouncedQ((prev) => {
        if (prev !== next) {
          queueMicrotask(() => setPage(0))
        }
        return next
      })
    }, 350)
    return () => clearTimeout(t)
  }, [searchInput])

  const {
    data: pageData,
    isLoading,
    isFetching,
    isError,
    error,
  } = useQuery({
    queryKey: ['public-jobs', page, debouncedQ, PAGE_SIZE],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
        sort: 'createdAt,desc',
      })
      if (debouncedQ) params.set('q', debouncedQ)
      const res = await api.get(`/api/jobs?${params.toString()}`)
      return res?.data ?? null
    },
  })

  const jobs = pageData?.content || []
  const totalPages = typeof pageData?.totalPages === 'number' ? pageData.totalPages : 0
  const totalElements = typeof pageData?.totalElements === 'number' ? pageData.totalElements : jobs.length

  const header = (
    <div className="mx-auto max-w-7xl py-10 lg:py-14 px-4 sm:px-6 space-y-6">
      <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-br from-[#2563eb] to-[#1d4ed8] text-white shadow-xl shadow-[#2563eb]/20">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_80%_20%,rgba(255,255,255,0.12),transparent_50%),radial-gradient(circle_at_20%_80%,rgba(255,255,255,0.08),transparent_45%)]"></div>
        <div className="absolute -right-16 -top-16 h-72 w-72 rounded-full bg-white/10 blur-3xl"></div>
        <div className="absolute -bottom-16 -left-16 h-72 w-72 rounded-full bg-[#1d4ed8]/40 blur-3xl"></div>
        <div className="relative p-8 sm:p-10 lg:p-12">
          <div className="max-w-4xl">
            <div className="mb-4 inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-white/90">
              VTHR Careers Hub
            </div>
            <h1 className="text-3xl font-bold leading-tight sm:text-4xl lg:text-5xl">
              Khám phá công việc phù hợp với kỹ năng và mục tiêu của bạn
            </h1>
            <p className="mt-4 max-w-3xl text-sm leading-relaxed text-white/90 sm:text-base">
              Tìm kiếm theo từ khóa, ngành nghề và địa điểm để rút ngắn thời gian ứng tuyển. Theo dõi công việc yêu
              thích và ứng tuyển nhanh chỉ với một lần nhấn.
            </p>
            <div className="mt-6 max-w-xl">
              <label htmlFor="job-search" className="sr-only">
                Tìm việc
              </label>
              <input
                id="job-search"
                type="search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Tìm theo tiêu đề, công ty, thành phố, phòng ban…"
                className="w-full rounded-xl border border-white/20 bg-white/10 px-4 py-3 text-sm text-white placeholder:text-white/60 backdrop-blur focus:border-white/40 focus:outline-none focus:ring-2 focus:ring-white/30"
              />
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">Danh sách việc làm nổi bật</h2>
          <p className="text-sm text-slate-600 dark:text-slate-300">
            <span className="font-semibold">{totalElements}</span> vị trí
            {totalPages > 1 ? (
              <span className="text-slate-500 dark:text-slate-400">
                {' '}
                · Trang {page + 1}/{Math.max(totalPages, 1)}
              </span>
            ) : null}
            {isFetching && !isLoading ? (
              <span className="ml-2 text-xs font-normal text-slate-400">· Đang cập nhật…</span>
            ) : null}
          </p>
        </div>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
          <code className="rounded bg-slate-100 px-1 dark:bg-slate-800">GET /api/jobs</code>
          {debouncedQ ? ` · q=${debouncedQ}` : ''}
        </p>
      </div>
    </div>
  )

  const pagination = (
    <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
      <button
        type="button"
        disabled={page <= 0 || isFetching}
        onClick={() => setPage(0)}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Đầu
      </button>
      <button
        type="button"
        disabled={page <= 0 || isFetching}
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
        disabled={isFetching || totalPages === 0 || page >= totalPages - 1}
        onClick={() => setPage((p) => p + 1)}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Sau
      </button>
      <button
        type="button"
        disabled={isFetching || totalPages === 0 || page >= totalPages - 1}
        onClick={() => setPage(Math.max(0, totalPages - 1))}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
      >
        Cuối
      </button>
    </div>
  )

  if (isLoading) {
    return (
      <div className="space-y-4">
        {header}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div
              key={idx}
              className="h-32 animate-pulse rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <div className="space-y-4">
        {header}
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error?.message || 'Không tải được danh sách job'}
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 pb-16 space-y-4">
      {header}
      {jobs.length === 0 ? (
        <p className="text-center text-sm text-slate-500 dark:text-slate-400">Không có tin tuyển dụng phù hợp.</p>
      ) : (
        <div className="grid grid-cols-1 gap-7 md:grid-cols-2 xl:grid-cols-3">
          {jobs.map((j) => {
            const money = formatMoney(j.salaryMin, j.salaryMax, j.salaryCurrency)
            return (
              <article
                key={j.id}
                className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 hover:shadow-lg hover:-translate-y-1 transition-all duration-200 flex flex-col h-full"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm text-slate-600 dark:text-slate-300">{j.companyName || 'VTHR'}</div>
                    <div className="mt-1 font-semibold leading-snug text-slate-900 dark:text-white">{j.title}</div>
                  </div>
                  <span className="inline-flex px-2.5 py-1 rounded-full text-xs font-semibold bg-[#2563eb]/12 text-[#2563eb]">
                    {j.department || 'General'}
                  </span>
                </div>

                <p className="mt-3 text-slate-700 dark:text-slate-200 text-sm leading-6 line-clamp-3">
                  {j.description || ''}
                </p>

                <div className="mt-4 flex flex-wrap gap-2">
                  {money ? (
                    <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold bg-[#2563eb]/10 text-[#2563eb]">
                      {money}
                    </span>
                  ) : null}
                  {j.city ? (
                    <div className="text-xs font-medium text-slate-500 dark:text-slate-400">📍 {j.city}</div>
                  ) : null}
                  {j.jobType ? (
                    <div className="text-xs font-medium text-slate-500 dark:text-slate-400">💼 {j.jobType}</div>
                  ) : null}
                </div>

                <div className="mt-5 pt-4 border-t border-slate-200 dark:border-slate-800 space-y-3 mt-auto">
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    {j.department ? `Dept: ${j.department}` : ''}
                  </div>
                  <div className="flex items-center justify-between gap-2">
                    <a
                      href={`/jobs/${j.id}`}
                      className="inline-flex items-center justify-center px-3 py-2 rounded-lg border border-[#2563eb] text-[#2563eb] text-sm font-semibold hover:bg-[#2563eb]/10 transition-colors"
                    >
                      Chi tiết
                    </a>
                    <a
                      href={`/jobs/${j.id}/apply`}
                      className="inline-flex items-center justify-center px-3 py-2 rounded-lg bg-[#2563eb] text-white text-sm font-semibold hover:bg-[#1d4ed8] transition-colors"
                    >
                      Quick Apply
                    </a>
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      )}
      {totalPages > 1 ? pagination : null}
    </div>
  )
}
