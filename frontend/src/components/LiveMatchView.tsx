import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { LiveItem, LiveSession, PlayStyle, TeamTalk } from '../api/types'
import { EVENT_ICONS, goalSuffix, PLAY_STYLE_LABELS, PLAY_STYLES, POSITION_SHORT, scoringSide } from '../labels'

const SPEEDS = [
  { value: 1, label: '1x' },
  { value: 3, label: '3x' },
  { value: 10, label: '10x' },
]
// 1x hızda yarım saat ~15 saniye
const MS_PER_MINUTE_1X = 333
const TALKS: { value: TeamTalk; label: string; hint: string }[] = [
  { value: 'NONE', label: 'Konuşma yok', hint: 'Etkisi yok' },
  { value: 'MOTIVATE', label: 'Motive et', hint: 'Geride ya da berabereyken gol şansını artırır' },
  { value: 'CALM', label: 'Sakinleştir', hint: 'Öndeyken rakibin gol şansını azaltır' },
  { value: 'CRITICIZE', label: 'Eleştir', hint: 'Gerideyken büyük etki, öndeyken ters teper; moral düşer' },
]
const ICONS: Record<string, string> = { ...EVENT_ICONS, SUBSTITUTION: '🔄' }

type Phase = 'first-half' | 'half-time' | 'second-half' | 'full-time'

/**
 * Kullanıcının maçı: ilk yarı backend'de oynanır ve burada dakika dakika gösterilir; devre arasında değişiklik,
 * stil ve konuşma seçilir; ikinci yarı oynanıp aynı şekilde gösterilir.
 */
