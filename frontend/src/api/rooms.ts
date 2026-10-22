import { appPath } from './config'

/**
 * Katılınan odalar ve aktif oda tarayıcıda (localStorage) saklanır. Hesap yok: odaya katılınca verilen token
 * yalnızca burada durur; silinirse odaya aynı kişi olarak dönülemez.
 */

export interface StoredRoom {
  code: string
  name: string
  token: string
  nickname: string
  owner: boolean
}

const ROOMS_KEY = 'fl.rooms'
const ACTIVE_KEY = 'fl.activeRoom'
const NICKNAME_KEY = 'fl.nickname'

function read(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function write(key: string, value: string | null) {
  try {
    if (value === null) {
      localStorage.removeItem(key)
    } else {
      localStorage.setItem(key, value)
    }
  } catch {
    // Gizli pencere vb.: oda yalnızca bu sayfa açıkken hatırlanır
  }
}

let rooms: StoredRoom[] = (() => {
  try {
    const parsed: unknown = JSON.parse(read(ROOMS_KEY) ?? '[]')
    return Array.isArray(parsed) ? (parsed as StoredRoom[]) : []
  } catch {
    return []
  }
})()
let activeCode: string | null = read(ACTIVE_KEY)

export function storedRooms(): StoredRoom[] {
  return rooms
}

export function findRoom(code: string): StoredRoom | undefined {
  return rooms.find((room) => room.code === code.toUpperCase())
}

export function saveRoom(room: StoredRoom) {
  rooms = [room, ...rooms.filter((existing) => existing.code !== room.code)]
  write(ROOMS_KEY, JSON.stringify(rooms))
  write(NICKNAME_KEY, room.nickname)
}

export function forgetRoom(code: string) {
  rooms = rooms.filter((room) => room.code !== code)
  write(ROOMS_KEY, JSON.stringify(rooms))
  if (activeCode === code) {
    setActiveRoom(null)
  }
}

export function activeRoom(): StoredRoom | null {
  return (activeCode && findRoom(activeCode)) || null
}

/** null: tek kişilik lig (oda yok). */
export function setActiveRoom(code: string | null) {
  activeCode = code
  write(ACTIVE_KEY, code)
}

/** Odada maçları zamanlayıcı oynatır; maç oynatan düğmeler gizlenir. */
export function inRoom(): boolean {
  return activeRoom() !== null
}

export function lastNickname(): string {
  return read(NICKNAME_KEY) ?? ''
}

/** Aktif odanın başlıkları; her API isteğine eklenir. */
export function roomHeaders(): Record<string, string> {
  const room = activeRoom()
  return room ? { 'X-Room-Code': room.code, 'X-Room-Token': room.token } : {}
}

/** Başlık gönderemeyen istekler (EventSource, indirme linkleri) için oda sorgu parametreleri. */
export function withRoom(path: string): string {
  const room = activeRoom()
  if (!room) {
    return path
  }
  const separator = path.includes('?') ? '&' : '?'
  return `${path}${separator}room=${encodeURIComponent(room.code)}&token=${encodeURIComponent(room.token)}`
}

const ROOM_CHANGED = 'fl:room-changed'

/** Oda durumu değişti (başlatıldı, hafta oynandı…): üst şerit hemen tazelenir. */
export function notifyRoomChanged() {
  window.dispatchEvent(new Event(ROOM_CHANGED))
}

export function onRoomChanged(listener: () => void): () => void {
  window.addEventListener(ROOM_CHANGED, listener)
  return () => window.removeEventListener(ROOM_CHANGED, listener)
}

export function inviteLink(code: string): string {
  return `${window.location.origin}${appPath(`/join/${code}`)}`
}
