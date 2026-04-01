import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { Lock, RefreshCw, Trash2, Unlock, UserPlus, X, User, Mail, Plus, Check, Building } from 'lucide-react'

function displayRole(role) {
  if (role === 'COMPANY' || role === 'HR_MANAGER') return 'Doanh nghiệp'
  if (role === 'HR') return 'Nhân sự'
  return role || '-'
}

function badgeActive(active) {
  return active
    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
    : 'bg-rose-100 text-rose-700 dark:bg-rose-900/40 dark:text-rose-300'
}

export function CompanyStaffPage() {
  const currentUser = useMemo(() => getUser(), [])

  const [users, setUsers] = useState([])
  const [departments, setDepartments] = useState([])

  const [roleFilter, setRoleFilter] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)

  const [showAddHrModal, setShowAddHrModal] = useState(false)
  const [hrSubmitting, setHrSubmitting] = useState(false)
  const [hrForm, setHrForm] = useState({ fullName: '', email: '', password: '', departmentId: '' })

  function isSelf(u) {
    return !!currentUser?.id && u.id === currentUser.id
  }

  async function fetchDepartments() {
    try {
      const res = await api.get('/api/company/departments')
      setDepartments(res?.data || [])
    } catch {
      // ignore
    }
  }

  async function fetchStaff(nextPage = page) {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        page: String(nextPage),
        size: String(size),
        sort: 'createdAt,desc',
      })
      if (roleFilter) params.append('role', roleFilter)
      const res = await api.get(`/api/company/staff?${params.toString()}`)
      const payload = res?.data || {}
      setUsers(payload.content || [])
      setTotalPages(payload.totalPages || 0)
      setTotalElements(payload.totalElements || 0)
      setPage(payload.number || 0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchStaff(0)
    fetchDepartments()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    setPage(0)
    fetchStaff(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roleFilter])

  async function toggleLock(userItem) {
    if (isSelf(userItem)) return
    const endpoint = userItem.active
      ? `/api/company/staff/${userItem.id}/lock`
      : `/api/company/staff/${userItem.id}/unlock`
    try {
      const res = await api.patch(endpoint, {})
      const updated = res?.data
      setUsers((prev) =>
        prev.map((x) => (x.id === userItem.id ? { ...x, active: updated?.active ?? !userItem.active } : x))
      )
    } catch (e) {
      alert(e?.message || 'Cập nhật thất bại')
    }
  }

  async function deleteUser(userItem) {
    if (isSelf(userItem)) return
    const ok = confirm(`Xóa tài khoản ${userItem.email}?`)
    if (!ok) return
    try {
      await api.delete(`/api/company/staff/${userItem.id}`)
      await fetchStaff(0)
    } catch (e) {
      alert(e?.message || 'Xóa thất bại')
    }
  }

  function openAddHrModal() {
    setHrForm({ fullName: '', email: '', password: '', departmentId: '' })
    setShowAddHrModal(true)
  }

  async function addNewDepartment() {
    const name = prompt('Tên phòng ban mới?')
    if (!name || !name.trim()) return
    try {
      const res = await api.post('/api/company/departments', { name: name.trim() })
      if (res?.data?.id) {
        await fetchDepartments()
        setHrForm((f) => ({ ...f, departmentId: res.data.id }))
      }
    } catch (e) {
      alert(e?.message || 'Tạo phòng ban thất bại')
    }
  }

  async function submitAddHr(e) {
    e.preventDefault()
    if (!hrForm.fullName || !hrForm.email || !hrForm.password) {
      alert('Vui lòng điền đầy đủ họ tên, email, mật khẩu.')
      return
    }
    setHrSubmitting(true)
    try {
      await api.post('/api/company/staff/hr', {
        fullName: hrForm.fullName,
        email: hrForm.email,
        password: hrForm.password,
        departmentId: hrForm.departmentId || null,
      })
      setShowAddHrModal(false)
      await fetchStaff(0)
    } catch (e) {
      alert(e?.message || 'Tạo tài khoản thất bại')
    } finally {
      setHrSubmitting(false)
    }
  }

  return (
    <div className="w-full max-w-full px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Nhân sự công ty</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Quản lý tài khoản HR thuộc công ty của bạn.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={openAddHrModal}
            className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-[#1d4ed8]"
          >
            <UserPlus className="h-4 w-4" />
            Thêm nhân sự
          </button>

          <label className="text-sm text-slate-600 dark:text-slate-300">Lọc</label>
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
          >
            <option value="">Tất cả</option>
            <option value="HR">HR</option>
            <option value="COMPANY">Doanh nghiệp</option>
          </select>

          <button
            type="button"
            onClick={() => fetchStaff(page)}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-4 py-2 text-sm text-[#2563eb] transition hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800"
          >
            <RefreshCw className="h-4 w-4" />
            Làm mới
          </button>
        </div>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {loading ? (
          <div className="px-6 py-4 text-sm text-slate-500 dark:text-slate-400">Đang tải...</div>
        ) : (
          <>
            <div className="w-full overflow-x-auto">
              <table className="w-full table-auto divide-y divide-slate-200 dark:divide-slate-800">
                <thead className="bg-slate-50 dark:bg-slate-800/60">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Họ tên
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Email
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Công ty
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Vai trò
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Phòng ban
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Trạng thái
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Hành động
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {users.map((u) => (
                    <tr key={u.id} className="transition hover:bg-slate-50/70 dark:hover:bg-slate-800/50">
                      <td className="px-4 py-3">
                        <p className="font-medium text-slate-800 dark:text-slate-100">{u.fullName || 'Không có'}</p>
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 dark:text-slate-300">{u.email}</td>
                      <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{u.companyName || '-'}</td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center rounded-full bg-blue-100 px-2.5 py-1 text-xs font-medium text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                          {displayRole(u.role)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-sm text-slate-600 dark:text-slate-300">{u.department || '-'}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={[
                            'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                            badgeActive(u.active),
                          ].join(' ')}
                        >
                          {u.active ? 'Đang hoạt động' : 'Đã khóa'}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => toggleLock(u)}
                            disabled={isSelf(u)}
                            title={u.active ? 'Khóa tài khoản' : 'Mở khóa'}
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-600 dark:hover:bg-slate-800"
                          >
                            {u.active ? <Lock className="h-4 w-4" /> : <Unlock className="h-4 w-4" />}
                          </button>
                          <button
                            type="button"
                            onClick={() => deleteUser(u)}
                            disabled={isSelf(u)}
                            title="Xóa tài khoản"
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-red-200 bg-red-50 text-red-600 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-red-900 dark:bg-red-950/40 dark:text-red-400 dark:hover:bg-red-950/60"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {!loading && users.length === 0 ? (
              <div className="px-6 py-10 text-center text-slate-500 dark:text-slate-400">Chưa có nhân sự nào.</div>
            ) : null}

            <div className="flex items-center justify-between border-t border-slate-200 px-6 py-4 dark:border-slate-800">
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Tổng: <span className="font-semibold text-slate-800 dark:text-slate-200">{totalElements}</span>
              </p>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => fetchStaff(Math.max(0, page - 1))}
                  disabled={page <= 0}
                  className="rounded border border-slate-300 px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50 hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-800"
                >
                  Trước
                </button>
                <span className="text-sm text-slate-600 dark:text-slate-300">
                  Trang {page + 1}/{Math.max(totalPages, 1)}
                </span>
                <button
                  type="button"
                  onClick={() => fetchStaff(Math.min(totalPages - 1, page + 1))}
                  disabled={page + 1 >= totalPages}
                  className="rounded border border-slate-300 px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50 hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-800"
                >
                  Sau
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {showAddHrModal ? (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => setShowAddHrModal(false)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl dark:bg-slate-900"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <UserPlus className="h-5 w-5 text-[#2563eb]" />
                Thêm nhân sự mới
              </h2>
              <button
                onClick={() => setShowAddHrModal(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={submitAddHr}>
              <div className="space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">Họ và tên</label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      value={hrForm.fullName}
                      onChange={(e) => setHrForm((f) => ({ ...f, fullName: e.target.value }))}
                      required
                      className="w-full rounded-lg border border-slate-300 bg-white py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    />
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">Email</label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      type="email"
                      value={hrForm.email}
                      onChange={(e) => setHrForm((f) => ({ ...f, email: e.target.value }))}
                      required
                      className="w-full rounded-lg border border-slate-300 bg-white py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    />
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">Mật khẩu</label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      type="password"
                      value={hrForm.password}
                      onChange={(e) => setHrForm((f) => ({ ...f, password: e.target.value }))}
                      required
                      minLength={6}
                      className="w-full rounded-lg border border-slate-300 bg-white py-2.5 pl-10 pr-3 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    />
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300 flex items-center gap-1">
                    <Building className="h-4 w-4 text-slate-400" />
                    Phòng ban
                  </label>
                  <div className="flex gap-2">
                    <select
                      value={hrForm.departmentId}
                      onChange={(e) => setHrForm((f) => ({ ...f, departmentId: e.target.value }))}
                      className="flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    >
                      <option value="">-- Không chọn --</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={addNewDepartment}
                      className="inline-flex items-center gap-1 rounded-lg border border-dashed border-[#2563eb] px-3 py-2 text-xs font-medium text-[#2563eb] transition hover:bg-blue-50 dark:hover:bg-blue-950/30"
                      title="Tạo phòng ban mới"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Mới
                    </button>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddHrModal(false)}
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={hrSubmitting}
                  className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-[#1d4ed8] disabled:opacity-50"
                >
                  {hrSubmitting ? (
                    <span className="inline-flex items-center gap-2">
                      <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                      Đang tạo...
                    </span>
                  ) : (
                    <>
                      <Check className="h-4 w-4" />
                      Tạo tài khoản
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  )
}
