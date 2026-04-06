import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Building2, ChevronDown, ChevronRight } from 'lucide-react'
import { api } from '../lib/api.js'

const MAX_LIST = 60

async function fetchCareerCompanies() {
  const res = await api.get('/api/jobs/meta/career-companies')
  const raw = res?.data
  return Array.isArray(raw) ? raw.filter(Boolean) : []
}

/**
 * Menu "Công ty" kiểu 2 cột (tham khảo ITviec): mục trái + danh sách công ty đang tuyển bên phải.
 */
export function CompanyCareerMegaMenu({ brandNav = false, variant = 'desktop', onNavigate }) {
  const afterNav = onNavigate || (() => {})
  const [open, setOpen] = useState(false)
  const [panel, setPanel] = useState('hiring')

  const { data: names = [], isLoading } = useQuery({
    queryKey: ['career-companies'],
    queryFn: fetchCareerCompanies,
    staleTime: 5 * 60_000,
  })

  const shown = useMemo(() => names.slice(0, MAX_LIST), [names])

  const triggerBtn = [
    'inline-flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium transition',
    brandNav
      ? open
        ? 'bg-white/20 text-white'
        : 'text-white/90 hover:bg-white/10 hover:text-white'
      : open
        ? 'bg-slate-100 text-slate-900 dark:bg-slate-800 dark:text-white'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white',
  ].join(' ')

  if (variant === 'mobile') {
    const m = brandNav
    return (
      <div className="space-y-1 px-1">
        <p className={`px-3 pt-2 text-xs font-semibold uppercase tracking-wide ${m ? 'text-white/70' : 'text-slate-500'}`}>
          Công ty
        </p>
        <details
          className={
            m ? 'group rounded-lg border border-white/20' : 'group rounded-lg border border-slate-100 dark:border-slate-800'
          }
        >
          <summary
            className={
              m
                ? 'flex cursor-pointer list-none items-center justify-between px-3 py-2 text-sm font-medium text-white marker:content-none [&::-webkit-details-marker]:hidden'
                : 'flex cursor-pointer list-none items-center justify-between px-3 py-2 text-sm font-medium text-slate-800 marker:content-none dark:text-slate-100 [&::-webkit-details-marker]:hidden'
            }
          >
            <span className="inline-flex items-center gap-2">
              <Building2 className="h-4 w-4 shrink-0 opacity-80" />
              Đang tuyển dụng
            </span>
            <ChevronDown className={`h-4 w-4 shrink-0 transition group-open:rotate-180 ${m ? 'text-white/70' : ''}`} />
          </summary>
          <div
            className={
              m
                ? 'max-h-56 overflow-y-auto border-t border-white/20 px-2 py-2'
                : 'max-h-56 overflow-y-auto border-t border-slate-100 px-2 py-2 dark:border-slate-800'
            }
          >
            {isLoading ? (
              <p className={`px-2 py-1 text-xs ${m ? 'text-white/70' : 'text-slate-500'}`}>Đang tải…</p>
            ) : shown.length === 0 ? (
              <p className={`px-2 py-1 text-xs ${m ? 'text-white/70' : 'text-slate-500'}`}>Chưa có công ty nào đang đăng tin.</p>
            ) : (
              shown.map((name) => (
                <Link
                  key={name}
                  to={`/jobs?q=${encodeURIComponent(name)}`}
                  className={
                    m
                      ? 'block truncate rounded-md px-2 py-1.5 text-sm text-white/90 hover:bg-white/10'
                      : 'block truncate rounded-md px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-800'
                  }
                  onClick={afterNav}
                  title={name}
                >
                  {name}
                </Link>
              ))
            )}
          </div>
        </details>
        <Link
          to="/jobs#job-browse-filters"
          className={
            m
              ? 'block rounded-lg px-3 py-2 text-sm text-white hover:bg-white/10'
              : 'block rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800'
          }
          onClick={afterNav}
        >
          Mẹo tìm việc & bộ lọc
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
      <button type="button" className={triggerBtn} aria-expanded={open} aria-haspopup="true">
        <Building2 className="h-4 w-4 shrink-0 opacity-90" />
        Công ty
        <ChevronDown className={`h-4 w-4 opacity-70 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open ? (
        <div className="absolute left-0 top-full z-50 pt-1" role="menu" aria-label="Công ty">
          <div className="flex max-h-[min(72vh,440px)] w-[min(92vw,560px)] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-slate-900">
            <div className="flex w-52 shrink-0 flex-col border-r border-slate-200 bg-slate-50 py-2 dark:border-slate-700 dark:bg-slate-950/80">
              <button
                type="button"
                role="menuitem"
                onMouseEnter={() => setPanel('hiring')}
                className={[
                  'flex w-full items-center justify-between px-3 py-2.5 text-left text-sm transition',
                  panel === 'hiring'
                    ? 'bg-white font-semibold text-slate-900 dark:bg-slate-900 dark:text-white'
                    : 'text-slate-600 hover:bg-white/80 dark:text-slate-300 dark:hover:bg-slate-900/80',
                ].join(' ')}
              >
                Công ty đang tuyển
                <ChevronRight className="h-4 w-4 shrink-0 text-slate-400" />
              </button>
              <Link
                to="/jobs#job-browse-filters"
                role="menuitem"
                className="px-3 py-2.5 text-sm text-slate-600 transition hover:bg-white dark:text-slate-300 dark:hover:bg-slate-900"
                onClick={() => setOpen(false)}
              >
                Mẹo tìm việc
              </Link>
            </div>

            <div className="flex min-h-[200px] min-w-0 flex-1 flex-col bg-white dark:bg-slate-900">
              {panel === 'hiring' ? (
                <>
                  <div className="border-b border-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:text-slate-400">
                    Việc làm theo công ty
                  </div>
                  <div className="grid flex-1 grid-cols-1 gap-0 overflow-y-auto p-2 sm:grid-cols-2">
                    {isLoading ? (
                      <p className="col-span-full px-2 py-3 text-sm text-slate-500">Đang tải…</p>
                    ) : shown.length === 0 ? (
                      <p className="col-span-full px-2 py-3 text-sm text-slate-500 dark:text-slate-400">
                        Chưa có công ty nào đang đăng tin mở.
                      </p>
                    ) : (
                      shown.map((name) => (
                        <Link
                          key={name}
                          to={`/jobs?q=${encodeURIComponent(name)}`}
                          role="menuitem"
                          className="block truncate rounded-lg px-2 py-2 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                          title={name}
                          onClick={() => setOpen(false)}
                        >
                          {name}
                        </Link>
                      ))
                    )}
                  </div>
                </>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
