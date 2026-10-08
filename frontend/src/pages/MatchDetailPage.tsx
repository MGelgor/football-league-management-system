import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { LineupEntry, MatchDetail, MatchEvent, MatchSide, MatchStats } from '../api/types'
import PitchFormation, { type PitchTeam } from '../components/PitchFormation'
import ProbabilityBar from '../components/ProbabilityBar'
import TeamLogo from '../components/TeamLogo'
import {
  EVENT_ICONS,
  formationLabel,
  goalSuffix,
  PLAY_STYLE_LABELS,
  POSITION_LABELS,
  POSITION_SHORT,
  POSITIONS,
  roundLabel,
  scoringSide,
} from '../labels'

const EVENT_NOTES: Partial<Record<MatchEvent['type'], string>> = {
  INJURY: 'sakatlandı',
  OWN_GOAL: 'kendi kalesine',
  PENALTY_MISSED: 'penaltı kaçırdı',
  VAR_DISALLOWED: 'gol VAR ile iptal',
}

/** Maç akışında olaylar ve (kadrodan türetilen) oyuncu değişiklikleri birlikte gösterilir. */
type TimelineEntry =
  | { kind: 'event'; minute: number; home: boolean; event: MatchEvent }
  | { kind: 'substitution'; minute: number; home: boolean; incoming: LineupEntry }

function buildTimeline(match: MatchDetail): TimelineEntry[] {
  const substitutions = (side: MatchSide, home: boolean): TimelineEntry[] =>
    side.lineup
      .filter((entry) => !entry.starter)
      .map((entry) => ({ kind: 'substitution', minute: entry.minuteOn, home, incoming: entry }))
  const entries: TimelineEntry[] = [
    ...match.events.map((event): TimelineEntry => ({ kind: 'event', minute: event.minute, home: event.home, event })),
    ...substitutions(match.home, true),
    ...substitutions(match.away, false),
  ]
  // Aynı dakikada önce olay (ör. sakatlık), sonra değişiklik; aynı türdekiler backend'deki sırasını korur
  const kindOrder = (entry: TimelineEntry) => (entry.kind === 'event' ? 0 : 1)
  return entries.toSorted((a, b) => a.minute - b.minute || kindOrder(a) - kindOrder(b))
}

type NumericStat = Exclude<keyof MatchStats, 'formation' | 'playStyle'>

const STAT_ROWS: { key: NumericStat; label: string; suffix?: string }[] = [
  { key: 'possession', label: 'Topla oynama', suffix: '%' },
  { key: 'shots', label: 'Toplam şut' },
  { key: 'shotsOnTarget', label: 'İsabetli şut' },
  { key: 'saves', label: 'Kurtarış' },
  { key: 'corners', label: 'Korner' },
  { key: 'offsides', label: 'Ofsayt' },
  { key: 'fouls', label: 'Faul' },
  { key: 'yellowCards', label: 'Sarı kart' },
  { key: 'redCards', label: 'Kırmızı kart' },
]

function MatchDetailPage() {
  const matchId = Number(useParams().matchId)
  const [match, setMatch] = useState<MatchDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getMatch(matchId)
      .then(setMatch)
      .catch((e) => setError(errorMessage(e)))
  }, [matchId])

  return (
    <section>
      <p>
        <Link to="/fixture">← Fikstür</Link>
      </p>
      {error && <p className="alert alert-error">{error}</p>}
      {match && <MatchDetailView match={match} />}
    </section>
  )
}

