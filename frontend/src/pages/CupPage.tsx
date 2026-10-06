import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Cup, CupRound, CupTie } from '../api/types'
import { CUP_ROUND_LABELS } from '../labels'

const ROUNDS: CupRound[] = ['QUARTER_FINAL', 'SEMI_FINAL', 'FINAL']

function CupPage() {
  const [cup, setCup] = useState<Cup | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api
      .getCup()
      .then(setCup)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  async function run(action: () => Promise<Cup>) {
    setBusy(true)
    setError(null)
    try {
      setCup(await action())
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const nextRound = cup?.rounds.find((round) => !round.played)?.round

  return (
    <section>
      <div className="page-header">
        <h1>
          Kupa{cup?.seasonNumber && <span className="muted"> · Sezon {cup.seasonNumber}</span>}
        </h1>
        <div className="page-actions">
          {cup?.status === 'NOT_STARTED' && (
            <button className="btn btn-primary" onClick={() => run(api.startCup)} disabled={busy}>
              Kupayı başlat
            </button>
          )}
          {cup?.status === 'IN_PROGRESS' && nextRound && (
            <button className="btn btn-primary" onClick={() => run(api.playCupRound)} disabled={busy}>
              {busy ? 'Oynanıyor…' : `${CUP_ROUND_LABELS[nextRound]} oynat`}
            </button>
          )}
          {(cup?.status === 'NOT_STARTED' || cup?.status === 'IN_PROGRESS') && (
            <button className="btn" onClick={() => run(api.playCupAll)} disabled={busy}>
              Tüm kupayı oynat
            </button>
          )}
        </div>
      </div>

      {error && <p className="alert alert-error">{error}</p>}

      {cup?.status === 'NO_SEASON' && (
        <p className="alert alert-info">
          Henüz sezon yok. Kupa, <Link to="/fixture">lig sezonu</Link> tamamlandıktan sonra ilk 8 takımla oynanır.
        </p>
      )}
      {cup?.status === 'LEAGUE_IN_PROGRESS' && (
        <p className="alert alert-info">
          Kupa, lig sezonu bitince ligin ilk 8 takımıyla oynanır. Ligi <Link to="/fixture">Fikstür</Link> ya da{' '}
          <Link to="/standings">Puan Durumu</Link> sayfasından tamamlayın.
        </p>
      )}
      {cup?.status === 'NOT_STARTED' && (
        <p className="alert alert-info">
          Lig tamamlandı. Ligin ilk 8 takımı tek maçlık eleme oynar: 1–8, 4–5, 2–7, 3–6. Ligde üst sıradaki takım ev
          sahibidir, beraberlikte penaltılar atılır. Kupa başlatılmadan yeni sezona geçilirse bu sezonun kupası oynanmaz.
        </p>
      )}

      {cup?.winnerName && (
        <div className="champion-banner" role="status">
          <span className="champion-trophy" aria-hidden>
            🏆
          </span>
          <div>
            <div className="champion-label">Sezon {cup.seasonNumber} kupa şampiyonu</div>
            <div className="champion-name">{cup.winnerName}</div>
          </div>
          <Link to="/fixture" className="btn champion-action">
            Yeni sezona geç →
          </Link>
        </div>
      )}

      {cup && cup.rounds.length > 0 && (
        <div className="bracket">
          {ROUNDS.map((round) => {
            const state = cup.rounds.find((r) => r.round === round)
            return (
              <div key={round} className="bracket-round">
                <h2>{CUP_ROUND_LABELS[round]}</h2>
                <div className="bracket-ties">
                  {state ? (
                    state.ties.map((tie) => <TieCard key={tie.match.id} tie={tie} />)
                  ) : (
                    <p className="muted">Önceki tur bekleniyor</p>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}

function TieCard({ tie }: { tie: CupTie }) {
  const { match } = tie
  const content = (
    <>
      <TieTeam
        name={match.homeTeamName}
        seed={tie.homeSeed}
        score={match.homeScore}
        penalties={match.homePenalties}
        winner={tie.winnerTeamId === match.homeTeamId}
      />
      <TieTeam
        name={match.awayTeamName}
        seed={tie.awaySeed}
        score={match.awayScore}
        penalties={match.awayPenalties}
        winner={tie.winnerTeamId === match.awayTeamId}
      />
      {match.homePenalties !== null && <div className="muted tie-note">Penaltılarla</div>}
    </>
  )
  return match.played ? (
    <Link to={`/matches/${match.id}`} className="card tie tie-link" title="Maç detayı">
      {content}
    </Link>
  ) : (
    <div className="card tie">{content}</div>
  )
}

interface TieTeamProps {
  name: string
  seed: number
  score: number | null
  penalties: number | null
  winner: boolean
}

function TieTeam({ name, seed, score, penalties, winner }: TieTeamProps) {
  return (
    <div className={`tie-team${winner ? ' winner' : ''}`}>
      <span className="tie-seed muted">{seed}.</span>
      <span className="tie-name">{name}</span>
      <span className="tie-score">
        {score ?? ''}
        {penalties !== null && <span className="muted"> ({penalties})</span>}
      </span>
    </div>
  )
}

export default CupPage
