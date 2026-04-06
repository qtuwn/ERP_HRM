/** Rút gọn mô tả job (HTML từ Quill) thành text thuần cho thẻ card. */
export function stripHtmlToText(html, maxLen = 220) {
  if (html == null || html === '') return ''
  const s = String(html)
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/\s+/g, ' ')
    .trim()
  if (s.toLowerCase() === 'null') return ''
  if (maxLen > 0 && s.length > maxLen) return `${s.slice(0, maxLen)}…`
  return s
}
