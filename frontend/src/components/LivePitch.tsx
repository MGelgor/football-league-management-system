import type { LiveItem, LivePitchPlayer, Position } from '../api/types'
import { EVENT_ICONS } from '../labels'

// Ev sahibi soldan sağa hücum eder; sütunların yatay konumu (% soldan). Deplasman aynalanır.
const HOME_COLUMNS: Record<Position, number> = { GOALKEEPER: 4.5, DEFENDER: 17, MIDFIELDER: 30.5, FORWARD: 43 }
// Olay balonundaki kısa açıklama
const EVENT_LABELS: Record<string, string> = {
  GOAL: 'GOL!',
  OWN_GOAL: 'Kendi kalesine',
  YELLOW_CARD: 'Sarı kart',
  RED_CARD: 'Kırmızı kart',
  INJURY: 'Sakatlandı',
  PENALTY_MISSED: 'Penaltı kaçtı',
  VAR_DISALLOWED: 'Gol iptal (VAR)',
  SUBSTITUTION: 'Oyuna girdi',
}
const BUBBLE_ICONS: Record<string, string> = { ...EVENT_ICONS, SUBSTITUTION: '🔄' }

/** Oyuncunun minute dakikasına kadar biriken maç içi yorgunluğu. */
function fatigueAt(player: LivePitchPlayer, minute: number) {
  const until = Math.min(minute, player.minuteOff ?? minute)
  return (player.fatigueRate * Math.max(0, until - player.minuteOn)) / 90
}

function onPitchAt(player: LivePitchPlayer, minute: number) {
  return player.minuteOn <= minute && (player.minuteOff === null || minute < player.minuteOff)
}

interface LivePitchProps {
  homeName: string
  awayName: string
  players: LivePitchPlayer[]
  events: LiveItem[]
  minute: number
  // Olay balonlarının ve oyundan çıkanların ekranda kalacağı dakika sayısı (0: gösterme)
  flashMinutes: number
  userHome: boolean
  // Devre arası: kullanıcının oyuncuları yedek bırakma hedefi olur
  onDropOnPlayer?: (outId: number, inId: number) => void
  onPlayerClick?: (playerId: number) => void
  pendingInIds?: Set<number>
  selectedId?: number | null
}

/**
 * Yatay saha: ev sahibi solda, deplasman sağda, dizilişe göre sütunlar. Oyuncunun altında o anki güç ve kondisyon
 * çubuğu (maç içi yorgunluk), üstünde maçtaki olay simgeleri; yeni olay olan oyuncunun üstünde balon çıkar.
 */
