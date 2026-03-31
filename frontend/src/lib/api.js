import { getAccessToken } from './storage.js'

async function request(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const token = getAccessToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const text = await res.text()
  const json = text ? JSON.parse(text) : null

  if (!res.ok) {
    const msg = json?.message || json?.error || `HTTP ${res.status}`
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
}

