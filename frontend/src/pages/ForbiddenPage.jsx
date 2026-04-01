import { Link } from 'react-router-dom'

export function ForbiddenPage() {
  return (
    <section className="mx-auto max-w-lg px-4 py-20 text-center">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Không có quyền truy cập</h1>
      <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">
        Tài khoản của bạn không đủ quyền xem trang này. Liên hệ quản trị viên nếu cần hỗ trợ.
      </p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link
          to="/jobs"
          className="inline-flex rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-semibold text-white hover:bg-[#1d4ed8]"
        >
          Về trang việc làm
        </Link>
        <Link
          to="/login"
          className="inline-flex rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
        >
          Đăng nhập tài khoản khác
        </Link>
      </div>
    </section>
  )
}
