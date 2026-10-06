import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Match, MatchWeek, Season } from '../api/types'
import ProbabilityBar from '../components/ProbabilityBar'

function isWeekPlayed(week: MatchWeek) {
  return week.matches.every((match) => match.played)
}

/** Sıradaki oynanacak hafta; sezon bittiyse null. */
function nextWeekNumber(weeks: MatchWeek[]) {
  return weeks.find((week) => !isWeekPlayed(week))?.weekNumber ?? null
}

function fetchFixtureAndSeasons() {
  return Promise.all([api.getFixture(), api.getSeasons()])
}

function FixturePage() {
  const [weeks, setWeeks] = useState<MatchWeek[] | null>(null)
  const [season, setSeason] = useState<Season | null>(null)
  const [selectedWeekNumber, setSelectedWeekNumber] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmReset, setConfirmReset] = useState(false)

  function show(fixture: MatchWeek[], seasons: Season[], keepWeek?: number) {
    setWeeks(fixture)
    setSeason(seasons[0] ?? null)
    setSelectedWeekNumber(keepWeek ?? nextWeekNumber(fixture) ?? fixture.at(-1)?.weekNumber ?? null)
  }

  async function load(keepWeek?: number) {
    const [fixture, seasons] = await fetchFixtureAndSeasons()
    show(fixture, seasons, keepWeek)
  }

  useEffect(() => {
    fetchFixtureAndSeasons()
      .then(([fixture, seasons]) => show(fixture, seasons))
      .catch((e) => setError(errorMessage(e)))
  }, [])

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const handleGenerate = () =>
    run(async () => {
      await api.generateFixture()
      await load()
    })

  // Oynanan hafta güçleri değiştirdiği için sonraki haftaların olasılıkları da yenilenir
  const handlePlayWeek = (weekNumber: number) =>
    run(async () => {
      await api.playWeek(weekNumber)
      await load(weekNumber)
    })

  const handleReset = () =>
    run(async () => {
      await api.resetFixture()
      setConfirmReset(false)
      await load()
    })

  const selectedWeek = weeks?.find((week) => week.weekNumber === selectedWeekNumber)
  const playedCount = weeks?.filter(isWeekPlayed).length ?? 0
  const nextWeek = weeks ? nextWeekNumber(weeks) : null
  const hasFixture = weeks !== null && weeks.length > 0
  const seasonFinished = season?.finished ?? false

  return (
    <section>
      <div className="page-header">
        <h1>
          Fikstür{season && <span className="muted"> · Sezon {season.seasonNumber}</span>}
        </h1>
        {hasFixture && !seasonFinished && (
          <div className="page-actions">
            {confirmReset ? (
              <>
                <span className="muted">Bu sezonun tüm maçları silinecek, güçler sezon başına dönecek.</span>
                <button className="btn btn-danger" onClick={handleReset} disabled={busy}>
                  Evet, sıfırla
                </button>
                <button className="btn" onClick={() => setConfirmReset(false)} disabled={busy}>
                  Vazgeç
                </button>
              </>
            ) : (
              <button className="btn btn-danger-outline" onClick={() => setConfirmReset(true)} disabled={busy}>
                Sezonu sıfırla
              </button>
            )}
          </div>
        )}
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      {weeks && weeks.length === 0 && (
        <div className="card">
          <p>
            Henüz fikstür oluşturulmadı. Fikstür kayıtlı takımlardan <strong>çift devreli</strong> olarak üretilir:
            her takım her rakiple bir kez kendi sahasında, bir kez deplasmanda oynar.
          </p>
          <p className="muted">
            En az 18 ve çift sayıda takım gerekir. Takım listesi için <Link to="/teams">Takımlar</Link> sayfasına bakın.
          </p>
          <button className="btn btn-primary" onClick={handleGenerate} disabled={busy}>
            {busy ? 'Oluşturuluyor…' : 'Fikstürü oluştur'}
          </button>
        </div>
      )}

      {hasFixture && (
        <>
          <p className="muted">
            {weeks.length} hafta · {playedCount} oynandı · {weeks.length - playedCount} kaldı
          </p>
          {seasonFinished && (
            <div className="alert alert-success season-done">
              <span>
                Sezon {season?.seasonNumber} tamamlandı! Şampiyon: <strong>{season?.championName}</strong>.{' '}
                <Link to="/standings">Puan Durumu</Link> · <Link to="/seasons">Tüm sezonlar</Link> ·{' '}
                {season?.cupWinnerName ? (
                  <>
                    Kupa: <strong>{season.cupWinnerName}</strong>
                  </>
                ) : (
                  <Link to="/cup">Kupayı oyna →</Link>
                )}
              </span>
              <span className="muted">
                Yeni sezondan önce <Link to="/teams">takım listesini</Link> değiştirebilirsiniz.
              </span>
              <button className="btn btn-primary" onClick={handleGenerate} disabled={busy}>
                {busy ? 'Oluşturuluyor…' : `Yeni sezonu başlat (Sezon ${(season?.seasonNumber ?? 0) + 1})`}
              </button>
            </div>
          )}

          <div className="week-strip" aria-label="Haftalar">
            {weeks.map((week) => (
              <button
                key={week.weekNumber}
                className={[
                  'week-chip',
                  isWeekPlayed(week) && 'played',
                  week.weekNumber === nextWeek && 'next',
                  week.weekNumber === selectedWeekNumber && 'selected',
                ]
                  .filter(Boolean)
                  .join(' ')}
                onClick={() => setSelectedWeekNumber(week.weekNumber)}
                title={`Hafta ${week.weekNumber} — ${
                  isWeekPlayed(week) ? 'oynandı' : week.weekNumber === nextWeek ? 'sıradaki hafta' : 'oynanmadı'
                }`}
                aria-pressed={week.weekNumber === selectedWeekNumber}
              >
                {week.weekNumber}
              </button>
            ))}
          </div>
          <p className="legend muted">
            <span className="week-chip played" aria-hidden /> Oynandı <span className="week-chip next" aria-hidden />{' '}
            Sıradaki <span className="week-chip" aria-hidden /> Oynanmadı
          </p>

          {selectedWeek && (
            <WeekCard
              week={selectedWeek}
              weekCount={weeks.length}
              nextWeek={nextWeek}
              busy={busy}
              onPlay={handlePlayWeek}
              onNavigate={setSelectedWeekNumber}
            />
          )}
        </>
      )}
    </section>
  )
}

