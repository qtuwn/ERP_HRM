import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser, normalizeUserRole } from '../lib/storage.js'
import { Plus, RefreshCw } from 'lucide-react'
import { QuillEditor } from '../components/QuillEditor.jsx'
import { VirtualizedJobsManagementList } from '../components/VirtualizedJobsManagementList.jsx'

function statusLabel(status) {
  const map = { DRAFT: 'Bản nháp', OPEN: 'Đang tuyển', CLOSED: 'Đã đóng' }
  return map[status] || status || '-'
}

function statusClass(status) {
  if (status === 'OPEN') return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
  if (status === 'CLOSED') return 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
  return 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
}

function toDateTimeLocal(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

function toISOStringFromDateTimeLocal(value) {
  if (!value) return null
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return null
  return d.toISOString()
}

const emptyForm = (department) => ({
  title: '',
  industry: '',
  level: '',
  jobType: '',
  salaryType: 'agreed',
  salaryMin: null,
  salaryMax: null,
  salaryCurrency: 'VND',
  description: '',
  requirements: '',
  benefits: '',
  tags: '',
  companyName: '',
  address: '',
  city: '',
  companySize: '',
  department: department || '',
  notificationEmail: '',
  numberOfPositions: 1,
  expiresAt: '',
})

export function JobsManagementPage() {
  const user = useMemo(() => getUser(), [])
  const userRole = useMemo(() => normalizeUserRole(user?.role), [user])
  const canOpenKanban = userRole === 'HR' || userRole === 'COMPANY'
  const canCreateOrEditJobs = userRole === 'HR' || userRole === 'COMPANY'
  const userDepartment = user?.department || ''

  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [size] = useState(40)
  const [sort, setSort] = useState('createdAt,desc')
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const [showModal, setShowModal] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [step, setStep] = useState(1)
  const [form, setForm] = useState(() => emptyForm(userDepartment))
  const [saving, setSaving] = useState(false)

  const fetchJobs = useCallback(
    async (nextPage) => {
      setLoading(true)
      try {
        const params = new URLSearchParams({
          page: String(nextPage),
          size: String(size),
          sort,
        })
        const res = await api.get(`/api/jobs/department?${params.toString()}`)
        const payload = res?.data || {}
        setJobs(payload.content || [])
        setTotalPages(payload.totalPages || 0)
        setTotalElements(payload.totalElements || 0)
        setPage(payload.number ?? nextPage)
      } finally {
        setLoading(false)
      }
    },
    [size, sort]
  )

  useEffect(() => {
    fetchJobs(page)
  }, [page, fetchJobs])

  function openCreate() {
    setEditingId(null)
    setStep(1)
    setForm(emptyForm(userDepartment))
    setShowModal(true)
  }

  const openEdit = useCallback(
    (job) => {
      setEditingId(job.id)
      setStep(1)
      setForm({
        title: job.title || '',
        industry: job.industry || '',
        level: job.level || '',
        jobType: job.jobType || '',
        salaryType: job.salaryType || 'agreed',
        salaryMin: job.salaryMin ?? null,
        salaryMax: job.salaryMax ?? null,
        salaryCurrency: job.salaryCurrency || 'VND',
        description: job.description || '',
        requirements: job.requirements || '',
        benefits: job.benefits || '',
        tags: job.tags || '',
        companyName: job.companyName || '',
        address: job.address || '',
        city: job.city || '',
        companySize: job.companySize || '',
        department: job.department || userDepartment,
        notificationEmail: job.notificationEmail || '',
        numberOfPositions: job.numberOfPositions || 1,
        expiresAt: toDateTimeLocal(job.expiresAt),
      })
      setShowModal(true)
    },
    [userDepartment]
  )

  function closeModal() {
    setShowModal(false)
    setSaving(false)
  }

  async function saveJob() {
    const payload = {
      ...form,
      requiredSkills: (form.tags || '').trim(),
      expiresAt: toISOStringFromDateTimeLocal(form.expiresAt),
      department: (form.department || userDepartment || '').trim(),
    }

    if (!payload.title || !payload.description || !payload.city) {
      alert('Vui lòng điền các trường bắt buộc (Tiêu đề, Mô tả, Địa điểm).')
      return
    }
    if (!payload.department) {
      alert('Tài khoản HR chưa được gán phòng ban.')
      return
    }

    setSaving(true)
    try {
      if (editingId) {
        await api.put(`/api/jobs/${editingId}`, payload)
      } else {
        await api.post('/api/jobs', payload)
      }
      closeModal()
      await fetchJobs(0)
    } catch (e) {
      alert(e?.message || 'Không thể lưu tin tuyển dụng')
    } finally {
      setSaving(false)
    }
  }

  const publishJob = useCallback(
    async (job) => {
      try {
        await api.patch(`/api/jobs/${job.id}/publish`, {})
        await fetchJobs(page)
      } catch (e) {
        alert(e?.message || 'Mở tuyển thất bại')
      }
    },
    [page, fetchJobs]
  )

  const closeJob = useCallback(
    async (job) => {
      try {
        await api.patch(`/api/jobs/${job.id}/close`, {})
        await fetchJobs(page)
      } catch (e) {
        alert(e?.message || 'Đóng tuyển thất bại')
      }
    },
    [page, fetchJobs]
  )

  const deleteJob = useCallback(
    async (job) => {
      if (!confirm(`Xóa tin "${job.title}"?`)) return
      try {
        await api.delete(`/api/jobs/${job.id}`)
        await fetchJobs(0)
      } catch (e) {
        alert(e?.message || 'Xóa thất bại')
      }
    },
    [fetchJobs]
  )

  const goKanban = useCallback((jobId) => {
    window.location.href = `/jobs/${jobId}/kanban`
  }, [])

  return (
    <div className="w-full min-w-0 max-w-full px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Quản lý tin tuyển dụng</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {userRole === 'COMPANY'
              ? 'Tất cả tin tuyển của công ty (theo mã công ty trên hệ thống), không phụ thuộc tên hiển thị.'
              : userRole === 'HR'
                ? 'Chỉ các tin do tài khoản của bạn tạo, trong phạm vi công ty (theo mã công ty).'
                : 'Admin: xem toàn bộ tin, chỉ mở/đóng/xóa (kiểm duyệt) — không tạo/sửa nội dung tin, không Kanban/chat (HR / công ty).'}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <label className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
            <span className="whitespace-nowrap font-medium">Sắp xếp</span>
            <select
              value={sort}
              onChange={(e) => {
                setSort(e.target.value)
                setPage(0)
              }}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
            >
              <option value="createdAt,desc">Mới tạo nhất</option>
              <option value="createdAt,asc">Cũ nhất</option>
              <option value="title,asc">Tiêu đề A → Z</option>
              <option value="title,desc">Tiêu đề Z → A</option>
              <option value="expiresAt,asc">Hạn nộp sớm → muộn</option>
              <option value="expiresAt,desc">Hạn nộp muộn → sớm</option>
              <option value="status,asc">Trạng thái (A→Z)</option>
              <option value="status,desc">Trạng thái (Z→A)</option>
              <option value="salaryMax,desc">Lương tối đa cao → thấp</option>
              <option value="salaryMax,asc">Lương tối đa thấp → cao</option>
            </select>
          </label>
          {canCreateOrEditJobs ? (
            <button
              type="button"
              onClick={openCreate}
              className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-[#1d4ed8]"
            >
              <Plus className="h-4 w-4" />
              Tạo tin tuyển dụng mới
            </button>
          ) : null}
          <button
            type="button"
            onClick={() => fetchJobs(page)}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-[#2563eb] shadow-sm transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-[#60a5fa] dark:hover:bg-slate-800"
          >
            <RefreshCw className="h-4 w-4" />
            Làm mới
          </button>
        </div>
      </div>

      <div className="w-full min-w-0 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {loading ? (
          <div className="p-6">
            <p className="text-sm text-slate-500 dark:text-slate-400">Đang tải danh sách job...</p>
          </div>
        ) : (
          <>
            {jobs.length > 0 ? (
              <VirtualizedJobsManagementList
                jobs={jobs}
                statusLabel={statusLabel}
                statusClass={statusClass}
                showKanbanButton={canOpenKanban}
                showEditButton={canCreateOrEditJobs}
                goKanban={goKanban}
                openEdit={openEdit}
                publishJob={publishJob}
                closeJob={closeJob}
                deleteJob={deleteJob}
              />
            ) : (
              <div className="px-6 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
                Chưa có tin tuyển dụng nào.
              </div>
            )}

            <div className="flex items-center justify-between border-t border-slate-200 px-4 py-4 dark:border-slate-700">
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Tổng: <span className="font-semibold text-slate-800 dark:text-slate-200">{totalElements}</span> tin
              </p>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => fetchJobs(Math.max(0, page - 1))}
                  disabled={page <= 0}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50 hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-800"
                >
                  Trước
                </button>
                <span className="text-sm text-slate-600 dark:text-slate-400">
                  {page + 1}/{Math.max(totalPages, 1)}
                </span>
                <button
                  type="button"
                  onClick={() => fetchJobs(Math.min(totalPages - 1, page + 1))}
                  disabled={page + 1 >= totalPages}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50 hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-800"
                >
                  Sau
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {showModal && canCreateOrEditJobs ? (
        <div className="fixed inset-0 z-[100] overflow-y-auto">
          <div className="flex min-h-screen items-center justify-center px-4 py-16">
            <div className="fixed inset-0 bg-black/40" onClick={closeModal} />

            <div className="relative max-h-[90vh] w-full min-w-0 max-w-3xl overflow-y-auto overflow-x-hidden rounded-xl bg-white shadow-xl dark:bg-slate-900">
              <div className="sticky top-0 bg-[#2563eb] px-6 py-4 text-white">
                <h2 className="text-xl font-bold">
                  {editingId ? 'Cập nhật tin tuyển dụng' : 'Tạo tin tuyển dụng mới'}
                </h2>
                <p className="mt-1 text-sm text-white/80">
                  Bước <span>{step}</span>/3
                </p>
              </div>

              <div className="min-w-0 px-6 pb-4 pt-6">
                <div className="flex min-w-0 items-center justify-between gap-1">
                  {[1, 2, 3].map((s) => (
                    <div
                      key={s}
                      className={['flex min-w-0 flex-1 items-center', s < 3 ? 'mr-1 sm:mr-2' : ''].join(' ')}
                    >
                      <div
                        className={[
                          'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition',
                          s <= step
                            ? 'bg-[#2563eb] text-white'
                            : 'bg-gray-200 text-gray-600 dark:bg-slate-700 dark:text-slate-300',
                        ].join(' ')}
                      >
                        {s}
                      </div>
                      {s < 3 ? (
                        <div
                          className={[
                            'mx-1 h-1 min-w-[8px] flex-1 rounded transition sm:mx-2',
                            s < step ? 'bg-[#2563eb]' : 'bg-gray-200 dark:bg-slate-700',
                          ].join(' ')}
                        />
                      ) : null}
                    </div>
                  ))}
                </div>
                <div className="flex justify-between text-xs text-gray-500 mt-3">
                  <span>Thông tin cơ bản</span>
                  <span>Chi tiết công việc</span>
                  <span>Cài đặt & Đăng tin</span>
                </div>
              </div>

              <div className="min-w-0 space-y-6 px-6 py-6">
                {step === 1 ? (
                  <div className="space-y-5">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Thông tin cơ bản</h3>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                        Tiêu đề công việc <span className="text-red-500">*</span>
                      </label>
                      <input
                        value={form.title}
                        onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                        type="text"
                        placeholder="Ví dụ: Senior Frontend Developer"
                        className="mt-1 w-full min-w-0 max-w-full box-border px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] focus:border-transparent dark:bg-slate-950 dark:border-slate-700"
                      />
                    </div>
                    <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-3">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Ngành nghề
                        </label>
                        <select
                          value={form.industry}
                          onChange={(e) => setForm((f) => ({ ...f, industry: e.target.value }))}
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        >
                          <option value="">-- Chọn --</option>
                          <option value="IT">IT/Công nghệ</option>
                          <option value="Marketing">Marketing</option>
                          <option value="Sales">Sales/Bán hàng</option>
                          <option value="HR">Human Resources</option>
                          <option value="Finance">Kế toán/Tài chính</option>
                          <option value="Operations">Operations</option>
                          <option value="Design">Design/UX</option>
                          <option value="Other">Khác</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">Cấp bậc</label>
                        <select
                          value={form.level}
                          onChange={(e) => setForm((f) => ({ ...f, level: e.target.value }))}
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        >
                          <option value="">-- Chọn --</option>
                          <option value="Intern">Thực tập sinh</option>
                          <option value="Fresher">Fresher</option>
                          <option value="Junior">Junior</option>
                          <option value="Senior">Senior</option>
                          <option value="Manager">Quản lý/Manager</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">Loại hình</label>
                        <select
                          value={form.jobType}
                          onChange={(e) => setForm((f) => ({ ...f, jobType: e.target.value }))}
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        >
                          <option value="">-- Chọn --</option>
                          <option value="Full-time">Toàn thời gian</option>
                          <option value="Part-time">Bán thời gian</option>
                          <option value="Internship">Thực tập</option>
                          <option value="Freelance">Freelance</option>
                        </select>
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-3 dark:text-slate-200">
                        Mức lương
                      </label>
                      <div className="space-y-3">
                        <div className="flex flex-wrap gap-3">
                          {[
                            ['agreed', 'Thỏa thuận'],
                            ['upto', 'Lên đến...'],
                            ['range', 'Khoảng từ - đến'],
                          ].map(([v, label]) => (
                            <label key={v} className="flex items-center gap-2 cursor-pointer">
                              <input
                                type="radio"
                                checked={form.salaryType === v}
                                onChange={() => setForm((f) => ({ ...f, salaryType: v }))}
                                className="w-4 h-4"
                              />
                              <span className="text-sm">{label}</span>
                            </label>
                          ))}
                        </div>
                        {form.salaryType === 'upto' ? (
                          <div className="flex gap-3">
                            <input
                              type="number"
                              value={form.salaryMax ?? ''}
                              onChange={(e) =>
                                setForm((f) => ({ ...f, salaryMax: e.target.value ? Number(e.target.value) : null }))
                              }
                              placeholder="Số tiền tối đa (VND)"
                              className="flex-1 px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                            />
                          </div>
                        ) : null}
                        {form.salaryType === 'range' ? (
                          <div className="flex gap-3">
                            <input
                              type="number"
                              value={form.salaryMin ?? ''}
                              onChange={(e) =>
                                setForm((f) => ({ ...f, salaryMin: e.target.value ? Number(e.target.value) : null }))
                              }
                              placeholder="Từ (VND)"
                              className="flex-1 px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                            />
                            <input
                              type="number"
                              value={form.salaryMax ?? ''}
                              onChange={(e) =>
                                setForm((f) => ({ ...f, salaryMax: e.target.value ? Number(e.target.value) : null }))
                              }
                              placeholder="Đến (VND)"
                              className="flex-1 px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                            />
                          </div>
                        ) : null}
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                        Địa điểm làm việc <span className="text-red-500">*</span>
                      </label>
                      <input
                        value={form.city}
                        onChange={(e) => setForm((f) => ({ ...f, city: e.target.value }))}
                        type="text"
                        placeholder="Ví dụ: Hà Nội, TP.HCM, Đà Nẵng"
                        className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] focus:border-transparent dark:bg-slate-950 dark:border-slate-700"
                      />
                    </div>
                  </div>
                ) : null}

                {step === 2 ? (
                  <div className="space-y-5">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Chi tiết công việc</h3>
                    <p className="text-sm text-gray-500 dark:text-slate-400">
                      Sử dụng trình soạn thảo để định dạng nội dung (bôi đậm, gạch đầu dòng, v.v.)
                    </p>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2 dark:text-slate-200">
                        Mô tả công việc <span className="text-red-500">*</span>
                      </label>
                      <QuillEditor
                        value={form.description}
                        onChange={(v) => setForm((f) => ({ ...f, description: v }))}
                        placeholder="Nhập mô tả công việc..."
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2 dark:text-slate-200">
                        Yêu cầu ứng viên
                      </label>
                      <QuillEditor
                        value={form.requirements}
                        onChange={(v) => setForm((f) => ({ ...f, requirements: v }))}
                        placeholder="Nhập yêu cầu ứng viên..."
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2 dark:text-slate-200">
                        Quyền lợi & Lợi ích
                      </label>
                      <QuillEditor
                        value={form.benefits}
                        onChange={(v) => setForm((f) => ({ ...f, benefits: v }))}
                        placeholder="Nhập quyền lợi & lợi ích..."
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                        Kỹ năng yêu cầu (dấu phẩy để phân cách)
                      </label>
                      <input
                        value={form.tags}
                        onChange={(e) => setForm((f) => ({ ...f, tags: e.target.value }))}
                        type="text"
                        placeholder="Ví dụ: React, Node.js, MongoDB, English"
                        className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                      />
                    </div>
                  </div>
                ) : null}

                {step === 3 ? (
                  <div className="space-y-5">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Cài đặt & Đăng tin</h3>
                    <div className="bg-gray-50 p-4 rounded-lg space-y-4 dark:bg-slate-950/40 dark:border dark:border-slate-800">
                      <h4 className="font-medium text-gray-900 dark:text-white">Thông tin công ty</h4>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Tên công ty
                        </label>
                        <input
                          value={form.companyName}
                          onChange={(e) => setForm((f) => ({ ...f, companyName: e.target.value }))}
                          type="text"
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Địa chỉ chi tiết
                        </label>
                        <input
                          value={form.address}
                          onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
                          type="text"
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Quy mô công ty
                        </label>
                        <select
                          value={form.companySize}
                          onChange={(e) => setForm((f) => ({ ...f, companySize: e.target.value }))}
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        >
                          <option value="">-- Chọn --</option>
                          <option value="1-50">1-50 nhân viên</option>
                          <option value="50-100">50-100 nhân viên</option>
                          <option value="100-500">100-500 nhân viên</option>
                          <option value="500-1000">500-1000 nhân viên</option>
                          <option value="1000+">1000+ nhân viên</option>
                        </select>
                      </div>
                    </div>

                    <div className="bg-blue-50 p-4 rounded-lg space-y-4 border border-blue-200 dark:bg-blue-950/20 dark:border-blue-900/50">
                      <h4 className="font-medium text-gray-900 dark:text-white">Cài đặt HR</h4>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Email nhận thông báo
                        </label>
                        <input
                          value={form.notificationEmail}
                          onChange={(e) => setForm((f) => ({ ...f, notificationEmail: e.target.value }))}
                          type="email"
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Số lượng tuyển
                        </label>
                        <input
                          value={form.numberOfPositions}
                          onChange={(e) =>
                            setForm((f) => ({ ...f, numberOfPositions: e.target.value ? Number(e.target.value) : 1 }))
                          }
                          type="number"
                          min={1}
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">
                          Hạn chót nộp hồ sơ <span className="text-red-500">*</span>
                        </label>
                        <input
                          value={form.expiresAt}
                          onChange={(e) => setForm((f) => ({ ...f, expiresAt: e.target.value }))}
                          type="datetime-local"
                          className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] dark:bg-slate-950 dark:border-slate-700"
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-slate-200">Phòng ban</label>
                      <input
                        value={form.department}
                        disabled
                        className="mt-1 w-full px-3 py-2 rounded border border-gray-300 bg-gray-100 text-gray-600 cursor-not-allowed pointer-events-none select-none dark:bg-slate-800 dark:border-slate-700 dark:text-slate-300"
                      />
                      {!form.department ? (
                        <p className="mt-1 text-xs text-amber-600">
                          Tài khoản HR chưa có phòng ban. Vui lòng liên hệ quản trị viên để gán phòng ban.
                        </p>
                      ) : null}
                    </div>
                  </div>
                ) : null}
              </div>

              <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-6 py-4 flex justify-between dark:bg-slate-950 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => (step > 1 ? setStep((s) => s - 1) : closeModal())}
                  className="px-4 py-2 rounded border border-gray-300 hover:bg-gray-100 dark:border-slate-700 dark:hover:bg-slate-900"
                >
                  {step > 1 ? '← Quay lại' : 'Hủy'}
                </button>
                <div className="flex gap-3">
                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => (step < 3 ? setStep((s) => s + 1) : saveJob())}
                    className="rounded bg-[#2563eb] px-4 py-2 text-white transition hover:bg-[#1d4ed8] disabled:opacity-60"
                  >
                    {step < 3 ? 'Tiếp tục →' : saving ? 'Đang lưu…' : 'Lưu & Đăng tin'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
