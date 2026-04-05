import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../lib/api.js'

export function RegisterPage() {
  const navigate = useNavigate()

  const [accountType, setAccountType] = useState('CANDIDATE') // CANDIDATE | HR
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phone, setPhone] = useState('')
  const [companyName, setCompanyName] = useState('')
  const [department, setDepartment] = useState('')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const isHr = useMemo(() => accountType === 'HR', [accountType])

  async function submit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      const payload = {
        email,
        password,
        fullName,
        phone,
        accountType,
        companyName: isHr ? companyName : null,
        department: isHr ? department : null,
      }
      await api.post('/api/auth/register', payload)
      setSuccess('Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.')
      setTimeout(() => navigate('/login'), 900)
    } catch (err) {
      setError(err?.message || 'Lỗi đăng ký')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="max-w-md mx-auto mt-20 p-8 bg-white rounded-xl shadow-sm border">
      <div className="text-center mb-8">
        <h1 className="text-2xl font-bold text-slate-900">Đăng ký tài khoản</h1>
        <p className="text-slate-500 mt-2">Hỗ trợ đăng ký Ứng viên hoặc HR/Doanh nghiệp</p>
      </div>

      <form onSubmit={submit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Loại tài khoản</label>
          <div className="grid grid-cols-2 gap-2 bg-slate-100 rounded-lg p-1">
            <button
              type="button"
              onClick={() => setAccountType('CANDIDATE')}
              className={[
                'py-2 rounded-md text-sm font-medium transition',
                accountType === 'CANDIDATE' ? 'bg-white shadow text-[#2563eb]' : 'text-slate-600',
              ].join(' ')}
            >
              Ứng viên
            </button>
            <button
              type="button"
              onClick={() => setAccountType('HR')}
              className={[
                'py-2 rounded-md text-sm font-medium transition',
                accountType === 'HR' ? 'bg-white shadow text-[#2563eb]' : 'text-slate-600',
              ].join(' ')}
            >
              HR / Công ty
            </button>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Họ và tên</label>
          <input
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            type="text"
            required
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Email</label>
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            required
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Mật khẩu</label>
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              required
              className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Điện thoại</label>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              type="text"
              required
              className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all"
            />
          </div>
        </div>

        {isHr ? (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Tên công ty</label>
              <input
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                type="text"
                required
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Phòng ban</label>
              <input
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                type="text"
                required
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-[#2563eb] focus:border-[#2563eb] outline-none transition-all"
              />
            </div>
          </div>
        ) : null}

        {error ? (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
        ) : null}
        {success ? (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
            {success}
          </div>
        ) : null}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-[#2563eb] text-white font-medium py-2.5 rounded-lg hover:bg-blue-700 transition-colors shadow-sm disabled:opacity-50 flex justify-center items-center"
        >
          {loading ? 'Đang đăng ký…' : 'Đăng ký'}
        </button>
      </form>

      <div className="mt-6 text-center text-sm text-slate-600">
        Đã có tài khoản?{' '}
        <Link to="/login" className="text-[#2563eb] font-medium hover:underline">
          Đăng nhập
        </Link>
      </div>
    </section>
  )
}
