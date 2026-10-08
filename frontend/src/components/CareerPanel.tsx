import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Career } from '../api/types'

/** Menajer kariyeri: takım takım dönemler, galibiyet oranı, kupalar. */
function CareerPanel() {
  const [career, setCareer] = useState<Career | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getCareer()
      .then(setCareer)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  if (error) {
    return <p className="alert alert-error">{error}</p>
  }
  if (!career) {
    return null
  }
  const winRate = career.matches === 0 ? 0 : Math.round((career.wins / career.matches) * 100)
  return (
    <div className="card">
      <h2>Kariyer · {career.managerName}</h2>
      <dl className="finance-summary">
        <div>
          <dt>Maç</dt>
          <dd>{career.matches}</dd>
        </div>
        <div>
          <dt>G / B / M</dt>
          <dd>
            {career.wins} / {career.draws} / {career.losses}
          </dd>
        </div>
        <div>
          <dt>Galibiyet</dt>
          <dd>%{winRate}</dd>
        </div>
        <div>
          <dt>Şampiyonluk</dt>
          <dd>🏆 {career.leagueTitles}</dd>
        </div>
        <div>
          <dt>Kupa</dt>
          <dd>🥇 {career.cups}</dd>
        </div>
      </dl>
      <div className="table-wrap">
        <table className="table compact">
          <thead>
            <tr>
              <th>Takım</th>
              <th>Dönem</th>
              <th className="num">Maç</th>
              <th className="num">G</th>
              <th className="num">B</th>
              <th className="num">M</th>
              <th className="num">🏆</th>
              <th className="num">🥇</th>
              <th>Ayrılış</th>
            </tr>
          </thead>
          <tbody>
            {career.spells.map((spell, index) => (
              <tr key={index}>
                <td>
                  <Link to={`/teams/${spell.teamId}`} className="team-link">
                    {spell.teamName}
                  </Link>
                </td>
                <td>
                  Sezon {spell.startSeason}
                  {spell.endSeason !== null ? ` – ${spell.endSeason}` : ' – …'}
                </td>
                <td className="num">{spell.matches}</td>
                <td className="num">{spell.wins}</td>
                <td className="num">{spell.draws}</td>
                <td className="num">{spell.losses}</td>
                <td className="num">{spell.leagueTitles || ''}</td>
                <td className="num">{spell.cups || ''}</td>
                <td className="muted">{spell.endReason ?? 'Görevde'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default CareerPanel
