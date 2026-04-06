import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { getUser, normalizeUserRole } from '../lib/storage.js'
import { ArrowLeft, CheckCircle2, ClipboardList, Loader2, Trash2, Upload, XCircle } from 'lucide-react'

const DOC_TYPE_VI = {
  ID_CARD: 'CMND / CCCD',
  HOUSEHOLD_BOOK: 'Sổ hộ khẩu',
  DEGREE: 'Bằng cấp / chứng chỉ',
  HEALTH_CERTIFICATE: 'Giấy khám sức khỏe',
  CRIMINAL_RECORD: 'Lý lịch tư pháp',
  BANK_INFO: 'Thông tin tài khoản ngân hàng',
  LABOR_CONTRACT_DRAFT: 'Nháp hợp đồng lao động',
  OTHER: 'Khác',
}

const STATUS_VI = {
  OPEN: 'Chưa nộp',
  SUBMITTED: 'Đã nộp — chờ HR',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Cần bổ sung',
}

function statusClass(s) {
  const x = String(s || '')
  if (x === 'APPROVED') return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-200'
  if (x === 'REJECTED') return 'bg-rose-100 text-rose-800 dark:bg-rose-950/40 dark:text-rose-200'
  if (x === 'SUBMITTED') return 'bg-amber-100 text-amber-900 dark:bg-amber-950/40 dark:text-amber-100'
  return 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200'
}

