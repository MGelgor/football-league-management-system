import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { MatchWeek, Standing } from '../api/types'

interface SeasonProgress {
  totalMatches: number
  playedMatches: number
}

function toSeasonProgress(fixture: MatchWeek[]): SeasonProgress {
  const matches = fixture.flatMap((week) => week.matches)
  return { totalMatches: matches.length, playedMatches: matches.filter((match) => match.played).length }
}

function StandingsPage() {
  const [standings, setStandings] = useState<Standing[] | null>(null)
  const [progress, setProgress] = useState<SeasonProgress | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [playing, setPlaying] = useState(false)

  useEffect(() => {
    Promise.all([api.getStandings(), api.getFixture()])
      .then(([table, fixture]) => {
        setStandings(table)
        setProgress(toSeasonProgress(fixture))
      })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  async function handlePlaySeason() {
    setPlaying(true)
    setError(null)
    try {
      const result = await api.playSeason()
      setStandings(result.finalStandings)
      setProgress((current) => current && { ...current, playedMatches: current.totalMatches })
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setPlaying(false)
    }
  }

  const hasFixture = progress !== null && progress.totalMatches > 0
  const seasonFinished = hasFixture && progress.playedMatches === progress.totalMatches
  const champion = seasonFinished ? standings?.[0] : undefined

  return (
    <section>
      <div className="page-header">
        <h1>Puan Durumu</h1>
        {hasFixture && !seasonFinished && (
          <button className="btn btn-primary" onClick={handlePlaySeason} disabled={playing}>
            {playing ? 'Sezon oynanıyor…' : 'Tüm Sezonu Oynat'}
          </button>
        )}
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      {champion && (
        <div className="champion-banner" role="status">
          <span className="champion-trophy" aria-hidden>
            🏆
          </span>
          <div>
            <div className="champion-label">Sezon şampiyonu</div>
            <div className="champion-name">{champion.teamName}</div>
            <div className="champion-stats">
              {champion.points} puan · {champion.won}G {champion.drawn}B {champion.lost}M · Averaj{' '}
              {formatGoalDifference(champion.goalDifference)}
            </div>
          </div>
        </div>
      )}

      {progress && !hasFixture && (
        <p className="alert alert-info">
          Henüz fikstür yok. Maç oynanabilmesi için önce <Link to="/fixture">Fikstür</Link> sayfasından fikstürü
          oluşturun.
        </p>
      )}
      {hasFixture && !seasonFinished && (
        <p className="muted">
          {progress.playedMatches} / {progress.totalMatches} maç oynandı. Kalan haftaları{' '}
          <Link to="/fixture">Fikstür</Link> sayfasından tek tek ya da buradan tek seferde oynatabilirsiniz.
        </p>
      )}

      {standings && standings.length === 0 && <p className="muted">Henüz takım yok.</p>}

      {standings && standings.length > 0 && (
        <div className="table-wrap card">
          <table className="table standings">
            <thead>
              <tr>
                <th className="num">#</th>
                <th>Takım</th>
                <th className="num" title="Oynanan">O</th>
                <th className="num" title="Galibiyet">G</th>
                <th className="num" title="Beraberlik">B</th>
                <th className="num" title="Mağlubiyet">M</th>
                <th className="num" title="Atılan gol">A</th>
                <th className="num" title="Yenilen gol">Y</th>
                <th className="num" title="Averaj">AV</th>
                <th className="num" title="Puan">P</th>
              </tr>
            </thead>
            <tbody>
              {standings.map((row) => (
                <tr key={row.teamId} className={row.teamId === champion?.teamId ? 'champion-row' : undefined}>
                  <td className="num">{row.rank}</td>
                  <td className="strong">
                    {row.teamName}
                    {row.teamId === champion?.teamId && <span aria-label="Şampiyon"> 🏆</span>}
                  </td>
                  <td className="num">{row.played}</td>
                  <td className="num">{row.won}</td>
                  <td className="num">{row.drawn}</td>
                  <td className="num">{row.lost}</td>
                  <td className="num">{row.goalsFor}</td>
                  <td className="num">{row.goalsAgainst}</td>
                  <td className="num">{formatGoalDifference(row.goalDifference)}</td>
                  <td className="num strong">{row.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="legend muted">Sıralama: Puan → Averaj → Atılan gol</p>
    </section>
  )
}

function formatGoalDifference(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

export default StandingsPage