function LiveMatchView({ onClose }: { onClose: () => void }) {
  const [session, setSession] = useState<LiveSession | null>(null)
  // İkinci yarı backend'de oynandı mı (devre arası kararları gönderildi)
  const [secondHalf, setSecondHalf] = useState(false)
  const [minute, setMinute] = useState(0)
  const [speed, setSpeed] = useState(3)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [substitutions, setSubstitutions] = useState<{ outId: number; inId: number }[]>([])
  const [style, setStyle] = useState<PlayStyle>('BALANCED')
  const [talk, setTalk] = useState<TeamTalk>('NONE')
  const target = secondHalf ? 90 : 45
  const phase: Phase = secondHalf
    ? minute < 90
      ? 'second-half'
      : 'full-time'
    : minute < 45
      ? 'first-half'
      : 'half-time'
  const running = session !== null && minute < target
  const started = useRef(false)

  useEffect(() => {
    if (started.current) {
      return
    }
    started.current = true
    api
      .startLive()
      .then((data) => {
        setSession(data)
        setStyle(data.playStyle)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  // Dakika sayacı: hedef dakikaya (45 / 90) kadar ilerler; faz dakikadan türetilir
  useEffect(() => {
    if (!running) {
      return
    }
    const timer = setTimeout(() => setMinute((current) => current + 1), MS_PER_MINUTE_1X / speed)
    return () => clearTimeout(timer)
  }, [running, minute, speed])

  async function startSecondHalf() {
    setBusy(true)
    setError(null)
    try {
      const data = await api.playSecondHalf(substitutions, style, talk)
      setSession(data)
      setSecondHalf(true)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  if (!session) {
    return error ? (
      <div className="card">
        <p className="alert alert-error">{error}</p>
        <button className="btn" onClick={onClose}>
          Panoya dön
        </button>
      </div>
    ) : (
      <p className="muted">İlk yarı oynanıyor…</p>
    )
  }

  const visible = session.events.filter((item) => item.minute <= minute)
  const homeGoals = visible.filter((item) => scoringSide(item) === 'home').length
  const awayGoals = visible.filter((item) => scoringSide(item) === 'away').length
  const { match } = session
  const subsUsed = substitutions.length
  const usedOut = new Set(substitutions.map((s) => s.outId))
  const usedIn = new Set(substitutions.map((s) => s.inId))

  return (
    <div className={`card live-round${phase === 'first-half' || phase === 'second-half' ? ' is-live' : ''}`}>
      <div className="card-header">
        <h2>
          {phase === 'half-time' ? (
            'Devre arası'
          ) : phase === 'full-time' ? (
            'Maç sonu'
          ) : (
            <span className="live-badge">
              <span className="live-dot" aria-hidden /> CANLI {minute}'
            </span>
          )}
        </h2>
        {(phase === 'first-half' || phase === 'second-half') && (
          <div className="live-controls">
            <span className="segmented" role="group" aria-label="Hız">
              {SPEEDS.map((option) => (
                <button
                  key={option.value}
                  className={`btn btn-sm${speed === option.value ? ' is-selected' : ''}`}
                  onClick={() => setSpeed(option.value)}
                  aria-pressed={speed === option.value}
                >
                  {option.label}
                </button>
              ))}
            </span>
            <button className="btn btn-sm" onClick={() => setMinute(target)}>
              {phase === 'first-half' ? 'Devre arasına geç' : 'Sona geç'}
            </button>
          </div>
        )}
      </div>
      {error && <p className="alert alert-error">{error}</p>}

      <div className="my-match-score">
        <span className={session.userHome ? 'strong' : undefined}>{match.homeTeamName}</span>
        <span className="live-score">
          {homeGoals} - {awayGoals}
        </span>
        <span className={!session.userHome ? 'strong' : undefined}>{match.awayTeamName}</span>
      </div>

      <ol className="live-feed my-match-feed">
        {visible.toReversed().map((item, index) => (
          <FeedLine key={`${item.minute}-${index}`} item={item} />
        ))}
        {visible.length === 0 && <li className="muted">Henüz önemli bir olay yok.</li>}
      </ol>

      {phase === 'half-time' && (
        <div className="half-time">
          <h3>Devre arası kararları</h3>
          <div className="half-time-grid">
            <div>
              <div className="muted">Değişiklik ({session.substitutionsLeft - subsUsed} hak)</div>
              {substitutions.map((substitution, index) => (
                <div key={index} className="sub-pair">
                  <span className="sub-out">↓ {session.onPitch.find((p) => p.id === substitution.outId)?.name}</span>
                  <span className="sub-in">↑ {session.bench.find((p) => p.id === substitution.inId)?.name}</span>
                  <button
                    className="btn btn-sm"
                    onClick={() => setSubstitutions((current) => current.filter((_, i) => i !== index))}
                  >
                    Geri al
                  </button>
                </div>
              ))}
              {subsUsed < session.substitutionsLeft && session.bench.length > usedIn.size && (
                <SubstitutionPicker
                  session={session}
                  usedOut={usedOut}
                  usedIn={usedIn}
                  onAdd={(outId, inId) => setSubstitutions((current) => [...current, { outId, inId }])}
                />
              )}
            </div>
            <div>
              <label className="field">
                İkinci yarı stili
                <select value={style} onChange={(event) => setStyle(event.target.value as PlayStyle)}>
                  {PLAY_STYLES.map((option) => (
                    <option key={option} value={option}>
                      {PLAY_STYLE_LABELS[option]}
                    </option>
                  ))}
                </select>
              </label>
              <div className="muted talk-title">Soyunma odası konuşması</div>
              <div className="talk-options">
                {TALKS.map((option) => (
                  <label key={option.value} className="talk-option" title={option.hint}>
                    <input
                      type="radio"
                      name="talk"
                      checked={talk === option.value}
                      onChange={() => setTalk(option.value)}
                    />{' '}
                    {option.label}
                  </label>
                ))}
              </div>
              <p className="legend muted">{TALKS.find((option) => option.value === talk)?.hint}</p>
            </div>
          </div>
          <button className="btn btn-primary" onClick={startSecondHalf} disabled={busy}>
            {busy ? 'Oynanıyor…' : 'İkinci yarıyı başlat'}
          </button>
        </div>
      )}

      {phase === 'full-time' && (
        <div className="full-time">
          {session.talkResult && <p className="alert alert-info">{session.talkResult}</p>}
          <h3>Oyuncu reytingleri</h3>
          <ul className="lineup">
            {session.onPitch
              .toSorted((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
              .map((player) => (
                <li key={player.id}>
                  <span className="lineup-number">{player.shirtNumber}</span>
                  <Link to={`/players/${player.id}`} className="team-link">
                    {player.name}
                  </Link>
                  <span className="muted lineup-pos">{POSITION_SHORT[player.position]}</span>
                  {player.rating !== null && (
                    <span
                      className={`rating rating-${player.rating >= 7.5 ? 'high' : player.rating < 6 ? 'low' : 'mid'}`}
                    >
                      {player.rating.toFixed(1)}
                    </span>
                  )}
                </li>
              ))}
          </ul>
          {session.otherResults.length > 0 && (
            <>
              <h3>Diğer sonuçlar</h3>
              <ul className="other-results">
                {session.otherResults.map((other) => (
                  <li key={other.id}>
                    <Link to={`/matches/${other.id}`}>
                      {other.homeTeamName} {other.homeScore} - {other.awayScore} {other.awayTeamName}
                      {other.homePenalties !== null && ` (pen. ${other.homePenalties}-${other.awayPenalties})`}
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          )}
          <div className="page-actions">
            <Link to={`/matches/${session.matchId}`} className="btn">
              Maç detayı
            </Link>
            <button className="btn btn-primary" onClick={onClose}>
              Panoya dön
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function FeedLine({ item }: { item: LiveItem }) {
  const side = scoringSide(item)
  return (
    <li className={side ? 'feed-goal' : undefined}>
      <span className="live-feed-minute">{item.minute}'</span>
      <span aria-hidden>{ICONS[item.type] ?? '•'}</span>
      <span>
        {item.text}
        {side && (
          <span className="muted">
            {' '}
            ({item.playerName}
            {goalSuffix(item)})
          </span>
        )}
      </span>
    </li>
  )
}

interface SubstitutionPickerProps {
  session: LiveSession
  usedOut: Set<number>
  usedIn: Set<number>
  onAdd: (outId: number, inId: number) => void
}

function SubstitutionPicker({ session, usedOut, usedIn, onAdd }: SubstitutionPickerProps) {
  const [outId, setOutId] = useState('')
  const [inId, setInId] = useState('')
  return (
    <div className="sub-picker">
      <select value={outId} onChange={(event) => setOutId(event.target.value)} aria-label="Çıkan oyuncu">
        <option value="">Çıkan</option>
        {session.onPitch
          .filter((player) => !usedOut.has(player.id))
          .map((player) => (
            <option key={player.id} value={player.id}>
              {POSITION_SHORT[player.position]} · {player.name}
            </option>
          ))}
      </select>
      <select value={inId} onChange={(event) => setInId(event.target.value)} aria-label="Giren oyuncu">
        <option value="">Giren</option>
        {session.bench
          .filter((player) => !usedIn.has(player.id))
          .map((player) => (
            <option key={player.id} value={player.id}>
              {POSITION_SHORT[player.position]} · {player.name} ({player.strength})
            </option>
          ))}
      </select>
      <button
        className="btn btn-sm"
        disabled={!outId || !inId}
        onClick={() => {
          onAdd(Number(outId), Number(inId))
          setOutId('')
          setInId('')
        }}
      >
        Ekle
      </button>
    </div>
  )
}

export default LiveMatchView
