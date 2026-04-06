import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { FEATURE_PASSWORD_RESET_EMAIL } from './config/featureFlags.js'
import { PublicShell } from './components/PublicShell.jsx'
import { AdminShell } from './components/AdminShell.jsx'
import { RequireAuth } from './routes/RequireAuth.jsx'
import { RequireRole } from './routes/RequireRole.jsx'

const JobsPage = lazy(() => import('./pages/JobsPage.jsx').then((m) => ({ default: m.JobsPage })))
const JobDetailPage = lazy(() => import('./pages/JobDetailPage.jsx').then((m) => ({ default: m.JobDetailPage })))
const LoginPage = lazy(() => import('./pages/LoginPage.jsx').then((m) => ({ default: m.LoginPage })))
const RegisterPage = lazy(() => import('./pages/RegisterPage.jsx').then((m) => ({ default: m.RegisterPage })))
const VerifyEmailPage = lazy(() => import('./pages/VerifyEmailPage.jsx').then((m) => ({ default: m.VerifyEmailPage })))
const VerifyOtpPage = lazy(() => import('./pages/VerifyOtpPage.jsx').then((m) => ({ default: m.VerifyOtpPage })))
const ForgotPasswordPage = lazy(() =>
  import('./pages/ForgotPasswordPage.jsx').then((m) => ({ default: m.ForgotPasswordPage }))
)
const ForgotPasswordConfirmPage = lazy(() =>
  import('./pages/ForgotPasswordConfirmPage.jsx').then((m) => ({ default: m.ForgotPasswordConfirmPage }))
)

const ProfilePage = lazy(() => import('./pages/ProfilePage.jsx').then((m) => ({ default: m.ProfilePage })))
const ResumeLibraryPage = lazy(() =>
  import('./pages/ResumeLibraryPage.jsx').then((m) => ({ default: m.ResumeLibraryPage }))
)
const CandidateApplicationsPage = lazy(() =>
  import('./pages/CandidateApplicationsPage.jsx').then((m) => ({ default: m.CandidateApplicationsPage }))
)
const ApplicationTasksPage = lazy(() =>
  import('./pages/ApplicationTasksPage.jsx').then((m) => ({ default: m.ApplicationTasksPage }))
)
const ApplyPage = lazy(() => import('./pages/ApplyPage.jsx').then((m) => ({ default: m.ApplyPage })))
const MessagesPage = lazy(() => import('./pages/MessagesPage.jsx').then((m) => ({ default: m.MessagesPage })))
const NotificationsPage = lazy(() =>
  import('./pages/NotificationsPage.jsx').then((m) => ({ default: m.NotificationsPage }))
)

const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx').then((m) => ({ default: m.DashboardPage })))
const JobsManagementPage = lazy(() =>
  import('./pages/JobsManagementPage.jsx').then((m) => ({ default: m.JobsManagementPage }))
)
const KanbanPage = lazy(() => import('./pages/KanbanPage.jsx').then((m) => ({ default: m.KanbanPage })))
const RecruiterMessagesPage = lazy(() =>
  import('./pages/RecruiterMessagesPage.jsx').then((m) => ({ default: m.RecruiterMessagesPage }))
)
const AdminUsersPage = lazy(() => import('./pages/AdminUsersPage.jsx').then((m) => ({ default: m.AdminUsersPage })))
const AdminCompaniesPage = lazy(() =>
  import('./pages/AdminCompaniesPage.jsx').then((m) => ({ default: m.AdminCompaniesPage }))
)
const AdminSkillDictionaryPage = lazy(() =>
  import('./pages/AdminSkillDictionaryPage.jsx').then((m) => ({ default: m.AdminSkillDictionaryPage }))
)
const AdminAnalyticsPage = lazy(() =>
  import('./pages/AdminAnalyticsPage.jsx').then((m) => ({ default: m.AdminAnalyticsPage }))
)
const ProfileSessionsPage = lazy(() =>
  import('./pages/ProfileSessionsPage.jsx').then((m) => ({ default: m.ProfileSessionsPage }))
)
const CompanyStaffPage = lazy(() =>
  import('./pages/CompanyStaffPage.jsx').then((m) => ({ default: m.CompanyStaffPage }))
)
const ForbiddenPage = lazy(() => import('./pages/ForbiddenPage.jsx').then((m) => ({ default: m.ForbiddenPage })))

