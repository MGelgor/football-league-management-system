import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { PlayerStats } from '../api/types'
import { POSITION_LABELS } from '../labels'

type SortKey = 'goals' | 'assists' | 'appearances' | 'averageRating' | 'playerOfTheMatch' | 'yellowCards' | 'redCards'

const SORT_COLUMNS: { key: SortKey; label: string; title: string }[] = [
  { key: 'appearances', label: 'MS', title: 'Oynadığı maç' },
  { key: 'goals', label: 'G', title: 'Gol' },
  { key: 'assists', label: 'A', title: 'Asist' },
  { key: 'averageRating', label: 'Ort', title: 'Ortalama reyting' },
  { key: 'playerOfTheMatch', label: '⭐', title: 'Maçın oyuncusu' },
  { key: 'yellowCards', label: '🟨', title: 'Sarı kart' },
  { key: 'redCards', label: '🟥', title: 'Kırmızı kart' },
]

const SECONDARY: Record<SortKey, SortKey> = {
  goals: 'assists',
  assists: 'goals',
  appearances: 'goals',
  averageRating: 'appearances',
  playerOfTheMatch: 'averageRating',
  yellowCards: 'redCards',
  redCards: 'yellowCards',
}

// Reyting sıralamasında az maç oynayanlar öne çıkmasın diye en az bu kadar maç gerekir
const MIN_APPEARANCES_FOR_RATING = 5

function value(row: PlayerStats, key: SortKey) {
  if (key === 'averageRating') {
    return row.appearances >= MIN_APPEARANCES_FOR_RATING ? (row.averageRating ?? 0) : 0
  }
  return row[key]
}

/** Puan durumundaki "Oyuncular" sekmesi: sezonun lig istatistikleri, başlığa tıklayınca o sütuna göre sıralanır. */
function PlayerStatsTable({ seasonId }: { seasonId?: number }) {
  const [stats, setStats] = useState<PlayerStats[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [sortKey, setSortKey] = useState<SortKey>('goals')

  useEffect(() => {
    api
      .getPlayerStats(seasonId)
      .then(setStats)
      .catch((e) => setError(errorMessage(e)))
  }, [seasonId])

  if (error) {
    return <p className="alert alert-error">{error}</p>
  }
  if (!stats) {
    return <p className="muted">Yükleniyor…</p>
  }
  if (stats.length === 0) {
    return <p className="muted">Bu sezonda henüz maç oynanmadı. Bir hafta oynatınca istatistikler oluşur.</p>
  }

  const secondary = SECONDARY[sortKey]
  const sorted = stats.toSorted(
    (a, b) =>
      value(b, sortKey) - value(a, sortKey) ||
      value(b, secondary) - value(a, secondary) ||
      a.playerName.localeCompare(b.playerName, 'tr'),
  )

  return (
    <>
      <div className="table-wrap card">
        <table className="table player-stats">
          <thead>
            <tr>
              <th className="num">#</th>
              <th>Oyuncu</th>
              <th>Takım</th>
              <th>Mevki</th>
              {SORT_COLUMNS.map((column) => (
                <th key={column.key} className="num" title={`${column.title} — sıralamak için tıklayın`}>
                  <button
                    className={`sort-button${sortKey === column.key ? ' active' : ''}`}
                    onClick={() => setSortKey(column.key)}
                    aria-pressed={sortKey === column.key}
                  >
                    {column.label}
                    {sortKey === column.key && ' ▾'}
                  </button>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.map((row, index) => (
              <tr key={row.playerId}>
                <td className="num">{index + 1}</td>
                <td className="strong player-cell">
                  <Link to={`/players/${row.playerId}`} className="team-link">
                    {row.playerName}
                  </Link>
                </td>
                <td>
                  <Link to={`/teams/${row.teamId}`} className="team-link">
                    {row.teamName}
                  </Link>
                </td>
                <td>
                  <span className={`pos pos-${row.position.toLowerCase()}`}>{POSITION_LABELS[row.position]}</span>
                </td>
                {SORT_COLUMNS.map((column) => (
                  <td key={column.key} className={`num${sortKey === column.key ? ' strong' : ''}`}>
                    {column.key === 'averageRating' ? (row.averageRating?.toFixed(2) ?? '') : row[column.key] || ''}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="legend muted">
        Lig maçları. Sıralamak için başlıklara tıklayın; reyting sıralamasında en az {MIN_APPEARANCES_FOR_RATING} maç
        oynayanlar öne alınır.
      </p>
    </>
  )
}

export default PlayerStatsTable
