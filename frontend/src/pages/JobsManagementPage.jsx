import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { Eye, Pencil, Plus, RefreshCw, Trash2, CircleCheck, Ban } from 'lucide-react'
import { QuillEditor } from '../components/QuillEditor.jsx'

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
  const userDepartment = user?.department || ''

  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const [showModal, setShowModal] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [step, setStep] = useState(1)
  const [form, setForm] = useState(() => emptyForm(userDepartment))
  const [saving, setSaving] = useState(false)

  async function fetchJobs(nextPage = page) {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        page: String(nextPage),
        size: String(size),
        sort: 'createdAt,desc',
      })
      const res = await api.get(`/api/jobs/department?${params.toString()}`)
      const payload = res?.data || {}
      setJobs(payload.content || [])
      setTotalPages(payload.totalPages || 0)
      setTotalElements(payload.totalElements || 0)
      setPage(payload.number || 0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchJobs(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function openCreate() {
    setEditingId(null)
    setStep(1)
    setForm(emptyForm(userDepartment))
    setShowModal(true)
  }

  function openEdit(job) {
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
  }

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

  async function publishJob(job) {
    try {
      await api.patch(`/api/jobs/${job.id}/publish`, {})
      await fetchJobs(page)
    } catch (e) {
      alert(e?.message || 'Mở tuyển thất bại')
    }
  }

  async function closeJob(job) {
    try {
      await api.patch(`/api/jobs/${job.id}/close`, {})
      await fetchJobs(page)
    } catch (e) {
      alert(e?.message || 'Đóng tuyển thất bại')
    }
  }

  async function deleteJob(job) {
    if (!confirm(`Xóa tin "${job.title}"?`)) return
    try {
      await api.delete(`/api/jobs/${job.id}`)
      await fetchJobs(0)
    } catch (e) {
      alert(e?.message || 'Xóa thất bại')
    }
  }

  function goKanban(jobId) {
    window.location.href = `/jobs/${jobId}/kanban`
  }

  return (
    <div className="w-full max-w-full px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Quản lý tin tuyển dụng</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Tạo, chỉnh sửa, mở/đóng tuyển dụng và xóa các vị trí công việc của công ty bạn.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-[#1d4ed8]"
          >
            <Plus className="h-4 w-4" />
            Tạo tin tuyển dụng mới
          </button>
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

      <div className="w-full overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {loading ? (
          <div className="p-6">
            <p className="text-sm text-slate-500 dark:text-slate-400">Đang tải danh sách job...</p>
          </div>
        ) : (
          <>
            <div className="w-full overflow-x-auto">
              <table className="w-full min-w-full table-fixed divide-y divide-slate-200 dark:divide-slate-700">
                <thead className="bg-slate-50 dark:bg-slate-800/80">
                  <tr>
                    <th className="w-[28%] px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Tiêu đề
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Cấp bậc
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Loại hình
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Trạng thái
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Hạn nộp
                    </th>
                    <th className="w-[200px] px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      Hành động
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {jobs.map((job) => (
                    <tr key={job.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50">
                      <td className="px-4 py-4">
                        <p className="font-medium text-slate-900 dark:text-white">{job.title || 'Không có tiêu đề'}</p>
                      </td>
                      <td className="px-4 py-4 text-sm text-slate-700 dark:text-slate-300">{job.level || '-'}</td>
                      <td className="px-4 py-4 text-sm text-slate-700 dark:text-slate-300">{job.jobType || '-'}</td>
                      <td className="px-4 py-4">
                        <span
                          className={[
                            'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                            statusClass(job.status),
                          ].join(' ')}
                        >
                          {statusLabel(job.status)}
                        </span>
                      </td>
                      <td className="px-4 py-4 text-sm text-slate-600 dark:text-slate-400">
                        {job.expiresAt ? new Date(job.expiresAt).toLocaleString('vi-VN') : '-'}
                      </td>
                      <td className="px-4 py-4">
                        <div className="flex flex-wrap items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => goKanban(job.id)}
                            title="Xem ứng viên (Kanban)"
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-[#2563eb] transition hover:bg-[#2563eb]/10 dark:border-slate-600 dark:hover:bg-[#2563eb]/20"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => openEdit(job)}
                            title="Sửa tin"
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          {job.status !== 'OPEN' ? (
                            <button
                              type="button"
                              onClick={() => publishJob(job)}
                              title="Mở tuyển"
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 text-emerald-700 transition hover:bg-emerald-50 dark:border-emerald-800 dark:text-emerald-400 dark:hover:bg-emerald-950/40"
                            >
                              <CircleCheck className="h-4 w-4" />
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={() => closeJob(job)}
                              title="Đóng tuyển"
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-amber-200 text-amber-700 transition hover:bg-amber-50 dark:border-amber-800 dark:text-amber-400 dark:hover:bg-amber-950/40"
                            >
                              <Ban className="h-4 w-4" />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => deleteJob(job)}
                            title="Xóa tin"
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-red-200 text-red-600 transition hover:bg-red-50 dark:border-red-900 dark:text-red-400 dark:hover:bg-red-950/40"
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

            {!loading && jobs.length === 0 ? (
              <div className="px-6 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
                Chưa có tin tuyển dụng nào.
              </div>
            ) : null}

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

      {showModal ? (
        <div className="fixed inset-0 z-[100] overflow-y-auto">
          <div className="flex min-h-screen items-center justify-center px-4 py-16">
            <div className="fixed inset-0 bg-black/40" onClick={closeModal} />

            <div className="relative max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl bg-white shadow-xl dark:bg-slate-900">
              <div className="sticky top-0 bg-gradient-to-r from-[#2563eb] to-[#1d4ed8] px-6 py-4 text-white">
                <h2 className="text-xl font-bold">
                  {editingId ? 'Cập nhật tin tuyển dụng' : 'Tạo tin tuyển dụng mới'}
                </h2>
                <p className="mt-1 text-sm text-white/80">
                  Bước <span>{step}</span>/3
                </p>
              </div>

              <div className="px-6 pt-6 pb-4">
                <div className="flex items-center justify-between">
                  {[1, 2, 3].map((s) => (
                    <div key={s} className={['flex-1 flex items-center', s < 3 ? 'mr-2' : ''].join(' ')}>
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
                            'flex-1 h-1 mx-2 rounded transition',
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

              <div className="px-6 py-6 space-y-6">
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
                        className="mt-1 w-full px-3 py-2 rounded border border-gray-300 focus:ring-2 focus:ring-[#2563eb] focus:border-transparent dark:bg-slate-950 dark:border-slate-700"
                      />
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
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
