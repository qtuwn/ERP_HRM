import { Bell } from 'lucide-react'

export function NotificationsPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
        <Bell className="h-8 w-8 text-amber-600 dark:text-amber-400" />
      </div>
      <h1 className="mt-6 text-2xl font-bold text-slate-900 dark:text-white">Trung tâm thông báo</h1>
      <p className="mt-3 text-sm leading-relaxed text-slate-600 dark:text-slate-400">
        Tính năng đang được phát triển: gom thông báo ứng tuyển, phỏng vấn và tin nhắn tại một nơi (tương tự ITviec).
      </p>
      <p className="mt-2 text-xs text-slate-500">Hiện tại vui lòng xem chi tiết trong mục Ứng tuyển và Tin nhắn.</p>
    </div>
  )
}
