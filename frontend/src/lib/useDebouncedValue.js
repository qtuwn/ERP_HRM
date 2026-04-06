import { useEffect, useState } from 'react'

/**
 * Trì hoãn cập nhật giá trị (debounce) — dùng cho filter client / giảm tính toán khi gõ ô tìm.
 * @param {unknown} value
 * @param {number} delayMs
 */
export function useDebouncedValue(value, delayMs = 280) {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(t)
  }, [value, delayMs])

  return debounced
}
