import { getAccessToken, clearSession } from './storage.js'
import { resolveApiUrl } from './config.js'

const AUTH_PATH_PREFIXES = ['/login', '/register', '/forgot-password', '/verify-email', '/verify-otp']

function shouldSkipAuthRedirect() {
  const path = window.location.pathname
  return AUTH_PATH_PREFIXES.some((p) => path === p || path.startsWith(`${p}/`))
}

function safeNextPath(next) {
  if (!next || typeof next !== 'string') return '/dashboard'
  if (!next.startsWith('/') || next.startsWith('//')) return '/dashboard'
  return next
}

function handleAuthStatus(status) {
  if (status === 401) {
    clearSession()
    if (!shouldSkipAuthRedirect()) {
      const raw = `${window.location.pathname}${window.location.search || ''}`
      const next = safeNextPath(raw)
      window.location.assign(`/login?next=${encodeURIComponent(next)}`)
    }
    return
  }
  if (status === 403) {
    if (!window.location.pathname.startsWith('/forbidden')) {
      window.location.assign('/forbidden')
    }
  }
}

async function parseBody(res) {
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { raw: text }
  }
}

async function request(path, { method = 'GET', body } = {}) {
  const url = resolveApiUrl(path)
  const headers = { Accept: 'application/json' }
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
  const isUrlEncoded = typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams
  if (body !== undefined && !isFormData && !isUrlEncoded) headers['Content-Type'] = 'application/json'

  const token = getAccessToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(url, {
    method,
    headers,
    body:
      body === undefined
        ? undefined
        : isFormData || isUrlEncoded
          ? body
          : typeof body === 'string'
            ? body
            : JSON.stringify(body),
  })

  const json = await parseBody(res)

  if (!res.ok) {
    handleAuthStatus(res.status)
    const msg = json?.message || json?.error || json?.raw || `HTTP ${res.status}`
    const err = new Error(msg)
    err.status = res.status
    err.payload = json
    throw err
  }

  if (json && json.success === false) {
    throw new Error(json.message || 'Request failed')
  }

  return json
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
}

export { resolveApiUrl }
