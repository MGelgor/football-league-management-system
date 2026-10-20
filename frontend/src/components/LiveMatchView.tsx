import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { LiveItem, LivePitchPlayer, LiveSession, PlayStyle, SessionPlayer, TeamTalk } from '../api/types'
import {
  EVENT_ICONS,
  FORMATION_LABELS,
  goalSuffix,
  PLAY_STYLE_LABELS,
  PLAY_STYLES,
  POSITION_LABELS,
  POSITION_SHORT,
  scoringSide,
} from '../labels'
import { scriptFromLive } from '../match2d/script'
import LivePitch from './LivePitch'
import MatchCanvas from './MatchCanvas'

const SPEEDS = [
  { value: 1, label: '1x' },
  { value: 3, label: '3x' },
  { value: 10, label: '10x' },
]
// 1x hızda yarım saat ~15 saniye
const MS_PER_MINUTE_1X = 333
// Gol dakikasında sayaç bu kadar bekler (1x'te)
const GOAL_PAUSE_MS = 1200
// Olay balonu yaklaşık bu kadar süre görünür (hıza göre dakikaya çevrilir, en fazla 6 dakika)
const FLASH_MS = 1600
const TALKS: { value: TeamTalk; label: string; hint: string }[] = [
  { value: 'NONE', label: 'Konuşma yok', hint: 'Etkisi yok' },
  { value: 'MOTIVATE', label: 'Motive et', hint: 'Geride ya da berabereyken gol şansını artırır' },
  { value: 'CALM', label: 'Sakinleştir', hint: 'Öndeyken rakibin gol şansını azaltır' },
  { value: 'CRITICIZE', label: 'Eleştir', hint: 'Gerideyken büyük etki, öndeyken ters teper; moral düşer' },
]
const ICONS: Record<string, string> = { ...EVENT_ICONS, SUBSTITUTION: '🔄' }

type Phase = 'first-half' | 'half-time' | 'second-half' | 'full-time'
type View = 'animation' | 'formation'
type Substitution = { outId: number; inId: number }

/**
 * Kullanıcının maçı: ilk yarı backend'de oynanır ve burada yatay sahada dakika dakika gösterilir; devre arasında
 * değişiklik (yedek sahadaki oyuncunun üstüne sürüklenir), stil ve konuşma seçilir; ikinci yarı oynanıp aynı şekilde
 * gösterilir. Devre arasında sayfadan ayrılıp dönülürse oturum kaldığı yerden (devre arası) açılır.
 */
