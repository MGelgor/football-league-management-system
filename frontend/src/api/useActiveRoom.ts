import { useEffect, useState } from 'react'
import { api, errorMessage } from './client'
import { activeRoom, onRoomChanged } from './rooms'
import type { Room } from './types'

const REFRESH_MS = 30_000

/** Aktif odanın güncel bilgisi (oda yoksa null); yarım dakikada bir ve oda değişince tazelenir. */
export function useActiveRoom(): { room: Room | null; error: string | null } {
  const stored = activeRoom()
  const [room, setRoom] = useState<Room | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!stored) {
      return
    }
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

  return { room, error }
}

/** Şu an (ms); geri sayımlar için yarım dakikada bir güncellenir. */
export function useNow(intervalMs = REFRESH_MS): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs)
    return () => window.clearInterval(timer)
  }, [intervalMs])
  return now
}
