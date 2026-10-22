import type { Room } from './api/types'

const DAY_MS = 24 * 60 * 60 * 1000

/** Olay zamanı tarayıcının saatiyle: "bugün 21:00", "yarın 21:00", "8 Nis Çar 21:00". */
export function formatEventTime(iso: string, now = new Date()): string {
  const date = new Date(iso)
  const time = date.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const dayDiff = Math.floor((date.getTime() - startOfToday) / DAY_MS)
  if (dayDiff === 0) {
    return `bugün ${time}`
  }
  if (dayDiff === 1) {
    return `yarın ${time}`
  }
  return `${date.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short', weekday: 'short' })} ${time}`
}

/** "2 sa 13 dk sonra", "birazdan" */
export function formatCountdown(iso: string, now = new Date()): string {
  const minutes = Math.round((new Date(iso).getTime() - now.getTime()) / 60000)
  if (minutes <= 0) {
    return 'birazdan'
  }
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  const rest = minutes % 60
  const parts = [days && `${days} gün`, hours && `${hours} sa`, !days && rest && `${rest} dk`].filter(Boolean)
  return `${parts.join(' ')} sonra`
}

/** Kadronun / taktiğin kilitlendiği an: sezonda maç saatinden 5 dk önce. */
export function lineupLockTime(room: Room): string | null {
  if (room.status !== 'SEASON' || !room.nextEventAt) {
    return null
  }
  return new Date(new Date(room.nextEventAt).getTime() - 5 * 60_000).toISOString()
}

/** Odanın tek satırlık durumu (üst şerit ve Odalar sayfası). */
export function roomStatusText(room: Room, now = new Date()): string {
  const when = room.nextEventAt
    ? `${formatEventTime(room.nextEventAt, now)} (${formatCountdown(room.nextEventAt, now)})`
    : ''
  switch (room.status) {
    case 'LOBBY':
      return 'Lobi: lig, oda sahibi başlatınca ya da herkes hazır olunca başlar'
    case 'SEASON':
      return `Sezon ${room.seasonNumber ?? 1} · Sıradaki: ${room.nextMatchDay ?? '—'}, ${when}`
    case 'TRANSFER_WINDOW': {
      const ready = room.members.filter((member) => member.ready).length
      return `Transfer penceresi açık · ${when} kapanır (${ready}/${room.members.length} hazır)`
    }
  }
}
