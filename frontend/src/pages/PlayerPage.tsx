import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { PlayerProfile } from '../api/types'
import PlayerStatus from '../components/PlayerStatus'
import Trend from '../components/Trend'
import { COMPETITION_LABELS, POSITION_LABELS, roundLabel } from '../labels'

function PlayerPage() {
  const playerId = Number(useParams().playerId)
  const [player, setPlayer] = useState<PlayerProfile | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getPlayerProfile(playerId)
      .then(setPlayer)
      .catch((e) => setError(errorMessage(e)))
  }, [playerId])

  if (error) {
    return <p className="alert alert-error">{error}</p>
  }
  if (!player) {
    return <p className="muted">Yükleniyor…</p>
  }

  const totals = player.seasons.reduce(
    (sum, line) => ({
      appearances: sum.appearances + line.appearances,
      goals: sum.goals + line.goals,
      assists: sum.assists + line.assists,
      motm: sum.motm + line.playerOfTheMatch,
    }),
    { appearances: 0, goals: 0, assists: 0, motm: 0 },
  )

  return (
    <section>
      <p>
        <Link to={`/teams/${player.teamId}`}>← {player.teamName}</Link>
      </p>

      <div className="card team-hero">
        <span className="shirt-number" aria-label="Forma numarası">
          {player.shirtNumber}
        </span>
        <div>
          <h1>
            {player.name}{' '}
            <PlayerStatus suspendedMatches={player.suspendedMatches} injuredMatches={player.injuredMatches} />
            {player.retired && <span className="status status-retired">Emekli</span>}
          </h1>
          <div className="muted">
            <span className={`pos pos-${player.position.toLowerCase()}`}>{POSITION_LABELS[player.position]}</span> ·{' '}
            <Link to={`/teams/${player.teamId}`}>{player.teamName}</Link>
            {!player.teamActive && ' (ligde değil)'}
          </div>
        </div>
        <dl className="hero-stats">
          <div>
            <dt>Yaş</dt>
            <dd>{player.age}</dd>
          </div>
          <div>
            <dt>Güç</dt>
            <dd>
              {player.strength} <Trend value={player.lastStrengthChange} title="Sezon sonu gelişimi" />
            </dd>
          </div>
          <div>
            <dt>Maç / Gol / Asist</dt>
            <dd>
              {totals.appearances} / {totals.goals} / {totals.assists}
            </dd>
          </div>
          <div>
            <dt>Maçın oyuncusu</dt>
            <dd>⭐ {totals.motm}</dd>
          </div>
        </dl>
      </div>

      <div className="card">
        <h2>Sezon sezon</h2>
        {player.seasons.length === 0 ? (
          <p className="muted">Henüz maça çıkmadı.</p>
        ) : (
          <div className="table-wrap">
            <table className="table compact">
              <thead>
                <tr>
                  <th>Sezon</th>
                  <th>Turnuva</th>
                  <th className="num" title="Maç">MS</th>
                  <th className="num" title="Dakika">Dk</th>
                  <th className="num" title="Gol">G</th>
                  <th className="num" title="Asist">A</th>
                  <th className="num" title="Gol / maç">G/M</th>
                  <th className="num" title="Sarı kart">🟨</th>
                  <th className="num" title="Kırmızı kart">🟥</th>
                  <th className="num" title="Ortalama reyting">Ort</th>
                  <th className="num" title="Maçın oyuncusu">⭐</th>
                </tr>
              </thead>
              <tbody>
                {player.seasons.map((line) => (
                  <tr key={`${line.seasonNumber}-${line.competition}`}>
                    <td className="strong">Sezon {line.seasonNumber}</td>
                    <td>{COMPETITION_LABELS[line.competition]}</td>
                    <td className="num">{line.appearances}</td>
                    <td className="num">{line.minutes}</td>
                    <td className="num">{line.goals}</td>
                    <td className="num">{line.assists}</td>
                    <td className="num">{line.appearances ? (line.goals / line.appearances).toFixed(2) : '–'}</td>
                    <td className="num">{line.yellowCards || ''}</td>
                    <td className="num">{line.redCards || ''}</td>
                    <td className="num">{line.averageRating?.toFixed(2) ?? '–'}</td>
                    <td className="num">{line.playerOfTheMatch || ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Goller ve asistler</h2>
        {player.goals.length === 0 ? (
          <p className="muted">Henüz gol ya da asist yok.</p>
        ) : (
          <ul className="goal-list">
            {player.goals.toReversed().map((goal, index) => (
              <li key={index}>
                <span className="goal-icon" aria-hidden>
                  {goal.type === 'GOAL' ? '⚽' : '🅰️'}
                </span>
                <span className="muted goal-when">
                  Sezon {goal.seasonNumber} · {roundLabel(goal.competition, goal.weekNumber, goal.cupRound)} ·{' '}
                  {goal.minute}'
                </span>
                <Link to={`/matches/${goal.matchId}`}>
                  {goal.home ? 'vs' : '@'} {goal.opponentName} ({goal.score})
                </Link>
                {goal.partnerName && (
                  <span className="muted">
                    {' '}
                    · {goal.type === 'GOAL' ? 'asist' : 'gol'}: {goal.partnerName}
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

export default PlayerPage
