import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { InboxMessage, InboxMessageType } from '../api/types'
import { formatMoney } from '../labels'

const ICONS: Record<InboxMessageType, string> = {
  WELCOME: '👋',
  BOARD: '🏛',
  TRANSFER_OFFER: '💶',
  CONTRACT: '📝',
  INJURY: '🩹',
  YOUTH: '🌱',
  SACKED: '🚪',
  JOB_OFFER: '💼',
}

const ACCEPT_LABELS: Partial<Record<InboxMessageType, string>> = {
  TRANSFER_OFFER: 'Sat',
  JOB_OFFER: 'Görevi kabul et',
  YOUTH: 'A takıma al',
}

const REJECT_LABELS: Partial<Record<InboxMessageType, string>> = {
  TRANSFER_OFFER: 'Reddet',
  JOB_OFFER: 'Reddet',
  YOUTH: 'Bırak',
}

/** Menajerin gelen kutusu: haberler ve kabul / ret bekleyen teklifler. */
function InboxPanel({ onChanged, onlyType }: { onChanged: () => void; onlyType?: InboxMessageType }) {
  const [messages, setMessages] = useState<InboxMessage[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api
      .getInbox()
      .then(setMessages)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  async function act(action: () => Promise<{ message: string }>) {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      setNotice((await action()).message)
      setMessages(await api.getInbox())
      onChanged()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const shown = (messages ?? []).filter((message) => !onlyType || message.type === onlyType)

  return (
    <div className="card">
      <div className="card-header">
        <h2>Gelen kutusu</h2>
        {!onlyType && shown.some((message) => !message.read) && (
          <button
            className="btn btn-sm"
            onClick={() =>
              act(async () => {
                await api.readAllInbox()
                return { message: 'Hepsi okundu' }
              })
            }
            disabled={busy}
          >
            Tümünü okundu say
          </button>
        )}
      </div>
      {error && <p className="alert alert-error">{error}</p>}
      {notice && <p className="alert alert-success">{notice}</p>}
      {messages && shown.length === 0 && <p className="muted">Mesaj yok.</p>}
      <ul className="inbox">
        {shown.map((message) => (
          <li
            key={message.id}
            className={`inbox-item${message.read ? '' : ' unread'}${message.resolved ? ' resolved' : ''}`}
          >
            <span className="inbox-icon" aria-hidden>
              {ICONS[message.type]}
            </span>
            <div className="inbox-content">
              <div className="inbox-title">
                {message.title} <span className="muted">· Sezon {message.seasonNumber}</span>
              </div>
              <div className="inbox-body">{message.body}</div>
              {(message.playerId || message.amount) && (
                <div className="muted inbox-meta">
                  {message.playerId && message.type !== 'YOUTH' && (
                    <Link to={`/players/${message.playerId}`}>{message.playerName}</Link>
                  )}
                  {message.amount !== null && <> · {formatMoney(message.amount)}</>}
                </div>
              )}
              {message.actionable && (
                <div className="inbox-actions">
                  <button
                    className="btn btn-sm btn-primary"
                    onClick={() => act(() => api.acceptMessage(message.id))}
                    disabled={busy}
                  >
                    {ACCEPT_LABELS[message.type] ?? 'Kabul et'}
                  </button>
                  <button
                    className="btn btn-sm"
                    onClick={() => act(() => api.rejectMessage(message.id))}
                    disabled={busy}
                  >
                    {REJECT_LABELS[message.type] ?? 'Reddet'}
                  </button>
                </div>
              )}
              {message.resolved && <div className="muted inbox-meta">Yanıtlandı</div>}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default InboxPanel
