import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js'
import { RefreshCw, Download, BarChart3, Briefcase, Users } from 'lucide-react'
import { api } from '../lib/api.js'
import {
  APPLICATION_CHART_ORDER,
  JOB_CHART_ORDER,
  ROLE_CHART_ORDER,
  labelApplicationStatus,
  labelJobStatus,
  labelRole,
  sortKeys,
} from '../lib/analyticsLabels.js'

Chart.register(
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
  ArcElement,
  DoughnutController
)

const CHART_COLORS = {
  primary: 'rgba(37, 99, 235, 0.75)',
  emerald: 'rgba(16, 185, 129, 0.8)',
  amber: 'rgba(245, 158, 11, 0.85)',
  violet: 'rgba(139, 92, 246, 0.8)',
  rose: 'rgba(244, 63, 94, 0.75)',
  slate: 'rgba(100, 116, 139, 0.75)',
}

const DOUGHNUT_BG = [
  CHART_COLORS.primary,
  CHART_COLORS.emerald,
  CHART_COLORS.amber,
  CHART_COLORS.violet,
  CHART_COLORS.rose,
  CHART_COLORS.slate,
]

function parseSummaryPayload(json) {
  if (!json || typeof json !== 'object') return null
  if (json.data && typeof json.data === 'object') return json.data
  if (
    json.applicationsByStatus ||
    json.jobsByStatus ||
    json.usersByRole
  ) {
    return json
  }
  return null
}

function sumMap(m) {
  if (!m || typeof m !== 'object') return 0
  return Object.values(m).reduce((a, v) => a + (Number(v) || 0), 0)
}

function mapToSortedChart(mapObj, order, labelFn) {
  const m = mapObj && typeof mapObj === 'object' ? mapObj : {}
  const keys = sortKeys(Object.keys(m), order)
  return {
    labels: keys.map(labelFn),
    values: keys.map((k) => Number(m[k]) || 0),
    keys,
  }
}

