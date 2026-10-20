import { useEffect, useMemo, useState } from 'react'
import type { MatchDetail } from '../api/types'
import { EVENT_ICONS, scoringSide } from '../labels'
import { scriptFromDetail } from '../match2d/script'
import MatchCanvas from './MatchCanvas'

const SPEEDS = [1, 3, 10]
// 1x hızda olaysız bir dakika; olaylı dakikada sayaç sahne bitene kadar bekler
const MS_PER_MINUTE_1X = 333
const FEED_LINES = 5

/** Oynanmış bir maçı baştan, sahada animasyonla izletir: hız, duraklat, baştan, sona atla. */
function MatchReplay({ match, onClose }: { match: MatchDetail; onClose: () => void }) {
  const script = useMemo(() => scriptFromDetail(match), [match])
  const [minute, setMinute] = useState(0)
  // Sahnesi tamamlanmış son dakika: skor ve anlatım buna göre açılır (gol animasyondan önce görünmesin)
  const [shown, setShown] = useState(0)
  const [speed, setSpeed] = useState(1)
  const [paused, setPaused] = useState(false)
  const finished = minute >= 90 && shown >= 90

  useEffect(() => {
    if (paused || minute >= 90 || shown < minute) {
      return
    }
    const timer = setTimeout(() => setMinute((current) => current + 1), MS_PER_MINUTE_1X / speed)
    return () => clearTimeout(timer)
  }, [paused, minute, shown, speed])

  const revealed = match.events.filter((event) => event.minute <= shown)
  const homeGoals = revealed.filter((event) => scoringSide(event) === 'home').length
  const awayGoals = revealed.filter((event) => scoringSide(event) === 'away').length
  const feed = revealed.slice(-FEED_LINES).toReversed()
  const hasPenalties = match.home.penalties !== null && match.away.penalties !== null

  function restart() {
    setMinute(0)
    setShown(0)
    setPaused(false)
  }

  return (
    <div className="card match-replay">
      <div className="card-header">
        <h2>
          {finished ? (
            'Maç sonu'
          ) : (
            <span className="live-badge">
              <span className="live-dot" aria-hidden /> {minute === 45 && shown === 45 ? 'İY' : `${minute}'`}
            </span>
          )}
        </h2>
        <div className="live-controls">
          <span className="segmented" role="group" aria-label="Hız">
            {SPEEDS.map((value) => (
              <button
                key={value}
                className={`btn btn-sm${speed === value ? ' is-selected' : ''}`}
                onClick={() => setSpeed(value)}
                aria-pressed={speed === value}
              >
                {value}x
              </button>
            ))}
          </span>
          {!finished && (
            <button className="btn btn-sm" onClick={() => setPaused((value) => !value)}>
              {paused ? 'Devam' : 'Duraklat'}
            </button>
          )}
          <button className="btn btn-sm" onClick={restart}>
            Baştan
          </button>
          {!finished && (
            <button className="btn btn-sm" onClick={() => setMinute(90)}>
              Sona atla
            </button>
          )}
          <button className="btn btn-sm" onClick={onClose}>
            Kapat
          </button>
        </div>
      </div>
      <div className="my-match-score">
        <span>{match.home.teamName}</span>
        <span className="live-score">
          {homeGoals} - {awayGoals}
        </span>
        <span>{match.away.teamName}</span>
      </div>
      {finished && hasPenalties && (
        <p className="muted legend">
          Penaltılar {match.home.penalties} - {match.away.penalties}
        </p>
      )}
      <MatchCanvas
        script={script}
        minute={minute}
        playing={!paused && !finished}
        speed={speed}
        onMinuteShown={setShown}
        homeName={match.home.teamName}
        awayName={match.away.teamName}
      />
      <ol className="live-feed replay-feed" aria-live="polite">
        {feed.map((event, index) => (
          <li key={`${event.minute}-${index}`}>
            <span className="live-feed-minute">{event.minute}'</span>
            <span aria-hidden>{EVENT_ICONS[event.type] ?? ''}</span>
            <span>{event.commentary}</span>
          </li>
        ))}
        {feed.length === 0 && <li className="muted">Henüz önemli bir olay yok.</li>}
      </ol>
    </div>
  )
}

export default MatchReplay
