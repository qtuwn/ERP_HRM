const THEME_KEY = 'theme'

export function getStoredTheme() {
  const v = localStorage.getItem(THEME_KEY)
  if (v === 'dark' || v === 'light') return v
  return 'light'
}

export function applyTheme(theme) {
  const isDark = theme === 'dark'
  document.documentElement.classList.toggle('dark', isDark)
  localStorage.setItem(THEME_KEY, isDark ? 'dark' : 'light')
  window.dispatchEvent(new CustomEvent('theme-changed', { detail: { theme: isDark ? 'dark' : 'light' } }))
}
