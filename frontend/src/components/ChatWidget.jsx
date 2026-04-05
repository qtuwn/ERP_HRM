import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../lib/api.js'
import { getSockJsUrl } from '../lib/config.js'
import { getAccessToken, getUser } from '../lib/storage.js'
import { X, Send } from 'lucide-react'

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

export function ChatWidget() {
  const currentUser = useMemo(() => getUser(), [])
  const token = getAccessToken()

  const [isOpen, setIsOpen] = useState(false)
  const [applicationId, setApplicationId] = useState(null)
  const [applicationTitle, setApplicationTitle] = useState('')
  const [messages, setMessages] = useState([])
  const [newMessage, setNewMessage] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [typingUser, setTypingUser] = useState(null)
  const [wsStatus, setWsStatus] = useState('disconnected') // disconnected | connecting | connected | retrying | failed
  const [wsError, setWsError] = useState('')
  const [wsRetryAttempt, setWsRetryAttempt] = useState(0)

  const typingLockRef = useRef(false)
  const typingTimeoutRef = useRef(null)
  const stompRef = useRef(null)
  const bottomRef = useRef(null)
  const reconnectAttemptsRef = useRef(0)
  const reconnectLockedRef = useRef(false)
  const reconnectTimerRef = useRef(null)
  const MAX_RECONNECT_ATTEMPTS = 10

  function scrollToBottom() {
    requestAnimationFrame(() => {
      bottomRef.current?.scrollIntoView({ block: 'end' })
    })
  }

  async function fetchHistory(appId) {
    try {
      const res = await api.get(`/api/applications/${appId}/messages?size=50`)
      setMessages(res?.data?.content || [])
      scrollToBottom()
    } catch {
      // ignore
    }
  }

  function disconnect({ keepRetryState = false } = {}) {
    try {
      stompRef.current?.deactivate()
    } catch {
      // ignore
    } finally {
      stompRef.current = null
    }
    if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current)
    reconnectTimerRef.current = null
    if (!keepRetryState) {
      reconnectLockedRef.current = false
      reconnectAttemptsRef.current = 0
      setWsRetryAttempt(0)
    }
  }

  function nextReconnectDelayMs(attempt) {
    const base = 1000 // 1s
    const max = 30000 // 30s
    const exp = Math.min(max, base * 2 ** Math.max(0, attempt - 1))
    const jitter = Math.floor(Math.random() * 300)
    return Math.min(max, exp + jitter)
  }

  function scheduleReconnect(appId, reason) {
    if (!isOpen) return
    if (!appId) return
    if (reconnectLockedRef.current) return

    reconnectAttemptsRef.current += 1
    const attempt = reconnectAttemptsRef.current
    setWsRetryAttempt(attempt)

    if (attempt > MAX_RECONNECT_ATTEMPTS) {
      reconnectLockedRef.current = true
      setWsStatus('failed')
      setWsError('Mất kết nối realtime. Đã dừng tự thử lại. Bấm "Kết nối lại" để thử tiếp.')
      return
    }

    const delay = nextReconnectDelayMs(attempt)
    setWsStatus('retrying')
    setWsError(reason || 'Mất kết nối realtime. Đang thử lại...')

    if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current)
    reconnectTimerRef.current = setTimeout(() => {
      connectStomp(appId, { resetRetry: false })
    }, delay)
  }

  async function connectStomp(appId, { resetRetry = true } = {}) {
    if (!token) return
    disconnect({ keepRetryState: !resetRetry })
    if (resetRetry) {
      reconnectAttemptsRef.current = 0
      reconnectLockedRef.current = false
      setWsRetryAttempt(0)
    }
    setWsError('')
    setWsStatus('connecting')

    // Lazy-load heavy deps only when chat opens
    const [{ default: SockJS }, { Client }] = await Promise.all([
      import('sockjs-client/dist/sockjs'),
      import('@stomp/stompjs'),
    ])

    const client = new Client({
      webSocketFactory: () => new SockJS(getSockJsUrl()),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    })

    client.onConnect = () => {
      reconnectAttemptsRef.current = 0
      setWsRetryAttempt(0)
      setWsStatus('connected')
      setWsError('')
      client.subscribe(`/topic/applications/${appId}`, (frame) => {
        try {
          const event = JSON.parse(frame.body)
          if (event.type === 'chat:new_message') {
            setMessages((prev) => [...prev, event.payload])
            setTypingUser((prevTyping) => {
              if (prevTyping && event.payload?.senderRole === prevTyping) return null
              return prevTyping
            })
            scrollToBottom()
          } else if (event.type === 'chat:typing') {
            const senderId = event?.payload?.senderId
            const senderRole = event?.payload?.senderRole
            if (String(senderId) !== String(currentUser?.id)) {
              setTypingUser(senderRole || 'User')
              if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current)
              typingTimeoutRef.current = setTimeout(() => setTypingUser(null), 2500)
              scrollToBottom()
            }
          }
        } catch {
          // ignore
        }
      })
    }

    client.onDisconnect = () => {
      if (!isOpen) return
      setWsStatus('disconnected')
    }

    client.onStompError = () => {
      if (!isOpen) return
      scheduleReconnect(appId, 'Kết nối realtime gặp lỗi (STOMP). Đang thử lại...')
    }

    client.onWebSocketClose = () => {
      scheduleReconnect(appId, 'Mất kết nối realtime. Đang thử lại...')
    }

    client.onWebSocketError = () => {
      scheduleReconnect(appId, 'Lỗi kết nối realtime. Đang thử lại...')
    }

    client.activate()
    stompRef.current = client
  }

  function manualReconnect() {
    if (!applicationId) return
    reconnectAttemptsRef.current = 0
    reconnectLockedRef.current = false
    setWsRetryAttempt(0)
    connectStomp(applicationId)
  }

  function closeChat() {
    setIsOpen(false)
    setTypingUser(null)
    setWsStatus('disconnected')
    setWsError('')
    disconnect()
  }

  async function sendMessage(e) {
    e.preventDefault()
    const content = newMessage.trim()
    if (!content || !applicationId) return
    setIsSending(true)
    try {
      await api.post(`/api/applications/${applicationId}/messages`, { content })
      setNewMessage('')
    } catch (err) {
      if (err?.status === 429) {
        alert('Bạn gửi quá nhanh. Vui lòng chờ vài giây rồi thử lại.')
      } else {
        alert(err?.message || 'Gửi tin nhắn thất bại')
      }
    } finally {
      setIsSending(false)
    }
  }

  async function onTyping() {
    if (!applicationId) return
    if (typingLockRef.current) return
    typingLockRef.current = true
    setTimeout(() => {
      typingLockRef.current = false
    }, 1500)
    try {
      await api.post(`/api/applications/${applicationId}/messages/typing`, {})
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    function onOpenChat(ev) {
      const detail = ev?.detail || {}
      const appId = detail.applicationId
      if (!appId) return
      setApplicationId(appId)
      setApplicationTitle(detail.applicationTitle || 'Chat')
      setIsOpen(true)
      setMessages([])
      fetchHistory(appId)
      connectStomp(appId)
    }
    window.addEventListener('open-chat', onOpenChat)
    return () => window.removeEventListener('open-chat', onOpenChat)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, currentUser?.id])

  useEffect(() => {
    if (isOpen) scrollToBottom()
  }, [isOpen, messages.length])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 overflow-hidden z-[100]" role="dialog" aria-modal="true">
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute inset-0 bg-black/40 transition-opacity" onClick={closeChat} aria-hidden="true" />

        <div className="fixed inset-y-0 right-0 pl-10 max-w-full flex">
          <div className="w-screen max-w-md">
            <div className="h-full flex flex-col bg-white dark:bg-slate-900 shadow-xl border-l border-slate-200 dark:border-slate-800">
              <div className="px-4 py-6 bg-[#2563eb] text-white sm:px-6 flex justify-between items-center">
                <div>
                  <h2 className="text-lg font-medium">Chat</h2>
                  <p className="text-sm opacity-80 flex items-center gap-2">
                    <span>{applicationTitle}</span>
                    <span
                      className={[
                        'inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium',
                        wsStatus === 'connected'
                          ? 'bg-emerald-200/20 text-white'
                          : wsStatus === 'connecting'
                            ? 'bg-white/20 text-white'
                            : wsStatus === 'retrying'
                              ? 'bg-amber-200/20 text-white'
                              : wsStatus === 'failed'
                                ? 'bg-rose-200/20 text-white'
                                : 'bg-white/10 text-white',
                      ].join(' ')}
                    >
                      {wsStatus === 'connected'
                        ? 'Realtime: ON'
                        : wsStatus === 'connecting'
                          ? 'Đang kết nối...'
                          : wsStatus === 'retrying'
                            ? `Đang thử lại... (${wsRetryAttempt}/${MAX_RECONNECT_ATTEMPTS})`
                            : wsStatus === 'failed'
                              ? 'Realtime: OFF'
                              : 'Realtime: OFF'}
                    </span>
                  </p>
                </div>
                <button
                  onClick={closeChat}
                  className="rounded-md text-white/90 hover:text-white focus:outline-none"
                  aria-label="Close"
                >
                  <X className="h-6 w-6" />
                </button>
              </div>

              <div className="flex-1 px-4 py-6 sm:px-6 overflow-y-auto bg-slate-50 dark:bg-slate-950/20">
                {wsError ? (
                  <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-900/40 dark:bg-amber-950/30 dark:text-amber-200">
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">{wsError}</div>
                      {wsStatus === 'failed' ? (
                        <button
                          type="button"
                          onClick={manualReconnect}
                          className="shrink-0 rounded-md bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700"
                        >
                          Kết nối lại
                        </button>
                      ) : null}
                    </div>
                  </div>
                ) : null}
                <ul className="space-y-4">
                  {messages.map((msg) => {
                    const isMe = String(msg.senderId) === String(currentUser?.id)
                    return (
                      <li key={msg.id} className={['flex', isMe ? 'justify-end' : 'justify-start'].join(' ')}>
                        <div
                          className={[
                            'max-w-[80%] rounded-lg px-4 py-2 shadow-sm',
                            isMe
                              ? 'bg-[#2563eb] text-white rounded-br-none'
                              : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-100 rounded-bl-none',
                          ].join(' ')}
                        >
                          <p className="text-xs font-semibold mb-1 opacity-75">{isMe ? 'You' : msg.senderRole}</p>
                          <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                          <p className="text-[10px] mt-1 text-right opacity-60">{formatTime(msg.createdAt)}</p>
                        </div>
                      </li>
                    )
                  })}
                </ul>

                {typingUser ? (
                  <div className="mt-4 flex justify-start">
                    <div className="bg-slate-200 dark:bg-slate-800 rounded-full px-4 py-2 text-xs text-slate-600 dark:text-slate-300 animate-pulse">
                      {typingUser} is typing...
                    </div>
                  </div>
                ) : null}
                <div ref={bottomRef} />
              </div>

              <div className="border-t border-slate-200 dark:border-slate-800 p-4 bg-white dark:bg-slate-900">
                <form onSubmit={sendMessage} className="flex gap-2">
                  <input
                    type="text"
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onInput={onTyping}
                    placeholder="Type a message..."
                    className="flex-1 w-full rounded-md border border-slate-300 dark:border-slate-700 px-3 py-2 shadow-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/20 sm:text-sm bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100"
                    required
                  />
                  <button
                    type="submit"
                    disabled={isSending || !newMessage.trim()}
                    className="inline-flex items-center gap-2 px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-[#2563eb] hover:bg-[#1d4ed8] disabled:opacity-50"
                  >
                    <Send className="h-4 w-4" />
                    Send
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
