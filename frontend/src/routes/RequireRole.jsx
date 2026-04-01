import { Navigate } from 'react-router-dom'
import { getUser, normalizeUserRole } from '../lib/storage.js'

/**
 * Dùng bên trong RequireAuth. Không đủ role → chuyển về redirectTo (mặc định trang public).
 */
export function RequireRole({ children, roles, redirectTo = '/jobs' }) {
  const mine = normalizeUserRole(getUser()?.role)
  const allowed = roles.some((r) => normalizeUserRole(r) === mine)
  if (!allowed) {
    return <Navigate to={redirectTo} replace />
  }
  return children
}
