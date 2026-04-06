import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api.js'
import { getUser, normalizeUserRole } from '../lib/storage.js'
import { Plus, RefreshCw, Trash2 } from 'lucide-react'

function cx(...parts) {
  return parts.filter(Boolean).join(' ')
}

export function AdminSkillDictionaryPage() {
  const user = useMemo(() => getUser(), [])
  const role = useMemo(() => normalizeUserRole(user?.role), [user])

  const [loading, setLoading] = useState(false)
  const [categories, setCategories] = useState([])
  const [skills, setSkills] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')

  async function fetchAll() {
    setLoading(true)
    try {
      const [cRes, sRes] = await Promise.all([
        api.get('/api/admin/skill-dictionary/categories'),
        api.get('/api/admin/skill-dictionary/skills'),
      ])
      setCategories(cRes?.data || [])
      setSkills(sRes?.data || [])
    } finally {
      setLoading(false)
    }
  }

  async function createCategory() {
    const name = prompt('Tên nhóm kỹ năng (category)')
    if (name === null) return
    try {
      await api.post('/api/admin/skill-dictionary/categories', { name })
      fetchAll()
    } catch (e) {
      alert(e?.message || 'Tạo category thất bại')
    }
  }

  async function deleteCategory(id) {
    if (!confirm('Xóa category? (skills vẫn còn, category_id sẽ thành null)')) return
    try {
      await api.delete(`/api/admin/skill-dictionary/categories/${id}`)
      if (selectedCategoryId === id) setSelectedCategoryId('')
      fetchAll()
    } catch (e) {
      alert(e?.message || 'Xóa category thất bại')
    }
  }

  async function createSkill() {
    const name = prompt('Tên skill')
    if (name === null) return
    try {
      await api.post('/api/admin/skill-dictionary/skills', {
        name,
        categoryId: selectedCategoryId || null,
      })
      fetchAll()
    } catch (e) {
      alert(e?.message || 'Tạo skill thất bại')
    }
  }

  async function deleteSkill(id) {
    if (!confirm('Xóa skill?')) return
    try {
      await api.delete(`/api/admin/skill-dictionary/skills/${id}`)
      fetchAll()
    } catch (e) {
      alert(e?.message || 'Xóa skill thất bại')
    }
  }

  useEffect(() => {
    if (role === 'ADMIN') fetchAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const filteredSkills = selectedCategoryId
    ? skills.filter((s) => String(s.categoryId || '') === String(selectedCategoryId))
    : skills

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-slate-900 dark:text-white">Master data: Skills</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">Quản lý danh mục kỹ năng + nhóm kỹ năng.</p>
        </div>

        <button
          type="button"
          onClick={fetchAll}
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
          disabled={loading}
        >
          <RefreshCw className={loading ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
          Làm mới
        </button>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-950">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <div className="font-semibold text-slate-900 dark:text-white">Categories</div>
            <button
              type="button"
              onClick={createCategory}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Thêm
            </button>
          </div>
          <div className="divide-y divide-slate-200 dark:divide-slate-800">
            <button
              type="button"
              onClick={() => setSelectedCategoryId('')}
              className={cx(
                'flex w-full items-center justify-between px-4 py-3 text-left text-sm',
                !selectedCategoryId ? 'bg-blue-50 dark:bg-blue-950/40' : 'hover:bg-slate-50 dark:hover:bg-slate-900/30',
              )}
            >
              <div className="font-medium text-slate-900 dark:text-white">Tất cả</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">{skills.length}</div>
            </button>
            {categories.map((c) => (
              <div key={c.id} className="flex items-center gap-2 px-4 py-3">
                <button
                  type="button"
                  onClick={() => setSelectedCategoryId(c.id)}
                  className={cx(
                    'min-w-0 flex-1 text-left text-sm hover:underline',
                    selectedCategoryId === c.id ? 'font-semibold text-blue-700 dark:text-blue-300' : 'text-slate-900 dark:text-white',
                  )}
                >
                  <div className="truncate">{c.name}</div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">{c.id}</div>
                </button>
                <button
                  type="button"
                  onClick={() => deleteCategory(c.id)}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
                  aria-label="Xóa category"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </section>

        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-950">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <div className="font-semibold text-slate-900 dark:text-white">Skills</div>
            <button
              type="button"
              onClick={createSkill}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Thêm
            </button>
          </div>
          <div className="divide-y divide-slate-200 dark:divide-slate-800">
            {filteredSkills.length === 0 ? (
              <div className="px-4 py-6 text-sm text-slate-500 dark:text-slate-400">
                {loading ? 'Đang tải…' : 'Chưa có skill nào.'}
              </div>
            ) : (
              filteredSkills.map((s) => (
                <div key={s.id} className="flex items-center gap-2 px-4 py-3">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-slate-900 dark:text-white">{s.name}</div>
                    <div className="text-xs text-slate-500 dark:text-slate-400">{s.id}</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteSkill(s.id)}
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
                    aria-label="Xóa skill"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  )
}

