import { Link } from 'react-router'
import type { Position } from '../api/types'
import { POSITIONS } from '../labels'

export interface PitchPlayer {
  id: number
  name: string
  shirtNumber: number
  position: Position
  // Oyuncunun altındaki rozet: maçta reyting, takım sayfasında güç
  badge: string
  badgeLevel: 'high' | 'mid' | 'low'
  // Gol / kart simgeleri, ör. "⚽⚽🟨"
  icons?: string
  // Oyundan çıktığı dakika gibi kısa not, ör. "↓63'"
  note?: string
  highlight?: boolean
}

export interface PitchTeam {
  name: string
  players: PitchPlayer[]
}

interface PitchFormationProps {
  home: PitchTeam
  // Verilirse iki takım aynı sahada: ev sahibi alt yarı, deplasman üst yarı
  away?: PitchTeam
}

// Satırların sahadaki dikey konumu (% olarak, üstten). Tek takımda saha tamamen o takımın.
const SINGLE_ROWS: Record<Position, number> = { GOALKEEPER: 90, DEFENDER: 70, MIDFIELDER: 46, FORWARD: 21 }
const HOME_ROWS: Record<Position, number> = { GOALKEEPER: 93, DEFENDER: 80.5, MIDFIELDER: 67, FORWARD: 56 }
const AWAY_ROWS: Record<Position, number> = { GOALKEEPER: 7, DEFENDER: 19.5, MIDFIELDER: 33, FORWARD: 44 }

/** Kuşbakışı saha: oyuncular mevkilerine göre satırlara, satır içinde eşit aralıkla yerleşir. */
function PitchFormation({ home, away }: PitchFormationProps) {
  return (
    <div className={`pitch${away ? ' pitch-match' : ''}`}>
      <PitchLines />
      <TeamMarkers team={home} rows={away ? HOME_ROWS : SINGLE_ROWS} side="home" />
      {away && <TeamMarkers team={away} rows={AWAY_ROWS} side="away" />}
    </div>
  )
}

function TeamMarkers({ team, rows, side }: { team: PitchTeam; rows: Record<Position, number>; side: 'home' | 'away' }) {
  return (
    <>
      {POSITIONS.map((position) => {
        const row = team.players
          .filter((player) => player.position === position)
          .toSorted((a, b) => a.shirtNumber - b.shirtNumber)
        return row.map((player, index) => (
          <div
            key={player.id}
            className={`pitch-player pitch-${side}${player.highlight ? ' pitch-highlight' : ''}`}
            style={{ left: `${((index + 1) / (row.length + 1)) * 100}%`, top: `${rows[position]}%` }}
          >
            <span className="pitch-shirt">{player.shirtNumber}</span>
            <Link to={`/players/${player.id}`} className="pitch-name" title={player.name}>
              <span className="pitch-name-full">{player.name}</span>
              {/* Dar ekranda yalnızca soyadı (CSS ile seçilir) */}
              <span className="pitch-name-short">{player.name.split(' ').at(-1)}</span>
            </Link>
            <span className="pitch-meta">
              <span className={`rating rating-${player.badgeLevel}`}>{player.badge}</span>
              {player.icons && <span className="pitch-icons">{player.icons}</span>}
              {player.highlight && <span title="Maçın oyuncusu">⭐</span>}
            </span>
            {player.note && <span className="pitch-note">{player.note}</span>}
          </div>
        ))
      })}
    </>
  )
}

/** Saha çizgileri: 68 × 105 m ölçülerinde dikey saha. */
function PitchLines() {
  return (
    <svg className="pitch-lines" viewBox="0 0 68 105" preserveAspectRatio="none" aria-hidden>
      <rect x="1" y="1" width="66" height="103" />
      <line x1="1" y1="52.5" x2="67" y2="52.5" />
      <circle cx="34" cy="52.5" r="9.15" />
      <circle cx="34" cy="52.5" r="0.5" className="pitch-spot" />
      <rect x="13.85" y="1" width="40.3" height="16.5" />
      <rect x="24.85" y="1" width="18.3" height="5.5" />
      <rect x="13.85" y="87.5" width="40.3" height="16.5" />
      <rect x="24.85" y="98.5" width="18.3" height="5.5" />
      <circle cx="34" cy="12" r="0.5" className="pitch-spot" />
      <circle cx="34" cy="93" r="0.5" className="pitch-spot" />
    </svg>
  )
}

export default PitchFormation