interface WeekCardProps {
  week: MatchWeek
  weekCount: number
  nextWeek: number | null
  busy: boolean
  onPlay: (weekNumber: number) => void
  onNavigate: (weekNumber: number) => void
}

function WeekCard({ week, weekCount, nextWeek, busy, onPlay, onNavigate }: WeekCardProps) {
  const played = isWeekPlayed(week)
  const isNext = week.weekNumber === nextWeek

  return (
    <div className="card">
      <div className="week-header">
        <button
          className="btn btn-sm"
          onClick={() => onNavigate(week.weekNumber - 1)}
          disabled={week.weekNumber === 1}
          aria-label="Önceki hafta"
        >
          ‹
        </button>
        <h2>Hafta {week.weekNumber}</h2>
        <button
          className="btn btn-sm"
          onClick={() => onNavigate(week.weekNumber + 1)}
          disabled={week.weekNumber === weekCount}
          aria-label="Sonraki hafta"
        >
          ›
        </button>
        <span className={`badge ${played ? 'badge-played' : 'badge-pending'}`}>{played ? 'Oynandı' : 'Oynanmadı'}</span>
        {isNext && (
          <button className="btn btn-primary week-play" onClick={() => onPlay(week.weekNumber)} disabled={busy}>
            {busy ? 'Oynanıyor…' : 'Haftayı Oynat'}
          </button>
        )}
        {!played && !isNext && nextWeek !== null && (
          <span className="week-play muted">
            Önce{' '}
            <button className="link-button" onClick={() => onNavigate(nextWeek)}>
              Hafta {nextWeek}
            </button>{' '}
            oynanmalı
          </span>
        )}
      </div>

      {!played && <p className="legend muted">Olasılıklar takımların güncel güç ve moraline göre hesaplanır.</p>}
      <ul className="match-list">
        {week.matches.map((match) => (
          <MatchRow key={match.id} match={match} />
        ))}
      </ul>
    </div>
  )
}

function MatchRow({ match }: { match: Match }) {
  const homeGoals = match.homeScore ?? 0
  const awayGoals = match.awayScore ?? 0

  const content = (
    <>
      <span className={`match-team match-home${match.played && homeGoals > awayGoals ? ' winner' : ''}`}>
        {match.homeTeamName}
      </span>
      <span className={`score${match.played ? '' : ' score-pending'}`}>
        {match.played ? `${homeGoals} - ${awayGoals}` : 'vs'}
      </span>
      <span className={`match-team${match.played && awayGoals > homeGoals ? ' winner' : ''}`}>
        {match.awayTeamName}
      </span>
    </>
  )

  if (match.played) {
    return (
      <li>
        <Link to={`/matches/${match.id}`} className="match match-link" title="Maç detayı">
          {content}
          <span className="match-more" aria-hidden>
            ›
          </span>
        </Link>
      </li>
    )
  }

  return (
    <li className="match match-upcoming">
      {content}
      <span />
      <div className="match-prob">
        <ProbabilityBar home={match.homeWinProbability} draw={match.drawProbability} away={match.awayWinProbability} />
      </div>
    </li>
  )
}

export default FixturePage
