import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import type { MatchDetail, MatchEvent } from '../api/types'

// Bir maçın 90 dakikası ekranda bu kadar sürer
const LIVE_DURATION_MS = 10_000
const MATCH_MINUTES = 90
const TICK_MS = 100
// Gol bu kadar dakika boyunca kartta "GOL!" olarak vurgulanır (~0,9 sn)
const GOAL_FLASH_MINUTES = 8

interface LiveRoundProps {
  title: string
  matches: MatchDetail[]
  // Süre dolduğunda (ya da atlanınca) bir kez çağrılır
  onFinish: () => void
  onClose: () => void
}

/**
 * Önceden simüle edilmiş maçları 90 dakika = 10 saniye olacak şekilde canlı gibi oynatır:
 * skor, gollerin dakikası geldikçe artar.
 */
function LiveRound({ title, matches, onFinish, onClose }: LiveRoundProps) {
  const [elapsed, setElapsed] = useState(0)
  // Geçen süre başlangıç anından ölçülür; adımları toplamak zamanla kayardı
  const [startedAt] = useState(() => Date.now())
  const reported = useRef(false)
  const finished = elapsed >= LIVE_DURATION_MS
  const minute = Math.min(MATCH_MINUTES, Math.floor((elapsed / LIVE_DURATION_MS) * MATCH_MINUTES))

  useEffect(() => {
    if (finished) {
      if (!reported.current) {
        reported.current = true
        onFinish()
      }
      return
    }
    const timer = setTimeout(() => setElapsed(Math.min(LIVE_DURATION_MS, Date.now() - startedAt)), TICK_MS)
    return () => clearTimeout(timer)
  }, [elapsed, finished, onFinish, startedAt])

  return (
    <div className={`card live-round${finished ? '' : ' is-live'}`}>
      <div className="card-header">
        <h2>
          {title}{' '}
          {finished ? (
            <span className="muted">· Maçlar bitti</span>
          ) : (
            <span className="live-badge">
              <span className="live-dot" aria-hidden /> CANLI {minute}'
            </span>
          )}
        </h2>
        {finished ? (
          <button className="btn btn-sm" onClick={onClose}>
            Kapat
          </button>
        ) : (
          <button className="btn btn-sm" onClick={() => setElapsed(LIVE_DURATION_MS)}>
            Atla
          </button>
        )}
      </div>
      <div className="live-matches">
        {matches.map((match) => (
          <LiveMatch key={match.id} match={match} minute={minute} finished={finished} />
        ))}
      </div>
    </div>
  )
}

function LiveMatch({ match, minute, finished }: { match: MatchDetail; minute: number; finished: boolean }) {
  const goals = match.events.filter((event) => event.type === 'GOAL' && event.minute <= minute)
  const homeGoals = goals.filter((goal) => goal.home).length
  const awayGoals = goals.length - homeGoals
  const latest = goals.at(-1)
  const flashing = !finished && latest !== undefined && minute - latest.minute < GOAL_FLASH_MINUTES
  const hasPenalties = finished && match.home.penalties !== null && match.away.penalties !== null

  return (
    <div className={`live-match${flashing ? ' goal-flash' : ''}`}>
      <div className="live-match-status">
        {finished ? (
          <span className="muted">MS{hasPenalties && ` · Penaltılar ${match.home.penalties} - ${match.away.penalties}`}</span>
        ) : (
          <span className="live-badge">
            <span className="live-dot" aria-hidden /> {minute}'
          </span>
        )}
        {flashing && <span className="goal-banner">GOL!</span>}
      </div>
      <div className="live-scoreline">
        <span className="live-team live-home">{match.home.teamName}</span>
        <span className="live-score" aria-live="polite">
          {homeGoals} - {awayGoals}
        </span>
        <span className="live-team">{match.away.teamName}</span>
      </div>
      {goals.length > 0 && (
        <ul className="live-goals">
          {goals.map((goal, index) => (
            <LiveGoal key={index} goal={goal} />
          ))}
        </ul>
      )}
      {finished && (
        <Link to={`/matches/${match.id}`} className="live-detail">
          Maç detayı →
        </Link>
      )}
    </div>
  )
}

function LiveGoal({ goal }: { goal: MatchEvent }) {
  const text = (
    <>
      ⚽ {goal.minute}' {goal.playerName}
    </>
  )
  return (
    <li className={goal.home ? 'live-goal-home' : 'live-goal-away'}>
      <span>{goal.home && text}</span>
      <span>{!goal.home && text}</span>
    </li>
  )
}

export default LiveRound