function PageFallback() {
  return <div className="mx-auto max-w-6xl px-4 py-10 text-sm text-slate-500 dark:text-slate-400">Đang tải…</div>
}

function withSuspense(element) {
  return <Suspense fallback={<PageFallback />}>{element}</Suspense>
}

export default function App() {
  return (
    <Routes>
      <Route element={<PublicShell />}>
        <Route index element={<Navigate to="/jobs" replace />} />
        <Route path="/login" element={withSuspense(<LoginPage />)} />
        <Route path="/register" element={withSuspense(<RegisterPage />)} />
        <Route path="/verify-email" element={withSuspense(<VerifyEmailPage />)} />
        <Route path="/verify-otp" element={withSuspense(<VerifyOtpPage />)} />
        {FEATURE_PASSWORD_RESET_EMAIL ? (
          <>
            <Route path="/forgot-password" element={withSuspense(<ForgotPasswordPage />)} />
            <Route path="/forgot-password/confirm" element={withSuspense(<ForgotPasswordConfirmPage />)} />
          </>
        ) : (
          <>
            <Route path="/forgot-password" element={<Navigate to="/login" replace />} />
            <Route path="/forgot-password/confirm" element={<Navigate to="/login" replace />} />
          </>
        )}
        <Route path="/forbidden" element={withSuspense(<ForbiddenPage />)} />
        <Route path="/jobs" element={withSuspense(<JobsPage />)} />
        <Route path="/jobs/:id" element={withSuspense(<JobDetailPage />)} />
        <Route path="/profile" element={<RequireAuth>{withSuspense(<ProfilePage />)}</RequireAuth>} />
        <Route
          path="/profile/sessions"
          element={<RequireAuth>{withSuspense(<ProfileSessionsPage />)}</RequireAuth>}
        />
        <Route
          path="/profile/resumes"
          element={
            <RequireAuth>
              <RequireRole roles={['CANDIDATE']}>{withSuspense(<ResumeLibraryPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/candidate/applications"
          element={<RequireAuth>{withSuspense(<CandidateApplicationsPage />)}</RequireAuth>}
        />
        <Route
          path="/candidate/applications/:applicationId/tasks"
          element={
            <RequireAuth>
              <RequireRole roles={['CANDIDATE']}>{withSuspense(<ApplicationTasksPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/messages"
          element={
            <RequireAuth>
              <RequireRole roles={['CANDIDATE']}>{withSuspense(<MessagesPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route path="/notifications" element={<RequireAuth>{withSuspense(<NotificationsPage />)}</RequireAuth>} />
        <Route path="/jobs/:jobId/apply" element={<RequireAuth>{withSuspense(<ApplyPage />)}</RequireAuth>} />
      </Route>

      <Route element={<AdminShell />}>
        <Route
          path="/dashboard"
          element={
            <RequireAuth>
              <RequireRole roles={['HR', 'COMPANY']} redirectTo="/admin/analytics">
                {withSuspense(<DashboardPage />)}
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/messages"
          element={
            <RequireAuth>
              <RequireRole roles={['HR', 'COMPANY']}>{withSuspense(<RecruiterMessagesPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/jobs/management"
          element={
            <RequireAuth>
              <RequireRole roles={['HR', 'ADMIN', 'COMPANY']}>{withSuspense(<JobsManagementPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/jobs/:jobId/kanban"
          element={
            <RequireAuth>
              <RequireRole roles={['HR', 'COMPANY']}>{withSuspense(<KanbanPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/applications/:applicationId/tasks"
          element={
            <RequireAuth>
              <RequireRole roles={['HR', 'COMPANY']}>{withSuspense(<ApplicationTasksPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/users"
          element={
            <RequireAuth>
              <RequireRole roles={['ADMIN']}>{withSuspense(<AdminUsersPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/companies"
          element={
            <RequireAuth>
              <RequireRole roles={['ADMIN']}>{withSuspense(<AdminCompaniesPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/master-data/skills"
          element={
            <RequireAuth>
              <RequireRole roles={['ADMIN']}>{withSuspense(<AdminSkillDictionaryPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/analytics"
          element={
            <RequireAuth>
              <RequireRole roles={['ADMIN']}>{withSuspense(<AdminAnalyticsPage />)}</RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/company/staff"
          element={
            <RequireAuth>
              <RequireRole roles={['COMPANY']}>{withSuspense(<CompanyStaffPage />)}</RequireRole>
            </RequireAuth>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/jobs" replace />} />
    </Routes>
  )
}
