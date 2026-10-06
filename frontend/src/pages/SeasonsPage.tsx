import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { RecordEntry, Season, TeamRef } from '../api/types'

interface TitleCount {
  name: string
  league: number
  cup: number
}

function countTitles(seasons: Season[]): TitleCount[] {
  const counts = new Map<string, TitleCount>()
  const add = (name: string | null, key: 'league' | 'cup') => {
    if (!name) {
      return
    }
    const entry = counts.get(name) ?? { name, league: 0, cup: 0 }
    entry[key]++
    counts.set(name, entry)
  }
  seasons.forEach((season) => {
    add(season.championName, 'league')
    add(season.cupWinnerName, 'cup')
  })
  return [...counts.values()].sort(
    (a, b) => b.league - a.league || b.cup - a.cup || a.name.localeCompare(b.name, 'tr'),
  )
}

function SeasonsPage() {
  const [seasons, setSeasons] = useState<Season[] | null>(null)
  const [records, setRecords] = useState<RecordEntry[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([api.getSeasons(), api.getRecords()])
      .then(([seasonList, recordList]) => {
        setSeasons(seasonList)
        setRecords(recordList)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  const titles = seasons ? countTitles(seasons) : []

  return (
    <section>
      <h1>Sezonlar</h1>
      {error && <p className="alert alert-error">{error}</p>}

      {seasons && seasons.length === 0 && (
        <p className="alert alert-info">
          Henüz sezon yok. İlk sezon <Link to="/fixture">Fikstür</Link> sayfasından fikstür oluşturulunca başlar.
        </p>
      )}

      {titles.length > 0 && (
        <div className="card">
          <h2>Şampiyonluklar</h2>
          <ul className="title-list">
            {titles.map((title) => (
              <li key={title.name}>
                <strong>{title.name}</strong>{' '}
                {title.league > 0 && <span title="Lig şampiyonluğu">🏆 × {title.league}</span>}{' '}
                {title.cup > 0 && <span title="Kupa">🥇 × {title.cup}</span>}
              </li>
            ))}
          </ul>
          <p className="legend muted">🏆 lig şampiyonluğu · 🥇 kupa</p>
        </div>
      )}

      {records.length > 0 && (
        <div className="card">
          <h2>Tarihsel rekorlar</h2>
          <p className="muted legend">Tüm sezonların lig maçlarından</p>
          <div className="records">
            {records.map((record) => (
              <div key={record.title} className="record">
                <div className="record-title">{record.title}</div>
                <div className="record-value">{record.value}</div>
                <div className="record-holder">
                  <RecordHolder record={record} />
                </div>
                {record.detail && <div className="muted record-detail">{record.detail}</div>}
              </div>
            ))}
          </div>
        </div>
      )}

      {seasons && seasons.length > 0 && (
        <div className="table-wrap card">
          <table className="table seasons-table">
            <thead>
              <tr>
                <th>Sezon</th>
                <th>Durum</th>
                <th>Şampiyon</th>
                <th>Kupa</th>
                <th>Düşen / Çıkan</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {seasons.map((season, index) => (
                <tr key={season.id}>
                  <td className="strong">Sezon {season.seasonNumber}</td>
                  <td>
                    {season.finished ? (
                      <span className="badge badge-played">Tamamlandı</span>
                    ) : (
                      <span className="badge badge-pending">
                        Devam ediyor · {season.playedMatches}/{season.totalMatches} maç
                      </span>
                    )}
                  </td>
                  <td>{season.championName ? `🏆 ${season.championName}` : <span className="muted">–</span>}</td>
                  <td>{season.cupWinnerName ? `🥇 ${season.cupWinnerName}` : <span className="muted">–</span>}</td>
                  <td className="team-changes">
                    <TeamChanges label="↓" className="sub-out" teams={season.relegated} />
                    <TeamChanges label="↑" className="sub-in" teams={season.promoted} />
                  </td>
                  <td className="row-actions">
                    <Link to={index === 0 ? '/standings' : `/standings?season=${season.id}`}>Puan tablosu →</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function RecordHolder({ record }: { record: RecordEntry }) {
  if (record.matchId) {
    return <Link to={`/matches/${record.matchId}`}>{record.holder}</Link>
  }
  if (record.playerId) {
    return <Link to={`/players/${record.playerId}`}>{record.holder}</Link>
  }
  if (record.teamId) {
    return <Link to={`/teams/${record.teamId}`}>{record.holder}</Link>
  }
  return <>{record.holder}</>
}

function TeamChanges({ label, className, teams }: { label: string; className: string; teams: TeamRef[] }) {
  if (teams.length === 0) {
    return null
  }
  return (
    <div>
      <span className={className}>{label}</span>{' '}
      {teams.map((team, index) => (
        <span key={team.id}>
          {index > 0 && ', '}
          <Link to={`/teams/${team.id}`} className="team-link">
            {team.name}
          </Link>
        </span>
      ))}
    </div>
  )
}

export default SeasonsPage
