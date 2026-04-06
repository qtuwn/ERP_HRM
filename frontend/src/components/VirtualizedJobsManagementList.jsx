import { memo, useRef } from 'react'
import { useVirtualizer } from '@tanstack/react-virtual'
import { Ban, CircleCheck, Eye, Pencil, Trash2 } from 'lucide-react'

const ROW_HEIGHT = 76
const GRID =
  'grid grid-cols-[minmax(0,2.2fr)_minmax(0,0.85fr)_minmax(0,0.85fr)_minmax(0,1fr)_minmax(0,1.35fr)_minmax(200px,200px)] gap-x-2 items-center'

/**
 * Danh sách tin tuyển dụng dạng lưới + cuộn ảo (giảm DOM khi một trang có nhiều dòng).
 */
export const VirtualizedJobsManagementList = memo(function VirtualizedJobsManagementList({
  jobs,
  statusLabel,
  statusClass,
  showKanbanButton = true,
  showEditButton = true,
  goKanban,
  openEdit,
  publishJob,
  closeJob,
  deleteJob,
}) {
  const parentRef = useRef(null)
  const virtualizer = useVirtualizer({
    count: jobs.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => ROW_HEIGHT,
    overscan: 8,
  })

  if (!jobs.length) return null

  return (
    <div className="w-full min-w-0 overflow-x-auto overscroll-x-contain">
      <div className="min-w-[720px]">
        <div
          className={`${GRID} border-b border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-700 dark:bg-slate-800/80`}
        >
          <div className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Tiêu đề
          </div>
          <div className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Cấp bậc
          </div>
          <div className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Loại hình
          </div>
          <div className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Trạng thái
          </div>
          <div className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Hạn nộp
          </div>
          <div className="text-right text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Hành động
          </div>
        </div>

        <div
          ref={parentRef}
          className="max-h-[min(70vh,640px)] overflow-auto border-b border-slate-200 dark:border-slate-700"
        >
          <div
            style={{
              height: `${virtualizer.getTotalSize()}px`,
              width: '100%',
              position: 'relative',
            }}
          >
            {virtualizer.getVirtualItems().map((vi) => {
              const job = jobs[vi.index]
              return (
                <div
                  key={job.id}
                  className={`${GRID} absolute left-0 top-0 w-full border-b border-slate-100 bg-white py-3 hover:bg-slate-50/80 dark:border-slate-800 dark:bg-slate-900 dark:hover:bg-slate-800/50`}
                  style={{
                    transform: `translateY(${vi.start}px)`,
                    height: `${vi.size}px`,
                  }}
                >
                  <div className="min-w-0 px-4">
                    <p className="truncate font-medium text-slate-900 dark:text-white">{job.title || 'Không có tiêu đề'}</p>
                  </div>
                  <div className="px-1 text-sm text-slate-700 dark:text-slate-300">{job.level || '-'}</div>
                  <div className="px-1 text-sm text-slate-700 dark:text-slate-300">{job.jobType || '-'}</div>
                  <div className="px-1">
                    <span
                      className={[
                        'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                        statusClass(job.status),
                      ].join(' ')}
                    >
                      {statusLabel(job.status)}
                    </span>
                  </div>
                  <div className="px-1 text-sm text-slate-600 dark:text-slate-400">
                    {job.expiresAt ? new Date(job.expiresAt).toLocaleString('vi-VN') : '-'}
                  </div>
                  <div className="flex flex-wrap items-center justify-end gap-1.5 px-2">
                    {showKanbanButton ? (
                      <button
                        type="button"
                        onClick={() => goKanban(job.id)}
                        title="Xem ứng viên (Kanban)"
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-[#2563eb] transition hover:bg-[#2563eb]/10 dark:border-slate-600 dark:hover:bg-[#2563eb]/20"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                    ) : null}
                    {showEditButton ? (
                      <button
                        type="button"
                        onClick={() => openEdit(job)}
                        title="Sửa tin"
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                    ) : null}
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
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
})