export function ApplicationTasksPage() {
  const { applicationId } = useParams()
  const user = useMemo(() => getUser(), [])
  const role = normalizeUserRole(user?.role)
  const isRecruiter = role === 'HR' || role === 'ADMIN' || role === 'COMPANY'

  const [tasks, setTasks] = useState([])
  const [docTypes, setDocTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [createOpen, setCreateOpen] = useState(false)
  const [createTitle, setCreateTitle] = useState('')
  const [createDesc, setCreateDesc] = useState('')
  const [createType, setCreateType] = useState('OTHER')
  const [createDue, setCreateDue] = useState('')
  const [createBusy, setCreateBusy] = useState(false)

  const [uploadBusy, setUploadBusy] = useState(false)
  const [reviewBusy, setReviewBusy] = useState(false)
  const [reviewNote, setReviewNote] = useState('')

  const backHref = isRecruiter ? '/jobs/management' : '/candidate/applications'

  const loadList = useCallback(async () => {
    setLoading(true)
    setErr('')
    try {
      const res = await api.get(`/api/applications/${applicationId}/tasks`)
      setTasks(Array.isArray(res?.data) ? res.data : [])
    } catch (e) {
      setErr(e?.message || 'Không tải được danh sách nhiệm vụ.')
      setTasks([])
    } finally {
      setLoading(false)
    }
  }, [applicationId])

  const loadDocTypes = useCallback(async () => {
    try {
      const res = await api.get('/api/application-tasks/document-types')
      setDocTypes(Array.isArray(res?.data) ? res.data : [])
    } catch {
      setDocTypes(Object.keys(DOC_TYPE_VI))
    }
  }, [])

  useEffect(() => {
    loadDocTypes()
  }, [loadDocTypes])

  useEffect(() => {
    loadList()
  }, [loadList])

  const loadDetail = useCallback(
    async (taskId) => {
      if (!taskId) {
        setDetail(null)
        return
      }
      setDetailLoading(true)
      try {
        const res = await api.get(`/api/applications/${applicationId}/tasks/${taskId}`)
        setDetail(res?.data || null)
        setReviewNote('')
      } catch (e) {
        alert(e?.message || 'Không tải chi tiết.')
        setDetail(null)
      } finally {
        setDetailLoading(false)
      }
    },
    [applicationId]
  )

  useEffect(() => {
    if (selectedId) loadDetail(selectedId)
    else setDetail(null)
  }, [selectedId, loadDetail])

  async function onCreateTask(e) {
    e.preventDefault()
    setCreateBusy(true)
    try {
      const body = {
        title: createTitle.trim(),
        description: createDesc.trim() || null,
        documentType: createType,
        dueAt: createDue ? new Date(createDue).toISOString() : null,
      }
      const res = await api.post(`/api/applications/${applicationId}/tasks`, body)
      const created = res?.data
      setCreateOpen(false)
      setCreateTitle('')
      setCreateDesc('')
      setCreateType('OTHER')
      setCreateDue('')
      await loadList()
      if (created?.id) setSelectedId(created.id)
    } catch (e) {
      alert(e?.message || 'Không tạo được nhiệm vụ.')
    } finally {
      setCreateBusy(false)
    }
  }

  async function onUpload(file) {
    if (!file || !selectedId) return
    setUploadBusy(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await api.post(`/api/applications/${applicationId}/tasks/${selectedId}/attachments`, fd)
      setDetail(res?.data || null)
      await loadList()
    } catch (e) {
      alert(e?.message || 'Upload thất bại.')
    } finally {
      setUploadBusy(false)
    }
  }

  async function onReview(status) {
    if (!selectedId) return
    setReviewBusy(true)
    try {
      const res = await api.patch(`/api/applications/${applicationId}/tasks/${selectedId}/review`, {
        status,
        hrFeedback: reviewNote.trim() || null,
      })
      setDetail(res?.data || null)
      await loadList()
    } catch (e) {
      alert(e?.message || 'Không lưu được đánh giá.')
    } finally {
      setReviewBusy(false)
    }
  }

  async function onDeleteTask() {
    if (!selectedId) return
    if (!confirm('Xóa nhiệm vụ này? Tệp đính kèm cũng sẽ bị xóa khỏi hệ thống.')) return
    try {
      await api.delete(`/api/applications/${applicationId}/tasks/${selectedId}`)
      setSelectedId(null)
      setDetail(null)
      await loadList()
    } catch (e) {
      alert(e?.message || 'Không xóa được.')
    }
  }

  async function onDeleteAttachment(attId) {
    if (!selectedId || !attId) return
    if (!confirm('Xóa tệp này?')) return
    try {
      await api.delete(`/api/applications/${applicationId}/tasks/${selectedId}/attachments/${attId}`)
      await loadDetail(selectedId)
      await loadList()
    } catch (e) {
      alert(e?.message || 'Không xóa được tệp.')
    }
  }

  return (
    <section className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:max-w-7xl">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to={backHref}
            className="inline-flex items-center gap-1 text-sm font-medium text-slate-600 hover:text-[#2563eb] dark:text-slate-400"
          >
            <ArrowLeft className="h-4 w-4" />
            Quay lại
          </Link>
        </div>
        <h1 className="flex items-center gap-2 text-xl font-bold text-slate-900 dark:text-white sm:text-2xl">
          <ClipboardList className="h-7 w-7 text-[#2563eb]" />
          Nhiệm vụ &amp; tài liệu
        </h1>
        <div className="w-full sm:w-auto sm:min-w-[120px]" />
      </div>

      <p className="mb-6 text-sm text-slate-600 dark:text-slate-400">
        {isRecruiter
          ? 'Giao việc cho ứng viên, xem tài liệu họ nộp và duyệt / yêu cầu bổ sung.'
          : 'Xem yêu cầu từ HR, tải lên đúng loại giấy tờ được chỉ định.'}
      </p>

      {isRecruiter ? (
        <div className="mb-6">
          <button
            type="button"
            onClick={() => setCreateOpen((v) => !v)}
            className="rounded-xl bg-[#2563eb] px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-[#1d4ed8]"
          >
            {createOpen ? 'Đóng form' : '+ Giao nhiệm vụ mới'}
          </button>
          {createOpen ? (
            <form
              onSubmit={onCreateTask}
              className="mt-4 space-y-4 rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-700 dark:bg-slate-900"
            >
              <div>
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Tiêu đề *</label>
                <input
                  required
                  value={createTitle}
                  onChange={(e) => setCreateTitle(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                  placeholder="VD: Nộp bản scan CCCD mặt trước & sau"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Mô tả / yêu cầu chi tiết</label>
                <textarea
                  value={createDesc}
                  onChange={(e) => setCreateDesc(e.target.value)}
                  rows={4}
                  className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Loại giấy tờ *</label>
                  <select
                    value={createType}
                    onChange={(e) => setCreateType(e.target.value)}
                    className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                  >
                    {(docTypes.length ? docTypes : Object.keys(DOC_TYPE_VI)).map((k) => (
                      <option key={k} value={k}>
                        {DOC_TYPE_VI[k] || k}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500 dark:text-slate-400">Hạn hoàn thành (tuỳ chọn)</label>
                  <input
                    type="datetime-local"
                    value={createDue}
                    onChange={(e) => setCreateDue(e.target.value)}
                    className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={createBusy}
                className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
              >
                {createBusy ? 'Đang tạo…' : 'Tạo nhiệm vụ'}
              </button>
            </form>
          ) : null}
        </div>
      ) : null}

      {err ? (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800 dark:border-rose-900/50 dark:bg-rose-950/30 dark:text-rose-200">
          {err}
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <h2 className="mb-3 text-sm font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Danh sách ({tasks.length})
          </h2>
          {loading ? (
            <div className="flex items-center gap-2 text-sm text-slate-500">
              <Loader2 className="h-4 w-4 animate-spin" /> Đang tải…
            </div>
          ) : tasks.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">Chưa có nhiệm vụ nào.</p>
          ) : (
            <ul className="space-y-2">
              {tasks.map((t) => (
                <li key={t.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(t.id)}
                    className={[
                      'w-full rounded-xl border px-4 py-3 text-left transition',
                      selectedId === t.id
                        ? 'border-[#2563eb] bg-blue-50/80 shadow-sm dark:border-blue-500 dark:bg-blue-950/30'
                        : 'border-slate-200 bg-white hover:border-slate-300 dark:border-slate-700 dark:bg-slate-900',
                    ].join(' ')}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-semibold text-slate-900 dark:text-white">{t.title}</span>
                      <span className={`shrink-0 rounded-md px-2 py-0.5 text-[10px] font-bold ${statusClass(t.status)}`}>
                        {STATUS_VI[t.status] || t.status}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                      {DOC_TYPE_VI[t.documentType] || t.documentType}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-700 dark:bg-slate-900 lg:col-span-3">
          {!selectedId ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">Chọn một nhiệm vụ để xem chi tiết.</p>
          ) : detailLoading ? (
            <div className="flex items-center gap-2 text-slate-500">
              <Loader2 className="h-5 w-5 animate-spin" /> Đang tải chi tiết…
            </div>
          ) : detail ? (
            <div className="space-y-5">
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 pb-4 dark:border-slate-700">
                <div>
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">{detail.title}</h3>
                  <p className="mt-1 text-xs text-slate-500">
                    Loại: <span className="font-medium">{DOC_TYPE_VI[detail.documentType] || detail.documentType}</span>
                  </p>
                  {detail.dueAt ? (
                    <p className="mt-1 text-xs text-slate-500">
                      Hạn: {new Date(detail.dueAt).toLocaleString('vi-VN')}
                    </p>
                  ) : null}
                </div>
                <span className={`rounded-lg px-2.5 py-1 text-xs font-bold ${statusClass(detail.status)}`}>
                  {STATUS_VI[detail.status] || detail.status}
                </span>
              </div>

              {detail.description ? (
                <div>
                  <h4 className="text-xs font-bold uppercase text-slate-500 dark:text-slate-400">Yêu cầu</h4>
                  <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700 dark:text-slate-200">{detail.description}</p>
                </div>
              ) : null}

              {detail.hrFeedback ? (
                <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3 text-sm text-amber-950 dark:border-amber-900/40 dark:bg-amber-950/20 dark:text-amber-100">
                  <span className="font-semibold">Phản hồi HR:</span> {detail.hrFeedback}
                </div>
              ) : null}

              <div>
                <h4 className="text-xs font-bold uppercase text-slate-500 dark:text-slate-400">Tệp đính kèm</h4>
                <ul className="mt-2 space-y-2">
                  {(detail.attachments || []).map((a) => (
                    <li
                      key={a.id}
                      className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-slate-200 px-3 py-2 dark:border-slate-600"
                    >
                      <div className="min-w-0">
                        <a
                          href={a.downloadUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="text-sm font-medium text-[#2563eb] hover:underline"
                        >
                          {a.originalFilename || 'Tải xuống'}
                        </a>
                        <p className="text-xs text-slate-500">
                          {a.createdAt ? new Date(a.createdAt).toLocaleString('vi-VN') : ''}
                        </p>
                      </div>
                      {!isRecruiter && detail.status !== 'APPROVED' ? (
                        <button
                          type="button"
                          onClick={() => onDeleteAttachment(a.id)}
                          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-rose-600 hover:bg-rose-50 dark:border-slate-600 dark:hover:bg-rose-950/30"
                        >
                          <Trash2 className="h-3 w-3" /> Xóa
                        </button>
                      ) : isRecruiter ? (
                        <button
                          type="button"
                          onClick={() => onDeleteAttachment(a.id)}
                          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-600 hover:bg-slate-50 dark:border-slate-600"
                        >
                          <Trash2 className="h-3 w-3" /> Gỡ
                        </button>
                      ) : null}
                    </li>
                  ))}
                </ul>
                {!isRecruiter && detail.status !== 'APPROVED' ? (
                  <label className="mt-4 flex cursor-pointer items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-300 px-4 py-6 text-sm font-medium text-[#2563eb] hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-800/50">
                    <Upload className="h-5 w-5" />
                    {uploadBusy ? 'Đang tải lên…' : 'Chọn tệp (PDF, DOCX, PNG, JPEG — tối đa 10MB)'}
                    <input
                      type="file"
                      accept=".pdf,.docx,.png,.jpg,.jpeg,application/pdf,image/*"
                      className="hidden"
                      disabled={uploadBusy}
                      onChange={(e) => {
                        const f = e.target.files?.[0]
                        e.target.value = ''
                        if (f) onUpload(f)
                      }}
                    />
                  </label>
                ) : null}
              </div>

              {isRecruiter && (detail.status === 'SUBMITTED' || detail.status === 'REJECTED' || detail.status === 'OPEN') ? (
                <div className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-700">
                  <h4 className="text-xs font-bold uppercase text-slate-500">Duyệt nhiệm vụ</h4>
                  <textarea
                    value={reviewNote}
                    onChange={(e) => setReviewNote(e.target.value)}
                    rows={2}
                    placeholder="Ghi chú cho ứng viên (bắt buộc khi từ chối)…"
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                  />
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      disabled={reviewBusy || detail.status === 'OPEN'}
                      onClick={() => onReview('APPROVED')}
                      className="inline-flex items-center gap-1 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                    >
                      <CheckCircle2 className="h-4 w-4" /> Duyệt
                    </button>
                    <button
                      type="button"
                      disabled={reviewBusy || detail.status === 'OPEN'}
                      onClick={() => onReview('REJECTED')}
                      className="inline-flex items-center gap-1 rounded-xl border border-rose-300 bg-white px-4 py-2 text-sm font-semibold text-rose-700 disabled:opacity-50 dark:bg-slate-900"
                    >
                      <XCircle className="h-4 w-4" /> Từ chối / yêu cầu bổ sung
                    </button>
                  </div>
                  {detail.status === 'OPEN' ? (
                    <p className="text-xs text-slate-500">Chờ ứng viên nộp tài liệu trước khi duyệt.</p>
                  ) : null}
                </div>
              ) : null}

              {isRecruiter ? (
                <div className="border-t border-slate-200 pt-4 dark:border-slate-700">
                  <button
                    type="button"
                    onClick={onDeleteTask}
                    className="text-sm font-medium text-rose-600 hover:underline"
                  >
                    Xóa nhiệm vụ
                  </button>
                </div>
              ) : null}
            </div>
          ) : (
            <p className="text-sm text-slate-500">Không có dữ liệu.</p>
          )}
        </div>
      </div>
    </section>
  )
}
