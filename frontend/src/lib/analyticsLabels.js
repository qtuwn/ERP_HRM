/** Nhãn tiếng Việt cho thống kê admin (khớp enum BE). */

export const APPLICATION_STATUS_VI = {
  APPLIED: 'Đã nộp',
  AI_SCREENING: 'AI sàng lọc',
  HR_REVIEW: 'HR duyệt',
  INTERVIEW: 'Phỏng vấn',
  OFFER: 'Offer',
  REJECTED: 'Từ chối',
  HIRED: 'Đã nhận việc',
  WITHDRAWN: 'Đã rút đơn',
  AI_QUEUED: 'Chờ AI',
  AI_PROCESSING: 'AI xử lý',
}

export const JOB_STATUS_VI = {
  DRAFT: 'Nháp',
  OPEN: 'Đang tuyển',
  CLOSED: 'Đã đóng',
}

export const ROLE_VI = {
  ADMIN: 'Quản trị',
  HR: 'Nhân sự (HR)',
  COMPANY: 'Quản lý công ty',
  CANDIDATE: 'Ứng viên',
}

export function labelApplicationStatus(key) {
  return APPLICATION_STATUS_VI[key] || key
}

export function labelJobStatus(key) {
  return JOB_STATUS_VI[key] || key
}

export function labelRole(key) {
  return ROLE_VI[key] || key
}

/** Sắp xếp khóa map theo thứ tự ưu tiên (ổn định trên biểu đồ). */
export function sortKeys(keys, order) {
  const set = new Set(keys)
  const first = order.filter((k) => set.has(k))
  const rest = [...set].filter((k) => !order.includes(k)).sort()
  return [...first, ...rest]
}

export const APPLICATION_CHART_ORDER = [
  'APPLIED',
  'AI_QUEUED',
  'AI_SCREENING',
  'AI_PROCESSING',
  'HR_REVIEW',
  'INTERVIEW',
  'OFFER',
  'HIRED',
  'REJECTED',
  'WITHDRAWN',
]

export const JOB_CHART_ORDER = ['DRAFT', 'OPEN', 'CLOSED']

export const ROLE_CHART_ORDER = ['ADMIN', 'COMPANY', 'HR', 'CANDIDATE']
