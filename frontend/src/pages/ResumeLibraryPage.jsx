import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { FileText, Loader2, Star, Trash2, UploadCloud } from 'lucide-react'
import { api } from '../lib/api.js'
import { getUser } from '../lib/storage.js'

function isValidCvFile(file) {
  if (!file) return false
  const name = file.name?.toLowerCase?.() || ''
  const type = file.type || ''
  const validTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  const byType = validTypes.includes(type)
  const byExt = name.endsWith('.pdf') || name.endsWith('.docx')
  return byType || byExt
}

export function ResumeLibraryPage() {
  const user = getUser()
  const [resumes, setResumes] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [dragOver, setDragOver] = useState(false)

  if (!user) return <Navigate to="/login" replace state={{ from: '/profile/resumes' }} />
  if (user.role !== 'CANDIDATE') return <Navigate to="/profile" replace />

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/api/users/me/resumes')
      setResumes(Array.isArray(res?.data) ? res.data : [])
    } catch (e) {
      setError(e?.message || 'Không tải được danh sách CV.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function uploadFile(file) {
    if (!isValidCvFile(file)) {
      alert('Chỉ được tải lên PDF hoặc DOCX.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      alert('Dung lượng file vượt quá 5MB.')
      return
    }
    setUploading(true)
    setError('')
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('title', file.name.replace(/\.[^.]+$/, '') || 'CV')
      form.append('makeDefault', resumes.length === 0 ? 'true' : 'false')
      await api.post('/api/users/me/resumes', form)
      await load()
    } catch (e) {
      setError(e?.message || 'Upload thất bại.')
    } finally {
      setUploading(false)
    }
  }

  async function setDefault(id) {
    try {
      await api.post(`/api/users/me/resumes/${id}/default`, {})
      await load()
    } catch (e) {
      alert(e?.message || 'Không đặt mặc định được.')
    }
  }

  async function remove(id) {
    if (!confirm('Xóa CV này khỏi kho?')) return
    try {
      await api.delete(`/api/users/me/resumes/${id}`)
      await load()
    } catch (e) {
      alert(e?.message || 'Không xóa được.')
    }
  }

  return (
    <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Kho CV cá nhân</h1>
          <p className="text-slate-600 dark:text-slate-400 mt-2">
            Tải CV lên để dùng lại khi ứng tuyển (chọn &quot;Chọn từ kho CV&quot; trên trang nộp hồ sơ).
          </p>
        </div>
        <Link
          to="/profile"
          className="text-sm font-medium text-[#2563eb] hover:underline"
        >
          ← Về hồ sơ
        </Link>
      </div>

      <div
        className={[
          'mb-8 rounded-xl border-2 border-dashed p-8 text-center transition',
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
          const f = e.dataTransfer?.files?.[0]
          if (f) uploadFile(f)
        }}
      >
        <input
          type="file"
          accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          className="hidden"
          id="resume-upload"
          disabled={uploading}
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) uploadFile(f)
            e.target.value = ''
          }}
        />
        <label htmlFor="resume-upload" className="cursor-pointer inline-flex flex-col items-center gap-2">
          <UploadCloud className="h-10 w-10 text-[#2563eb]" />
          <span className="text-sm font-medium text-slate-800 dark:text-slate-100">
            Kéo thả file PDF/DOCX vào đây hoặc click để chọn
          </span>
          <span className="text-xs text-slate-500">Tối đa 5MB</span>
        </label>
        {uploading ? (
          <div className="mt-4 flex items-center justify-center gap-2 text-sm text-slate-600">
            <Loader2 className="h-4 w-4 animate-spin" />
            Đang tải lên…
          </div>
        ) : null}
      </div>

      {error ? (
        <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:border-rose-900/40 dark:bg-rose-950/20 dark:text-rose-300">
          {error}
        </div>
      ) : null}

      <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800">
          <h2 className="text-lg font-medium text-slate-900 dark:text-slate-100">Danh sách CV</h2>
        </div>
        <div className="p-6">
          {loading ? (
            <div className="flex items-center gap-2 text-sm text-slate-500">
              <Loader2 className="h-4 w-4 animate-spin" />
              Đang tải…
            </div>
          ) : resumes.length === 0 ? (
            <p className="text-sm text-slate-500">Chưa có CV nào trong kho.</p>
          ) : (
            <ul className="space-y-3">
              {resumes.map((r) => (
                <li
                  key={r.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3"
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <FileText className="h-8 w-8 shrink-0 text-slate-400" />
                    <div className="min-w-0">
                      <div className="font-medium text-slate-900 dark:text-slate-100 truncate">
                        {r.title || 'CV'}
                        {r.isDefault ? (
                          <span className="ml-2 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
                            Mặc định
                          </span>
                        ) : null}
                      </div>
                      <div className="text-xs text-slate-500 truncate">{r.originalFilename || ''}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {r.downloadUrl ? (
                      <a
                        href={r.downloadUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm font-medium text-[#2563eb] hover:underline"
                      >
                        Xem
                      </a>
                    ) : null}
                    {!r.isDefault ? (
                      <button
                        type="button"
                        onClick={() => setDefault(r.id)}
                        className="inline-flex items-center gap-1 rounded-lg border border-slate-200 dark:border-slate-700 px-2 py-1 text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800"
                        title="Đặt làm CV mặc định"
                      >
                        <Star className="h-3.5 w-3.5" />
                        Mặc định
                      </button>
                    ) : null}
                    <button
                      type="button"
                      onClick={() => remove(r.id)}
                      className="inline-flex items-center gap-1 rounded-lg border border-rose-200 dark:border-rose-900/40 px-2 py-1 text-xs font-medium text-rose-700 dark:text-rose-300 hover:bg-rose-50 dark:hover:bg-rose-950/30"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      Xóa
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
