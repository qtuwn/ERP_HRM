import { useEffect, useMemo, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Briefcase, ClipboardList, LayoutDashboard, Menu, Moon, Sun, Users, X, Building2, Tags } from 'lucide-react'
import { clearSession, getUser, normalizeUserRole } from '../lib/storage.js'
import { applyTheme, getStoredTheme } from '../lib/theme.js'
import LogoImage from '../assets/LOGO.png'

function isKanbanPath(path) {
  return /\/jobs\/[^/]+\/kanban$/.test(path)
}

function navItemClass(active) {
  return [
    'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
    active
      ? 'bg-blue-50 font-semibold text-blue-600 dark:bg-blue-950/50 dark:text-blue-300'
      : 'text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900',
  ].join(' ')
}

function iconClass(active) {
  return [
    'h-5 w-5 shrink-0',
    active
      ? '!text-[#2563eb] dark:!text-blue-300'
      : 'text-slate-500 group-hover:text-blue-600 dark:text-slate-400 dark:group-hover:text-blue-300',
  ].join(' ')
}

export function AdminShell() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useMemo(() => getUser(), [])
  const role = useMemo(() => normalizeUserRole(user?.role), [user])
  const [isDark, setIsDark] = useState(() => getStoredTheme() === 'dark')
  const [sidebarOpen, setSidebarOpen] = useState(false)

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

  const path = location.pathname
  const navDashboard = path === '/dashboard'
  const navJobsManagement = path.startsWith('/jobs/management')
  const navApplicationTracking = isKanbanPath(path)
  const navAdminUsers = path === '/admin/users'
  const navAdminCompanies = path === '/admin/companies'
  const navAdminSkills = path === '/admin/master-data/skills'
  const navCompanyStaff = path === '/company/staff'

  return (
    <div className="relative min-h-screen bg-slate-50 font-sans antialiased text-slate-800 dark:bg-slate-950 dark:text-slate-100">
      <a
        href="#admin-main"
        className="absolute left-[-9999px] top-0 z-[100] rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-semibold text-white shadow-lg focus:fixed focus:left-4 focus:top-4 focus:outline-none focus:ring-2 focus:ring-white/50"
      >
        Bỏ qua đến nội dung chính
      </a>
      {sidebarOpen ? (
        <div className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden" onClick={() => setSidebarOpen(false)} />
      ) : null}

      <div className="flex min-h-screen">
        <aside
          className={[
            'fixed inset-y-0 left-0 z-40 w-64 flex flex-col border-r border-slate-200 bg-white shadow-sm transition-transform duration-200 ease-out dark:border-slate-800 dark:bg-slate-950',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
          ].join(' ')}
          aria-label="Điều hướng quản trị"
        >
          <div className="flex h-14 items-center gap-2 border-b border-slate-200 px-4 dark:border-slate-800">
            <Link to="/dashboard" className="flex min-w-0 items-center gap-2">
              <img src={LogoImage} alt="VTHR Logo" className="h-8 w-auto shrink-0 object-contain" />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-slate-900 dark:text-white">VTHR Careers Hub</p>
                <p className="truncate text-xs text-slate-500 dark:text-slate-400">Quản trị tuyển dụng</p>
              </div>
            </Link>

            <button
              type="button"
              className="ml-auto inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-900 lg:hidden"
              onClick={() => setSidebarOpen(false)}
              aria-label="Close"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
            <NavLink to="/dashboard" className={navItemClass(navDashboard)}>
              <LayoutDashboard className={iconClass(navDashboard)} />
              <span>Tổng quan</span>
            </NavLink>

            <NavLink to="/jobs/management" className={navItemClass(navJobsManagement)}>
              <Briefcase className={iconClass(navJobsManagement)} />
              <span>Quản lý tin tuyển</span>
            </NavLink>

            <NavLink to="/jobs/management" className={navItemClass(navApplicationTracking)}>
              <ClipboardList className={iconClass(navApplicationTracking)} />
              <span>Theo dõi hồ sơ</span>
            </NavLink>
            <p className="px-3 pb-1 pt-0 text-[11px] leading-snug text-slate-500 dark:text-slate-500">
              Mở từng tin → <span className="text-slate-600 dark:text-slate-400">Ứng viên (Kanban)</span>
            </p>

            {user && role === 'ADMIN' ? (
              <>
                <NavLink to="/admin/users" className={navItemClass(navAdminUsers)}>
                  <Users className={iconClass(navAdminUsers)} />
                  <span>Quản lý tài khoản</span>
                </NavLink>
                <NavLink to="/admin/companies" className={navItemClass(navAdminCompanies)}>
                  <Building2 className={iconClass(navAdminCompanies)} />
                  <span>Duyệt công ty</span>
                </NavLink>
                <NavLink to="/admin/master-data/skills" className={navItemClass(navAdminSkills)}>
                  <Tags className={iconClass(navAdminSkills)} />
                  <span>Master data (Skills)</span>
                </NavLink>
              </>
            ) : null}

            {user && role === 'COMPANY' ? (
              <NavLink to="/company/staff" className={navItemClass(navCompanyStaff)}>
                <Users className={iconClass(navCompanyStaff)} />
                <span>Nhân sự công ty</span>
              </NavLink>
            ) : null}
          </nav>

          <div className="border-t border-slate-200 p-3 dark:border-slate-800">
            <button
              type="button"
              onClick={toggleTheme}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
            >
              {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
              <span>{isDark ? 'Chế độ sáng' : 'Chế độ tối'}</span>
            </button>
          </div>
        </aside>

        <div className="flex min-h-screen min-w-0 flex-1 flex-col lg:pl-64">
          <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur dark:border-slate-800 dark:bg-slate-950/80">
            <div className="flex h-14 items-center justify-between px-4 sm:px-6 lg:px-8">
              <button
                type="button"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-900 lg:hidden"
                onClick={() => setSidebarOpen(true)}
                aria-label="Menu"
              >
                <Menu className="h-4 w-4" />
              </button>

              <div className="ml-auto flex items-center gap-2">
                {user ? (
                  <>
                    <div className="hidden text-sm text-slate-600 dark:text-slate-300 md:block">
                      {user.fullName || user.email} ({user.role})
                    </div>
                    <button
                      type="button"
                      onClick={logout}
                      className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                      Logout
                    </button>
                  </>
                ) : (
                  <Link
                    to="/login"
                    className="rounded-lg bg-[#2563eb] px-3 py-2 text-sm font-semibold text-white hover:bg-[#1d4ed8]"
                  >
                    Login
                  </Link>
                )}
              </div>
            </div>
          </header>

          <main id="admin-main" className="flex-1 scroll-mt-14" tabIndex={-1}>
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  )
}