function MatchDetailView({ match }: { match: MatchDetail }) {
  const { home, away } = match
  const homeTotal = (home.score ?? 0) * 100 + (home.penalties ?? 0)
  const awayTotal = (away.score ?? 0) * 100 + (away.penalties ?? 0)
  const homeWon = match.played && homeTotal > awayTotal
  const awayWon = match.played && awayTotal > homeTotal
  const playerOfTheMatch = [...home.lineup, ...away.lineup].find((entry) => entry.playerOfTheMatch)
  const timeline = buildTimeline(match)

  return (
    <>
      <div className="card scoreboard">
        <div className="muted scoreboard-meta">
          Sezon {match.seasonNumber} · {roundLabel(match.competition, match.weekNumber, match.cupRound)}
          {match.referee && (
            <>
              {' '}
              · Hakem:{' '}
              <Link to="/referees" title={`Sertlik ${match.referee.strictness}/10`}>
                {match.referee.name}
              </Link>
            </>
          )}
        </div>
        <div className="scoreboard-main">
          <Link to={`/teams/${home.teamId}`} className={`scoreboard-team${homeWon ? ' winner' : ''}`}>
            <TeamLogo name={home.teamName} logoUrl={home.logoUrl} large />
            <span>{home.teamName}</span>
          </Link>
          <div>
            <div className="scoreboard-score">{match.played ? `${home.score} - ${away.score}` : 'vs'}</div>
            {home.penalties !== null && away.penalties !== null && (
              <div className="muted penalties">
                Penaltılar {home.penalties} - {away.penalties}
              </div>
            )}
          </div>
          <Link to={`/teams/${away.teamId}`} className={`scoreboard-team${awayWon ? ' winner' : ''}`}>
            <TeamLogo name={away.teamName} logoUrl={away.logoUrl} large />
            <span>{away.teamName}</span>
          </Link>
        </div>
        <ScorerSummary events={match.events} />
        {playerOfTheMatch && (
          <div className="motm">
            ⭐ Maçın oyuncusu:{' '}
            <Link to={`/players/${playerOfTheMatch.playerId}`} className="strong">
              {playerOfTheMatch.playerName}
            </Link>{' '}
            <RatingBadge rating={playerOfTheMatch.rating} />
          </div>
        )}
        <div className="scoreboard-prob">
          <div className="muted legend">{match.played ? 'Maç öncesi olasılıklar' : 'Kazanma olasılıkları'}</div>
          <ProbabilityBar home={match.homeWinProbability} draw={match.drawProbability} away={match.awayWinProbability} />
        </div>
      </div>

      {!match.played && <p className="alert alert-info">Bu maç henüz oynanmadı.</p>}

      {match.played && home.stats && away.stats && (
        <div className="detail-grid">
          <div className="card">
            <h2>Maç akışı</h2>
            {timeline.length === 0 ? (
              <p className="muted">Gol, kart veya değişiklik yok.</p>
            ) : (
              <ol className="timeline">
                {timeline.map((entry, index) => (
                  <TimelineItem key={index} entry={entry} />
                ))}
              </ol>
            )}
          </div>

          <div className="card">
            <h2>İstatistikler</h2>
            <table className="stats-table">
              <thead>
                <tr>
                  <th className="stats-home">{home.teamName}</th>
                  <th />
                  <th className="stats-away">{away.teamName}</th>
                </tr>
              </thead>
              <tbody>
                {STAT_ROWS.map((row) => (
                  <StatRow
                    key={row.key}
                    label={row.label}
                    suffix={row.suffix}
                    home={home.stats![row.key]}
                    away={away.stats![row.key]}
                  />
                ))}
              </tbody>
            </table>

            <h2 className="section-gap">Gollerin mevkilere dağılımı</h2>
            <GoalsByPosition events={match.events} homeName={home.teamName} awayName={away.teamName} />
          </div>
        </div>
      )}

      {match.played && home.lineup.length > 0 && away.lineup.length > 0 && (
        <div className="card">
          <h2>Diziliş</h2>
          <div className="pitch-legend">
            <span>
              <span className="pitch-dot pitch-away" /> {away.teamName}{' '}
              <span className="muted">
                {formationLabel(away.lineup.filter((e) => e.starter))}
                {away.stats?.playStyle && ` · ${PLAY_STYLE_LABELS[away.stats.playStyle]}`}
              </span>
            </span>
            <span>
              <span className="pitch-dot pitch-home" /> {home.teamName}{' '}
              <span className="muted">
                {formationLabel(home.lineup.filter((e) => e.starter))}
                {home.stats?.playStyle && ` · ${PLAY_STYLE_LABELS[home.stats.playStyle]}`}
              </span>
            </span>
          </div>
          <PitchFormation home={toPitchTeam(home, match.events)} away={toPitchTeam(away, match.events)} />
          <p className="legend muted">İlk 11 · rozet: maç reytingi · ↓ oyundan çıktığı dakika</p>
        </div>
      )}

      {match.played && (home.lineup.length > 0 || away.lineup.length > 0) && (
        <div className="card">
          <h2>Kadrolar ve reytingler</h2>
          <div className="lineups">
            <Lineup side={home} />
            <Lineup side={away} />
          </div>
          <p className="legend muted">
            Reyting 3.0–10.0: gol, asist, sonuç, gol yememe, kurtarış ve kartlara göre. ↓ oyundan çıktığı, ↑ oyuna
            girdiği dakika.
          </p>
        </div>
      )}
    </>
  )
}

