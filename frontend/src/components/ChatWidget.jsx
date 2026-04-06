import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { ApplicationChatPanel } from './ApplicationChatPanel.jsx'

export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false)
  const [applicationId, setApplicationId] = useState(null)
  const [applicationTitle, setApplicationTitle] = useState('')

  function closeChat() {
    setIsOpen(false)
    setApplicationId(null)
    setApplicationTitle('')
  }

  useEffect(() => {
    function onOpenChat(ev) {
      const detail = ev?.detail || {}
      const appId = detail.applicationId
      if (!appId) return
      setApplicationId(appId)
      setApplicationTitle(detail.applicationTitle || 'Chat')
      setIsOpen(true)
    }
    window.addEventListener('open-chat', onOpenChat)
    return () => window.removeEventListener('open-chat', onOpenChat)
  }, [])

  if (!isOpen || !applicationId) return null

  return (
    <div className="fixed inset-0 z-[100] overflow-hidden" role="dialog" aria-modal="true">
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute inset-0 bg-black/40 transition-opacity" onClick={closeChat} aria-hidden="true" />

        <div className="fixed inset-y-0 right-0 flex max-w-full pl-10">
          <div className="w-screen max-w-md">
            <div className="relative flex h-full flex-col border-l border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900">
              <button
                type="button"
                onClick={closeChat}
                className="absolute right-3 top-3 z-20 rounded-lg bg-slate-100 p-2 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
                aria-label="Đóng"
              >
                <X className="h-5 w-5" />
              </button>
              <ApplicationChatPanel
                applicationId={applicationId}
                applicationTitle={applicationTitle}
                className="min-h-0 flex-1 pt-10"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