function LivePitch({
  homeName,
  awayName,
  players,
  events,
  minute,
  flashMinutes,
  userHome,
  onDropOnPlayer,
  onPlayerClick,
  pendingInIds,
  selectedId,
}: LivePitchProps) {
  const visibleEvents = events.filter((item) => item.minute <= minute)
  const byId = new Map(players.map((player) => [player.id, player]))

  // Değişiklikle giren oyuncu çıkanın sırasını alır (aynı mevkideyse)
  function slotKey(player: LivePitchPlayer, depth = 0): number {
    const replaced = player.replacedPlayerId !== null ? byId.get(player.replacedPlayerId) : undefined
    if (replaced && replaced.position === player.position && depth < 5) {
      return slotKey(replaced, depth + 1)
    }
    return player.shirtNumber
  }

  const shown = players.filter(
    (player) =>
      onPitchAt(player, minute) ||
      (flashMinutes > 0 &&
        player.minuteOff !== null &&
        minute >= player.minuteOff &&
        minute < player.minuteOff + flashMinutes),
  )

  return (
    <div className="hpitch-wrap">
      <div className="pitch-legend">
        <span>
          <span className="pitch-dot pitch-home" /> {homeName}
          {userHome && ' (sen)'} →
        </span>
        <span>
          ← {awayName}
          {!userHome && ' (sen)'} <span className="pitch-dot pitch-away" />
        </span>
      </div>
      <div className="hpitch">
        <HorizontalPitchLines />
        {(['home', 'away'] as const).map((side) => {
          const sidePlayers = shown.filter((player) => player.home === (side === 'home'))
          return (['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'] as Position[]).map((position) => {
            const column = sidePlayers
              .filter((player) => player.position === position)
              .toSorted((a, b) => slotKey(a) - slotKey(b) || a.minuteOn - b.minuteOn)
            const x = side === 'home' ? HOME_COLUMNS[position] : 100 - HOME_COLUMNS[position]
            return column.map((player, index) => {
              const y = 8 + ((index + 1) / (column.length + 1)) * 84
              const mine = player.home === userHome
              const leaving = !onPitchAt(player, minute)
              const fatigue = fatigueAt(player, minute)
              const current = Math.max(1, player.effectiveStrength - fatigue)
              const stamina = Math.max(0, 100 - fatigue * 4)
              const playerEvents = visibleEvents.filter(
                (item) => item.playerId === player.id && item.type !== 'SUBSTITUTION',
              )
              const icons = playerEvents.map((item) => BUBBLE_ICONS[item.type] ?? '').join('')
              const flash =
                flashMinutes > 0
                  ? visibleEvents.findLast((item) => item.playerId === player.id && item.minute > minute - flashMinutes)
                  : undefined
              const droppable = mine && !leaving && onDropOnPlayer !== undefined
              return (
                <div
                  key={player.id}
                  className={`hpitch-player pitch-${side}${leaving ? ' is-leaving' : ''}${flash ? ' is-flash' : ''}${
                    droppable ? ' is-droppable' : ''
                  }${pendingInIds?.has(player.id) ? ' is-pending' : ''}${selectedId === player.id ? ' is-selected' : ''}`}
                  style={{ left: `${x}%`, top: `${y}%` }}
                  title={`${player.name} · güç ${current.toFixed(1)}${fatigue >= 0.5 ? ` (yorgunluk −${fatigue.toFixed(1)})` : ''}`}
                  onDragOver={droppable ? (event) => event.preventDefault() : undefined}
                  onDrop={
                    droppable
                      ? (event) => {
                          event.preventDefault()
                          const inId = Number(event.dataTransfer.getData('text/plain'))
                          if (inId) {
                            onDropOnPlayer(player.id, inId)
                          }
                        }
                      : undefined
                  }
                  onClick={droppable && onPlayerClick ? () => onPlayerClick(player.id) : undefined}
                >
                  {flash && (
                    <span key={`${flash.minute}-${flash.type}`} className={`hpitch-bubble bubble-${flash.type}`}>
                      {BUBBLE_ICONS[flash.type]} {EVENT_LABELS[flash.type] ?? ''}
                    </span>
                  )}
                  <span className="pitch-shirt">{player.shirtNumber}</span>
                  <span className="hpitch-name">{player.name.split(' ').at(-1)}</span>
                  <span className="hpitch-meta">
                    <span className="hpitch-strength">{Math.round(current)}</span>
                    {icons && <span className="hpitch-icons">{icons}</span>}
                  </span>
                  <span className="hpitch-stamina" aria-hidden>
                    <span
                      className={stamina >= 80 ? 'fresh' : stamina >= 65 ? 'tired' : 'exhausted'}
                      style={{ width: `${stamina}%` }}
                    />
                  </span>
                </div>
              )
            })
          })
        })}
      </div>
    </div>
  )
}

/** Saha çizgileri: 105 × 68 m yatay saha. */
function HorizontalPitchLines() {
  return (
    <svg className="pitch-lines" viewBox="0 0 105 68" preserveAspectRatio="none" aria-hidden>
      <rect x="1" y="1" width="103" height="66" />
      <line x1="52.5" y1="1" x2="52.5" y2="67" />
      <circle cx="52.5" cy="34" r="9.15" />
      <circle cx="52.5" cy="34" r="0.5" className="pitch-spot" />
      <rect x="1" y="13.85" width="16.5" height="40.3" />
      <rect x="1" y="24.85" width="5.5" height="18.3" />
      <rect x="87.5" y="13.85" width="16.5" height="40.3" />
      <rect x="98.5" y="24.85" width="5.5" height="18.3" />
      <circle cx="12" cy="34" r="0.5" className="pitch-spot" />
      <circle cx="93" cy="34" r="0.5" className="pitch-spot" />
    </svg>
  )
}

export default LivePitch
