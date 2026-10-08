import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import type { LiveItem, LiveMatch, LiveMinute } from '../api/types'
import { EVENT_ICONS, goalSuffix, scoringSide } from '../labels'

const SPEEDS = [
  { value: 1, label: '1x', title: 'Maç 30 saniye sürer' },
  { value: 3, label: '3x', title: 'Maç 10 saniye sürer' },
  { value: 10, label: '10x', title: 'Maç 3 saniye sürer' },
]
const DEFAULT_SPEED = 3
// Gol bu kadar dakika boyunca kartta "GOL!" olarak vurgulanır
const GOAL_FLASH_MINUTES = 8
const FEED_LENGTH = 14
const ITEM_ICONS: Record<string, string> = { ...EVENT_ICONS, SUBSTITUTION: '🔄' }
const PHASE_TEXT = { KICK_OFF: 'Maçlar başladı.', HALF_TIME: 'İlk yarı sona erdi.', FULL_TIME: 'Maçlar sona erdi.' }

interface LiveBroadcastProps {
  weekNumber: number
  title: string
  // Bu takımların maçı öne alınır ve vurgulanır (ör. yönetilen takım)
  highlightTeamIds?: number[]
  // Yayın bitince (ya da atlanınca) bir kez çağrılır
  onFinish: () => void
  onClose: () => void
}

/**
 * Oynanmış haftayı backend'in Server-Sent Events yayınından dakika dakika gösterir. Hız değişince ya da
 * "Atla"ya basılınca bağlantı kaldığı dakikadan yeni hızla yeniden açılır; kopan bağlantıyı tarayıcı
 * Last-Event-ID ile kendisi yeniler.
 */
function LiveBroadcast({ weekNumber, title, highlightTeamIds = [], onFinish, onClose }: LiveBroadcastProps) {
  const [matches, setMatches] = useState<LiveMatch[]>([])
  const [minute, setMinute] = useState(0)
  const [scores, setScores] = useState<Record<number, [number, number]>>({})
  const [items, setItems] = useState<LiveItem[]>([])
  const [feed, setFeed] = useState<{ minute: number; text: string; icon: string; team?: string }[]>([])
  const [finished, setFinished] = useState(false)
  const [stream, setStream] = useState({ speed: DEFAULT_SPEED, from: 0 })
  const lastMinute = useRef(-1)
  const finishRef = useRef(onFinish)

  useEffect(() => {
    finishRef.current = onFinish
  }, [onFinish])

  useEffect(() => {
    const source = new EventSource(`/api/weeks/${weekNumber}/live?speed=${stream.speed}&from=${stream.from}`)
    source.addEventListener('start', (event) => setMatches(JSON.parse((event as MessageEvent).data)))
    source.addEventListener('minute', (event) => {
      const data: LiveMinute = JSON.parse((event as MessageEvent).data)
      if (data.minute <= lastMinute.current) {
        return
      }
      lastMinute.current = data.minute
      setMinute(data.minute)
      setScores(Object.fromEntries(data.scores.map((score) => [score.matchId, [score.home, score.away]])))
      setItems((previous) => [...previous, ...data.items])
      setFeed((previous) => {
        const lines = data.items.map((item) => ({
          minute: item.minute,
          text: item.text,
          icon: ITEM_ICONS[item.type] ?? '•',
        }))
        if (data.phase) {
          lines.push({ minute: data.minute, text: PHASE_TEXT[data.phase], icon: '⏱' })
        }
        return [...lines.reverse(), ...previous].slice(0, FEED_LENGTH)
      })
    })
    source.addEventListener('end', (event) => {
      setMatches(JSON.parse((event as MessageEvent).data))
      setFinished(true)
      source.close()
      finishRef.current()
    })
    return () => source.close()
  }, [weekNumber, stream])

  const changeSpeed = (speed: number) => setStream({ speed, from: lastMinute.current + 1 })

  const highlighted = (match: LiveMatch) =>
    highlightTeamIds.includes(match.homeTeamId) || highlightTeamIds.includes(match.awayTeamId)
  const ordered = matches.toSorted((a, b) => Number(highlighted(b)) - Number(highlighted(a)))

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
        <div className="live-controls">
          {!finished && (
            <>
              <span className="segmented" role="group" aria-label="Yayın hızı">
                {SPEEDS.map((option) => (
                  <button
                    key={option.value}
                    className={`btn btn-sm${stream.speed === option.value ? ' is-selected' : ''}`}
                    onClick={() => changeSpeed(option.value)}
                    title={option.title}
                    aria-pressed={stream.speed === option.value}
                  >
                    {option.label}
                  </button>
                ))}
              </span>
              <button className="btn btn-sm" onClick={() => changeSpeed(0)}>
                Atla
              </button>
            </>
          )}
          {finished && (
            <button className="btn btn-sm" onClick={onClose}>
              Kapat
            </button>
          )}
        </div>
      </div>
      <div className="live-layout">
        <div className="live-matches">
          {ordered.map((match) => (
            <LiveMatchCard
              key={match.matchId}
              match={match}
              score={scores[match.matchId] ?? [0, 0]}
              goals={items.filter((item) => item.matchId === match.matchId && scoringSide(item) !== null)}
              minute={minute}
              finished={finished}
              highlighted={highlighted(match)}
            />
          ))}
        </div>
        <ol className="live-feed" aria-live="polite" aria-label="Maç anlatımı">
          {feed.map((line, index) => (
            <li key={index}>
              <span className="live-feed-minute">{line.minute}'</span>
              <span aria-hidden>{line.icon}</span>
              <span>{line.text}</span>
            </li>
          ))}
        </ol>
      </div>
    </div>
  )
}

interface LiveMatchCardProps {
  match: LiveMatch
  score: [number, number]
  goals: LiveItem[]
  minute: number
  finished: boolean
  highlighted: boolean
}

function LiveMatchCard({ match, score, goals, minute, finished, highlighted }: LiveMatchCardProps) {
  const latest = goals.at(-1)
  const flashing = !finished && latest !== undefined && minute - latest.minute < GOAL_FLASH_MINUTES
  const hasPenalties = finished && match.homePenalties !== null && match.awayPenalties !== null
  const [home, away] = finished ? [match.homeScore, match.awayScore] : score

  return (
    <div className={`live-match${flashing ? ' goal-flash' : ''}${highlighted ? ' live-match-mine' : ''}`}>
      <div className="live-match-status">
        {finished ? (
          <span className="muted">
            MS{hasPenalties && ` · Penaltılar ${match.homePenalties} - ${match.awayPenalties}`}
          </span>
        ) : (
          <span className="live-badge">
            <span className="live-dot" aria-hidden /> {minute}'
          </span>
        )}
        {flashing && <span className="goal-banner">GOL!</span>}
      </div>
      <div className="live-scoreline">
        <span className="live-team live-home">{match.homeTeamName}</span>
        <span className="live-score">
          {home} - {away}
        </span>
        <span className="live-team">{match.awayTeamName}</span>
      </div>
      {goals.length > 0 && (
        <ul className="live-goals">
          {goals.map((goal, index) => {
            const text = `⚽ ${goal.minute}' ${goal.playerName}${goalSuffix(goal)}`
            const home = scoringSide(goal) === 'home'
            return (
              <li key={index} className={home ? 'live-goal-home' : 'live-goal-away'}>
                <span>{home && text}</span>
                <span>{!home && text}</span>
              </li>
            )
          })}
        </ul>
      )}
      {finished && (
        <Link to={`/matches/${match.matchId}`} className="live-detail">
          Maç detayı →
        </Link>
      )}
    </div>
  )
}

export default LiveBroadcast
