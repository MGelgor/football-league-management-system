import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Match, MatchWeek } from '../api/types'

function isWeekPlayed(week: MatchWeek) {
  return week.matches.every((match) => match.played)
}

function nextWeekToPlay(weeks: MatchWeek[]) {
  return (weeks.find((week) => !isWeekPlayed(week)) ?? weeks.at(-1))?.weekNumber ?? null
}

function FixturePage() {
  const [weeks, setWeeks] = useState<MatchWeek[] | null>(null)
  const [selectedWeekNumber, setSelectedWeekNumber] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmReset, setConfirmReset] = useState(false)

  function showFixture(fixture: MatchWeek[]) {
    setWeeks(fixture)
    setSelectedWeekNumber(nextWeekToPlay(fixture))
  }

  useEffect(() => {
    api
      .getFixture()
      .then(showFixture)
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

  const handleGenerate = () => run(async () => showFixture(await api.generateFixture()))

  const handlePlayWeek = (weekNumber: number) =>
    run(async () => {
      const playedWeek = await api.playWeek(weekNumber)
      setWeeks((current) => current?.map((week) => (week.weekNumber === weekNumber ? playedWeek : week)) ?? null)
    })

  const handleReset = () =>
    run(async () => {
      await api.resetFixture()
      setConfirmReset(false)
      showFixture([])
    })

  const selectedWeek = weeks?.find((week) => week.weekNumber === selectedWeekNumber)
  const playedCount = weeks?.filter(isWeekPlayed).length ?? 0
  const hasFixture = weeks !== null && weeks.length > 0

  return (
    <section>
      <div className="page-header">
        <h1>Fikstür</h1>
        {hasFixture && (
          <div className="page-actions">
            {confirmReset ? (
              <>
                <span className="muted">Tüm maçlar ve sonuçlar silinecek.</span>
                <button className="btn btn-danger" onClick={handleReset} disabled={busy}>
                  Evet, sıfırla
                </button>
                <button className="btn" onClick={() => setConfirmReset(false)} disabled={busy}>
                  Vazgeç
                </button>
              </>
            ) : (
              <button className="btn btn-danger-outline" onClick={() => setConfirmReset(true)} disabled={busy}>
                Fikstürü sıfırla
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
          {playedCount === weeks.length && (
            <p className="alert alert-success">
              Sezon tamamlandı! Şampiyonu görmek için <Link to="/standings">Puan Durumu</Link> sayfasına geçin.
            </p>
          )}

          <div className="week-strip" aria-label="Haftalar">
            {weeks.map((week) => (
              <button
                key={week.weekNumber}
                className={[
                  'week-chip',
                  isWeekPlayed(week) && 'played',
                  week.weekNumber === selectedWeekNumber && 'selected',
                ]
                  .filter(Boolean)
                  .join(' ')}
                onClick={() => setSelectedWeekNumber(week.weekNumber)}
                title={`Hafta ${week.weekNumber} — ${isWeekPlayed(week) ? 'oynandı' : 'oynanmadı'}`}
                aria-pressed={week.weekNumber === selectedWeekNumber}
              >
                {week.weekNumber}
              </button>
            ))}
          </div>
          <p className="legend muted">
            <span className="week-chip played" aria-hidden /> Oynandı <span className="week-chip" aria-hidden /> Oynanmadı
          </p>

          {selectedWeek && (
            <WeekCard
              week={selectedWeek}
              weekCount={weeks.length}
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
  busy: boolean
  onPlay: (weekNumber: number) => void
  onNavigate: (weekNumber: number) => void
}

function WeekCard({ week, weekCount, busy, onPlay, onNavigate }: WeekCardProps) {
  const played = isWeekPlayed(week)

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
        {!played && (
          <button className="btn btn-primary week-play" onClick={() => onPlay(week.weekNumber)} disabled={busy}>
            {busy ? 'Oynanıyor…' : 'Haftayı Oynat'}
          </button>
        )}
      </div>

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

  return (
    <li className="match">
      <span className={`match-team match-home${match.played && homeGoals > awayGoals ? ' winner' : ''}`}>
        {match.homeTeamName}
      </span>
      <span className={`score${match.played ? '' : ' score-pending'}`}>
        {match.played ? `${homeGoals} - ${awayGoals}` : 'vs'}
      </span>
      <span className={`match-team${match.played && awayGoals > homeGoals ? ' winner' : ''}`}>
        {match.awayTeamName}
      </span>
    </li>
  )
}

export default FixturePage
