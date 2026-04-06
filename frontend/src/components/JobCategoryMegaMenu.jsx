import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronDown, ChevronRight } from 'lucide-react'

const MAX_ITEMS_PANEL = 40

const CATEGORIES = [
  { id: 'industry', label: 'Theo ngành', param: 'industry', bucket: 'industries' },
  { id: 'city', label: 'Theo địa điểm', param: 'city', bucket: 'cities' },
  { id: 'jobType', label: 'Hình thức', param: 'jobType', bucket: 'jobTypes' },
  { id: 'level', label: 'Cấp bậc', param: 'level', bucket: 'levels' },
]

function normalizeBuckets(filterOpts) {
  const o = filterOpts || {}
  return {
    industries: Array.isArray(o.industries) ? o.industries.filter(Boolean) : [],
    cities: Array.isArray(o.cities) ? o.cities.filter(Boolean) : [],
    jobTypes: Array.isArray(o.jobTypes) ? o.jobTypes.filter(Boolean) : [],
    levels: Array.isArray(o.levels) ? o.levels.filter(Boolean) : [],
  }
}

/**
 * Menustrip kiểu mega menu: 4 danh mục cơ bản (ngành, địa điểm, hình thức, cấp bậc) — dữ liệu từ GET /api/jobs/filter-options.
 */
export function JobCategoryMegaMenu({ filterOpts, variant = 'desktop', brandNav = false, onNavigate }) {
  const buckets = useMemo(() => normalizeBuckets(filterOpts), [filterOpts])
  const [activeId, setActiveId] = useState('industry')
  const [open, setOpen] = useState(false)

  const active = CATEGORIES.find((c) => c.id === activeId) || CATEGORIES[0]
  const items = buckets[active.bucket] || []
  const shown = items.slice(0, MAX_ITEMS_PANEL)

  const afterNav = onNavigate || (() => {})

  if (variant === 'mobile') {
    const m = brandNav
    return (
      <div className="space-y-1 px-1">
        <Link
          to="/jobs"
          className={
            m
              ? 'block rounded-lg px-3 py-2 text-sm font-medium text-white hover:bg-white/10'
              : 'block rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800'
          }
          onClick={afterNav}
        >
          Tất cả việc làm
        </Link>
        {CATEGORIES.map((cat) => {
          const list = buckets[cat.bucket] || []
          return (
            <details
              key={cat.id}
              className={
                m
                  ? 'group rounded-lg border border-white/20'
                  : 'group rounded-lg border border-slate-100 dark:border-slate-800'
              }
            >
              <summary
                className={
                  m
                    ? 'flex cursor-pointer list-none items-center justify-between px-3 py-2 text-sm font-medium text-white marker:content-none [&::-webkit-details-marker]:hidden'
                    : 'flex cursor-pointer list-none items-center justify-between px-3 py-2 text-sm font-medium text-slate-800 marker:content-none dark:text-slate-100 [&::-webkit-details-marker]:hidden'
                }
              >
                {cat.label}
                <ChevronDown
                  className={`h-4 w-4 shrink-0 transition group-open:rotate-180 ${m ? 'text-white/70' : 'text-slate-400'}`}
                />
              </summary>
              <div
                className={
                  m
                    ? 'max-h-48 overflow-y-auto border-t border-white/20 px-2 py-2'
                    : 'max-h-48 overflow-y-auto border-t border-slate-100 px-2 py-2 dark:border-slate-800'
                }
              >
                {list.length === 0 ? (
                  <p className={`px-2 py-1 text-xs ${m ? 'text-white/70' : 'text-slate-500'}`}>Chưa có dữ liệu</p>
                ) : (
                  list.slice(0, 24).map((val) => (
                    <Link
                      key={`${cat.id}-${val}`}
                      to={`/jobs?${cat.param}=${encodeURIComponent(val)}`}
                      className={
                        m
                          ? 'block rounded-md px-2 py-1.5 text-sm text-white/90 hover:bg-white/10'
                          : 'block rounded-md px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-800'
                      }
                      onClick={afterNav}
                    >
                      {val}
                    </Link>
                  ))
                )}
              </div>
            </details>
          )
        })}
        <Link
          to="/jobs#job-browse-filters"
          className={
            m
              ? 'mt-2 flex items-center gap-1 px-3 py-2 text-xs font-semibold text-white underline-offset-2 hover:underline'
              : 'mt-2 flex items-center gap-1 px-3 py-2 text-xs font-semibold text-[#2563eb] hover:underline'
          }
          onClick={afterNav}
        >
          Bộ lọc đầy đủ
          <ChevronRight className="h-3 w-3" />
        </Link>
      </div>
    )
  }

  return (
    <div
      className="relative"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        type="button"
        className={[
          'inline-flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium transition',
          brandNav
            ? open
              ? 'bg-white/20 text-white'
              : 'text-white/90 hover:bg-white/10 hover:text-white'
            : open
              ? 'bg-slate-100 text-slate-900 dark:bg-slate-800 dark:text-white'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white',
        ].join(' ')}
        aria-expanded={open}
        aria-haspopup="true"
      >
        Danh mục việc làm
        <ChevronDown className={`h-4 w-4 opacity-70 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open ? (
        <div
          className="absolute left-0 top-full z-50 pt-1"
          role="menu"
          aria-label="Danh mục việc làm"
        >
          <div className="flex max-h-[min(70vh,420px)] w-[min(92vw,640px)] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-slate-900">
            <div className="flex w-44 shrink-0 flex-col border-r border-slate-200 bg-slate-50 py-2 dark:border-slate-700 dark:bg-slate-950/80">
              <Link
                to="/jobs"
                className="px-3 py-2.5 text-sm font-semibold text-[#2563eb] hover:bg-white dark:hover:bg-slate-900"
                onClick={() => {
                  setOpen(false)
                  afterNav()
                }}
              >
                Tất cả việc làm
              </Link>
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  role="menuitem"
                  onMouseEnter={() => setActiveId(cat.id)}
                  className={[
                    'w-full px-3 py-2.5 text-left text-sm transition',
                    activeId === cat.id
                      ? 'bg-white font-semibold text-slate-900 dark:bg-slate-900 dark:text-white'
                      : 'text-slate-600 hover:bg-white/80 dark:text-slate-300 dark:hover:bg-slate-900/80',
                  ].join(' ')}
                >
                  {cat.label}
                </button>
              ))}
            </div>
            <div className="flex min-w-0 flex-1 flex-col">
              <div className="border-b border-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:text-slate-400">
                {active.label}
              </div>
              <div className="grid flex-1 grid-cols-2 gap-0 overflow-y-auto p-3 sm:grid-cols-2">
                {shown.length === 0 ? (
                  <p className="col-span-2 text-sm text-slate-500 dark:text-slate-400">Đang tải hoặc chưa có tin OPEN.</p>
                ) : (
                  shown.map((val) => (
                    <Link
                      key={`${active.param}-${val}`}
                      to={`/jobs?${active.param}=${encodeURIComponent(val)}`}
                      role="menuitem"
                      className="block truncate rounded-lg px-2 py-2 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                      onClick={() => {
                        setOpen(false)
                        afterNav()
                      }}
                      title={val}
                    >
                      {val}
                    </Link>
                  ))
                )}
              </div>
              <div className="border-t border-slate-100 px-3 py-2 dark:border-slate-800">
                <Link
                  to="/jobs#job-browse-filters"
                  className="text-xs font-semibold text-[#2563eb] hover:underline"
                  onClick={() => {
                    setOpen(false)
                    afterNav()
                  }}
                >
                  Mở bộ lọc đầy đủ trên trang việc làm →
                </Link>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
