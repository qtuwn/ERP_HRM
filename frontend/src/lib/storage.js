const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_KEY = 'user'

export function setSession({ accessToken, refreshToken, user }) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function setUser(user) {
  if (!user) return
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/** Chuẩn hóa role từ JWT/user JSON (ADMIN, ROLE_HR, …) → ADMIN | HR | COMPANY | CANDIDATE */
export function normalizeUserRole(role) {
  if (role == null) return ''
  let s = String(role).trim().toUpperCase()
  if (s.startsWith('ROLE_')) s = s.slice(5)
  if (s === 'SUPER_ADMIN' || s === 'SUPERADMIN') return 'ADMIN'
  if (s === 'HR_MANAGER') return 'COMPANY'
  return s
}
