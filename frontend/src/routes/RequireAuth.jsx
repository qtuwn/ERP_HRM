import { Navigate, useLocation } from 'react-router-dom'
import { getAccessToken } from '../lib/storage.js'

export function RequireAuth({ children }) {
  const token = getAccessToken()
  const location = useLocation()
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}