function ScorerSummary({ events }: { events: MatchEvent[] }) {
  const goals = events.filter((event) => scoringSide(event) !== null)
  if (goals.length === 0) {
    return null
  }
  const line = (home: boolean) =>
    goals
      .filter((goal) => scoringSide(goal) === (home ? 'home' : 'away'))
      .map((goal) => `${goal.playerName}${goalSuffix(goal)} ${goal.minute}'`)
      .join(', ')
  return (
    <div className="scorers">
      <span className="scorers-home">{line(true)}</span>
      <span aria-hidden>⚽</span>
      <span className="scorers-away">{line(false)}</span>
    </div>
  )
}

function TimelineItem({ entry }: { entry: TimelineEntry }) {
  const description =
    entry.kind === 'event' ? (
      <span className="timeline-text">
        <span aria-label={entry.event.type}>{EVENT_ICONS[entry.event.type]}</span>{' '}
        <Link to={`/players/${entry.event.playerId}`} className="strong team-link">
          {entry.event.playerName}
        </Link>{' '}
        <span className="muted">
          #{entry.event.shirtNumber} {POSITION_SHORT[entry.event.position]}
        </span>
        {entry.event.assistName && <span className="muted"> · asist: {entry.event.assistName}</span>}
        {entry.event.type === 'GOAL' && entry.event.penalty && <span className="muted"> · penaltı</span>}
        {EVENT_NOTES[entry.event.type] && <span className="muted"> · {EVENT_NOTES[entry.event.type]}</span>}
        <span className="timeline-commentary">{entry.event.commentary}</span>
      </span>
    ) : (
      <span className="timeline-text">
        <span aria-label="Oyuncu değişikliği">🔄</span>{' '}
        <span className="sub-in">↑ {entry.incoming.playerName}</span>{' '}
        <span className="sub-out">↓ {entry.incoming.replacedPlayerName}</span>
      </span>
    )
  return (
    <li className={`timeline-item ${entry.home ? 'is-home' : 'is-away'}`}>
      <span className="timeline-side">{entry.home && description}</span>
      <span className="timeline-minute">{entry.minute}'</span>
      <span className="timeline-side">{!entry.home && description}</span>
    </li>
  )
}

function Lineup({ side }: { side: MatchSide }) {
  const starters = side.lineup.filter((entry) => entry.starter)
  const substitutes = side.lineup.filter((entry) => !entry.starter)
  return (
    <div>
      <h3>{side.teamName}</h3>
      <ul className="lineup">
        {starters.map((entry) => (
          <LineupRow key={entry.playerId} entry={entry} />
        ))}
      </ul>
      {substitutes.length > 0 && (
        <>
          <div className="muted lineup-subtitle">Oyuna girenler</div>
          <ul className="lineup">
            {substitutes.map((entry) => (
              <LineupRow key={entry.playerId} entry={entry} />
            ))}
          </ul>
        </>
      )}
    </div>
  )
}

