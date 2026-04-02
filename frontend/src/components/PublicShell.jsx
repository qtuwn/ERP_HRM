import { useEffect, useMemo, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Bell, ChevronDown, ExternalLink, FileText, LogOut, Menu, MessageCircle, Moon, Settings, Sun } from 'lucide-react'
import { api } from '../lib/api.js'
import { clearSession, getUser, normalizeUserRole } from '../lib/storage.js'
import { applyTheme, getStoredTheme } from '../lib/theme.js'
import { ChatWidget } from './ChatWidget.jsx'
import LogoImage from '../assets/LOGO.png'

function navBase(isActive) {
  return [
    'rounded-lg px-3 py-2 text-sm font-medium transition',
    isActive
      ? 'bg-slate-100 text-slate-900 dark:bg-slate-800 dark:text-white'
      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white',
  ].join(' ')
}

export function PublicShell() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useMemo(() => getUser(), [])
  const candidate = Boolean(user && normalizeUserRole(user.role) === 'CANDIDATE')
  const [isDark, setIsDark] = useState(() => getStoredTheme() === 'dark')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)

  const { data: filterOpts } = useQuery({
    queryKey: ['job-filter-options'],
    queryFn: async () => {
      const j = await api.get('/api/jobs/filter-options')
      return j?.data ?? {}
    },
    staleTime: 5 * 60_000,
  })
  const industries = Array.isArray(filterOpts?.industries) ? filterOpts.industries.filter(Boolean) : []

  useEffect(() => {
    applyTheme(getStoredTheme())
  }, [])

  useEffect(() => {
    function onThemeChanged(e) {
      const next = e?.detail?.theme
      if (next === 'dark' || next === 'light') setIsDark(next === 'dark')
    }
    function onStorage(ev) {
      if (ev.key === 'theme' && (ev.newValue === 'dark' || ev.newValue === 'light')) {
        setIsDark(ev.newValue === 'dark')
      }
    }
    window.addEventListener('theme-changed', onThemeChanged)
    window.addEventListener('storage', onStorage)
    return () => {
      window.removeEventListener('theme-changed', onThemeChanged)
      window.removeEventListener('storage', onStorage)
    }
  }, [])

  function toggleTheme() {
    setIsDark((prev) => {
      const next = !prev
      applyTheme(next ? 'dark' : 'light')
      return next
    })
  }

  function logout() {
    clearSession()
    navigate('/login')
  }

  return (
    <div className="relative min-h-screen bg-slate-50 font-sans antialiased text-slate-800 dark:bg-slate-950 dark:text-slate-100">
      <a
        href="#main-content"
        className="absolute left-[-9999px] top-0 z-[100] rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-semibold text-white shadow-lg focus:fixed focus:left-4 focus:top-4 focus:outline-none focus:ring-2 focus:ring-white/50"
      >
        Bỏ qua đến nội dung chính
      </a>
      <nav className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/90 backdrop-blur-md dark:border-slate-800 dark:bg-slate-900/90">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between gap-4">
            <Link to="/" className="flex shrink-0 items-center gap-2">
              <img src={LogoImage} alt="VTHR Logo" className="hidden h-9 w-auto object-contain md:block" />
              <span className="text-lg font-bold tracking-tight text-slate-900 dark:text-white">VTHR</span>
            </Link>

            <div className="hidden items-center gap-2 md:flex">
              <NavLink to="/jobs" className={({ isActive }) => navBase(isActive)}>
                Việc làm
              </NavLink>

              <details className="group relative">
                <summary
                  className={[
                    'cursor-pointer list-none rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition marker:content-none hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white',
                    '[&::-webkit-details-marker]:hidden',
                  ].join(' ')}
                >
                  <span className="inline-flex items-center gap-1">
                    Danh mục
                    <ChevronDown className="h-4 w-4 opacity-70 group-open:rotate-180 transition-transform" />
                  </span>
                </summary>
                <div className="absolute left-0 z-50 mt-1 max-h-72 w-56 overflow-y-auto rounded-xl border border-slate-200 bg-white py-1 shadow-lg dark:border-slate-700 dark:bg-slate-900">
                  <Link
                    to="/jobs"
                    className="block px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                  >
                    Tất cả việc làm
                  </Link>
                  {industries.length === 0 ? (
                    <div className="px-4 py-2 text-xs text-slate-500">Đang tải danh mục…</div>
                  ) : (
                    industries.map((ind) => (
                      <Link
                        key={ind}
                        to={`/jobs?industry=${encodeURIComponent(ind)}`}
                        className="block px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                      >
                        {ind}
                      </Link>
                    ))
                  )}
                </div>
              </details>

              <button
                type="button"
                onClick={toggleTheme}
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-400 dark:hover:bg-slate-800"
                title="Chế độ sáng/tối"
              >
                {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
              </button>

              {!user ? (
                <div className="ml-2 flex items-center gap-2">
                  <Link
                    to="/login"
                    className="inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-slate-500"
                  >
                    Đăng nhập
                  </Link>
                  <Link
                    to="/register"
                    className="inline-flex items-center justify-center rounded-lg border-2 border-[#2563eb] bg-white px-4 py-2 text-sm font-semibold text-[#2563eb] transition hover:bg-[#2563eb] hover:text-white dark:bg-slate-900 dark:hover:bg-[#2563eb]"
                  >
                    Đăng ký
                  </Link>
                </div>
              ) : (
                <div className="ml-2 flex items-center gap-2">
                  {candidate ? (
                    <Link
                      to="/notifications"
                      className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      title="Thông báo"
                    >
                      <Bell className="h-4 w-4" />
                    </Link>
                  ) : null}
                  {user && ['HR', 'ADMIN', 'COMPANY'].includes(user.role) ? (
                    <Link
                      to="/dashboard"
                      className="inline-flex items-center gap-1.5 rounded-lg border border-[#2563eb]/40 bg-[#2563eb]/5 px-3 py-2 text-sm font-semibold text-[#2563eb] transition hover:bg-[#2563eb]/10 dark:border-[#2563eb]/50 dark:text-blue-300 dark:hover:bg-[#2563eb]/20"
                    >
                      <ExternalLink className="h-4 w-4" />
                      Vào bảng điều khiển
                    </Link>
                  ) : null}

                  {candidate ? (
                    <NavLink
                      to="/messages"
                      className={({ isActive }) =>
                        [
                          'inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition',
                          isActive || location.pathname.startsWith('/messages')
                            ? 'border-[#2563eb] bg-[#2563eb]/10 text-[#2563eb] dark:border-[#2563eb]/60 dark:bg-[#2563eb]/20 dark:text-blue-200'
                            : 'border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800',
                        ].join(' ')
                      }
                    >
                      <MessageCircle className="h-4 w-4 shrink-0" />
                      <span>Tin nhắn</span>
                    </NavLink>
                  ) : null}

                  <div className="relative">
                    <button
                      type="button"
                      onClick={() => setProfileOpen((v) => !v)}
                      onBlur={() => setTimeout(() => setProfileOpen(false), 120)}
                      className="flex max-w-[240px] items-center gap-2 rounded-lg border border-slate-200 py-1.5 pl-1.5 pr-2 transition hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800"
                    >
                      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#2563eb]/15 text-sm font-semibold text-[#2563eb] dark:bg-[#2563eb]/30 dark:text-white">
                        {(user.fullName || user.email || '?').charAt(0)}
                      </span>
                      <span className="min-w-0 flex-1 text-left">
                        <span className="block truncate text-sm font-medium text-slate-900 dark:text-white">
                          {user.fullName || user.email}
                        </span>
                      </span>
                      <ChevronDown className="h-4 w-4 shrink-0 text-slate-400" />
                    </button>

                    {profileOpen ? (
                      <div className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-xl border border-slate-200 bg-white py-1 shadow-lg dark:border-slate-700 dark:bg-slate-900">
                        {normalizeUserRole(user?.role) === 'CANDIDATE' ? (
                          <Link
                            to="/messages"
                            className="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                            onClick={() => setProfileOpen(false)}
                          >
                            <MessageCircle className="h-4 w-4 text-slate-400" />
                            Trung tâm tin nhắn
                          </Link>
                        ) : null}
                        <Link
                          to="/candidate/applications"
                          className="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                          onClick={() => setProfileOpen(false)}
                        >
                          <FileText className="h-4 w-4 text-slate-400" />
                          Ứng tuyển của tôi
                        </Link>
                        <Link
                          to="/profile"
                          className="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                        >
                          <Settings className="h-4 w-4 text-slate-400" />
                          Cài đặt tài khoản
                        </Link>
                        <div className="my-1 border-t border-slate-100 dark:border-slate-800" />
                        <button
                          type="button"
                          onClick={logout}
                          className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30"
                        >
                          <LogOut className="h-4 w-4" />
                          Đăng xuất
                        </button>
                      </div>
                    ) : null}
                  </div>
                </div>
              )}
            </div>

            <div className="flex items-center gap-2 md:hidden">
              <button
                type="button"
                onClick={toggleTheme}
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 dark:border-slate-700"
              >
                {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
              </button>
              {candidate && user ? (
                <Link
                  to="/messages"
                  className={[
                    'inline-flex h-9 w-9 items-center justify-center rounded-lg border text-slate-600 dark:text-slate-300',
                    location.pathname.startsWith('/messages')
                      ? 'border-[#2563eb] bg-[#2563eb]/10 text-[#2563eb] dark:text-blue-300'
                      : 'border-slate-200 dark:border-slate-700',
                  ].join(' ')}
                  title="Tin nhắn"
                >
                  <MessageCircle className="h-4 w-4" />
                </Link>
              ) : null}
              <button
                type="button"
                onClick={() => setMobileOpen((v) => !v)}
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 dark:border-slate-700"
                aria-label="Menu"
              >
                <Menu className="h-5 w-5" />
              </button>
            </div>
          </div>

          {mobileOpen ? (
            <div className="border-t border-slate-200 py-4 dark:border-slate-800 md:hidden">
              <Link
                to="/jobs"
                className="block rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                onClick={() => setMobileOpen(false)}
              >
                Việc làm
              </Link>
              <p className="px-3 pt-1 text-xs font-semibold uppercase tracking-wide text-slate-500">Danh mục</p>
              <Link
                to="/jobs"
                className="block rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                onClick={() => setMobileOpen(false)}
              >
                Tất cả
              </Link>
              {industries.map((ind) => (
                <Link
                  key={ind}
                  to={`/jobs?industry=${encodeURIComponent(ind)}`}
                  className="block rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                  onClick={() => setMobileOpen(false)}
                >
                  {ind}
                </Link>
              ))}

              {!user ? (
                <div className="mt-3 flex flex-col gap-2">
                  <Link
                    to="/login"
                    className="rounded-lg border border-slate-300 px-4 py-2.5 text-center text-sm font-semibold"
                    onClick={() => setMobileOpen(false)}
                  >
                    Đăng nhập
                  </Link>
                  <Link
                    to="/register"
                    className="rounded-lg border-2 border-[#2563eb] px-4 py-2.5 text-center text-sm font-semibold text-[#2563eb]"
                    onClick={() => setMobileOpen(false)}
                  >
                    Đăng ký
                  </Link>
                </div>
              ) : (
                <div className="mt-3 space-y-2">
                  {candidate ? (
                    <>
                      <Link
                        to="/messages"
                        className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                        onClick={() => setMobileOpen(false)}
                      >
                        <MessageCircle className="h-4 w-4" />
                        Tin nhắn
                      </Link>
                      <Link
                        to="/notifications"
                        className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                        onClick={() => setMobileOpen(false)}
                      >
                        <Bell className="h-4 w-4" />
                        Thông báo
                      </Link>
                    </>
                  ) : null}
                  {user && ['HR', 'ADMIN', 'COMPANY'].includes(user.role) ? (
                    <Link
                      to="/dashboard"
                      className="flex items-center gap-2 rounded-lg border border-[#2563eb]/40 bg-[#2563eb]/5 px-3 py-2 text-sm font-semibold text-[#2563eb]"
                      onClick={() => setMobileOpen(false)}
                    >
                      <ExternalLink className="h-4 w-4" />
                      Vào bảng điều khiển
                    </Link>
                  ) : null}
                  <Link
                    to="/candidate/applications"
                    className="block rounded-lg px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                    onClick={() => setMobileOpen(false)}
                  >
                    Ứng tuyển của tôi
                  </Link>
                  <Link
                    to="/profile"
                    className="block rounded-lg px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                    onClick={() => setMobileOpen(false)}
                  >
                    Cài đặt tài khoản
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      setMobileOpen(false)
                      logout()
                    }}
                    className="w-full rounded-lg px-3 py-2 text-left text-sm text-red-600"
                  >
                    Đăng xuất
                  </button>
                </div>
              )}
            </div>
          ) : null}
        </div>
      </nav>

      <main id="main-content" className="w-full scroll-mt-16" tabIndex={-1}>
        <Outlet />
      </main>

      <ChatWidget />

      <footer className="mt-auto border-t border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-4">
            <div className="lg:col-span-1">
              <Link to="/" className="mb-4 flex items-center gap-2 hover:opacity-90 transition-opacity">
                <img src={LogoImage} alt="VTHR Logo" className="hidden h-10 w-auto object-contain md:block" />
                <span className="text-lg font-bold text-slate-900 dark:text-white">VTHR Careers Hub</span>
              </Link>
              <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Về VTHR
              </h4>
              <p className="text-sm leading-relaxed text-slate-600 dark:text-slate-400">
                Nền tảng kết nối ứng viên và doanh nghiệp, tập trung trải nghiệm ứng tuyển minh bạch và công cụ tuyển
                dụng hiện đại.
              </p>
            </div>

            <div>
              <h4 className="mb-4 text-sm font-semibold text-slate-900 dark:text-white">Mạng xã hội</h4>
              <ul className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                <li>
                  <a
                    href="https://facebook.com"
                    target="_blank"
                    rel="noreferrer"
                    className="transition hover:text-[#2563eb]"
                  >
                    Facebook
                  </a>
                </li>
                <li>
                  <a
                    href="https://linkedin.com"
                    target="_blank"
                    rel="noreferrer"
                    className="transition hover:text-[#2563eb]"
                  >
                    LinkedIn
                  </a>
                </li>
                <li>
                  <a
                    href="https://github.com"
                    target="_blank"
                    rel="noreferrer"
                    className="transition hover:text-[#2563eb]"
                  >
                    GitHub
                  </a>
                </li>
              </ul>
            </div>

            <div>
              <h4 className="mb-4 text-sm font-semibold text-slate-900 dark:text-white">Hỗ trợ</h4>
              <ul className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                <li>
                  <a href="/jobs" className="transition hover:text-[#2563eb]">
                    Trung tâm trợ giúp
                  </a>
                </li>
                <li>
                  <a href="mailto:support@vthr.local" className="transition hover:text-[#2563eb]">
                    Liên hệ hỗ trợ
                  </a>
                </li>
                <li>
                  <a href="/jobs" className="transition hover:text-[#2563eb]">
                    Câu hỏi thường gặp
                  </a>
                </li>
              </ul>
            </div>

            <div>
              <h4 className="mb-4 text-sm font-semibold text-slate-900 dark:text-white">Liên hệ</h4>
              <p className="text-sm leading-relaxed text-slate-600 dark:text-slate-400">
                Tầng 12, Innovation Hub
                <br />
                Quận 1, TP. Hồ Chí Minh
                <br />
                <span className="text-[#2563eb]">Hotline: 1900 2026</span>
              </p>
            </div>
          </div>

          <div className="mt-10 border-t border-slate-200 pt-8 text-center text-sm text-slate-500 dark:border-slate-800 dark:text-slate-500">
            <p>© 2026 VTHR Solutions. Bản quyền thuộc VTHR Group.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
