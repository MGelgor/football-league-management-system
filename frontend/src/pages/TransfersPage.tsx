import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { MarketPlayer, OfferResult, Position, Team, TransferEntry, TransferWindow } from '../api/types'
import FormIndicator from '../components/FormIndicator'
import { formatMoney, POSITION_LABELS, POSITION_SHORT, POSITIONS } from '../labels'

const PAGE_SIZE = 40

function fetchTransferData() {
  return Promise.all([api.getTransferWindow(), api.getMarket(), api.getTeams(), api.getTransfers(), api.getMyTeam()])
}

/** Transfer penceresi: piyasa (tüm takımların oyuncuları + serbest oyuncular), teklif / imza ve son transferler. */
function TransfersPage() {
  const [windowState, setWindowState] = useState<TransferWindow | null>(null)
  const [market, setMarket] = useState<MarketPlayer[]>([])
  const [teams, setTeams] = useState<Team[]>([])
  const [transfers, setTransfers] = useState<TransferEntry[]>([])
  const [buyerId, setBuyerId] = useState<number | null>(null)
  const [managedTeamId, setManagedTeamId] = useState<number | null>(null)
  const [query, setQuery] = useState('')
  const [position, setPosition] = useState<Position | ''>('')
  const [onlyFree, setOnlyFree] = useState(false)
  const [maxPrice, setMaxPrice] = useState('')
  const [limit, setLimit] = useState(PAGE_SIZE)
  const [error, setError] = useState<string | null>(null)

  function show([windowData, marketList, teamList, transferList, myTeam]: Awaited<
    ReturnType<typeof fetchTransferData>
  >) {
    setWindowState(windowData)
    setMarket(marketList)
    setTeams(teamList.toSorted((a, b) => a.name.localeCompare(b.name, 'tr')))
    setTransfers(transferList)
    // Menajer modunda alıcı her zaman yönetilen takım
    const managedId = myTeam.active && myTeam.team ? myTeam.team.id : null
    setManagedTeamId(managedId)
    setBuyerId((current) => managedId ?? current ?? teamList[0]?.id ?? null)
  }

  const reload = () =>
    fetchTransferData()
      .then(show)
      .catch((e) => setError(errorMessage(e)))

  useEffect(() => {
    fetchTransferData()
      .then(show)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  const buyer = teams.find((team) => team.id === buyerId) ?? null
  const filtered = useMemo(() => {
    const text = query.trim().toLocaleLowerCase('tr')
    const max = maxPrice ? Number(maxPrice) * 1_000_000 : null
    return market.filter(
      (player) =>
        player.teamId !== buyerId &&
        (!text ||
          player.name.toLocaleLowerCase('tr').includes(text) ||
          player.teamName?.toLocaleLowerCase('tr').includes(text)) &&
        (!position || player.position === position) &&
        (!onlyFree || player.freeAgent) &&
        (max === null || player.askingPrice <= max),
    )
  }, [market, buyerId, query, position, onlyFree, maxPrice])

  return (
    <section>
      <h1>Transferler</h1>
      {error && <p className="alert alert-error">{error}</p>}
      {windowState && (
        <p className={`alert ${windowState.open ? 'alert-success' : 'alert-info'}`}>{windowState.message}</p>
      )}

      {windowState?.open && (
        <div className="card">
          <div className="transfer-toolbar">
            <label className="field">
              Alıcı takım
              <select
                value={buyerId ?? ''}
                onChange={(event) => setBuyerId(Number(event.target.value))}
                disabled={managedTeamId !== null}
                title={
                  managedTeamId !== null ? 'Menajer modunda yalnızca kendi takımın için transfer yaparsın' : undefined
                }
              >
                {teams.map((team) => (
                  <option key={team.id} value={team.id}>
                    {team.name}
                  </option>
                ))}
              </select>
            </label>
            {buyer && (
              <div className="transfer-budget">
                <div className="muted">Bütçe</div>
                <div className="strong">{formatMoney(buyer.budget ?? 0)}</div>
              </div>
            )}
            <label className="field">
              Ara
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Oyuncu ya da takım"
              />
            </label>
            <label className="field">
              Mevki
              <select value={position} onChange={(event) => setPosition(event.target.value as Position | '')}>
                <option value="">Tümü</option>
                {POSITIONS.map((option) => (
                  <option key={option} value={option}>
                    {POSITION_LABELS[option]}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              En fazla (milyon €)
              <input
                type="number"
                min={0}
                value={maxPrice}
                onChange={(event) => setMaxPrice(event.target.value)}
                className="input-num"
              />
            </label>
            <label className="checkbox">
              <input type="checkbox" checked={onlyFree} onChange={(event) => setOnlyFree(event.target.checked)} />{' '}
              Yalnızca serbest
            </label>
          </div>

          <div className="table-wrap">
            <table className="table compact">
              <thead>
                <tr>
                  <th>Oyuncu</th>
                  <th>Mevki</th>
                  <th className="num">Yaş</th>
                  <th className="num">Güç</th>
                  <th>Takım</th>
                  <th className="num">Değer</th>
                  <th className="num">İstenen</th>
                  <th className="num">Maaş / hafta</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {filtered.slice(0, limit).map((player) => (
                  <MarketRow key={player.playerId} player={player} buyerId={buyerId} onDone={reload} />
                ))}
              </tbody>
            </table>
          </div>
          <div className="table-footer">
            <span className="muted">
              {Math.min(limit, filtered.length)} / {filtered.length} oyuncu
            </span>
            {filtered.length > limit && (
              <button className="btn btn-sm" onClick={() => setLimit((current) => current + PAGE_SIZE)}>
                Daha fazla
              </button>
            )}
          </div>
          <p className="legend muted">
            İstenen bedel değerin 1,1 katı (takımın en güçlü üç oyuncusunda 1,4 katı); %85'ine kadar olan teklife karşı
            teklif gelir, altı reddedilir. Satan takımın kadrosu 18'in ya da mevki gereksiniminin (2 KL, 6 DEF, 6 OS, 4
            FV) altına düşecekse satış olmaz. Transfer iki takımın gücünü ilk 11 ortalamasındaki değişim kadar etkiler.
          </p>
        </div>
      )}

      <div className="card">
        <h2>Son transferler</h2>
        {transfers.length === 0 ? (
          <p className="muted">Henüz transfer yok.</p>
        ) : (
          <ul className="transfer-feed">
            {transfers.map((transfer) => (
              <li key={transfer.id}>
                <span className="muted">Sezon {transfer.seasonNumber} öncesi</span>
                <Link to={`/players/${transfer.playerId}`} className="strong team-link">
                  {transfer.playerName}
                </Link>
                <span className="muted">{POSITION_SHORT[transfer.position]}</span>
                <span>
                  {transfer.fromTeamName ?? 'Serbest'} → <strong>{transfer.toTeamName}</strong>
                </span>
                <span className="transfer-fee">{transfer.fee > 0 ? formatMoney(transfer.fee) : 'Bedelsiz'}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

interface MarketRowProps {
  player: MarketPlayer
  buyerId: number | null
  onDone: () => void
}

function MarketRow({ player, buyerId, onDone }: MarketRowProps) {
  const [offering, setOffering] = useState(false)
  const [fee, setFee] = useState(String(Math.round(player.askingPrice / 10_000) / 100))
  const [result, setResult] = useState<OfferResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(amount: number) {
    if (buyerId === null) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      if (player.freeAgent) {
        await api.signFreeAgent(player.playerId, buyerId)
        onDone()
        return
      }
      const offer = await api.makeOffer(player.playerId, buyerId, amount)
      setResult(offer)
      if (offer.status === 'ACCEPTED') {
        onDone()
      }
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <tr>
        <td>
          <Link to={`/players/${player.playerId}`} className="team-link">
            {player.name}
          </Link>
          <FormIndicator form={player.form} />
        </td>
        <td>{POSITION_SHORT[player.position]}</td>
        <td className="num">{player.age}</td>
        <td className="num strong">{player.strength}</td>
        <td>{player.teamName ?? <span className="muted">Serbest</span>}</td>
        <td className="num">{formatMoney(player.marketValue)}</td>
        <td className="num">{player.freeAgent ? '–' : formatMoney(player.askingPrice)}</td>
        <td className="num">{formatMoney(player.expectedWage)}</td>
        <td className="row-actions">
          {player.freeAgent ? (
            <button className="btn btn-sm btn-primary" onClick={() => submit(0)} disabled={busy}>
              İmzala
            </button>
          ) : (
            <button className="btn btn-sm" onClick={() => setOffering((open) => !open)} disabled={busy}>
              Teklif
            </button>
          )}
        </td>
      </tr>
      {(offering || error) && (
        <tr className="offer-row">
          <td colSpan={9}>
            {offering && (
              <form
                className="inline-form offer-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  submit(Math.round(Number(fee) * 1_000_000))
                }}
              >
                <label>
                  Teklif (milyon €){' '}
                  <input
                    className="input-sm input-num"
                    type="number"
                    min={0}
                    step={0.01}
                    value={fee}
                    onChange={(event) => setFee(event.target.value)}
                  />
                </label>
                <button className="btn btn-sm btn-primary" disabled={busy}>
                  Gönder
                </button>
                {result && (
                  <span className={`offer-result offer-${result.status.toLowerCase()}`}>
                    {result.status === 'COUNTER'
                      ? 'Karşı teklif: '
                      : result.status === 'REJECTED'
                        ? 'Reddedildi: '
                        : ''}
                    {result.message}
                  </span>
                )}
                {result?.status === 'COUNTER' && (
                  <button
                    type="button"
                    className="btn btn-sm"
                    onClick={() => submit(result.askingPrice)}
                    disabled={busy}
                  >
                    {formatMoney(result.askingPrice)} öde
                  </button>
                )}
              </form>
            )}
            {error && <span className="offer-result offer-rejected">{error}</span>}
          </td>
        </tr>
      )}
    </>
  )
}

export default TransfersPage
