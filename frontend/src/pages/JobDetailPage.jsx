import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../lib/api.js'
import { getUser, normalizeUserRole } from '../lib/storage.js'

function skillsFrom(value) {
  if (!value) return []
  return String(value)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

export function JobDetailPage() {
  const { id } = useParams()
  const user = useMemo(() => getUser(), [])

  const [loading, setLoading] = useState(true)
  const [job, setJob] = useState(null)

  useEffect(() => {
    let mounted = true
    ;(async () => {
      try {
        setLoading(true)
        const res = await api.get(`/api/jobs/${id}`)
        if (mounted) setJob(res?.data || null)
      } catch {
        if (mounted) setJob(null)
      } finally {
        if (mounted) setLoading(false)
      }
    })()
    return () => {
      mounted = false
    }
  }, [id])

  if (loading) {
    return (
      <section className="max-w-4xl mx-auto py-10 px-4 sm:px-6">
        <div className="animate-pulse flex flex-col gap-6">
          <div className="h-48 bg-slate-200 rounded-xl" />
          <div className="h-64 bg-slate-200 rounded-xl" />
        </div>
      </section>
    )
  }

  if (!job) {
    return (
      <section className="max-w-4xl mx-auto py-10 px-4 sm:px-6">
        <div className="text-center py-20 bg-white rounded-xl border mt-6">
          <h2 className="text-2xl font-bold text-slate-800 mb-2">Không tìm thấy!</h2>
          <p className="text-slate-500 mb-6">Công việc này có thể đã đóng hoặc không tồn tại.</p>
          <Link to="/jobs" className="text-[#2563eb] hover:underline font-medium">
            Trở về Về danh sách
          </Link>
        </div>
      </section>
    )
  }

  const role = normalizeUserRole(user?.role)
  const isHrAdmin = ['ADMIN', 'HR', 'COMPANY'].includes(role)
  const isCandidate = role === 'CANDIDATE'
  const applyNext = `/jobs/${id}/apply`
  const skills = skillsFrom(job.requiredSkills)

  return (
    <section className="max-w-4xl mx-auto py-10 px-4 sm:px-6">
      <div>
        <div className="bg-white rounded-xl shadow-sm border p-8 mb-6 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-2 h-full bg-[#2563eb]" />
          <div className="flex justify-between items-start gap-4 flex-col sm:flex-row">
            <div>
              <span className="inline-block px-3 py-1 rounded-full text-sm font-semibold bg-blue-50 text-blue-700 mb-3">
                {job.department || ''}
              </span>
              <h1 className="text-3xl font-bold text-slate-900 mb-4">{job.title}</h1>
              <div className="text-slate-500 flex items-center gap-4 text-sm font-medium">
                <span className="flex items-center gap-1">
                  Hạn nộp: <span>{job.expiresAt ? new Date(job.expiresAt).toLocaleDateString('vi-VN') : ''}</span>
                </span>
                <span className="flex items-center gap-1">
                  Trạng thái: <span className="text-green-600">{job.status}</span>
                </span>
              </div>
            </div>
            <div className="w-full sm:w-auto">
              {isHrAdmin ? (
                <a
                  href={`/jobs/${job.id}/kanban`}
                  className="inline-flex items-center justify-center bg-slate-800 text-white font-medium px-8 py-3 rounded-xl hover:bg-slate-900 transition-all w-full sm:w-auto"
                >
                  Xem hồ sơ (Kanban)
                </a>
              ) : isCandidate ? (
                <Link
                  to={applyNext}
                  className="inline-flex items-center justify-center bg-[#2563eb] text-white font-medium px-8 py-3 rounded-xl shadow-lg shadow-blue-500/30 hover:bg-blue-700 hover:-translate-y-0.5 transition-all w-full sm:w-auto"
                >
                  Ứng tuyển ngay
                </Link>
              ) : user ? (
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  Chỉ tài khoản <span className="font-semibold">ứng viên</span> mới nộp hồ sơ qua hệ thống.
                </p>
              ) : (
                <Link
                  to={`/login?next=${encodeURIComponent(applyNext)}`}
                  className="inline-flex items-center justify-center bg-[#2563eb] text-white font-medium px-8 py-3 rounded-xl shadow-lg shadow-blue-500/30 hover:bg-blue-700 hover:-translate-y-0.5 transition-all w-full sm:w-auto"
                >
                  Đăng nhập để ứng tuyển
                </Link>
              )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-6">
            <div className="bg-white rounded-xl shadow-sm border p-8">
              <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center gap-2">Mô tả công việc</h2>
              <div
                className="prose max-w-none text-slate-600 space-y-4 whitespace-pre-line"
                dangerouslySetInnerHTML={{ __html: job.description || '' }}
              />
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-white rounded-xl shadow-sm border p-8">
              <h2 className="text-lg font-bold text-slate-900 mb-4">Kỹ năng yêu cầu</h2>
              <div className="flex flex-wrap gap-2">
                {skills.map((s) => (
                  <span
                    key={s}
                    className="bg-slate-100 text-slate-700 px-3 py-1.5 rounded-lg text-sm font-medium border border-slate-200"
                  >
                    {s}
                  </span>
                ))}
              </div>
            </div>

            <div className="bg-slate-50 rounded-xl p-6 border border-slate-200">
              <h3 className="font-bold text-slate-900 mb-2">Bạn có thắc mắc?</h3>
              <p className="text-sm text-slate-600 mb-4">
                Vui lòng liên hệ bộ phận tuyển dụng nếu bạn có bất kỳ câu hỏi nào về vị trí này.
              </p>
              <a href="mailto:hr@vthr.com" className="text-[#2563eb] font-medium text-sm hover:underline">
                hr@vthr.com
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