function downloadRecruitmentCsv(summary) {
  if (!summary) return
  const rows = [['Khu vực', 'Mã / khóa', 'Giá trị']]
  const pushSection = (title, obj) => {
    if (!obj || typeof obj !== 'object') return
    Object.entries(obj).forEach(([k, v]) => {
      rows.push([title, k, String(v ?? '')])
    })
  }
  pushSection('Ứng tuyển theo trạng thái', summary.applicationsByStatus)
  pushSection('Việc làm theo trạng thái', summary.jobsByStatus)
  pushSection('Người dùng theo vai trò', summary.usersByRole)

  const escape = (cell) => {
    const s = String(cell ?? '')
    if (/[",\n]/.test(s)) return `"${s.replace(/"/g, '""')}"`
    return s
  }
  const csv = rows.map((r) => r.map(escape).join(',')).join('\n')
  const bom = '\uFEFF'
  const blob = new Blob([bom + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `thong-ke-tuyen-dung-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function formatError(err) {
  const msg = err?.message || 'Không tải được dữ liệu'
  const st = err?.status
  if (st === 502 || msg.includes('502')) {
    return `${msg}. Gợi ý: khi chạy Vite (npm run dev), cần bật Spring Boot tại http://localhost:8080 để proxy /api hoạt động.`
  }
  if (st === 401 || msg.toLowerCase().includes('unauthorized')) {
    return 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.'
  }
  if (st === 403 || msg.toLowerCase().includes('forbidden')) {
    return 'Bạn không có quyền xem thống kê (chỉ Admin).'
  }
  return msg
}

export function AdminAnalyticsPage() {
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const appCanvasRef = useRef(null)
  const jobCanvasRef = useRef(null)
  const userCanvasRef = useRef(null)
  const chartsRef = useRef([])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/api/admin/analytics/recruitment')
      const data = parseSummaryPayload(res)
      setSummary(data)
      if (!data) {
        setError('Phản hồi API không đúng định dạng (thiếu data).')
      }
    } catch (e) {
      setSummary(null)
      setError(formatError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const totals = useMemo(() => {
    if (!summary) {
      return { apps: 0, jobs: 0, users: 0 }
    }
    return {
      apps: sumMap(summary.applicationsByStatus),
      jobs: sumMap(summary.jobsByStatus),
      users: sumMap(summary.usersByRole),
    }
  }, [summary])

  const appChart = useMemo(
    () => mapToSortedChart(summary?.applicationsByStatus, APPLICATION_CHART_ORDER, labelApplicationStatus),
    [summary?.applicationsByStatus]
  )
  const jobChart = useMemo(
    () => mapToSortedChart(summary?.jobsByStatus, JOB_CHART_ORDER, labelJobStatus),
    [summary?.jobsByStatus]
  )
  const userChart = useMemo(
    () => mapToSortedChart(summary?.usersByRole, ROLE_CHART_ORDER, labelRole),
    [summary?.usersByRole]
  )

  useEffect(() => {
    chartsRef.current.forEach((c) => c.destroy())
    chartsRef.current = []

    if (loading || !summary) return

    const makeBar = (canvas, labels, values, label, color) => {
      if (!canvas) return null
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      const hasData = values.some((v) => v > 0)
      return new Chart(ctx, {
        type: 'bar',
        data: {
          labels: hasData ? labels : ['(Chưa có dữ liệu)'],
          datasets: [
            {
              label,
              data: hasData ? values : [0],
              backgroundColor: hasData ? color : 'rgba(148, 163, 184, 0.35)',
              borderRadius: 6,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: true } },
          scales: {
            y: { beginAtZero: true, ticks: { precision: 0 } },
          },
        },
      })
    }

    const makeDoughnut = (canvas, labels, values) => {
      if (!canvas) return null
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      const hasData = values.some((v) => v > 0)
      return new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: hasData ? labels : ['(Trống)'],
          datasets: [
            {
              data: hasData ? values : [1],
              backgroundColor: hasData ? DOUGHNUT_BG : ['rgba(148, 163, 184, 0.4)'],
              borderWidth: 0,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'bottom', labels: { boxWidth: 12 } },
          },
        },
      })
    }

    const c1 = makeBar(
      appCanvasRef.current,
      appChart.labels,
      appChart.values,
      'Số đơn',
      CHART_COLORS.primary
    )
    const c2 = makeDoughnut(jobCanvasRef.current, jobChart.labels, jobChart.values)
    const c3 = makeDoughnut(userCanvasRef.current, userChart.labels, userChart.values)
    chartsRef.current = [c1, c2, c3].filter(Boolean)

    return () => {
      chartsRef.current.forEach((c) => c.destroy())
      chartsRef.current = []
    }
  }, [loading, summary, appChart, jobChart, userChart])

  return (
    <div className="w-full max-w-full px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Thống kê tuyển dụng</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Biểu đồ và báo cáo tổng quan: đơn ứng tuyển, việc làm.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => downloadRecruitmentCsv(summary)}
            disabled={!summary}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            <Download className="h-4 w-4" />
            Tải CSV
          </button>
          <button
            type="button"
            onClick={() => load()}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-3 py-2 text-sm font-semibold text-white hover:bg-[#1d4ed8] disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Tải lại
          </button>
        </div>
      </div>

      {error ? (
        <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-200">
          {error}
        </div>
      ) : null}

      {loading ? (
        <p className="text-sm text-slate-500">Đang tải dữ liệu…</p>
      ) : (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-3">
            <KpiCard
              icon={<BarChart3 className="h-5 w-5 text-blue-600 dark:text-blue-400" />}
              title="Tổng đơn ứng tuyển"
              value={totals.apps}
              hint="Mọi trạng thái trong hệ thống"
            />
            <KpiCard
              icon={<Briefcase className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />}
              title="Tổng tin tuyển dụng"
              value={totals.jobs}
              hint="Theo trạng thái job (nháp / mở / đóng)"
            />
            <KpiCard
              icon={<Users className="h-5 w-5 text-violet-600 dark:text-violet-400" />}
              title="Tổng tài khoản (theo vai trò)"
              value={totals.users}
              hint="User chưa xóa mềm"
            />
          </div>

          <div className="grid gap-6 xl:grid-cols-5">
            <div className="xl:col-span-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h2 className="mb-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
                Ứng tuyển theo trạng thái
              </h2>
              <p className="mb-3 text-xs text-slate-500 dark:text-slate-400">
                Phân bổ pipeline tuyển dụng — hỗ trợ đọc nhanh tình trạng hồ sơ.
              </p>
              <div className="h-80">
                <canvas ref={appCanvasRef} />
              </div>
            </div>

            <div className="space-y-6 xl:col-span-2">
              <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <h2 className="mb-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
                  Việc làm theo trạng thái
                </h2>
                <div className="h-56">
                  <canvas ref={jobCanvasRef} />
                </div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <h2 className="mb-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
                  Người dùng theo vai trò
                </h2>
                <div className="h-56">
                  <canvas ref={userCanvasRef} />
                </div>
              </div>
            </div>
          </div>

          <div className="mt-8 grid gap-6 lg:grid-cols-2">
            <DetailTable
              title="Bảng chi tiết — ứng tuyển"
              rows={appChart.keys.map((k, i) => ({
                key: k,
                label: appChart.labels[i],
                value: appChart.values[i],
              }))}
            />
            <DetailTable
              title="Bảng chi tiết — việc làm & người dùng"
              rows={[
                ...jobChart.keys.map((k, i) => ({
                  key: `job-${k}`,
                  label: `Job · ${jobChart.labels[i]}`,
                  value: jobChart.values[i],
                })),
                ...userChart.keys.map((k, i) => ({
                  key: `user-${k}`,
                  label: `User · ${userChart.labels[i]}`,
                  value: userChart.values[i],
                })),
              ]}
            />
          </div>
        </>
      )}
    </div>
  )
}

function KpiCard({ icon, title, value, hint }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{title}</p>
          <p className="mt-2 text-3xl font-bold tabular-nums text-slate-900 dark:text-slate-50">{value}</p>
          {hint ? <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p> : null}
        </div>
        <div className="rounded-lg bg-slate-50 p-2 dark:bg-slate-800">{icon}</div>
      </div>
    </div>
  )
}

function DetailTable({ title, rows }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <h3 className="mb-3 text-sm font-semibold text-slate-800 dark:text-slate-100">{title}</h3>
      {rows.length === 0 ? (
        <p className="text-sm text-slate-500">Không có dòng dữ liệu.</p>
      ) : (
        <div className="max-h-64 overflow-auto rounded-lg border border-slate-100 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-50 text-xs font-semibold uppercase text-slate-600 dark:bg-slate-800 dark:text-slate-300">
              <tr>
                <th className="px-3 py-2">Hạng mục</th>
                <th className="px-3 py-2 text-right">Số lượng</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr
                  key={r.key}
                  className="border-t border-slate-100 dark:border-slate-800 hover:bg-slate-50/80 dark:hover:bg-slate-800/50"
                >
                  <td className="px-3 py-2 text-slate-700 dark:text-slate-200">{r.label}</td>
                  <td className="px-3 py-2 text-right tabular-nums font-medium text-slate-900 dark:text-slate-100">
                    {r.value}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
