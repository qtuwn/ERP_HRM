import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { Briefcase, Inbox, Percent, Radio, RefreshCw } from 'lucide-react'

function displayStatus(status) {
  const map = {
    APPLIED: 'Đã nộp',
    AI_SCREENING: 'Sàng lọc AI',
    AI_PROCESSING: 'AI đang xử lý',
    HR_REVIEW: 'Đánh giá HR',
    INTERVIEW: 'Phỏng vấn',
    OFFER: 'Đề nghị',
    HIRED: 'Đã tuyển',
    REJECTED: 'Từ chối',
  }
  return map[status] || status
}

function StatCard({ label, value, Icon, hint }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow duration-200 hover:shadow-lg dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-slate-900 dark:text-white">{value ?? '…'}</p>
          {hint ? <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p> : null}
        </div>
        {Icon ? (
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#2563eb]/10 text-[#2563eb] dark:bg-[#2563eb]/20 dark:text-white">
            <Icon className="h-5 w-5" />
          </div>
        ) : null}
      </div>
    </div>
  )
}

export function DashboardPage() {
  const chartRef = useRef(null)
  const chartInstanceRef = useRef(null)
  const [themeTick, setThemeTick] = useState(0)

  const user = useMemo(() => getUser(), [])

  const {
    data: stats,
    isLoading: loading,
    isError,
    error: queryError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: async () => {
      const res = await api.get('/api/dashboard/stats')
      return res?.data ?? null
    },
  })

  const error = isError ? queryError?.message || 'Không thể tải dữ liệu bảng điều khiển.' : ''

  useEffect(() => {
    const el = document.documentElement
    let prev = el.classList.contains('dark')
    const ob = new MutationObserver(() => {
      const next = el.classList.contains('dark')
      if (next !== prev) {
        prev = next
        setThemeTick((t) => t + 1)
      }
    })
    ob.observe(el, { attributes: true, attributeFilter: ['class'] })
    return () => ob.disconnect()
  }, [])

  useEffect(() => {
    async function renderChart() {
      if (!stats?.applicationsByStatus) return
      const el = chartRef.current
      if (!el) return

      const { default: Chart } = await import('chart.js/auto')

      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy()
        chartInstanceRef.current = null
      }

      const rawData = stats.applicationsByStatus || {}
      const labels = Object.keys(rawData).map((s) => displayStatus(s))
      const data = Object.values(rawData)

      const bluesLight = ['#eff6ff', '#dbeafe', '#bfdbfe', '#93c5fd', '#60a5fa', '#3b82f6', '#2563eb']
      const backgroundColors = labels.map((_, i) => bluesLight[Math.min(i, bluesLight.length - 1)])
      const borderColors = labels.map((_, i) => (i % 2 === 0 ? '#2563eb' : '#1d4ed8'))
      const isDark = document.documentElement.classList.contains('dark')

      chartInstanceRef.current = new Chart(el, {
        type: 'doughnut',
        data: {
          labels,
          datasets: [
            {
              data,
              backgroundColor: backgroundColors,
              borderColor: borderColors,
              borderWidth: 1,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'right',
              labels: { color: isDark ? '#e2e8f0' : '#334155' },
            },
          },
        },
      })
    }

    renderChart()
    return () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy()
        chartInstanceRef.current = null
      }
    }
  }, [stats, themeTick])

  return (
    <div className="w-full px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Phân tích nền tảng</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Tổng quan việc làm, hồ sơ và tỷ lệ tuyển thành công.
          </p>
        </div>
        <button
          type="button"
          onClick={() => refetch()}
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-[#2563eb] shadow-sm transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-[#60a5fa] dark:hover:bg-slate-800"
        >
          <RefreshCw className={['h-4 w-4 shrink-0', loading || isFetching ? 'animate-spin' : ''].join(' ')} />
          Làm mới
        </button>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 mb-8">
          {Array.from({ length: 4 }).map((_, idx) => (
            <div
              key={idx}
              className="h-[108px] animate-pulse rounded-xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900/40 dark:bg-red-950/20 dark:text-red-300">
          {error}
        </div>
      ) : stats ? (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 mb-8">
            <StatCard label="Tổng số công việc" value={stats.totalJobs} Icon={Briefcase} />
            <StatCard label="Công việc đang tuyển" value={stats.activeJobs} Icon={Radio} />
            <StatCard label="Tổng số hồ sơ" value={stats.totalApplications} Icon={Inbox} />
            <StatCard label="Tỷ lệ đạt tổng quan" value={`${stats.passRate ?? 0}%`} Icon={Percent} hint="được tuyển" />
          </div>

          {stats.totalJobs === 0 && stats.totalApplications === 0 ? (
            <p className="mb-6 rounded-xl border border-dashed border-slate-200 bg-slate-50/80 px-4 py-3 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-900/50 dark:text-slate-300">
              Chưa có dữ liệu tuyển dụng. Tạo tin tuyển dụng hoặc chờ ứng viên nộp hồ sơ.
            </p>
          ) : stats.totalApplications === 0 ? (
            <p className="mb-6 rounded-xl border border-dashed border-amber-200/80 bg-amber-50/80 px-4 py-3 text-sm text-amber-900 dark:border-amber-900/40 dark:bg-amber-950/20 dark:text-amber-100">
              Chưa có hồ sơ ứng viên. Chia sẻ tin tuyển dụng để nhận đơn.
            </p>
          ) : null}

          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h3 className="mb-4 text-lg font-semibold text-slate-900 dark:text-white">Phân bố phễu hồ sơ</h3>
              <div className="relative flex h-72 items-center justify-center">
                {!stats ? <div className="animate-pulse text-slate-400">Đang tải biểu đồ...</div> : null}
                <canvas ref={chartRef} className="h-full w-full max-h-72" />
              </div>
            </div>

            <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="border-b border-slate-200 px-6 py-4 dark:border-slate-700">
                <h3 className="text-lg font-semibold text-slate-900 dark:text-white">Chi tiết theo giai đoạn</h3>
              </div>
              <div className="px-6 py-4">
                <ul className="divide-y divide-slate-200 dark:divide-slate-700">
                  {Object.entries(stats.applicationsByStatus || {}).map(([status, count]) => (
                    <li
                      key={status}
                      className="flex items-center justify-between rounded-lg py-3 transition hover:bg-slate-50 dark:hover:bg-slate-800/80"
                    >
                      <span className="text-sm font-medium text-slate-600 dark:text-slate-300">
                        {displayStatus(status)}
                      </span>
                      <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-800 dark:bg-slate-800 dark:text-slate-200">
                        {count}
                      </span>
                    </li>
                  ))}
                </ul>
                {!stats ? <div className="py-6 text-center text-sm text-slate-400">Đang tải dữ liệu...</div> : null}
              </div>
            </div>
          </div>

          {user ? (
            <div className="mt-6 text-xs text-slate-500 dark:text-slate-400">
              Đang đăng nhập:{' '}
              <span className="font-semibold text-slate-700 dark:text-slate-200">{user.fullName || user.email}</span> (
              {user.role})
            </div>
          ) : null}
        </>
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
          Không có dữ liệu.
        </div>
      )}
    </div>
  )
}
