import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../lib/api.js'
import { getSockJsUrl } from '../lib/config.js'
import { getAccessToken, getUser } from '../lib/storage.js'
import { Send } from 'lucide-react'

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

const MAX_RECONNECT_ATTEMPTS = 10

/**
 * Khối chat theo applicationId — dùng trong modal (ChatWidget) hoặc layout 2 cột (MessagesPage).
 */
export function ApplicationChatPanel({ applicationId, applicationTitle, className = '' }) {
  const currentUser = useMemo(() => getUser(), [])
  const token = getAccessToken()

  const [messages, setMessages] = useState([])
  const [newMessage, setNewMessage] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [typingUser, setTypingUser] = useState(null)
  const [wsStatus, setWsStatus] = useState('disconnected')
  const [wsError, setWsError] = useState('')
  const [wsRetryAttempt, setWsRetryAttempt] = useState(0)

  const typingLockRef = useRef(false)
  const typingTimeoutRef = useRef(null)
  const stompRef = useRef(null)
  const bottomRef = useRef(null)
  const reconnectAttemptsRef = useRef(0)
  const reconnectLockedRef = useRef(false)
  const reconnectTimerRef = useRef(null)
  /** Tránh reconnect khi đóng socket có chủ đích (đổi thread / unmount). */
  const activeAppIdRef = useRef(null)
  /** Hủy kết quả connect bất đồng bộ cũ (React Strict Mode / đổi applicationId nhanh). */
  const connectGenRef = useRef(0)
  const stompSubRef = useRef(null)

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
      stompSubRef.current?.unsubscribe()
    } catch {
      // ignore
    } finally {
      stompSubRef.current = null
    }
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
    const base = 1000
    const max = 30000
    const exp = Math.min(max, base * 2 ** Math.max(0, attempt - 1))
    const jitter = Math.floor(Math.random() * 300)
    return Math.min(max, exp + jitter)
  }

  function scheduleReconnect(appId, reason) {
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
      const g = connectGenRef.current
      connectStomp(appId, g, { resetRetry: false })
    }, delay)
  }

  async function connectStomp(appId, connectGen, { resetRetry = true } = {}) {
    if (!token || !appId) return
    disconnect({ keepRetryState: !resetRetry })
    if (connectGen !== connectGenRef.current) return

    if (resetRetry) {
      reconnectAttemptsRef.current = 0
      reconnectLockedRef.current = false
      setWsRetryAttempt(0)
    }
    setWsError('')
    setWsStatus('connecting')

    const [{ default: SockJS }, { Client }] = await Promise.all([
      import('sockjs-client/dist/sockjs'),
      import('@stomp/stompjs'),
    ])

    if (connectGen !== connectGenRef.current) return

    const client = new Client({
      webSocketFactory: () => new SockJS(getSockJsUrl()),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    })

    client.onConnect = () => {
      if (connectGen !== connectGenRef.current || activeAppIdRef.current !== appId) return
      reconnectAttemptsRef.current = 0
      setWsRetryAttempt(0)
      setWsStatus('connected')
      setWsError('')
      try {
        stompSubRef.current?.unsubscribe()
      } catch {
        // ignore
      }
      stompSubRef.current = client.subscribe(`/topic/applications/${appId}`, (frame) => {
        if (connectGen !== connectGenRef.current || activeAppIdRef.current !== appId) return
        try {
          const event = JSON.parse(frame.body)
          if (event.type === 'chat:new_message') {
            const p = event.payload
            setMessages((prev) => {
              if (p?.id != null && prev.some((m) => String(m.id) === String(p.id))) return prev
              if (
                p?.content != null &&
                p?.senderId != null &&
                p?.createdAt &&
                prev.some(
                  (m) =>
                    String(m.senderId) === String(p.senderId) &&
                    m.content === p.content &&
                    String(m.createdAt) === String(p.createdAt)
                )
              ) {
                return prev
              }
              return [...prev, p]
            })
            setTypingUser((prevTyping) => {
              if (prevTyping && p?.senderRole === prevTyping) return null
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
            }
          }
        } catch {
          // ignore
        }
      })
    }

    client.onDisconnect = () => {
      if (connectGen !== connectGenRef.current) return
      if (activeAppIdRef.current !== appId) return
      setWsStatus('disconnected')
    }

    client.onStompError = () => {
      if (connectGen !== connectGenRef.current || activeAppIdRef.current !== appId) return
      scheduleReconnect(appId, 'Kết nối realtime gặp lỗi (STOMP). Đang thử lại...')
    }

    client.onWebSocketClose = () => {
      if (connectGen !== connectGenRef.current) return
      if (activeAppIdRef.current !== appId) return
      scheduleReconnect(appId, 'Mất kết nối realtime. Đang thử lại...')
    }

    client.onWebSocketError = () => {
      if (connectGen !== connectGenRef.current || activeAppIdRef.current !== appId) return
      scheduleReconnect(appId, 'Lỗi kết nối realtime. Đang thử lại...')
    }

    if (connectGen !== connectGenRef.current) return
    client.activate()
    stompRef.current = client
  }

  function manualReconnect() {
    if (!applicationId) return
    reconnectAttemptsRef.current = 0
    reconnectLockedRef.current = false
    setWsRetryAttempt(0)
    connectGenRef.current += 1
    const g = connectGenRef.current
    activeAppIdRef.current = applicationId
    connectStomp(applicationId, g)
  }

  useEffect(() => {
    if (!applicationId) return
    connectGenRef.current += 1
    const myGen = connectGenRef.current
    activeAppIdRef.current = applicationId
    setMessages([])
    setTypingUser(null)
    fetchHistory(applicationId)
    connectStomp(applicationId, myGen)
    return () => {
      activeAppIdRef.current = null
      disconnect()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applicationId, token, currentUser?.id])

  useEffect(() => {
    scrollToBottom()
  }, [messages.length])

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

  if (!applicationId) {
    return (
      <div className={`flex flex-1 items-center justify-center p-8 text-sm text-slate-500 ${className}`}>
        Chọn một cuộc trò chuyện bên trái.
      </div>
    )
  }

  return (
    <div className={`flex min-h-0 flex-1 flex-col bg-white dark:bg-slate-900 ${className}`}>
      <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
        <h2 className="font-semibold text-slate-900 dark:text-white">{applicationTitle || 'Chat'}</h2>
        <p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
          <span
            className={[
              'inline-flex items-center rounded-full px-2 py-0.5 font-medium',
              wsStatus === 'connected'
                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200'
                : wsStatus === 'connecting'
                  ? 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                  : wsStatus === 'retrying'
                    ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200'
                    : wsStatus === 'failed'
                      ? 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-200'
                      : 'bg-slate-100 text-slate-600 dark:bg-slate-800',
            ].join(' ')}
          >
            {wsStatus === 'connected'
              ? 'Realtime: ON'
              : wsStatus === 'connecting'
                ? 'Đang kết nối...'
                : wsStatus === 'retrying'
                  ? `Thử lại (${wsRetryAttempt}/${MAX_RECONNECT_ATTEMPTS})`
                  : wsStatus === 'failed'
                    ? 'Realtime: OFF'
                    : 'Realtime: OFF'}
          </span>
        </p>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto bg-slate-50 px-4 py-4 dark:bg-slate-950/30">
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
                      ? 'rounded-br-none bg-[#2563eb] text-white'
                      : 'rounded-bl-none border border-slate-200 bg-white text-slate-800 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100',
                  ].join(' ')}
                >
                  <p className="mb-1 text-xs font-semibold opacity-75">{isMe ? 'Bạn' : msg.senderRole}</p>
                  <p className="whitespace-pre-wrap text-sm">{msg.content}</p>
                  <p className="mt-1 text-right text-[10px] opacity-60">{formatTime(msg.createdAt)}</p>
                </div>
              </li>
            )
          })}
        </ul>
        {typingUser ? (
          <div className="mt-4 flex justify-start">
            <div className="animate-pulse rounded-full bg-slate-200 px-4 py-2 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
              {typingUser} đang nhập...
            </div>
          </div>
        ) : null}
        <div ref={bottomRef} />
      </div>

      <div className="border-t border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
        <form onSubmit={sendMessage} className="flex gap-2">
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onInput={onTyping}
            placeholder="Nhập tin nhắn..."
            className="w-full flex-1 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            required
          />
          <button
            type="submit"
            disabled={isSending || !newMessage.trim()}
            className="inline-flex items-center gap-2 rounded-md border border-transparent bg-[#2563eb] px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-[#1d4ed8] disabled:opacity-50"
          >
            <Send className="h-4 w-4" />
            Gửi
          </button>
        </form>
      </div>
    </div>
  )
}
