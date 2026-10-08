import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Referee } from '../api/types'

/** Hakemler: sertlik ve maç başına kart / penaltı istatistikleri (tüm sezonlar, lig + kupa). */
function RefereesPage() {
  const [referees, setReferees] = useState<Referee[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getReferees()
      .then(setReferees)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  return (
    <section>
      <p>
        <Link to="/seasons">← Sezonlar</Link>
      </p>
      <h1>Hakemler</h1>
      {error && <p className="alert alert-error">{error}</p>}
      <p className="muted legend">
        Sertlik (1–10) kart ve penaltı olasılığını değiştirir: 10 puanlık hakem, 1 puanlık hakeme göre yaklaşık iki kat
        kart gösterir. Hakemler her hafta maçlara rastgele atanır.
      </p>
      {referees && (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Hakem</th>
                <th>Sertlik</th>
                <th className="num">Maç</th>
                <th className="num" title="Sarı kart">
                  🟨
                </th>
                <th className="num" title="Kırmızı kart">
                  🟥
                </th>
                <th className="num">Penaltı</th>
                <th className="num">🟨 / maç</th>
                <th className="num">🟥 / maç</th>
              </tr>
            </thead>
            <tbody>
              {referees.map((referee) => (
                <tr key={referee.id}>
                  <td className="strong">{referee.name}</td>
                  <td>
                    <span className="strictness" aria-label={`${referee.strictness} / 10`}>
                      <span className="strictness-bar" style={{ width: `${referee.strictness * 10}%` }} />
                    </span>{' '}
                    {referee.strictness}
                  </td>
                  <td className="num">{referee.matches}</td>
                  <td className="num">{referee.yellowCards}</td>
                  <td className="num">{referee.redCards}</td>
                  <td className="num">{referee.penalties}</td>
                  <td className="num">{referee.yellowCardsPerMatch.toFixed(2)}</td>
                  <td className="num">{referee.redCardsPerMatch.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

export default RefereesPage