function LiveMatchView({ onClose }: { onClose: () => void }) {
  const [session, setSession] = useState<LiveSession | null>(null)
  // İkinci yarı backend'de oynandı mı (devre arası kararları gönderildi)
  const [secondHalf, setSecondHalf] = useState(false)
  const [minute, setMinute] = useState(0)
  const [speed, setSpeed] = useState(3)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [substitutions, setSubstitutions] = useState<Substitution[]>([])
  const [style, setStyle] = useState<PlayStyle>('BALANCED')
  const [talk, setTalk] = useState<TeamTalk>('NONE')
  // Tıklamayla değişiklik: önce yedek seçilir, sonra çıkacak oyuncu
  const [selectedIn, setSelectedIn] = useState<number | null>(null)
  // Animasyon: oyuncular ve top hareket eder; diziliş: oyuncular sabit sütunlarda (devre arasında hep diziliş)
  const [view, setView] = useState<View>('animation')
  // Animasyonda sahnesi tamamlanmış son dakika: skor, anlatım ve sayaç buna göre ilerler
  const [shownMinute, setShownMinute] = useState(0)
  const target = secondHalf ? 90 : 45
  // Animasyonda son dakikanın sahnesi bitmeden devre arası / maç sonu ekranına geçilmez
  const settled = view !== 'animation' || shownMinute >= minute
  const phase: Phase = secondHalf
    ? minute < 90 || !settled
      ? 'second-half'
      : 'full-time'
    : minute < 45 || !settled
      ? 'first-half'
      : 'half-time'
  const running = session !== null && minute < target
  const animated = view === 'animation' && phase !== 'half-time'
  const started = useRef(false)
  const script = useMemo(() => (session ? scriptFromLive(session) : null), [session])

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
        // Devre arasında bekleyen maça dönüldü: ilk yarı yeniden oynatılmaz
        if (data.resumed) {
          setMinute(data.minute)
        }
      })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  // Dakika sayacı: hedef dakikaya (45 / 90) kadar ilerler; faz dakikadan türetilir
  useEffect(() => {
    if (!running || (animated && shownMinute < minute)) {
      return
    }
    const goalNow = !animated && session.events.some((item) => item.minute === minute && scoringSide(item) !== null)
    const timer = setTimeout(
      () => setMinute((current) => current + 1),
      (MS_PER_MINUTE_1X + (goalNow ? GOAL_PAUSE_MS : 0)) / speed,
    )
    return () => clearTimeout(timer)
  }, [running, minute, speed, session, animated, shownMinute])

  function substitute(outId: number, inId: number) {
    if (!session) {
      return
    }
    setSelectedIn(null)
    const result = addSubstitution(session, substitutions, outId, inId)
    if (typeof result === 'string') {
      setError(result)
    } else {
      setError(null)
      setSubstitutions(result)
    }
  }

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

  // Animasyonda gol, sahnesi oynanmadan skora ve anlatıma yazılmaz
  const revealedMinute = animated ? Math.min(minute, shownMinute) : minute
  const visible = session.events.filter((item) => item.minute <= revealedMinute)
  const homeGoals = visible.filter((item) => scoringSide(item) === 'home').length
  const awayGoals = visible.filter((item) => scoringSide(item) === 'away').length
  const { match } = session
  const flashMinutes = running ? Math.min(6, Math.max(1, Math.ceil(FLASH_MS / (MS_PER_MINUTE_1X / speed)))) : 0
  const pitchPlayers = phase === 'half-time' ? withPendingSubs(session, substitutions) : session.pitch

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
        {phase !== 'half-time' && (
          <span className="segmented" role="group" aria-label="Görünüm">
            {(
              [
                ['animation', 'Animasyon'],
                ['formation', 'Diziliş'],
              ] as [View, string][]
            ).map(([value, label]) => (
              <button
                key={value}
                className={`btn btn-sm${view === value ? ' is-selected' : ''}`}
                onClick={() => setView(value)}
                aria-pressed={view === value}
              >
                {label}
              </button>
            ))}
          </span>
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
      <p className="muted legend live-formations">
        {FORMATION_LABELS[session.userHome ? session.formation : session.opponentFormation]} ·{' '}
        {FORMATION_LABELS[session.userHome ? session.opponentFormation : session.formation]}
      </p>

      <div className="live-layout">
        {animated && script ? (
          <MatchCanvas
            script={script}
            minute={minute}
            playing={running || shownMinute < minute}
            speed={speed}
            onMinuteShown={setShownMinute}
            homeName={match.homeTeamName + (session.userHome ? ' (sen)' : '')}
            awayName={match.awayTeamName + (session.userHome ? '' : ' (sen)')}
          />
        ) : (
        <LivePitch
          homeName={match.homeTeamName}
          awayName={match.awayTeamName}
          players={pitchPlayers}
          events={session.events}
          minute={minute}
          flashMinutes={flashMinutes}
          userHome={session.userHome}
          pendingInIds={phase === 'half-time' ? new Set(substitutions.map((sub) => sub.inId)) : undefined}
          onDropOnPlayer={phase === 'half-time' ? substitute : undefined}
          onPlayerClick={
            phase === 'half-time' ? (outId) => selectedIn !== null && substitute(outId, selectedIn) : undefined
          }
        />
        )}
        <ol className="live-feed my-match-feed">
          {visible.toReversed().map((item, index) => (
            <FeedLine key={`${item.minute}-${index}`} item={item} />
          ))}
          {visible.length === 0 && <li className="muted">Henüz önemli bir olay yok.</li>}
        </ol>
      </div>

      {phase === 'half-time' && (
        <div className="half-time">
          <h3>Devre arası kararları</h3>
          <SubstitutionBoard
            session={session}
            substitutions={substitutions}
            selectedIn={selectedIn}
            onSelectIn={setSelectedIn}
            onSubstitute={substitute}
            onUndo={(outId) => setSubstitutions((current) => current.filter((sub) => sub.outId !== outId))}
          />
          <div className="half-time-grid">
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
            <div>
              <div className="muted">Soyunma odası konuşması</div>
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
          {session.moraleChange !== null && (
            <MoraleResult
              change={session.moraleChange}
              morale={session.morale}
              talkCost={talk === 'CRITICIZE' ? 5 : 0}
              outcome={Math.sign(session.userHome ? homeGoals - awayGoals : awayGoals - homeGoals)}
            />
          )}
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

/** Devre arası önizlemesi: bekleyen değişikliklerde çıkan 45'te çıkmış, giren 45'te girmiş gibi gösterilir. */
function withPendingSubs(session: LiveSession, substitutions: Substitution[]): LivePitchPlayer[] {
  const outIds = new Set(substitutions.map((sub) => sub.outId))
  const pitch = session.pitch.map((player) => (outIds.has(player.id) ? { ...player, minuteOff: 45 } : player))
  substitutions.forEach((sub) => {
    const incoming = session.bench.find((player) => player.id === sub.inId)
    if (incoming) {
      pitch.push({
        id: incoming.id,
        name: incoming.name,
        position: incoming.position,
        shirtNumber: incoming.shirtNumber,
        home: session.userHome,
        effectiveStrength: incoming.currentStrength,
        fatigueRate: 0,
        minuteOn: 45,
        minuteOff: null,
        replacedPlayerId: sub.outId,
      })
    }
  })
  return pitch
}

/** Kondisyon (%): saha görünümündeki çubukla aynı ölçek (yorgunluk × 4 düşer). */
function stamina(player: SessionPlayer) {
  return Math.max(0, 100 - player.matchFatigue * 4)
}

function staminaClass(value: number) {
  return value >= 80 ? 'fresh' : value >= 65 ? 'tired' : 'exhausted'
}

/**
 * Bekleyen değişikliklere outId → inId ekler: çıkan zaten değişiyorsa gireni değişir; giren başka değişiklikte
 * kullanılıyorsa ya da hak kalmadıysa eklenmez (hata mesajı döner).
 */
function addSubstitution(
  session: LiveSession,
  substitutions: Substitution[],
  outId: number,
  inId: number,
): Substitution[] | string {
  if (substitutions.some((sub) => sub.inId === inId) || !session.bench.some((player) => player.id === inId)) {
    return substitutions
  }
  if (substitutions.some((sub) => sub.outId === outId)) {
    return substitutions.map((sub) => (sub.outId === outId ? { outId, inId } : sub))
  }
  if (substitutions.length >= session.substitutionsLeft) {
    return 'Değişiklik hakkın kalmadı'
  }
  return [...substitutions, { outId, inId }]
}

interface SubstitutionBoardProps {
  session: LiveSession
  substitutions: Substitution[]
  selectedIn: number | null
  onSelectIn: (id: number | null) => void
  onSubstitute: (outId: number, inId: number) => void
  onUndo: (outId: number) => void
}

/**
 * Değişiklik tahtası: yedek oyuncu sahadaki bir oyuncunun üstüne sürüklenir (ya da önce yedeğe, sonra sahadakine
 * tıklanır). Sahadakilerin gücü ve ilk yarıda düşen gücü (yorgunluk), yedeklerin dinç gücü görünür.
 */
function SubstitutionBoard({
  session,
  substitutions,
  selectedIn,
  onSelectIn,
  onSubstitute,
  onUndo,
}: SubstitutionBoardProps) {
  const [dragOver, setDragOver] = useState<number | null>(null)
  const byOut = new Map(substitutions.map((sub) => [sub.outId, sub]))
  const usedIn = new Set(substitutions.map((sub) => sub.inId))
  const left = session.substitutionsLeft - substitutions.length

  function substitute(outId: number, inId: number) {
    setDragOver(null)
    onSubstitute(outId, inId)
  }

  return (
    <div className="sub-board">
      <div className="muted">
        Değişiklik: <strong>{left}</strong> hak kaldı · yedeği sahadaki oyuncunun üstüne sürükle ya da önce yedeğe,
        sonra çıkacak oyuncuya dokun
      </div>
      <div className="sub-board-grid">
        <div>
          <div className="lineup-squad-title">Sahada</div>
          <ul className="sub-list">
            {session.onPitch.map((player) => {
              const sub = byOut.get(player.id)
              const incoming = sub ? session.bench.find((bench) => bench.id === sub.inId) : undefined
              return (
                <li
                  key={player.id}
                  className={`sub-row sub-target${dragOver === player.id ? ' is-over' : ''}${sub ? ' is-out' : ''}${
                    selectedIn !== null ? ' is-armed' : ''
                  }`}
                  onDragOver={(event) => {
                    event.preventDefault()
                    setDragOver(player.id)
                  }}
                  onDragLeave={() => setDragOver((current) => (current === player.id ? null : current))}
                  onDrop={(event) => {
                    event.preventDefault()
                    const inId = Number(event.dataTransfer.getData('text/plain'))
                    if (inId) {
                      substitute(player.id, inId)
                    }
                  }}
                  onClick={() => selectedIn !== null && substitute(player.id, selectedIn)}
                >
                  <span className="lineup-chip-number">{player.shirtNumber}</span>
                  <span className="muted sub-pos">{POSITION_SHORT[player.position]}</span>
                  <span className="sub-name">{player.name}</span>
                  <span
                    className="sub-strength"
                    title={`Güç ${player.strength}; form ve yorgunlukla şu an ${player.currentStrength}`}
                  >
                    {player.strength} → <strong>{Math.round(player.currentStrength)}</strong>
                    <span className="sub-fatigue"> −{player.matchFatigue.toFixed(1)}</span>
                  </span>
                  <span className="hpitch-stamina sub-stamina" aria-hidden>
                    <span className={staminaClass(stamina(player))} style={{ width: `${stamina(player)}%` }} />
                  </span>
                  {incoming && (
                    <span className="sub-swap">
                      <span className="sub-out">↓ {player.name.split(' ').at(-1)}</span>
                      <span className="sub-in">
                        ↑ {incoming.name} ({Math.round(incoming.currentStrength)})
                      </span>
                      {incoming.position !== player.position && (
                        <span className="muted"> · {POSITION_LABELS[incoming.position].toLocaleLowerCase('tr')}</span>
                      )}
                      <button
                        className="btn btn-sm"
                        onClick={(event) => {
                          event.stopPropagation()
                          onUndo(player.id)
                        }}
                      >
                        Geri al
                      </button>
                    </span>
                  )}
                </li>
              )
            })}
          </ul>
        </div>
        <div>
          <div className="lineup-squad-title">Yedekler</div>
          <ul className="sub-list">
            {session.bench.map((player) => {
              const used = usedIn.has(player.id)
              return (
                <li
                  key={player.id}
                  className={`sub-row sub-source${used ? ' is-used' : ''}${selectedIn === player.id ? ' selected' : ''}`}
                  draggable={!used}
                  onDragStart={(event) => event.dataTransfer.setData('text/plain', String(player.id))}
                  onClick={() => !used && onSelectIn(selectedIn === player.id ? null : player.id)}
                  title={used ? 'Oyuna girecek' : 'Sahadaki bir oyuncunun üstüne sürükle'}
                >
                  <span className="lineup-chip-number">{player.shirtNumber}</span>
                  <span className="muted sub-pos">{POSITION_SHORT[player.position]}</span>
                  <span className="sub-name">{player.name}</span>
                  <span className="sub-strength">
                    <strong>{Math.round(player.currentStrength)}</strong>
                    <span className="muted"> dinç</span>
                  </span>
                </li>
              )
            })}
            {session.bench.length === 0 && <li className="muted">Yedek oyuncu yok.</li>}
          </ul>
        </div>
      </div>
    </div>
  )
}

/** Maç sonu moral değişimi; güçlü rakibe karşı alınan sonuç ve atılan goller ekstra moral getirir. */
function MoraleResult({
  change,
  morale,
  talkCost,
  outcome,
}: {
  change: number
  morale: number | null
  talkCost: number
  outcome: number
}) {
  const matchChange = change + talkCost
  const bonus = outcome > 0 ? matchChange > 10 : outcome === 0 ? matchChange > 0 : matchChange > -10
  return (
    <p className={`alert ${change >= 0 ? 'alert-success' : 'alert-warning'}`}>
      {change === 0 ? 'Moral değişmedi' : `Moral ${change > 0 ? '+' : ''}${change}`}
      {morale !== null && ` (şimdi ${morale})`}
      {bonus && ' · Güçlü rakibe karşı alınan sonuç ve atılan goller ekstra moral kazandırdı!'}
    </p>
  )
}

export default LiveMatchView
