import { useEffect, useMemo, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { getUser, setUser as persistUser } from '../lib/storage.js'
import { api } from '../lib/api.js'

export function ProfilePage() {
  const initialUser = useMemo(() => getUser(), [])
  const [user, setUser] = useState(initialUser)
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSubmitting, setProfileSubmitting] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState('')
  const [fullName, setFullName] = useState(initialUser?.fullName || '')
  const [phone, setPhone] = useState(initialUser?.phone || '')

  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [pwLoading, setPwLoading] = useState(false)
  const [pwError, setPwError] = useState('')
  const [pwSuccess, setPwSuccess] = useState('')

  if (!user) return <Navigate to="/login" replace state={{ from: '/profile' }} />

  useEffect(() => {
    let alive = true
    async function loadNotifications() {
      setNotificationsLoading(true)
      try {
        const res = await api.get('/api/notifications')
        if (alive) {
          setNotifications(res?.data || [])
        }
      } catch (err) {
        console.error('Failed to load notifications:', err)
      } finally {
        if (alive) setNotificationsLoading(false)
      }
    }
    loadNotifications()
    return () => {
      alive = false
    }
  }, [])

  async function saveProfile(e) {
    e.preventDefault()
    setProfileError('')
    setProfileSuccess('')
    setProfileSubmitting(true)
    try {
      const res = await api.put('/api/users/me', {
        fullName: fullName?.trim() || null,
        phone: phone?.trim() || null,
      })
      const u = res?.data
      if (u) {
        setUser(u)
        persistUser(u)
      }
      setProfileSuccess('Cập nhật hồ sơ thành công.')
    } catch (err) {
      setProfileError(err?.message || 'Cập nhật hồ sơ thất bại.')
    } finally {
      setProfileSubmitting(false)
    }
  }

  async function changePassword(e) {
    e.preventDefault()
    setPwError('')
    setPwSuccess('')

    if (!oldPassword || !newPassword) {
      setPwError('Vui lòng nhập đầy đủ mật khẩu hiện tại và mật khẩu mới.')
      return
    }
    if (newPassword.length < 6) {
      setPwError('Mật khẩu mới phải có ít nhất 6 ký tự.')
      return
    }
    if (newPassword !== confirmNewPassword) {
      setPwError('Xác nhận mật khẩu mới không khớp.')
      return
    }

    setPwLoading(true)
    try {
      await api.post('/api/auth/change-password', { oldPassword, newPassword })
      setPwSuccess('Đổi mật khẩu thành công.')
      setOldPassword('')
      setNewPassword('')
      setConfirmNewPassword('')
    } catch (err) {
      setPwError(err?.message || 'Đổi mật khẩu thất bại.')
    } finally {
      setPwLoading(false)
    }
  }

  return (
    <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Hồ sơ của tôi</h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">Quản lý thông tin cá nhân và cài đặt tài khoản</p>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 mb-6">
        <div className="flex items-center gap-4">
          <div className="w-20 h-20 rounded-full bg-[#2563eb] flex items-center justify-center text-white text-2xl font-bold">
            {(user?.fullName?.charAt(0) || 'U').toUpperCase()}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-100">{user?.fullName || '—'}</h2>
            <p className="text-slate-600 dark:text-slate-400">{user?.email || '—'}</p>
            <span className="mt-1 inline-flex items-center rounded-full bg-blue-100 px-2.5 py-1 text-xs font-medium text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
              {user?.role || '—'}
            </span>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800">
          <h3 className="text-lg font-medium text-slate-900 dark:text-slate-100">Thông tin hồ sơ</h3>
        </div>
        <div className="px-6 py-6">
          <form onSubmit={saveProfile} className="space-y-4 max-w-lg">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Họ và tên</label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                minLength={2}
                maxLength={100}
                className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
                placeholder="Nhập họ tên"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Số điện thoại</label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                maxLength={20}
                className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
                placeholder="Nhập số điện thoại"
              />
            </div>

            <div className="text-sm text-slate-600 dark:text-slate-400">
              <div>Email: <span className="font-medium text-slate-800 dark:text-slate-100">{user?.email || '—'}</span></div>
              <div>Phòng ban: <span className="font-medium text-slate-800 dark:text-slate-100">{user?.department || 'Chưa cập nhật'}</span></div>
              <div>
                Trạng thái:{' '}
                <span
                  className={[
                    'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                    user?.isActive
                      ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
                      : 'bg-rose-100 text-rose-700 dark:bg-rose-900/40 dark:text-rose-300',
                  ].join(' ')}
                >
                  {user?.isActive ? 'Đang hoạt động' : 'Đã khóa'}
                </span>
              </div>
              {profileLoading ? <div className="mt-2">Đang tải hồ sơ…</div> : null}
            </div>

            {profileError ? (
              <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:border-rose-900/40 dark:bg-rose-950/20 dark:text-rose-300">
                {profileError}
              </div>
            ) : null}
            {profileSuccess ? (
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-900/40 dark:bg-emerald-950/20 dark:text-emerald-300">
                {profileSuccess}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={profileSubmitting}
              className="inline-flex items-center justify-center rounded-lg bg-[#2563eb] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#1d4ed8] disabled:opacity-50"
            >
              {profileSubmitting ? 'Đang lưu…' : 'Lưu thay đổi'}
            </button>
          </form>
        </div>
      </div>

      <div className="mt-6 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800">
          <h3 className="text-lg font-medium text-slate-900 dark:text-slate-100">Đổi mật khẩu</h3>
        </div>
        <div className="px-6 py-6">
          <form onSubmit={changePassword} className="space-y-4 max-w-lg">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                Mật khẩu hiện tại
              </label>
              <input
                type="password"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                autoComplete="current-password"
                required
                className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Mật khẩu mới
                </label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  minLength={6}
                  className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Xác nhận mật khẩu mới
                </label>
                <input
                  type="password"
                  value={confirmNewPassword}
                  onChange={(e) => setConfirmNewPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  minLength={6}
                  className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/30 outline-none"
                />
              </div>
            </div>

            {pwError ? (
              <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:border-rose-900/40 dark:bg-rose-950/20 dark:text-rose-300">
                {pwError}
              </div>
            ) : null}
            {pwSuccess ? (
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-900/40 dark:bg-emerald-950/20 dark:text-emerald-300">
                {pwSuccess}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={pwLoading}
              className="inline-flex items-center justify-center rounded-lg bg-[#2563eb] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#1d4ed8] disabled:opacity-50"
            >
              {pwLoading ? 'Đang đổi…' : 'Đổi mật khẩu'}
            </button>
          </form>
        </div>
      </div>

      {user?.role === 'CANDIDATE' ? (
        <div className="mt-6">
          <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="text-lg font-medium text-slate-900 dark:text-slate-100">Đơn ứng tuyển</h3>
            </div>
            <div className="px-6 py-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Xem danh sách đơn ứng tuyển và trạng thái xử lý.
              </p>
              <Link
                to="/candidate/applications"
                className="inline-flex items-center rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-[#1d4ed8]"
              >
                Xem đơn ứng tuyển
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
