import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { MyTeam, Team } from '../api/types'
import CareerPanel from '../components/CareerPanel'
import ContractsPanel from '../components/ContractsPanel'
import FormBadges from '../components/FormBadges'
import InboxPanel from '../components/InboxPanel'
import LineupEditor from '../components/LineupEditor'
import LiveMatchView from '../components/LiveMatchView'
import ProbabilityBar from '../components/ProbabilityBar'
import TeamLogo from '../components/TeamLogo'
import { CUP_ROUND_LABELS, formatMoney, INJURY_LABELS, POSITION_SHORT } from '../labels'

type View = 'dashboard' | 'lineup' | 'live' | 'inbox' | 'contracts' | 'career'

const TABS: { view: View; label: string }[] = [
  { view: 'dashboard', label: 'Pano' },
  { view: 'inbox', label: 'Gelen kutusu' },
  { view: 'contracts', label: 'Sözleşmeler' },
  { view: 'career', label: 'Kariyer' },
]

function confidenceColor(confidence: number) {
  return confidence >= 60 ? 'var(--accent)' : confidence >= 30 ? 'var(--warning)' : 'var(--danger)'
}

/** "Takımımı Yönet" modu: takım seçimi, pano, kadro editörü ve canlı maç. */
function MyTeamPage() {
  const [myTeam, setMyTeam] = useState<MyTeam | null>(null)
  const [view, setView] = useState<View>('dashboard')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmStop, setConfirmStop] = useState(false)

  const reload = () =>
    api
      .getMyTeam()
      .then(setMyTeam)
      .catch((e) => setError(errorMessage(e)))

  useEffect(() => {
    api
      .getMyTeam()
      .then(setMyTeam)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  async function run(action: () => Promise<MyTeam | void>) {
    setBusy(true)
    setError(null)
    try {
      const result = await action()
      setMyTeam(result ?? (await api.getMyTeam()))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  if (!myTeam) {
    return error ? <p className="alert alert-error">{error}</p> : <p className="muted">Yükleniyor…</p>
  }
  if (myTeam.unemployed && !myTeam.active) {
    return (
      <section>
        <h1>Takımım</h1>
        <p className="alert alert-warning">
          {myTeam.managerName}, şu an bir takımın yok. Gelen iş tekliflerinden birini kabul edebilir ya da istediğin bir
          takımla yeniden başlayabilirsin.
        </p>
        <InboxPanel onlyType="JOB_OFFER" onChanged={reload} />
        <CareerPanel />
        <TeamPicker
          onStart={(name, teamId) => run(() => api.startMyTeam(name, teamId))}
          busy={busy}
          error={error}
          defaultName={myTeam.managerName ?? ''}
        />
      </section>
    )
  }
  if (!myTeam.active || !myTeam.team) {
    return <TeamPicker onStart={(name, teamId) => run(() => api.startMyTeam(name, teamId))} busy={busy} error={error} />
  }

  const { team, nextMatch } = myTeam
  const opponent = nextMatch ? (nextMatch.home ? nextMatch.match.awayTeamName : nextMatch.match.homeTeamName) : null

  return (
    <section>
      <div className="card team-hero my-team-hero">
        <TeamLogo name={team.name} logoUrl={team.logoUrl} large />
        <div>
          <div className="muted">Menajer {myTeam.managerName}</div>
          <h1>
            <Link to={`/teams/${team.id}`} className="team-link">
              {team.name}
            </Link>
          </h1>
          <div className="muted">
            {myTeam.seasonNumber !== null && `Sezon ${myTeam.seasonNumber} · `}
            {myTeam.status}
          </div>
        </div>
        <dl className="hero-stats">
          <div>
            <dt>Sıra</dt>
            <dd>{myTeam.standing && myTeam.standing.played > 0 ? `${myTeam.standing.rank}.` : '–'}</dd>
          </div>
          <div>
            <dt>Güç</dt>
            <dd>{team.strength}</dd>
          </div>
          <div>
            <dt>Moral</dt>
            <dd>{team.morale}</dd>
          </div>
          <div>
            <dt>Bütçe</dt>
            <dd>{formatMoney(team.budget ?? 0)}</dd>
          </div>
          <div title="Yönetim kurulunun güveni; 0'a düşerse görevine son verilir">
            <dt>Güven</dt>
            <dd>
              {myTeam.confidence}
              <div className="confidence" aria-hidden>
                <div
                  className="confidence-bar"
                  style={{ width: `${myTeam.confidence}%`, background: confidenceColor(myTeam.confidence) }}
                />
              </div>
            </dd>
          </div>
        </dl>
      </div>

      {(view === 'dashboard' || view === 'inbox' || view === 'contracts' || view === 'career') && (
        <div className="tab-bar" role="tablist">
          {TABS.map((tab) => (
            <button
              key={tab.view}
              role="tab"
              aria-selected={view === tab.view}
              className={`btn${view === tab.view ? ' is-selected' : ''}`}
              onClick={() => setView(tab.view)}
            >
              {tab.label}
              {tab.view === 'inbox' && myTeam.unreadMessages > 0 && (
                <span className="tab-count">{myTeam.unreadMessages}</span>
              )}
            </button>
          ))}
        </div>
      )}

      {view === 'inbox' && <InboxPanel onChanged={reload} />}
      {view === 'contracts' && (
        <ContractsPanel
          teamId={team.id}
          seasonNumber={myTeam.seasonNumber}
          transferWindowOpen={myTeam.transferWindowOpen}
          onChanged={reload}
        />
      )}
      {view === 'career' && <CareerPanel />}

      {error && <p className="alert alert-error">{error}</p>}

      {view === 'live' && (
        <LiveMatchView
          onClose={() => {
            setView('dashboard')
            reload()
          }}
        />
      )}

      {view === 'lineup' && (
        <LineupEditor
          onSaved={() => {
            setView('dashboard')
            reload()
          }}
          onCancel={() => setView('dashboard')}
        />
      )}

      {view === 'dashboard' && (
        <>
          {nextMatch ? (
            <div className="card next-match">
              <div className="muted">
                Sıradaki maç ·{' '}
                {nextMatch.competition === 'CUP' && nextMatch.cupRound
                  ? `Kupa ${CUP_ROUND_LABELS[nextMatch.cupRound]}`
                  : `Hafta ${nextMatch.weekNumber}`}{' '}
                · {nextMatch.home ? 'İç saha' : 'Deplasman'}
              </div>
              <h2 className="next-match-title">
                {nextMatch.match.homeTeamName} <span className="muted">vs</span> {nextMatch.match.awayTeamName}
              </h2>
              <ProbabilityBar
                home={nextMatch.match.homeWinProbability}
                draw={nextMatch.match.drawProbability}
                away={nextMatch.match.awayWinProbability}
              />
              <p className="muted legend">
                Rakip: <strong>{opponent}</strong> · Kadro:{' '}
                {nextMatch.lineupSaved ? 'seçildi ✓' : 'seçilmedi (yapay zekâ önerisi kullanılır)'}
              </p>
              {myTeam.targetRank !== null && (
                <p className="muted legend">
                  Yönetim kurulu hedefi: ligi en kötü {myTeam.targetRank}. sırada bitir
                  {myTeam.cupTarget && `, kupada en az ${CUP_ROUND_LABELS[myTeam.cupTarget].toLocaleLowerCase('tr')}`}
                </p>
              )}
              <div className="page-actions">
                <button className="btn" onClick={() => setView('lineup')} disabled={busy}>
                  Kadroyu belirle
                </button>
                <button className="btn btn-primary" onClick={() => setView('live')} disabled={busy}>
                  Maçı canlı oyna
                </button>
                <button className="btn" onClick={() => run(api.quickPlay)} disabled={busy}>
                  {busy ? 'Oynanıyor…' : 'Hızlı oynat'}
                </button>
              </div>
            </div>
          ) : (
            <div className="card">
              <p>{myTeam.status}</p>
              <div className="page-actions">
                <Link to="/fixture" className="btn">
                  Fikstür
                </Link>
                <Link to="/cup" className="btn">
                  Kupa
                </Link>
                {myTeam.transferWindowOpen && (
                  <Link to="/transfers" className="btn btn-primary">
                    Transfer penceresi açık →
                  </Link>
                )}
              </div>
            </div>
          )}

          <div className="detail-grid">
            <div className="card">
              <h2>Puan durumu</h2>
              {myTeam.standingsAround.length === 0 ? (
                <p className="muted">Sezon başlamadı.</p>
              ) : (
                <div className="table-wrap">
                  <table className="table compact">
                    <thead>
                      <tr>
                        <th className="num">#</th>
                        <th>Takım</th>
                        <th className="num">O</th>
                        <th className="num">AV</th>
                        <th className="num">P</th>
                        <th>Form</th>
                      </tr>
                    </thead>
                    <tbody>
                      {myTeam.standingsAround.map((row) => (
                        <tr key={row.teamId} className={row.teamId === team.id ? 'my-row' : undefined}>
                          <td className="num">{row.rank}</td>
                          <td>{row.teamName}</td>
                          <td className="num">{row.played}</td>
                          <td className="num">{row.goalDifference}</td>
                          <td className="num strong">{row.points}</td>
                          <td>
                            <FormBadges form={row.form} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <Link to="/standings" className="hero-link">
                Tüm puan durumu →
              </Link>
            </div>

            <div className="card">
              <h2>Son maçlar</h2>
              {myTeam.recentResults.length === 0 ? (
                <p className="muted">Henüz maç yok.</p>
              ) : (
                <ul className="recent-results">
                  {myTeam.recentResults.map((recent) => (
                    <li key={recent.match.id}>
                      <span className={`form-badge form-${recent.result}`}>{recent.result}</span>
                      <Link to={`/matches/${recent.match.id}`}>
                        {recent.match.homeTeamName} {recent.match.homeScore} - {recent.match.awayScore}{' '}
                        {recent.match.awayTeamName}
                      </Link>
                      <span className="muted">
                        {recent.competition === 'CUP' && recent.cupRound
                          ? CUP_ROUND_LABELS[recent.cupRound]
                          : `H${recent.weekNumber}`}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
              <h3 className="section-gap">Kadro dışı</h3>
              {myTeam.unavailable.length === 0 ? (
                <p className="muted">Herkes oynamaya hazır.</p>
              ) : (
                <ul className="recent-results">
                  {myTeam.unavailable.map((player) => (
                    <li key={player.id}>
                      <span className="muted">{POSITION_SHORT[player.position]}</span>
                      <Link to={`/players/${player.id}`}>{player.name}</Link>
                      <span className="muted">
                        {player.injuredMatches > 0
                          ? `${player.injurySeverity ? INJURY_LABELS[player.injurySeverity] : ''} sakatlık · ${player.injuredMatches} maç`
                          : `Cezalı · ${player.suspendedMatches} maç`}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div className="card my-team-footer">
            <div className="page-actions">
              <Link to={`/teams/${team.id}`} className="btn">
                Kadro ve finans
              </Link>
              <Link to="/transfers" className="btn">
                Transferler
              </Link>
            </div>
            {confirmStop ? (
              <span className="confirm-actions">
                <span className="muted">Takım yapay zekâya devredilecek.</span>
                <button className="btn btn-danger" onClick={() => run(api.stopMyTeam)} disabled={busy}>
                  Evet, bırak
                </button>
                <button className="btn" onClick={() => setConfirmStop(false)}>
                  Vazgeç
                </button>
              </span>
            ) : (
              <button className="btn btn-danger-outline" onClick={() => setConfirmStop(true)}>
                Takımı bırak
              </button>
            )}
          </div>
        </>
      )}
    </section>
  )
}

interface TeamPickerProps {
  onStart: (managerName: string, teamId: number) => void
  busy: boolean
  error: string | null
  defaultName?: string
}

/** Mod kapalıyken: menajer adı ve yönetilecek takım seçimi. */
function TeamPicker({ onStart, busy, error, defaultName = '' }: TeamPickerProps) {
  const [teams, setTeams] = useState<Team[]>([])
  const [name, setName] = useState(defaultName)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getTeams()
      .then((list) => setTeams(list.toSorted((a, b) => b.strength - a.strength)))
      .catch((e) => setLoadError(errorMessage(e)))
  }, [])

  return (
    <section>
      <h1>Takımım</h1>
      <p className="muted">
        Bir takımın menajeri ol: takımının maçı geldiğinde lig seni bekler, ilk 11'i, dizilişi ve stili sen seçersin,
        maçı canlı izleyip devre arasında değişiklik yaparsın. İstediğin zaman takımı bırakabilirsin; bu modda değilken
        lig tamamen otomatik oynar.
      </p>
      {(error || loadError) && <p className="alert alert-error">{error ?? loadError}</p>}
      <div className="card">
        <label className="field">
          Menajer adın
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={60}
            placeholder="ör. Fatih"
          />
        </label>
      </div>
      {teams.length === 0 ? (
        <p className="alert alert-info">
          Önce <Link to="/teams">takım ekleyin</Link>.
        </p>
      ) : (
        <div className="team-picker">
          {teams.map((team) => (
            <button
              key={team.id}
              className="card team-pick"
              onClick={() => onStart(name.trim() || 'Menajer', team.id)}
              disabled={busy}
            >
              <TeamLogo name={team.name} logoUrl={team.logoUrl} />
              <span className="strong">{team.name}</span>
              <span className="muted">
                Güç {team.strength} · {formatMoney(team.budget ?? 0)}
              </span>
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

export default MyTeamPage
