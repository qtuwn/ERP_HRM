/** @type {import('tailwindcss').Config} */
export default {
  // Khớp theme.js: chỉ khi <html class="dark"> thì các lớp dark: mới áp dụng.
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
}
