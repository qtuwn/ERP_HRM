import { memo } from 'react'
import { Link } from 'react-router-dom'
import {
  ClipboardList,
  Eye,
  Info,
  Loader2,
  Mail,
  MessageSquare,
  StickyNote,
} from 'lucide-react'

function getScoreColor(score) {
  if (score === null || score === undefined) return 'bg-gray-100 text-gray-800'
  if (score >= 80) return 'bg-green-100 text-green-800'
  if (score >= 50) return 'bg-yellow-100 text-yellow-800'
  return 'bg-red-100 text-red-800 border bg-opacity-70 border-red-200'
}

function badgeText(app) {
  const score = app?.aiScore
  if (score === null || score === undefined) return 'AI: N/A'
  if (score >= 80) return `TOP MATCH • ${score}`
  if (score >= 50) return `MATCH • ${score}`
  return `LOW • ${score}`
}

export const KanbanApplicationCard = memo(function KanbanApplicationCard({
  app,
  selected,
  canSelect,
  onToggleSelect,
  onDragStart,
  onDragEnd,
  onOpenAiInsights,
  onOpenReview,
  onOpenChat,
}) {
  return (
    <div
      className="bg-white p-4 rounded-md shadow border border-gray-200 cursor-move hover:border-indigo-400 hover:shadow-md transition duration-150 dark:bg-slate-950 dark:border-slate-800"
      draggable
      onDragStart={() => onDragStart(app)}
      onDragEnd={onDragEnd}
    >
      <div className="flex justify-between items-start mb-2">
        <div className="flex items-center gap-2 overflow-hidden">
          <input
            type="checkbox"
            title={canSelect ? 'Chọn để từ chối hàng loạt' : 'Hồ sơ đã kết thúc — không thể chọn'}
            disabled={!canSelect}
            checked={selected}
            onChange={() => onToggleSelect(app.id, app)}
            onClick={(e) => e.stopPropagation()}
            className="h-4 w-4 flex-shrink-0 cursor-pointer rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 disabled:cursor-not-allowed disabled:opacity-40"
          />
          <h3 className="font-medium text-gray-900 text-sm truncate pr-2 dark:text-white">{app.candidateName}</h3>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {app.aiStatus === 'AI_PROCESSING' || app.aiStatus === 'AI_QUEUED' ? (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold bg-indigo-50 text-indigo-700">
              <Loader2 className="h-3 w-3 animate-spin" />
              AI Screening...
            </span>
          ) : (
            <span
              className={['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold', getScoreColor(app.aiScore)].join(
                ' '
              )}
            >
              {badgeText(app)}
            </span>
          )}
        </div>
      </div>

      <a
        href={`mailto:${app.candidateEmail}`}
        className="text-xs text-gray-500 hover:text-indigo-600 hover:underline truncate block mb-1"
      >
        <span className="inline-flex items-center gap-1">
          <Mail className="h-3 w-3" />
          {app.candidateEmail}
        </span>
      </a>

      <div className="mt-3 flex flex-wrap gap-2 items-center">
        {app.cvUrl ? (
          <a
            href={app.cvUrl}
            target="_blank"
            rel="noreferrer"
            className="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center"
          >
            <Eye className="h-3 w-3 mr-1" />
            CV
          </a>
        ) : null}

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            onOpenAiInsights(app)
          }}
          className="text-xs text-slate-700 hover:text-indigo-700 font-medium flex items-center bg-slate-50 hover:bg-indigo-50 px-2 py-1 rounded border border-slate-200 dark:bg-slate-900 dark:border-slate-700"
        >
          <Info className="h-3 w-3 mr-1" />
          AI Insights
        </button>

        <Link
          to={`/dashboard/applications/${app.id}/tasks`}
          onClick={(e) => e.stopPropagation()}
          className="text-xs text-slate-700 hover:text-indigo-700 font-medium flex items-center bg-slate-50 hover:bg-indigo-50 px-2 py-1 rounded border border-slate-200 dark:bg-slate-900 dark:border-slate-700 dark:text-slate-200"
          title="Nhiệm vụ & tài liệu ứng viên"
        >
          <ClipboardList className="h-3 w-3 mr-1" />
          Nhiệm vụ
        </Link>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            onOpenReview(app)
          }}
          className="text-xs text-amber-800 hover:text-amber-950 font-medium flex items-center bg-amber-50 hover:bg-amber-100 px-2 py-1 rounded border border-amber-200 dark:bg-amber-950/40 dark:border-amber-800 dark:text-amber-200"
          title="Xem hồ sơ, CV, lịch PV và ghi chú nội bộ"
        >
          <StickyNote className="h-3 w-3 mr-1" />
          Đánh giá
        </button>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            onOpenChat(app.id)
          }}
          className="text-xs text-blue-600 hover:text-blue-800 font-medium flex items-center ml-auto bg-blue-50 px-2 py-1 rounded dark:bg-blue-950/40 dark:text-blue-300"
          title="Mở chat trong hub Tin nhắn (HR)"
        >
          <MessageSquare className="h-3 w-3 mr-1" />
          Chat
        </button>
      </div>
    </div>
  )
})
