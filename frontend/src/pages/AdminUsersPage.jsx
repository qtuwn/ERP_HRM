import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { Lock, RefreshCw, Trash2, Unlock, Pencil, UserPlus, X, User, Mail, Building, Plus, Check } from 'lucide-react'

function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return d.toLocaleString('vi-VN')
}

function displayRole(role) {
  if (role === 'ADMIN' || role === 'SUPER_ADMIN') return 'Quản trị viên'
  if (role === 'COMPANY' || role === 'HR_MANAGER') return 'Doanh nghiệp'
  if (role === 'HR') return 'Nhân sự'
  if (role === 'CANDIDATE') return 'Ứng viên'
  return role || '-'
}

function badgeActive(active) {
  return active
    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
    : 'bg-rose-100 text-rose-700 dark:bg-rose-900/40 dark:text-rose-300'
}

const fieldSelectClass =
  'h-9 w-full min-w-0 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-800 shadow-sm focus:border-[#2563eb] focus:outline-none focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100'

const tableSelectClass =
  'h-8 w-full max-w-[9.5rem] rounded-lg border border-slate-200 bg-white px-2 text-xs font-medium text-slate-800 shadow-sm focus:border-[#2563eb] focus:outline-none focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100'

export function AdminUsersPage() {
  const currentUser = useMemo(() => getUser(), [])
  const isCompanyRole = currentUser?.role === 'COMPANY'

  const [users, setUsers] = useState([])
  const [companies, setCompanies] = useState([])
  const [departments, setDepartments] = useState([])

  const [roleFilter, setRoleFilter] = useState('')
  const [companyFilter, setCompanyFilter] = useState('')

  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)

  const [showAddHrModal, setShowAddHrModal] = useState(false)
  const [hrSubmitting, setHrSubmitting] = useState(false)
  const [hrForm, setHrForm] = useState({
    fullName: '',
    email: '',
    password: '',
    departmentId: '',
    companyId: '',
  })

  async function fetchCompanies() {
    try {
      const res = await api.get('/api/admin/companies')
      setCompanies(res?.data || [])
    } catch {
      // ignore
    }
  }

  async function fetchDepartmentsCompany() {
    try {
      const res = await api.get('/api/company/departments')
      setDepartments(res?.data || [])
    } catch {
      // ignore
    }
  }

  async function fetchUsers(nextPage = page) {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        page: String(nextPage),
        size: String(size),
        sort: 'createdAt,desc',
      })
      if (roleFilter) params.append('role', roleFilter)
      if (!isCompanyRole && companyFilter) params.append('companyId', companyFilter)

      const res = await api.get(`/api/admin/users?${params.toString()}`)
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
    fetchUsers(0)
    if (!isCompanyRole) fetchCompanies()
    if (isCompanyRole) fetchDepartmentsCompany()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    setPage(0)
    fetchUsers(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roleFilter, companyFilter])

  function isSelf(u) {
    return !!currentUser?.id && u.id === currentUser.id
  }

  async function updateRole(userItem, role) {
    if (!role || userItem.role === role) return
    try {
      const res = await api.patch(`/api/admin/users/${userItem.id}/role`, { role })
      const updated = res?.data
      setUsers((prev) => prev.map((x) => (x.id === userItem.id ? { ...x, role: updated?.role || role } : x)))
    } catch (e) {
      alert(e?.message || 'Cập nhật vai trò thất bại')
      fetchUsers(page)
    }
  }

  async function editDepartment(userItem) {
    const val = prompt('Nhập phòng ban mới', userItem.department || '')
    if (val === null) return
    try {
      const res = await api.patch(`/api/admin/users/${userItem.id}/department`, { department: val })
      const updated = res?.data
      setUsers((prev) => prev.map((x) => (x.id === userItem.id ? { ...x, department: updated?.department ?? val } : x)))
    } catch (e) {
      alert(e?.message || 'Cập nhật phòng ban thất bại')
    }
  }

  async function toggleLock(userItem) {
    if (isSelf(userItem)) return
    const endpoint = userItem.active ? `/api/admin/users/${userItem.id}/lock` : `/api/admin/users/${userItem.id}/unlock`
    try {
      const res = await api.patch(endpoint, {})
      const updated = res?.data
      setUsers((prev) =>
        prev.map((x) => (x.id === userItem.id ? { ...x, active: updated?.active ?? !userItem.active } : x))
      )
    } catch (e) {
      alert(e?.message || 'Cập nhật trạng thái thất bại')
    }
  }

  async function deleteUser(userItem) {
    if (isSelf(userItem)) return
    const ok = confirm(`Xóa tài khoản ${userItem.email}?`)
    if (!ok) return
    try {
      await api.delete(`/api/admin/users/${userItem.id}`)
      await fetchUsers(0)
    } catch (e) {
      alert(e?.message || 'Xóa thất bại')
    }
  }

  function openAddHrModal() {
    setHrForm({
      fullName: '',
      email: '',
      password: '',
      departmentId: '',
      companyId: !isCompanyRole && companyFilter ? companyFilter : '',
    })
    setShowAddHrModal(true)
  }

  async function fetchDepartmentsForAdminCompany(companyId) {
    if (!companyId) {
      setDepartments([])
      return
    }
    try {
      const res = await api.get(`/api/companies/${companyId}/departments`)
      setDepartments(res?.data || [])
    } catch {
      setDepartments([])
    }
  }

  useEffect(() => {
    if (!showAddHrModal || isCompanyRole || !hrForm.companyId) return
    fetchDepartmentsForAdminCompany(hrForm.companyId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showAddHrModal, isCompanyRole, hrForm.companyId])

  async function addNewDepartment() {
    const name = prompt('Tên phòng ban mới?')
    if (!name || !name.trim()) return
    try {
      if (isCompanyRole) {
        const res = await api.post('/api/company/departments', { name: name.trim() })
        if (res?.data?.id) {
          await fetchDepartmentsCompany()
          setHrForm((f) => ({ ...f, departmentId: res.data.id }))
        }
      } else {
        if (!hrForm.companyId) {
          alert('Chọn công ty trước khi tạo phòng ban.')
          return
        }
        const res = await api.post(`/api/companies/${hrForm.companyId}/departments`, { name: name.trim() })
        if (res?.data?.id) {
          await fetchDepartmentsForAdminCompany(hrForm.companyId)
          setHrForm((f) => ({ ...f, departmentId: res.data.id }))
        }
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
      if (isCompanyRole) {
        await api.post('/api/company/staff/hr', {
          fullName: hrForm.fullName,
          email: hrForm.email,
          password: hrForm.password,
          departmentId: hrForm.departmentId || null,
        })
      } else {
        if (!hrForm.companyId) {
          alert('Vui lòng chọn công ty.')
          setHrSubmitting(false)
          return
        }
        await api.post(`/api/companies/${hrForm.companyId}/hr-accounts`, {
          fullName: hrForm.fullName,
          email: hrForm.email,
          password: hrForm.password,
          departmentId: hrForm.departmentId || null,
        })
      }
      setShowAddHrModal(false)
      await fetchUsers(0)
    } catch (e) {
      alert(e?.message || 'Tạo tài khoản thất bại')
    } finally {
      setHrSubmitting(false)
    }
  }

  return (
    <div className="w-full max-w-full px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Quản lý tài khoản</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isCompanyRole
              ? 'Quản lý nhân sự thuộc công ty của bạn.'
              : 'Quản trị vai trò và trạng thái tài khoản người dùng.'}
          </p>
        </div>

        <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:flex-row sm:flex-wrap sm:items-end">
          {!isCompanyRole ? (
            <div className="flex min-w-[10rem] flex-1 flex-col gap-1 sm:max-w-xs">
              <label className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Công ty
              </label>
              <select
                value={companyFilter}
                onChange={(e) => setCompanyFilter(e.target.value)}
                className={fieldSelectClass}
              >
                <option value="">Tất cả</option>
                {companies.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          ) : null}

          <div className="flex min-w-[10rem] flex-1 flex-col gap-1 sm:max-w-[11rem]">
            <label className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Lọc vai trò
            </label>
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className={fieldSelectClass}
            >
              <option value="">Tất cả</option>
              {isCompanyRole ? (
                <>
                  <option value="HR">HR</option>
                  <option value="COMPANY">Doanh nghiệp</option>
                </>
              ) : (
                <>
                  <option value="ADMIN">Quản trị viên</option>
                  <option value="COMPANY">Doanh nghiệp</option>
                  <option value="HR">Nhân sự</option>
                  <option value="CANDIDATE">Ứng viên</option>
                </>
              )}
            </select>
          </div>

          <div className="flex flex-wrap items-center gap-2 sm:ml-auto">
            {isCompanyRole ? (
              <button
                type="button"
                onClick={openAddHrModal}
                className="inline-flex h-9 items-center gap-2 rounded-lg bg-[#2563eb] px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-[#1d4ed8]"
              >
                <UserPlus className="h-4 w-4 shrink-0" />
                Thêm nhân sự
              </button>
            ) : (
              <button
                type="button"
                onClick={openAddHrModal}
                className="inline-flex h-9 items-center gap-2 rounded-lg bg-[#2563eb] px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-[#1d4ed8]"
              >
                <UserPlus className="h-4 w-4 shrink-0" />
                Thêm tài khoản HR
              </button>
            )}
            <button
              type="button"
              onClick={() => fetchUsers(page)}
              className="inline-flex h-9 items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700/80"
            >
              <RefreshCw className="h-4 w-4 shrink-0" />
              Làm mới
            </button>
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {loading ? (
          <div className="px-6 py-4 text-sm text-slate-500 dark:text-slate-400">Đang tải danh sách người dùng...</div>
        ) : (
          <>
            <div className="w-full overflow-x-auto">
              <table className="w-full min-w-[56rem] table-fixed">
                <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/60">
                  <tr>
                    <th className="w-[12%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Họ tên
                    </th>
                    <th className="w-[18%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Email
                    </th>
                    <th className="w-[12%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Công ty
                    </th>
                    <th className="w-[11%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Vai trò
                    </th>
                    <th className="w-[10%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Phòng ban
                    </th>
                    <th className="w-[12%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Trạng thái
                    </th>
                    <th className="w-[14%] px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Tạo lúc
                    </th>
                    <th className="w-[11%] px-3 py-2.5 text-right text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Thao tác
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {users.map((u) => (
                    <tr key={u.id} className="align-middle transition hover:bg-slate-50/80 dark:hover:bg-slate-800/40">
                      <td className="px-3 py-2.5 align-middle">
                        <p className="truncate font-medium text-slate-800 dark:text-slate-100" title={u.fullName || ''}>
                          {u.fullName || 'Không có'}
                        </p>
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        <p
                          className="truncate text-sm text-slate-700 dark:text-slate-300"
                          title={u.email || ''}
                        >
                          {u.email}
                        </p>
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        <p className="truncate text-sm text-slate-600 dark:text-slate-300" title={u.companyName || ''}>
                          {u.companyName || '—'}
                        </p>
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        {isSelf(u) || isCompanyRole ? (
                          <span className="inline-flex max-w-full items-center rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-200">
                            <span className="truncate">{displayRole(u.role)}</span>
                          </span>
                        ) : (
                          <select
                            value={u.role}
                            onChange={(e) => updateRole(u, e.target.value)}
                            className={tableSelectClass}
                          >
                            <option value="ADMIN">Quản trị viên</option>
                            <option value="COMPANY">Doanh nghiệp</option>
                            <option value="HR">Nhân sự</option>
                            <option value="CANDIDATE">Ứng viên</option>
                          </select>
                        )}
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        <div className="group flex min-w-0 items-center gap-1">
                          <span className="truncate text-sm text-slate-600 dark:text-slate-300" title={u.department || ''}>
                            {u.department || '—'}
                          </span>
                          <button
                            type="button"
                            onClick={() => editDepartment(u)}
                            className="shrink-0 rounded p-0.5 text-[#2563eb] opacity-60 transition hover:opacity-100 group-hover:opacity-100"
                            title="Chỉnh sửa phòng ban"
                          >
                            <Pencil className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        <span
                          className={[
                            'inline-flex whitespace-nowrap rounded-md px-2 py-1 text-xs font-medium',
                            badgeActive(u.active),
                          ].join(' ')}
                        >
                          {u.active ? 'Hoạt động' : 'Đã khóa'}
                        </span>
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        <span className="whitespace-nowrap text-xs tabular-nums text-slate-600 dark:text-slate-400">
                          {formatDate(u.createdAt)}
                        </span>
                      </td>
                      <td className="px-3 py-2.5 align-middle">
                        <div className="flex justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => toggleLock(u)}
                            disabled={isSelf(u)}
                            title={u.active ? 'Khóa tài khoản' : 'Mở khóa'}
                            className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800 dark:hover:bg-slate-700"
                          >
                            {u.active ? <Lock className="h-3.5 w-3.5" /> : <Unlock className="h-3.5 w-3.5" />}
                          </button>
                          <button
                            type="button"
                            onClick={() => deleteUser(u)}
                            disabled={isSelf(u)}
                            title="Xóa tài khoản"
                            className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-red-200 bg-red-50 text-red-600 shadow-sm transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-40 dark:border-red-900/60 dark:bg-red-950/50 dark:text-red-400 dark:hover:bg-red-950/70"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {!loading && users.length === 0 ? (
              <div className="px-6 py-10 text-center text-slate-500 dark:text-slate-400">
                Không tìm thấy tài khoản nào với bộ lọc hiện tại.
              </div>
            ) : null}

            <div className="flex items-center justify-between border-t border-slate-200 px-6 py-4 dark:border-slate-800">
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Tổng: <span className="font-semibold text-slate-800 dark:text-slate-200">{totalElements}</span> tài
                khoản
              </p>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => fetchUsers(Math.max(0, page - 1))}
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
                  onClick={() => fetchUsers(Math.min(totalPages - 1, page + 1))}
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
                {!isCompanyRole ? (
                  <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">
                      Công ty
                    </label>
                    <select
                      required
                      value={hrForm.companyId}
                      onChange={(e) =>
                        setHrForm((f) => ({ ...f, companyId: e.target.value, departmentId: '' }))
                      }
                      className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    >
                      <option value="">— Chọn công ty —</option>
                      {companies.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : null}

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
                      placeholder="Nguyễn Văn A"
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
                      placeholder="hr@company.com"
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
                      placeholder="Tối thiểu 6 ký tự"
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