function LineupRow({ entry }: { entry: LineupEntry }) {
  return (
    <li className={entry.playerOfTheMatch ? 'lineup-motm' : undefined}>
      <span className="lineup-number">{entry.shirtNumber}</span>
      <Link to={`/players/${entry.playerId}`} className="team-link">
        {entry.playerName}
      </Link>
      <span className="muted lineup-pos">{POSITION_SHORT[entry.position]}</span>
      {!entry.starter && <span className="sub-in">↑{entry.minuteOn}'</span>}
      {entry.minuteOff < 90 && <span className="sub-out">↓{entry.minuteOff}'</span>}
      {entry.playerOfTheMatch && <span title="Maçın oyuncusu">⭐</span>}
      <RatingBadge rating={entry.rating} />
    </li>
  )
}

function ratingLevel(rating: number) {
  return rating >= 7.5 ? 'high' : rating < 6 ? 'low' : 'mid'
}

/** Reyting rozeti: 7.5+ yeşil, 6.0 altı kırmızı. */
function RatingBadge({ rating }: { rating: number }) {
  return <span className={`rating rating-${ratingLevel(rating)}`}>{rating.toFixed(1)}</span>
}

/** Sahadaki dizilişte takımın ilk 11'i: reyting, bu maçtaki gol / kart simgeleri, çıkış dakikası. */
function toPitchTeam(side: MatchSide, events: MatchEvent[]): PitchTeam {
  // Türe göre gruplu: önce goller (3'ten fazlaysa "⚽×4"), sonra kartlar
  const iconsFor = (playerId: number) => {
    const count = (type: MatchEvent['type']) =>
      events.filter((event) => event.playerId === playerId && event.type === type).length
    const goals = count('GOAL')
    const goalIcons = goals > 3 ? `${EVENT_ICONS.GOAL}×${goals}` : EVENT_ICONS.GOAL.repeat(goals)
    return goalIcons + EVENT_ICONS.YELLOW_CARD.repeat(count('YELLOW_CARD')) + EVENT_ICONS.RED_CARD.repeat(count('RED_CARD'))
  }
  return {
    name: side.teamName,
    players: side.lineup
      .filter((entry) => entry.starter)
      .map((entry) => ({
        id: entry.playerId,
        name: entry.playerName,
        shirtNumber: entry.shirtNumber,
        position: entry.position,
        badge: entry.rating.toFixed(1),
        badgeLevel: ratingLevel(entry.rating),
        icons: iconsFor(entry.playerId),
        note: entry.minuteOff < 90 ? `↓${entry.minuteOff}'` : undefined,
        highlight: entry.playerOfTheMatch,
      })),
  }
}

function StatRow({ label, home, away, suffix = '' }: { label: string; home: number; away: number; suffix?: string }) {
  const total = home + away
  const homeShare = total === 0 ? 50 : (home / total) * 100
  return (
    <tr>
      <td className={`stats-home${home > away ? ' strong' : ''}`}>
        {home}
        {suffix}
      </td>
      <td className="stats-label">
        {label}
        <div className={`stat-bar${total === 0 ? ' stat-bar-empty' : ''}`} aria-hidden>
          <span className="stat-bar-home" style={{ width: `${homeShare}%` }} />
          <span className="stat-bar-away" style={{ width: `${100 - homeShare}%` }} />
        </div>
      </td>
      <td className={`stats-away${away > home ? ' strong' : ''}`}>
        {away}
        {suffix}
      </td>
    </tr>
  )
}

function GoalsByPosition({ events, homeName, awayName }: { events: MatchEvent[]; homeName: string; awayName: string }) {
  const goals = events.filter((event) => event.type === 'GOAL')
  if (goals.length === 0) {
    return <p className="muted">Oyuncu golü yok.</p>
  }
  const count = (home: boolean, position: string) =>
    goals.filter((goal) => goal.home === home && goal.position === position).length

  return (
    <table className="table compact">
      <thead>
        <tr>
          <th>Mevki</th>
          <th className="num">{homeName}</th>
          <th className="num">{awayName}</th>
        </tr>
      </thead>
      <tbody>
        {POSITIONS.filter((position) => position !== 'GOALKEEPER').map((position) => (
          <tr key={position}>
            <td>{POSITION_LABELS[position]}</td>
            <td className="num">{count(true, position)}</td>
            <td className="num">{count(false, position)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default MatchDetailPage
