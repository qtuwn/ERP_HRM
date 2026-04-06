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
 * SockJS cần URL HTTP(S) tới endpoint Spring `/ws/hrm`.
 * - Dev (Vite): mặc định `/ws/hrm` → proxy `vite.config.js` tới :8080.
 * - Ưu tiên: VITE_WS_ORIGIN (nếu set).
 * - Nếu chỉ set VITE_API_BASE_URL (API khác origin): dùng luôn base đó cho WS, tránh nối nhầm vào port FE.
 */
export function getSockJsUrl() {
  const wsOrigin = String(import.meta.env.VITE_WS_ORIGIN || '')
    .trim()
    .replace(/\/$/, '')
  if (wsOrigin) return `${wsOrigin}/ws/hrm`
  const apiBase = getApiBaseUrl()
  if (apiBase) return `${apiBase}/ws/hrm`
  return '/ws/hrm'
}
