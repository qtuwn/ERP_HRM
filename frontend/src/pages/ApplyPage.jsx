import { useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'
import { UploadCloud, X, CheckCircle2 } from 'lucide-react'

function isValidCvFile(file) {
  if (!file) return false
  const name = file.name?.toLowerCase?.() || ''
  const type = file.type || ''
  const validTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  const byType = validTypes.includes(type)
  const byExt = name.endsWith('.pdf') || name.endsWith('.docx')
  return byType || byExt
}

export function ApplyPage() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const user = useMemo(() => getUser(), [])

  const [step, setStep] = useState(1)
  const [file, setFile] = useState(null)
  const [resumeMode, setResumeMode] = useState('upload') // upload | library
  const [resumes, setResumes] = useState([])
  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [resumesLoading, setResumesLoading] = useState(false)
  const [note, setNote] = useState('')
  const [dragOver, setDragOver] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  if (!user) return <Navigate to="/login" replace state={{ from: `/jobs/${jobId}/apply` }} />

  useEffect(() => {
    let alive = true
    async function loadResumes() {
      if (user?.role !== 'CANDIDATE') return
      setResumesLoading(true)
      try {
        const res = await api.get('/api/users/me/resumes')
        const data = Array.isArray(res?.data) ? res.data : []
        if (!alive) return
        setResumes(data)
        const def = data.find((x) => x.isDefault)?.id
        if (def) setSelectedResumeId(def)
      } catch {
        // ignore
      } finally {
        if (alive) setResumesLoading(false)
      }
    }
    loadResumes()
    return () => {
      alive = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.role])

  function validateAndSetFile(f) {
    if (!isValidCvFile(f)) {
      alert('Chỉ được tải lên PDF hoặc DOCX.')
      return
    }
    if (f.size > 5 * 1024 * 1024) {
      alert('Dung lượng file vượt quá 5MB.')
      return
    }
    setFile(f)
  }

  function next() {
    if (step === 2 && resumeMode === 'upload' && !file) {
      alert('Vui lòng upload CV của bạn.')
      return
    }
    if (step === 2 && resumeMode === 'library' && !selectedResumeId) {
      alert('Vui lòng chọn CV từ kho.')
      return
    }
    setStep((s) => Math.min(4, s + 1))
  }

  function prev() {
    setStep((s) => Math.max(1, s - 1))
  }

  async function submitApplication() {
    if (resumeMode === 'upload' && !file) return
    if (resumeMode === 'library' && !selectedResumeId) return
    setSubmitting(true)
    try {
      if (resumeMode === 'library') {
        await api.post(`/api/jobs/${jobId}/applications/by-resume`, { resumeId: selectedResumeId })
      } else {
        const formData = new FormData()
        formData.append('cv', file)
        // NOTE: backend hiện chỉ nhận cv, chưa nhận note/coverLetter
        await api.post(`/api/jobs/${jobId}/applications`, formData)
      }
      setStep(4)
      setTimeout(() => navigate('/candidate/applications'), 700)
    } catch (e) {
      alert(e?.message || 'Có lỗi xảy ra')
      setStep(2)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="max-w-4xl mx-auto py-10 px-4 sm:px-6">
      <div className="bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 p-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mb-6">Nộp hồ sơ ứng tuyển</h1>
          <div className="flex items-center justify-between relative">
            <div className="absolute left-0 top-1/2 -translate-y-1/2 w-full h-1 bg-slate-200 dark:bg-slate-800 -z-10 rounded-full" />
            {[1, 2, 3, 4].map((s) => (
              <div key={s} className="flex flex-col items-center gap-2 bg-white dark:bg-slate-900 px-2">
                <div
                  className={[
                    'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-colors border-2',
                    step >= s
                      ? 'bg-[#2563eb] border-[#2563eb] text-white'
                      : 'bg-white dark:bg-slate-900 border-slate-300 dark:border-slate-700 text-slate-400',
                  ].join(' ')}
                >
                  {s}
                </div>
                <span className={['text-xs font-medium', step >= s ? 'text-[#2563eb]' : 'text-slate-400'].join(' ')}>
                  {['Thông tin', 'CV', 'Ghi chú', 'Hoàn tất'][s - 1]}
                </span>
              </div>
            ))}
          </div>
        </div>

        {step === 1 ? (
          <div className="space-y-6">
            <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-800 pb-2">
              1. Thông tin cá nhân
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Họ và tên</label>
                <input
                  type="text"
                  disabled
                  value={user?.fullName || ''}
                  className="w-full px-4 py-2 border rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-500 dark:text-slate-300 border-slate-200 dark:border-slate-700"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Email</label>
                <input
                  type="email"
                  disabled
                  value={user?.email || ''}
                  className="w-full px-4 py-2 border rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-500 dark:text-slate-300 border-slate-200 dark:border-slate-700"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Số điện thoại
                </label>
                <input
                  type="text"
                  disabled
                  value={user?.phone || ''}
                  className="w-full px-4 py-2 border rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-500 dark:text-slate-300 border-slate-200 dark:border-slate-700"
                />
              </div>
            </div>
            <p className="text-sm text-slate-500 dark:text-slate-400 italic">
              * Thông tin được lấy từ hồ sơ hiện tại của bạn.
            </p>
            {user?.role === 'CANDIDATE' ? (
              <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 dark:border-blue-900/40 dark:bg-blue-950/30">
                <p className="text-sm text-slate-800 dark:text-slate-200">
                  Bạn có thể upload CV vào <span className="font-semibold">kho cá nhân</span> trước, rồi ở bước 2 chọn
                  &quot;Chọn từ kho CV&quot; thay vì upload mới mỗi lần.
                </p>
                <Link
                  to="/profile/resumes"
                  className="mt-2 inline-block text-sm font-semibold text-[#2563eb] hover:underline"
                >
                  Mở kho CV cá nhân →
                </Link>
              </div>
            ) : null}
          </div>
        ) : null}

        {step === 2 ? (
          <div className="space-y-6">
            <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-800 pb-2">
              2. CV / Resume
            </h2>

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => {
                  setResumeMode('library')
                  setFile(null)
                }}
                className={[
                  'rounded-lg border px-3 py-2 text-sm font-medium',
                  resumeMode === 'library'
                    ? 'border-[#2563eb] bg-[#2563eb]/10 text-[#2563eb]'
                    : 'border-slate-300 bg-white text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200',
                ].join(' ')}
              >
                Chọn từ kho CV
              </button>
              <button
                type="button"
                onClick={() => {
                  setResumeMode('upload')
                  setSelectedResumeId('')
                }}
                className={[
                  'rounded-lg border px-3 py-2 text-sm font-medium',
                  resumeMode === 'upload'
                    ? 'border-[#2563eb] bg-[#2563eb]/10 text-[#2563eb]'
                    : 'border-slate-300 bg-white text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200',
                ].join(' ')}
              >
                Upload CV mới
              </button>
            </div>

            {resumeMode === 'library' ? (
              <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4">
                <div className="text-sm font-semibold text-slate-800 dark:text-slate-100 mb-2">Kho CV của bạn</div>
                {resumesLoading ? (
                  <div className="text-sm text-slate-500">Đang tải...</div>
                ) : resumes.length === 0 ? (
                  <div className="text-sm text-slate-500">
                    Chưa có CV nào trong kho. Vui lòng chọn “Upload CV mới” để tải lên.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {resumes.map((r) => (
                      <label
                        key={r.id}
                        className={[
                          'flex items-center justify-between gap-3 rounded-lg border px-3 py-2 cursor-pointer',
                          String(selectedResumeId) === String(r.id)
                            ? 'border-[#2563eb] bg-[#2563eb]/5'
                            : 'border-slate-200 dark:border-slate-800',
                        ].join(' ')}
                      >
                        <div className="min-w-0">
                          <div className="text-sm font-medium text-slate-800 dark:text-slate-100 truncate">
                            {r.title || 'CV'}
                            {r.isDefault ? (
                              <span className="ml-2 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
                                (Mặc định)
                              </span>
                            ) : null}
                          </div>
                          <div className="text-xs text-slate-500 truncate">{r.originalFilename || ''}</div>
                        </div>
                        <input
                          type="radio"
                          name="resume"
                          value={r.id}
                          checked={String(selectedResumeId) === String(r.id)}
                          onChange={() => setSelectedResumeId(r.id)}
                        />
                      </label>
                    ))}
                  </div>
                )}
              </div>
            ) : null}

            {resumeMode === 'upload' ? (
            <div
              className={[
                'border-2 border-dashed rounded-xl p-8 text-center transition',
                dragOver
                  ? 'border-[#2563eb] bg-blue-50 dark:bg-blue-950/20'
                  : 'border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800/40',
              ].join(' ')}
              onDragOver={(e) => {
                e.preventDefault()
                setDragOver(true)
              }}
              onDragLeave={(e) => {
                e.preventDefault()
                setDragOver(false)
              }}
              onDrop={(e) => {
                e.preventDefault()
                setDragOver(false)
                if (e.dataTransfer.files && e.dataTransfer.files[0]) validateAndSetFile(e.dataTransfer.files[0])
              }}
            >
              <UploadCloud className="mx-auto h-12 w-12 text-slate-400 mb-4" />

              {!file ? (
                <div>
                  <p className="text-slate-600 dark:text-slate-300 mb-2">Kéo thả file CV vào đây hoặc</p>
                  <label className="cursor-pointer bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 px-4 py-2 rounded-lg font-medium hover:bg-slate-50 dark:hover:bg-slate-800 shadow-sm inline-block">
                    Chọn File
                    <input
                      type="file"
                      className="hidden"
                      accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                      onChange={(e) => {
                        const f = e.target.files?.[0]
                        if (f) validateAndSetFile(f)
                      }}
                    />
                  </label>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-4">
                    Chỉ chấp nhận file PDF hoặc DOCX (Max 5MB).
                  </p>
                </div>
              ) : (
                <div className="flex items-center justify-between bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-lg shadow-sm text-left max-w-sm mx-auto">
                  <div className="flex items-center gap-3 overflow-hidden">
                    <div className="w-8 h-8 rounded bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-300 flex items-center justify-center shrink-0">
                      PDF
                    </div>
                    <div className="truncate">
                      <p className="text-sm font-medium text-slate-900 dark:text-slate-100 truncate">{file.name}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {(file.size / 1024 / 1024).toFixed(2)} MB
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setFile(null)}
                    className="text-slate-400 hover:text-rose-500 p-1"
                    aria-label="Remove"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>
              )}
            </div>
            ) : null}
          </div>
        ) : null}

        {step === 3 ? (
          <div className="space-y-6">
            <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-800 pb-2">
              3. Lời nhắn (Tùy chọn)
            </h2>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                Ghi chú cho bộ phận tuyển dụng (Cover Letter)
              </label>
              <textarea
                value={note == null ? '' : note}
                onChange={(e) => setNote(e.target.value)}
                rows={5}
                className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-[#2563eb]/30 focus:border-[#2563eb] outline-none text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 resize-none"
                placeholder="Viết giới thiệu ngắn về bản thân..."
              />
            </div>
          </div>
        ) : null}

        {step === 4 ? (
          <div className="space-y-4 text-center py-10">
            {submitting ? (
              <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-[#2563eb]/20 border-t-[#2563eb]" />
            ) : (
              <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-600" />
            )}
            <div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mb-2">Hoàn tất!</h2>
              <p className="text-slate-500 dark:text-slate-400">Đang chuyển về danh sách đơn ứng tuyển...</p>
              <div className="mt-4">
                <Link to="/candidate/applications" className="text-[#2563eb] font-medium hover:underline">
                  Xem đơn ứng tuyển
                </Link>
              </div>
            </div>
          </div>
        ) : null}

        {step < 4 ? (
          <div className="mt-10 flex justify-between border-t border-slate-200 dark:border-slate-800 pt-6">
            <button
              type="button"
              onClick={prev}
              disabled={step === 1 || submitting}
              className="px-6 py-2.5 rounded-lg font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30 transition-colors"
            >
              Quay lại
            </button>
            {step === 3 ? (
              <button
                type="button"
                onClick={submitApplication}
                disabled={submitting}
                className="bg-[#2563eb] text-white px-6 py-2.5 rounded-lg font-medium hover:bg-[#1d4ed8] transition-colors shadow-sm"
              >
                Xác nhận nộp hồ sơ
              </button>
            ) : (
              <button
                type="button"
                onClick={next}
                disabled={submitting}
                className="bg-slate-900 dark:bg-slate-100 dark:text-slate-900 text-white px-6 py-2.5 rounded-lg font-medium hover:bg-slate-800 dark:hover:bg-white transition-colors shadow-sm"
              >
                Tiếp tục
              </button>
            )}
          </div>
        ) : null}

        <div className="mt-6 text-xs text-slate-500 dark:text-slate-400">
          Quay về{' '}
          <Link to={`/jobs/${jobId}`} className="text-[#2563eb] font-medium hover:underline">
            chi tiết công việc
          </Link>
          .
        </div>
      </div>
    </section>
  )
}
