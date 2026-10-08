import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Season, Standing } from '../api/types'
import ErrorAlert from '../components/ErrorAlert'
import FormBadges from '../components/FormBadges'
import PlayerStatsTable from '../components/PlayerStatsTable'
import Trend from '../components/Trend'
import { ZONE_LABELS } from '../labels'

type View = 'teams' | 'players'

function StandingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedSeasonId = searchParams.get('season') ? Number(searchParams.get('season')) : undefined
  const view: View = searchParams.get('view') === 'players' ? 'players' : 'teams'

  // Sezon ve sekme URL'de tutulur (?season=ID&view=players), biri değişince diğeri korunur
  function updateParams(changes: Record<string, string | null>) {
    const next = new URLSearchParams(searchParams)
    Object.entries(changes).forEach(([key, value]) => (value === null ? next.delete(key) : next.set(key, value)))
    setSearchParams(next)
  }

  const [standings, setStandings] = useState<Standing[] | null>(null)
  const [seasons, setSeasons] = useState<Season[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [playing, setPlaying] = useState(false)

  useEffect(() => {
    Promise.all([api.getStandings(requestedSeasonId), api.getSeasons()])
      .then(([table, seasonList]) => {
        setStandings(table)
        setSeasons(seasonList)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [requestedSeasonId])

  async function handlePlaySeason(auto = false) {
    setPlaying(true)
    setError(null)
    try {
      const result = await api.playSeason(auto)
      setStandings(result.finalStandings)
      setSeasons(await api.getSeasons())
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setPlaying(false)
    }
  }

  const currentSeason = seasons?.[0]
  const season = requestedSeasonId === undefined ? currentSeason : seasons?.find((s) => s.id === requestedSeasonId)
  const isCurrent = season !== undefined && season.id === currentSeason?.id
  const champion = season?.finished ? standings?.[0] : undefined
  // Sıra değişimi yalnızca devam eden sezonda anlamlı (son haftaya göre)
  const showRankChange = isCurrent && !season?.finished

  return (
    <section>
      <div className="page-header">
        <h1>Puan Durumu</h1>
        <div className="page-actions">
          {seasons && seasons.length > 1 && (
            <select
              className="select"
              value={season?.id ?? ''}
              onChange={(e) =>
                updateParams({ season: e.target.value === String(currentSeason?.id) ? null : e.target.value })
              }
              aria-label="Sezon seç"
            >
              {seasons.map((s) => (
                <option key={s.id} value={s.id}>
                  Sezon {s.seasonNumber}
                  {s.finished ? '' : ' (devam ediyor)'}
                </option>
              ))}
            </select>
          )}
          {isCurrent && !season.finished && (
            <button className="btn btn-primary" onClick={() => handlePlaySeason()} disabled={playing}>
              {playing ? 'Sezon oynanıyor…' : 'Tüm Sezonu Oynat'}
            </button>
          )}
        </div>
      </div>

      {error && <ErrorAlert message={error} onAuto={() => handlePlaySeason(true)} />}

      {champion && season && (
        <div className="champion-banner" role="status">
          <span className="champion-trophy" aria-hidden>
            🏆
          </span>
          <div>
            <div className="champion-label">Sezon {season.seasonNumber} şampiyonu</div>
            <div className="champion-name">{champion.teamName}</div>
            <div className="champion-stats">
              {champion.points} puan · {champion.won}G {champion.drawn}B {champion.lost}M · Averaj{' '}
              {formatGoalDifference(champion.goalDifference)}
            </div>
          </div>
          {isCurrent && (
            <Link to="/fixture" className="btn champion-action">
              Yeni sezona geç →
            </Link>
          )}
        </div>
      )}

      {seasons && seasons.length === 0 && (
        <p className="alert alert-info">
          Henüz fikstür yok. Maç oynanabilmesi için önce <Link to="/fixture">Fikstür</Link> sayfasından fikstürü
          oluşturun.
        </p>
      )}
      {season && !season.finished && (
        <p className="muted">
          Sezon {season.seasonNumber}: {season.playedMatches} / {season.totalMatches} maç oynandı. Kalan haftaları{' '}
          <Link to="/fixture">Fikstür</Link> sayfasından tek tek ya da buradan tek seferde oynatabilirsiniz.
        </p>
      )}

      <div className="tabs" role="tablist">
        <button
          role="tab"
          aria-selected={view === 'teams'}
          className={`tab${view === 'teams' ? ' active' : ''}`}
          onClick={() => updateParams({ view: null })}
        >
          Takımlar
        </button>
        <button
          role="tab"
          aria-selected={view === 'players'}
          className={`tab${view === 'players' ? ' active' : ''}`}
          onClick={() => updateParams({ view: 'players' })}
        >
          Oyuncular
        </button>
      </div>

      {view === 'players' && seasons && (
        // Sezon oynatılınca tablo yenilensin diye oynanan maç sayısı da key'de
        <PlayerStatsTable key={`${season?.id}-${season?.playedMatches}`} seasonId={requestedSeasonId} />
      )}

      {view === 'teams' && standings && standings.length === 0 && <p className="muted">Henüz takım yok.</p>}

      {view === 'teams' && standings && standings.length > 0 && (
        <div className="table-wrap card">
          <table className="table standings">
            <thead>
              <tr>
                <th className="num">#</th>
                {showRankChange && <th aria-label="Sıra değişimi" />}
                <th>Takım</th>
                <th className="num" title="Oynanan">O</th>
                <th className="num" title="Galibiyet">G</th>
                <th className="num" title="Beraberlik">B</th>
                <th className="num" title="Mağlubiyet">M</th>
                <th className="num" title="Atılan gol">A</th>
                <th className="num" title="Yenilen gol">Y</th>
                <th className="num" title="Averaj">AV</th>
                <th className="num" title="Puan">P</th>
                <th className="form-col" title="Son 5 maç (eskiden yeniye)">Form</th>
              </tr>
            </thead>
            <tbody>
              {standings.map((row) => (
                <tr
                  key={row.teamId}
                  className={[row.teamId === champion?.teamId && 'champion-row', row.zone && `zone-${row.zone}`]
                    .filter(Boolean)
                    .join(' ')}
                  title={row.zone ? ZONE_LABELS[row.zone] : undefined}
                >
                  <td className="num">{row.rank}</td>
                  {showRankChange && (
                    <td className="rank-change">
                      <Trend value={row.rankChange} title="Geçen haftaya göre sıra değişimi" />
                    </td>
                  )}
                  <td className="strong team-cell">
                    <Link to={`/teams/${row.teamId}`} className="team-link">
                      {row.teamName}
                    </Link>
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
                  <td className="form-col">
                    <FormBadges form={row.form} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {view === 'teams' && (
        <>
          <p className="legend muted zone-legend">
            <span className="zone-swatch zone-CHAMPIONS_LEAGUE" /> {ZONE_LABELS.CHAMPIONS_LEAGUE}
            <span className="zone-swatch zone-EUROPA_LEAGUE" /> {ZONE_LABELS.EUROPA_LEAGUE}
            <span className="zone-swatch zone-RELEGATION" /> {ZONE_LABELS.RELEGATION} (4 büyükler düşmez)
          </p>
          <p className="legend muted">
            Sıralama: Puan → Averaj → Atılan gol{showRankChange && ' · ▲▼ geçen haftaya göre sıra değişimi'}
          </p>
        </>
      )}
    </section>
  )
}

function formatGoalDifference(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

export default StandingsPage
