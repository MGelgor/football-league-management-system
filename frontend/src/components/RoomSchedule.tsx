import { useState } from 'react'
import { api, errorMessage } from '../api/client'
import { notifyRoomChanged, type StoredRoom } from '../api/rooms'
import type { Room } from '../api/types'
import { roomStatusText } from '../roomTime'

// Tarayıcının bildiği saat dilimleri (öneri listesi)
const TIME_ZONES: string[] = (() => {
  try {
    return Intl.supportedValuesOf('timeZone')
  } catch {
    return ['Europe/Istanbul', 'Europe/Berlin', 'Europe/London', 'UTC']
  }
})()

interface RoomScheduleProps {
  room: Room
  stored: StoredRoom
  onChange: (room: Room) => void
}

/** Odanın takvimi: durum, başlat / hazırım / şimdi oynat, sahibin takvim ayarları. */
function RoomSchedule({ room, stored, onChange }: RoomScheduleProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [matchTime, setMatchTime] = useState(room.matchTime)
  const [timeZone, setTimeZone] = useState(room.timeZone)
  const [transferHours, setTransferHours] = useState(room.transferHours)
  const me = room.members.find((member) => member.id === room.you)
  const owner = me?.owner ?? false
  const readyPhase = room.status === 'LOBBY' || room.status === 'TRANSFER_WINDOW'

  async function run(action: () => Promise<Room>) {
    setBusy(true)
    setError(null)
    try {
      onChange(await action())
      notifyRoomChanged()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="section-gap">
      <h3>Takvim</h3>
      <p className="room-status">{roomStatusText(room)}</p>
      {error && <p className="alert alert-error">{error}</p>}
      <p className="muted legend">
        Maçlar her gün {room.matchTime}'da ({room.timeZone}) oynanır: önce 34 lig haftası, sonra kupa turları. Sezon
        bitince transfer penceresi {room.transferHours} saat açık kalır; herkes hazır derse erken kapanır.
      </p>

      <div className="page-actions">
        {readyPhase && (
          <button
            className={me?.ready ? 'btn' : 'btn btn-primary'}
            onClick={() => run(() => api.setReady(stored.code, stored.token, !me?.ready))}
            disabled={busy}
          >
            {me?.ready ? 'Hazır değilim' : room.status === 'LOBBY' ? 'Hazırım' : 'Transferlerim bitti'}
          </button>
        )}
        {owner && room.status === 'LOBBY' && (
          <button
            className="btn btn-primary"
            onClick={() => run(() => api.startRoom(stored.code, stored.token))}
            disabled={busy}
          >
            {busy ? 'Başlatılıyor…' : 'Ligi şimdi başlat'}
          </button>
        )}
        {owner && room.status !== 'LOBBY' && (
          <button className="btn" onClick={() => run(() => api.runRoomNow(stored.code, stored.token))} disabled={busy}>
            {busy
              ? 'Oynanıyor…'
              : room.status === 'SEASON'
                ? `${room.nextMatchDay ?? 'Sıradaki gün'}: şimdi oynat`
                : 'Pencereyi şimdi kapat'}
          </button>
        )}
      </div>

      {owner && (
        <details className="room-settings">
          <summary>Takvim ayarları</summary>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              run(() => api.updateRoomSettings(stored.code, stored.token, { matchTime, timeZone, transferHours }))
            }}
          >
            <div className="form-grid">
              <label className="field">
                Maç saati
                <input type="time" value={matchTime} onChange={(event) => setMatchTime(event.target.value)} required />
              </label>
              <label className="field">
                Saat dilimi
                <input
                  list="room-time-zones"
                  value={timeZone}
                  onChange={(event) => setTimeZone(event.target.value)}
                  required
                />
                <datalist id="room-time-zones">
                  {TIME_ZONES.map((zone) => (
                    <option key={zone} value={zone} />
                  ))}
                </datalist>
              </label>
              <label className="field">
                Transfer penceresi (saat)
                <input
                  type="number"
                  min={1}
                  max={72}
                  value={transferHours}
                  onChange={(event) => setTransferHours(Number(event.target.value))}
                  required
                />
              </label>
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" disabled={busy}>
                Kaydet
              </button>
            </div>
          </form>
        </details>
      )}
    </div>
  )
}

export default RoomSchedule
