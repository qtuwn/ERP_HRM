import { useEffect, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'

function StatCard({ label, value, hint }) {
  return (
    <div className="rounded-2xl border bg-white p-5 hover:shadow-lg transition-shadow">
      <div className="text-sm text-slate-600">{label}</div>
      <div className="mt-2 text-3xl font-semibold tracking-tight">{value}</div>
      {hint ? <div className="mt-2 text-xs text-slate-500">{hint}</div> : null}
    </div>
  )
}

export function DashboardPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [stats, setStats] = useState(null)

  useEffect(() => {
    let mounted = true
    ;(async () => {
      try {
        setLoading(true)
        setError('')
        const res = await api.get('/api/dashboard/stats')
        if (mounted) setStats(res?.data || null)
      } catch (err) {
        if (mounted) setError(err?.message || 'Không tải được dashboard')
      } finally {
        if (mounted) setLoading(false)
      }
    })()
    return () => {
      mounted = false
    }
  }, [])

  const user = getUser()

  return (
    <div className="space-y-4">
      <div className="rounded-2xl border bg-white p-6">
        <div className="text-sm text-slate-600">HR / Admin</div>
        <h1 className="mt-1 text-xl font-semibold tracking-tight">Dashboard</h1>
        <p className="mt-1 text-sm text-slate-600">
          Data từ <code className="rounded bg-slate-100 px-1">GET /api/dashboard/stats</code> (cần JWT)
        </p>
        {user ? (
          <div className="mt-3 text-sm text-slate-700">
            Đang đăng nhập: <span className="font-medium">{user.fullName || user.email}</span> ({user.role})
          </div>
        ) : null}
      </div>

      {loading ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, idx) => (
            <div key={idx} className="h-28 animate-pulse rounded-2xl border bg-white" />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
      ) : stats ? (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
            <StatCard label="Tổng jobs" value={stats.totalJobs} />
            <StatCard label="Jobs đang active" value={stats.activeJobs} />
            <StatCard label="Tổng ứng tuyển" value={stats.totalApplications} />
            <StatCard label="Pass rate" value={`${Math.round((stats.passRate || 0) * 100)}%`} hint="(ước tính)" />
          </div>

          <div className="rounded-2xl border bg-white p-6">
            <div className="text-sm font-semibold">Applications by status</div>
            <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-4">
              {Object.entries(stats.applicationsByStatus || {}).map(([k, v]) => (
                <div key={k} className="rounded-xl border bg-slate-50 p-3">
                  <div className="text-xs text-slate-600">{k}</div>
                  <div className="mt-1 text-xl font-semibold">{v}</div>
                </div>
              ))}
            </div>
          </div>
        </>
      ) : (
        <div className="rounded-2xl border bg-white p-6 text-sm text-slate-600">Không có dữ liệu.</div>
      )}
    </div>
  )
}

