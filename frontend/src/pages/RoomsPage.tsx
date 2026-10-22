import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import { appPath } from '../api/config'
import {
  activeRoom,
  findRoom,
  forgetRoom,
  inviteLink,
  lastNickname,
  saveRoom,
  setActiveRoom,
  storedRooms,
} from '../api/rooms'
import type { Room, RoomJoin } from '../api/types'
import RoomSchedule from '../components/RoomSchedule'

/** Odaya girince tüm sayfalar odanın verisini yüklesin diye sayfa yeniden açılır. */
function enter(code: string | null) {
  setActiveRoom(code)
  window.location.assign(appPath('/teams'))
}

function remember(joined: RoomJoin, nickname: string) {
  const me = joined.room.members.find((member) => member.id === joined.room.you)
  saveRoom({
    code: joined.room.code,
    name: joined.room.name,
    token: joined.token,
    nickname,
    owner: me?.owner ?? false,
  })
}

/** Oda kur / kodla katıl / katıldığın odalar; davet linki (/join/:code) da bu sayfayı açar. */
function RoomsPage() {
  const { code: inviteCode } = useParams()
  const current = activeRoom()
  const [room, setRoom] = useState<Room | null>(null)
  const [roomError, setRoomError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [roomName, setRoomName] = useState('')
  const [nickname, setNickname] = useState(lastNickname())
  const [joinCode, setJoinCode] = useState(inviteCode?.toUpperCase() ?? '')
  const invitedRoom = inviteCode ? findRoom(inviteCode) : undefined

  useEffect(() => {
    if (!current) {
      return
    }
    api
      .getRoom(current.code, current.token)
      .then(setRoom)
      .catch((e) => setRoomError(errorMessage(e)))
  }, [current])

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(errorMessage(e))
      setBusy(false)
    }
  }

  const handleCreate = () =>
    run(async () => {
      const joined = await api.createRoom(roomName.trim(), nickname.trim())
      remember(joined, nickname.trim())
      enter(joined.room.code)
    })

  const handleJoin = () =>
    run(async () => {
      const joined = await api.joinRoom(joinCode.trim(), nickname.trim())
      remember(joined, nickname.trim())
      enter(joined.room.code)
    })

  const handleLeave = () =>
    run(async () => {
      if (!current) {
        return
      }
      if (current.owner) {
        await api.deleteRoom(current.code, current.token)
      } else {
        await api.leaveRoom(current.code, current.token)
      }
      forgetRoom(current.code)
      enter(null)
    })

  async function copyInvite(code: string) {
    try {
      await navigator.clipboard.writeText(inviteLink(code))
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  // Oda silinmiş ya da üyelikten çıkarılmışsa token artık geçmez
  const lostAccess = roomError !== null && current !== null

  const currentCard = current && (
    <div className="card">
      <div className="page-header">
        <h2>{current.name}</h2>
        <span className="room-code">{current.code}</span>
      </div>
      {lostAccess ? (
        <>
          <p className="alert alert-error">{roomError}</p>
          <div className="page-actions">
            <button
              className="btn"
              onClick={() => {
                forgetRoom(current.code)
                enter(null)
              }}
            >
              Odayı listeden kaldır
            </button>
          </div>
        </>
      ) : (
        <>
          <p className="muted legend">
            {current.owner
              ? 'Odanın sahibisin: ligi başlatır, takvimi ayarlarsın. Maçları zamanlayıcı oynatır.'
              : 'Üyesin: maçlar oda takviminde kendiliğinden oynanır.'}
          </p>
          <div className="field">
            Davet linki
            <div className="inline-form">
              <input className="input-sm" readOnly value={inviteLink(current.code)} aria-label="Davet linki" />
              <button className="btn" onClick={() => copyInvite(current.code)}>
                {copied ? 'Kopyalandı' : 'Kopyala'}
              </button>
            </div>
          </div>
          {room && (
            <>
              <h3 className="section-gap">
                Oyuncular ({room.members.length}/{room.maxMembers})
              </h3>
              <ul className="room-members">
                {room.members.map((member) => (
                  <li key={member.id}>
                    <span className="strong">{member.nickname}</span>
                    {member.owner && <span className="badge badge-played">sahip</span>}
                    {member.id === room.you && <span className="muted">(sen)</span>}
                    {member.ready && room.status !== 'SEASON' && <span className="muted">✓ hazır</span>}
                  </li>
                ))}
              </ul>
              <RoomSchedule room={room} stored={current} onChange={setRoom} />
            </>
          )}
          <div className="page-actions section-gap">
            <button className="btn" onClick={() => enter(null)} disabled={busy}>
              Tek kişilik lige geç
            </button>
            {confirmDelete ? (
              <>
                <button className="btn btn-danger" onClick={handleLeave} disabled={busy}>
                  {current.owner ? 'Evet, odayı sil' : 'Evet, ayrıl'}
                </button>
                <button className="btn" onClick={() => setConfirmDelete(false)} disabled={busy}>
                  Vazgeç
                </button>
              </>
            ) : (
              <button className="btn btn-danger-outline" onClick={() => setConfirmDelete(true)} disabled={busy}>
                {current.owner ? 'Odayı sil' : 'Odadan ayrıl'}
              </button>
            )}
          </div>
          {confirmDelete && current.owner && (
            <p className="alert alert-warning section-gap">
              Odanın ligi, maçları ve tüm üyeleri kalıcı olarak silinir.
            </p>
          )}
        </>
      )}
    </div>
  )

  return (
    <section>
      <h1>Odalar</h1>
      {error && <p className="alert alert-error">{error}</p>}
      {!inviteCode && currentCard}

      {invitedRoom && invitedRoom.code !== current?.code && (
        <div className="card">
          <h2>{invitedRoom.name}</h2>
          <p className="muted legend">Bu odaya daha önce "{invitedRoom.nickname}" olarak katıldın.</p>
          <button className="btn btn-primary" onClick={() => enter(invitedRoom.code)}>
            Odaya gir
          </button>
        </div>
      )}

      {!invitedRoom && (
        <div className="room-forms">
          <form
            className="card"
            onSubmit={(event) => {
              event.preventDefault()
              handleJoin()
            }}
          >
            <h2>{inviteCode ? 'Davet edildin' : 'Koda katıl'}</h2>
            <div className="form-grid">
              <label className="field">
                Oda kodu
                <input
                  value={joinCode}
                  onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                  maxLength={6}
                  placeholder="ABC123"
                  required
                />
              </label>
              <label className="field">
                Takma ad
                <input value={nickname} onChange={(event) => setNickname(event.target.value)} maxLength={30} required />
              </label>
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" disabled={busy || joinCode.trim().length !== 6 || !nickname.trim()}>
                Katıl
              </button>
            </div>
          </form>

          {!inviteCode && (
            <form
              className="card"
              onSubmit={(event) => {
                event.preventDefault()
                handleCreate()
              }}
            >
              <h2>Oda kur</h2>
              <div className="form-grid">
                <label className="field">
                  Oda adı
                  <input
                    value={roomName}
                    onChange={(event) => setRoomName(event.target.value)}
                    maxLength={60}
                    placeholder="Cuma akşamı ligi"
                    required
                  />
                </label>
                <label className="field">
                  Takma ad
                  <input
                    value={nickname}
                    onChange={(event) => setNickname(event.target.value)}
                    maxLength={30}
                    required
                  />
                </label>
              </div>
              <p className="muted legend">Lig 18 takımla hazırlanır (4 büyükler + rastgele takımlar).</p>
              <div className="form-actions">
                <button className="btn btn-primary" disabled={busy || !roomName.trim() || !nickname.trim()}>
                  {busy ? 'Oda hazırlanıyor…' : 'Oda kur'}
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      {inviteCode && currentCard}

      {storedRooms().length > 0 && (
        <div className="card">
          <h2>Odalarım</h2>
          <div className="table-wrap">
            <table className="table">
              <tbody>
                {storedRooms().map((stored) => (
                  <tr key={stored.code}>
                    <td className="strong">{stored.name}</td>
                    <td className="muted">{stored.code}</td>
                    <td className="muted">
                      {stored.nickname}
                      {stored.owner && ' · sahip'}
                    </td>
                    <td className="row-actions">
                      {stored.code === current?.code ? (
                        <span className="badge badge-played">aktif</span>
                      ) : (
                        <button className="btn btn-sm" onClick={() => enter(stored.code)}>
                          Gir
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}

export default RoomsPage
