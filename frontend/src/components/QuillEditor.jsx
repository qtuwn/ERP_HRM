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

  return (
    <div className="w-full min-w-0 max-w-full overflow-hidden rounded border border-gray-300 bg-white dark:border-slate-700">
      <div
        ref={containerRef}
        className="quill-host [&_.ql-container]:max-w-full [&_.ql-editor]:max-w-full [&_.ql-editor]:break-words [&_.ql-editor]:[overflow-wrap:anywhere] [&_.ql-toolbar]:flex-wrap"
        style={{ minHeight: 200 }}
      />
    </div>
  )
}
