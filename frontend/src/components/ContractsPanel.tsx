import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { ActionResult, Player } from '../api/types'
import { formatMoney, POSITION_SHORT } from '../labels'

interface ContractsPanelProps {
  teamId: number
  seasonNumber: number | null
  transferWindowOpen: boolean
  onChanged: () => void
}

/** Yönetilen takımın sözleşmeleri: uzatma pazarlığı ve (transfer penceresinde) satışa çıkarma. */
function ContractsPanel({ teamId, seasonNumber, transferWindowOpen, onChanged }: ContractsPanelProps) {
  const [players, setPlayers] = useState<Player[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const reload = () =>
    api
      .getPlayers(teamId)
      .then((list) => setPlayers(list.toSorted((a, b) => (a.contractUntil ?? 99) - (b.contractUntil ?? 99))))
      .catch((e) => setError(errorMessage(e)))

  useEffect(() => {
    api
      .getPlayers(teamId)
      .then((list) => setPlayers(list.toSorted((a, b) => (a.contractUntil ?? 99) - (b.contractUntil ?? 99))))
      .catch((e) => setError(errorMessage(e)))
  }, [teamId])

  return (
    <div className="card">
      <h2>Sözleşmeler</h2>
      {error && <p className="alert alert-error">{error}</p>}
      <p className="legend muted">
        Oyuncu güncel değerine göre maaş ister (genç ve yıldızlar daha fazla). İsteğin %95'i kabul, %80'i karşı istek
        getirir. Bu sezon biten sözleşmeler uzatılmazsa oyuncu sezon sonunda serbest kalır.
        {transferWindowOpen &&
          ' Transfer penceresi açık: oyuncuyu satışa çıkarırsan alıcılar gelen kutuna teklif gönderir.'}
      </p>
      <div className="table-wrap">
        <table className="table compact">
          <thead>
            <tr>
              <th>Oyuncu</th>
              <th>Mevki</th>
              <th className="num">Yaş</th>
              <th className="num">Güç</th>
              <th className="num">Değer</th>
              <th className="num">Maaş</th>
              <th className="num">Bitiş</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {players?.map((player) => (
              <ContractRow
                key={player.id}
                player={player}
                ending={seasonNumber !== null && player.contractUntil === seasonNumber}
                transferWindowOpen={transferWindowOpen}
                onChanged={() => {
                  reload()
                  onChanged()
                }}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

interface ContractRowProps {
  player: Player
  ending: boolean
  transferWindowOpen: boolean
  onChanged: () => void
}

function ContractRow({ player, ending, transferWindowOpen, onChanged }: ContractRowProps) {
  const [open, setOpen] = useState(false)
  const [wage, setWage] = useState(String(Math.round((player.wage ?? 0) / 100) / 10))
  const [seasons, setSeasons] = useState('2')
  const [result, setResult] = useState<ActionResult | null>(null)
  const [busy, setBusy] = useState(false)

  async function run(action: () => Promise<ActionResult>) {
    setBusy(true)
    try {
      const response = await action()
      setResult(response)
      if (response.status === 'ACCEPTED' || response.status === 'DONE') {
        onChanged()
      }
    } catch (e) {
      setResult({ status: 'REJECTED', amount: null, message: errorMessage(e) })
    } finally {
      setBusy(false)
    }
  }

  const offer = (amount: number) => run(() => api.renewContract(player.id, amount, Number(seasons)))

  return (
    <>
      <tr>
        <td>
          <Link to={`/players/${player.id}`} className="team-link">
            {player.name}
          </Link>
        </td>
        <td>{POSITION_SHORT[player.position]}</td>
        <td className="num">{player.age}</td>
        <td className="num">{player.strength}</td>
        <td className="num">{formatMoney(player.marketValue)}</td>
        <td className="num">{formatMoney(player.wage ?? 0)}</td>
        <td className={`num${ending ? ' contract-ending' : ''}`}>
          {player.contractUntil ? `Sezon ${player.contractUntil}` : '–'}
        </td>
        <td className="row-actions">
          <button className="btn btn-sm" onClick={() => setOpen((shown) => !shown)}>
            Uzat
          </button>
          {transferWindowOpen && (
            <button className="btn btn-sm" onClick={() => run(() => api.listForSale(player.id))} disabled={busy}>
              Satışa çıkar
            </button>
          )}
        </td>
      </tr>
      {(open || result) && (
        <tr className="offer-row">
          <td colSpan={8}>
            {open && (
              <form
                className="inline-form offer-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  offer(Math.round(Number(wage) * 1000))
                }}
              >
                <label>
                  Haftalık maaş (bin €){' '}
                  <input
                    className="input-sm input-num"
                    type="number"
                    min={0}
                    step={0.1}
                    value={wage}
                    onChange={(event) => setWage(event.target.value)}
                  />
                </label>
                <label>
                  Süre{' '}
                  <select className="input-sm" value={seasons} onChange={(event) => setSeasons(event.target.value)}>
                    {[1, 2, 3, 4].map((count) => (
                      <option key={count} value={count}>
                        {count} sezon
                      </option>
                    ))}
                  </select>
                </label>
                <button className="btn btn-sm btn-primary" disabled={busy}>
                  Teklif et
                </button>
                {(result?.status === 'COUNTER' || result?.status === 'REJECTED') && result.amount !== null && (
                  <button type="button" className="btn btn-sm" onClick={() => offer(result.amount!)} disabled={busy}>
                    {formatMoney(result.amount)} ver
                  </button>
                )}
              </form>
            )}
            {result && (
              <span
                className={`offer-result offer-${result.status === 'DONE' ? 'accepted' : result.status.toLowerCase()}`}
              >
                {result.message}
              </span>
            )}
          </td>
        </tr>
      )}
    </>
  )
}

export default ContractsPanel
