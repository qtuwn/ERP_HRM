import { useEffect, useRef } from 'react'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

export function QuillEditor({ value, onChange, placeholder = '' }) {
  const containerRef = useRef(null)
  const quillRef = useRef(null)

  useEffect(() => {
    if (!containerRef.current) return
    if (quillRef.current) return

    const q = new Quill(containerRef.current, {
      theme: 'snow',
      placeholder,
      modules: {
        toolbar: [
          ['bold', 'italic', 'underline', 'strike'],
          ['blockquote', 'code-block'],
          [{ list: 'ordered' }, { list: 'bullet' }],
          ['link'],
        ],
      },
    })

    q.on('text-change', () => {
      onChange?.(q.root.innerHTML)
    })

    quillRef.current = q
  }, [onChange, placeholder])

  useEffect(() => {
    const q = quillRef.current
    if (!q) return
    const html = value || ''
    if (q.root.innerHTML !== html) {
      const sel = q.getSelection()
      q.root.innerHTML = html
      if (sel) q.setSelection(sel)
    }
  }, [value])

  return <div className="bg-white border border-gray-300 rounded" style={{ height: 200 }} ref={containerRef} />
}
