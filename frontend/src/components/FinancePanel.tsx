import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Finances } from '../api/types'
import { FINANCE_LABELS, formatMoney, POSITION_SHORT } from '../labels'

/** Takımın bütçesi, bu sezonun gelir / giderleri, maaş yükü ve sözleşmeler. */
function FinancePanel({ teamId, currentSeason }: { teamId: number; currentSeason: number | null }) {
  const [finances, setFinances] = useState<Finances | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showEntries, setShowEntries] = useState(false)

  useEffect(() => {
    api
      .getFinances(teamId)
      .then(setFinances)
      .catch((e) => setError(errorMessage(e)))
  }, [teamId])

  if (error) {
    return <p className="alert alert-error">{error}</p>
  }
  if (!finances) {
    return null
  }
  const net = finances.income - finances.expenses
  return (
    <div className="card">
      <h2>Finans{finances.seasonNumber !== null && <span className="muted"> · Sezon {finances.seasonNumber}</span>}</h2>
      <dl className="finance-summary">
        <div>
          <dt>Bütçe</dt>
          <dd className={finances.budget < 0 ? 'negative' : undefined}>{formatMoney(finances.budget)}</dd>
        </div>
        <div>
          <dt>Sezon geliri</dt>
          <dd className="positive">{formatMoney(finances.income)}</dd>
        </div>
        <div>
          <dt>Sezon gideri</dt>
          <dd className="negative">{formatMoney(-finances.expenses)}</dd>
        </div>
        <div>
          <dt>Net</dt>
          <dd className={net >= 0 ? 'positive' : 'negative'}>{formatMoney(net)}</dd>
        </div>
        <div>
          <dt>Haftalık maaş</dt>
          <dd>{formatMoney(finances.weeklyWageBill)}</dd>
        </div>
        <div>
          <dt>Kadro değeri</dt>
          <dd>{formatMoney(finances.squadValue)}</dd>
        </div>
      </dl>

      {finances.totals.length > 0 && (
        <ul className="finance-totals">
          {finances.totals.map((total) => (
            <li key={total.type}>
              <span>{FINANCE_LABELS[total.type]}</span>
              <span className={total.amount >= 0 ? 'positive' : 'negative'}>{formatMoney(total.amount)}</span>
            </li>
          ))}
        </ul>
      )}

      <h3>Sözleşmeler</h3>
      <div className="table-wrap">
        <table className="table compact">
          <thead>
            <tr>
              <th>Oyuncu</th>
              <th>Mevki</th>
              <th className="num">Yaş</th>
              <th className="num">Güç</th>
              <th className="num">Değer</th>
              <th className="num">Maaş / hafta</th>
              <th className="num">Bitiş</th>
            </tr>
          </thead>
          <tbody>
            {finances.contracts.map((contract) => {
              const ending = currentSeason !== null && contract.contractUntil === currentSeason
              return (
                <tr key={contract.playerId}>
                  <td>
                    <Link to={`/players/${contract.playerId}`} className="team-link">
                      {contract.name}
                    </Link>
                  </td>
                  <td>{POSITION_SHORT[contract.position]}</td>
                  <td className="num">{contract.age}</td>
                  <td className="num">{contract.strength}</td>
                  <td className="num">{formatMoney(contract.marketValue)}</td>
                  <td className="num">{formatMoney(contract.wage)}</td>
                  <td
                    className={`num${ending ? ' contract-ending' : ''}`}
                    title={ending ? 'Bu sezon bitiyor' : undefined}
                  >
                    {contract.contractUntil !== null ? `Sezon ${contract.contractUntil}` : '–'}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {finances.recentEntries.length > 0 && (
        <>
          <button className="link-button section-gap" onClick={() => setShowEntries((shown) => !shown)}>
            {showEntries ? 'Son kayıtları gizle' : 'Son kayıtları göster'}
          </button>
          {showEntries && (
            <ul className="finance-totals">
              {finances.recentEntries.map((entry, index) => (
                <li key={index}>
                  <span>
                    <span className="muted">
                      {entry.weekNumber !== null && entry.weekNumber <= 100 && `H${entry.weekNumber} · `}
                    </span>
                    {entry.description}
                  </span>
                  <span className={entry.amount >= 0 ? 'positive' : 'negative'}>{formatMoney(entry.amount)}</span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      <p className="legend muted">
        Gelir: iç saha bilet geliri, sezon sonu lig ödülü (1. €20M … son €2M), kupa ödülleri. Gider: her lig haftası
        maaşlar. Sözleşmesi biten oyuncu sezon sonunda ya uzatılır ya da serbest kalır.
      </p>
    </div>
  )
}

export default FinancePanel
