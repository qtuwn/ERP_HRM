/**
 * Deploy FE tách domain: đặt VITE_API_BASE_URL=https://api.example.com (không có / cuối).
 * Dev để trống → fetch tương đối `/api` (Vite proxy hoặc cùng origin với Spring).
 */
export function getApiBaseUrl() {
  return String(import.meta.env.VITE_API_BASE_URL || '')
    .trim()
    .replace(/\/$/, '')
}

export function resolveApiUrl(path) {
  const base = getApiBaseUrl()
  if (!path) return base || '/'
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const p = path.startsWith('/') ? path : `/${path}`
  return base ? `${base}${p}` : p
}

/**
 * SockJS cần URL HTTP(S) tới endpoint; mặc định cùng origin `/ws/hrm`.
 * Khi API ở domain khác: VITE_WS_ORIGIN=https://api.example.com
 */
export function getSockJsUrl() {
  const origin = String(import.meta.env.VITE_WS_ORIGIN || '')
    .trim()
    .replace(/\/$/, '')
  return origin ? `${origin}/ws/hrm` : '/ws/hrm'
}
