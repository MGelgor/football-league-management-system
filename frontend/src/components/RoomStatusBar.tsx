import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import { onRoomChanged, type StoredRoom } from '../api/rooms'
import type { Room } from '../api/types'
import { roomStatusText } from '../roomTime'

const REFRESH_MS = 30_000

/** Aktif odanın takvimi: üst çubuğun altında, yarım dakikada bir tazelenir. */
function RoomStatusBar({ stored }: { stored: StoredRoom }) {
  const [room, setRoom] = useState<Room | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = () =>
      api
        .getRoom(stored.code, stored.token)
        .then((loaded) => {
          setRoom(loaded)
          setError(null)
        })
        .catch((e) => setError(errorMessage(e)))
    load()
    const timer = window.setInterval(load, REFRESH_MS)
    const unsubscribe = onRoomChanged(load)
    return () => {
      window.clearInterval(timer)
      unsubscribe()
    }
  }, [stored])

  if (!room && !error) {
    return null
  }
  return (
    <div className="room-bar">
      <div className="room-bar-inner">
        <span>{error ?? (room && roomStatusText(room))}</span>
        <Link to="/rooms">Oda →</Link>
      </div>
    </div>
  )
}

export default RoomStatusBar
