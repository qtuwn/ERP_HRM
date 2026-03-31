import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'

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
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [jobs, setJobs] = useState([])

  useEffect(() => {
    let mounted = true
    ;(async () => {
      try {
        setLoading(true)
        setError('')
        const res = await api.get('/api/jobs?page=0&size=12&sort=createdAt,desc')
        const page = res?.data
        const items = page?.content || []
        if (mounted) setJobs(items)
      } catch (err) {
        if (mounted) setError(err?.message || 'Không tải được danh sách job')
      } finally {
        if (mounted) setLoading(false)
      }
    })()
    return () => {
      mounted = false
    }
  }, [])

  const header = useMemo(() => {
    return (
      <div className="rounded-2xl border bg-white p-6">
        <div className="text-sm text-slate-600">Public</div>
        <h1 className="mt-1 text-xl font-semibold tracking-tight">Danh sách việc làm</h1>
        <p className="mt-1 text-sm text-slate-600">
          Data từ <code className="rounded bg-slate-100 px-1">GET /api/jobs</code>
        </p>
      </div>
    )
  }, [])

  if (loading) {
    return (
      <div className="space-y-4">
        {header}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div key={idx} className="h-32 animate-pulse rounded-2xl border bg-white" />
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-4">
        {header}
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {header}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {jobs.map((j) => {
          const money = formatMoney(j.salaryMin, j.salaryMax, j.salaryCurrency)
          return (
            <div key={j.id} className="rounded-2xl border bg-white p-5 hover:shadow-lg transition-shadow">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="text-sm text-slate-600">{j.companyName || 'VTHR'}</div>
                  <div className="mt-1 font-semibold leading-snug">{j.title}</div>
                </div>
                <div className="rounded-lg bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
                  {j.status || 'OPEN'}
                </div>
              </div>

              <div className="mt-3 space-y-1 text-sm text-slate-700">
                {j.city ? <div>📍 {j.city}</div> : null}
                {j.jobType ? <div>💼 {j.jobType}</div> : null}
                {money ? <div>💰 {money}</div> : null}
              </div>

              <div className="mt-4 flex items-center justify-between">
                <div className="text-xs text-slate-500">{j.department ? `Dept: ${j.department}` : ''}</div>
                <button className="rounded-lg border px-3 py-2 text-sm font-medium hover:bg-slate-50">
                  Xem chi tiết
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

