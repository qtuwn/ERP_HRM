import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser, normalizeUserRole } from '../lib/storage.js'
import { Check, RefreshCw, X } from 'lucide-react'

function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return d.toLocaleString('vi-VN')
}

export function AdminCompaniesPage() {
  const user = useMemo(() => getUser(), [])
  const role = useMemo(() => normalizeUserRole(user?.role), [user])

  const [companies, setCompanies] = useState([])
  const [loading, setLoading] = useState(false)
  const [actingId, setActingId] = useState(null)

  async function fetchPending() {
    setLoading(true)
    try {
      const res = await api.get('/api/admin/companies/pending')
      setCompanies(res?.data || [])
    } finally {
      setLoading(false)
    }
  }

  async function setVerify(companyId, verified) {
    setActingId(companyId)
    try {
      await api.patch(`/api/admin/companies/${companyId}/verify`, { verified })
      setCompanies((prev) => prev.filter((c) => c.id !== companyId))
    } catch (e) {
      alert(e?.message || 'Thao tác thất bại')
    } finally {
      setActingId(null)
    }
  }

  useEffect(() => {
    if (role === 'ADMIN') fetchPending()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-slate-900 dark:text-white">Duyệt công ty</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
            Danh sách công ty đang chờ admin xác minh.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchPending}
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
          disabled={loading}
        >
          <RefreshCw className={loading ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
          Làm mới
        </button>
      </div>

      <div className="mt-6 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-950">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 dark:bg-slate-900/40">
              <tr>
                <th className="px-4 py-3 text-left font-semibold text-slate-700 dark:text-slate-200">Công ty</th>
                <th className="px-4 py-3 text-left font-semibold text-slate-700 dark:text-slate-200">Ngày tạo</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-700 dark:text-slate-200">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {companies.length === 0 ? (
                <tr>
                  <td className="px-4 py-6 text-slate-500 dark:text-slate-400" colSpan={3}>
                    {loading ? 'Đang tải…' : 'Không có công ty nào đang chờ duyệt.'}
                  </td>
                </tr>
              ) : (
                companies.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50 dark:hover:bg-slate-900/30">
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-white">{c.name}</div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">{c.id}</div>
                    </td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{formatDate(c.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setVerify(c.id, true)}
                          disabled={actingId === c.id}
                          className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-3 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-60"
                        >
                          <Check className="h-4 w-4" />
                          Duyệt
                        </button>
                        <button
                          type="button"
                          onClick={() => setVerify(c.id, false)}
                          disabled={actingId === c.id}
                          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
                        >
                          <X className="h-4 w-4" />
                          Từ chối
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

