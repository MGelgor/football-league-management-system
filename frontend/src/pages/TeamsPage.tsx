import { Fragment, useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Player, Team } from '../api/types'
import TeamForm from '../components/TeamForm'
import TeamLogo from '../components/TeamLogo'
import Trend from '../components/Trend'
import { POSITION_LABELS, POSITIONS } from '../labels'

const MIN_TEAM_COUNT = 18
const MAX_RANDOM_TEAMS = 50

/** Fikstür için eksik takım sayısı: 18'e tamamla, 18+ ise çift sayıya tamamla (çiftse 2 öner). */
function suggestedRandomCount(count: number) {
  if (count < MIN_TEAM_COUNT) {
    return MIN_TEAM_COUNT - count
  }
  return count % 2 === 0 ? 2 : 1
}

function TeamsPage() {
  const [teams, setTeams] = useState<Team[] | null>(null)
  // Sezon devam ederken takım ekleme/silme kapalı
  const [seasonInProgress, setSeasonInProgress] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<Team | 'new' | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null)
  // Satırına tıklanıp oyuncuları açılan takım; kadrolar ilk açılışta yüklenip saklanır
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [squads, setSquads] = useState<Record<number, Player[]>>({})

  function toggleSquad(teamId: number) {
    if (expandedId === teamId) {
      setExpandedId(null)
      return
    }
    setExpandedId(teamId)
    if (!squads[teamId]) {
      api
        .getPlayers(teamId)
        .then((players) => setSquads((current) => ({ ...current, [teamId]: players })))
        .catch((e) => setError(errorMessage(e)))
    }
  }

  const load = useCallback(
    () =>
      Promise.all([api.getTeams(), api.getSeasons()])
        .then(([teamList, seasons]) => {
          setTeams(teamList.toSorted((a, b) => a.name.localeCompare(b.name, 'tr')))
          setSeasonInProgress(seasons.length > 0 && !seasons[0].finished)
        })
        .catch((e) => setError(errorMessage(e))),
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  async function handleSaved() {
    setEditing(null)
    setError(null)
    await load()
  }

  async function handleDelete(id: number) {
    try {
      await api.deleteTeam(id)
      setError(null)
      await load()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setConfirmDeleteId(null)
    }
  }

  return (
    <section>
      <div className="page-header">
        <h1>Takımlar</h1>
        <div className="page-actions">
          {teams && (
            <RandomTeamsControl
              suggested={suggestedRandomCount(teams.length)}
              disabled={seasonInProgress || editing !== null}
              onAdded={handleSaved}
              onError={setError}
            />
          )}
          <button
            className="btn btn-primary"
            onClick={() => setEditing('new')}
            disabled={seasonInProgress || editing !== null}
          >
            + Takım ekle
          </button>
        </div>
      </div>

      {error && <p className="alert alert-error">{error}</p>}
      {teams && <TeamCountStatus count={teams.length} seasonInProgress={seasonInProgress} />}

      {editing && (
        <TeamForm
          key={editing === 'new' ? 'new' : editing.id}
          team={editing === 'new' ? null : editing}
          onSaved={handleSaved}
          onCancel={() => setEditing(null)}
        />
      )}

      {teams && teams.length === 0 && <p className="muted">Henüz takım yok. "Takım ekle" ile başlayın.</p>}

      {teams && teams.length > 0 && (
        <div className="table-wrap card">
          <table className="table">
            <thead>
              <tr>
                <th aria-label="Logo" />
                <th>Takım</th>
                <th>Kuruluş</th>
                <th>Renkler</th>
                <th className="num" title="Güç ve son maçtaki değişimi">
                  Güç
                </th>
                <th className="num" title="Sezon başına göre güç değişimi">
                  Sezon
                </th>
                <th className="num">Moral</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {teams.map((team) => (
                <Fragment key={team.id}>
                  <tr className="team-row" onClick={() => toggleSquad(team.id)}>
                    <td>
                      <TeamLogo name={team.name} logoUrl={team.logoUrl} />
                    </td>
                    <td className="strong">
                      <button
                        type="button"
                        className="team-toggle"
                        aria-expanded={expandedId === team.id}
                      >
                        <span className="team-toggle-icon" aria-hidden>
                          {expandedId === team.id ? '▾' : '▸'}
                        </span>
                        {team.name}
                      </button>
                    </td>
                    <td>{team.foundedYear}</td>
                    <td>{team.colors}</td>
                    <td className="num">
                      {team.strength} <Trend value={team.lastStrengthChange} title="Son değişim" />
                    </td>
                    <td className="num">
                      <Trend value={team.seasonStrengthChange} title="Sezon başına göre" />
                    </td>
                    <td className="num">{team.morale}</td>
                    <td className="row-actions" onClick={(e) => e.stopPropagation()}>
                      {team.bigFour ? (
                        <span className="muted" title="Bu takım sistem tarafından tanımlıdır">
                          Sabit takım
                        </span>
                      ) : confirmDeleteId === team.id ? (
                        <>
                          <span className="muted">Silinsin mi?</span>
                          <button className="btn btn-sm btn-danger" onClick={() => handleDelete(team.id)}>
                            Evet, sil
                          </button>
                          <button className="btn btn-sm" onClick={() => setConfirmDeleteId(null)}>
                            Vazgeç
                          </button>
                        </>
                      ) : (
                        <>
                          <button className="btn btn-sm" onClick={() => setEditing(team)} disabled={editing !== null}>
                            Düzenle
                          </button>
                          <button
                            className="btn btn-sm btn-danger-outline"
                            onClick={() => setConfirmDeleteId(team.id)}
                            disabled={seasonInProgress}
                            title={seasonInProgress ? 'Sezon devam ederken takım silinemez' : undefined}
                          >
                            Sil
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                  {expandedId === team.id && (
                    <tr className="squad-row">
                      <td colSpan={8}>
                        <SquadPreview teamId={team.id} players={squads[team.id]} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

/** Takımlar listesinde açılan kadro özeti: mevkilere göre oyuncu adları. */
function SquadPreview({ teamId, players }: { teamId: number; players: Player[] | undefined }) {
  if (!players) {
    return <span className="muted">Oyuncular yükleniyor…</span>
  }
  return (
    <div className="squad-preview">
      {POSITIONS.map((position) => {
        const group = players
          .filter((player) => player.position === position)
          .toSorted((a, b) => a.shirtNumber - b.shirtNumber)
        return (
          <div key={position} className="squad-group">
            <span className={`pos pos-${position.toLowerCase()}`}>{POSITION_LABELS[position]}</span>
            <ul>
              {group.map((player) => (
                <li key={player.id}>
                  <span className="muted">{player.shirtNumber}</span>{' '}
                  <Link to={`/players/${player.id}`} className="team-link">
                    {player.name}
                  </Link>
                  {player.suspendedMatches > 0 && <span title="Cezalı"> 🟥</span>}
                  {player.injuredMatches > 0 && <span title="Sakat"> 🩹</span>}
                  {player.goals > 0 && <span className="squad-goals"> ⚽{player.goals}</span>}
                </li>
              ))}
            </ul>
          </div>
        )
      })}
      <Link to={`/teams/${teamId}`} className="squad-link">
        Kadro sayfası ve istatistikler →
      </Link>
    </div>
  )
}

interface RandomTeamsControlProps {
  suggested: number
  disabled: boolean
  onAdded: () => void
  onError: (message: string | null) => void
}

/** Rastgele ad, kuruluş yılı ve renklerle N takım ekler (her biri 18 kişilik kadroyla). */
function RandomTeamsControl({ suggested, disabled, onAdded, onError }: RandomTeamsControlProps) {
  // null = kullanıcı bir şey yazmadı, önerilen sayı gösterilir (takım sayısı değiştikçe güncellenir)
  const [count, setCount] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const value = count ?? String(suggested)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAdding(true)
    onError(null)
    try {
      await api.createRandomTeams(Number(value))
      setCount(null)
      onAdded()
    } catch (e) {
      onError(errorMessage(e))
    } finally {
      setAdding(false)
    }
  }

  return (
    <form className="random-teams" onSubmit={handleSubmit}>
      <input
        className="input-sm"
        type="number"
        min={1}
        max={MAX_RANDOM_TEAMS}
        required
        value={value}
        onChange={(e) => setCount(e.target.value)}
        disabled={disabled || adding}
        aria-label="Eklenecek rastgele takım sayısı"
      />
      <button type="submit" className="btn" disabled={disabled || adding}>
        {adding ? 'Ekleniyor…' : 'Rastgele takım ekle'}
      </button>
    </form>
  )
}

function TeamCountStatus({ count, seasonInProgress }: { count: number; seasonInProgress: boolean }) {
  if (seasonInProgress) {
    return (
      <p className="alert alert-info">
        Sezon devam ediyor, bu yüzden takım ekleme ve silme kapalı (düzenleme açık). Takım listesini değiştirmek için
        sezonu tamamlayın ya da <Link to="/fixture">Fikstür</Link> sayfasından sıfırlayın.
      </p>
    )
  }
  if (count < MIN_TEAM_COUNT) {
    return (
      <div className="alert alert-warning">
        <strong>
          {count} / {MIN_TEAM_COUNT} takım
        </strong>{' '}
        — Fikstür oluşturmak için en az {MIN_TEAM_COUNT} takım gerekli. {MIN_TEAM_COUNT - count} takım daha ekleyin.
        <div className="progress">
          <div className="progress-bar" style={{ width: `${(count / MIN_TEAM_COUNT) * 100}%` }} />
        </div>
      </div>
    )
  }
  if (count % 2 !== 0) {
    return (
      <p className="alert alert-warning">
        <strong>{count} takım</strong> — Fikstür için takım sayısı çift olmalı. Bir takım ekleyin ya da silin.
      </p>
    )
  }
  return (
    <p className="alert alert-success">
      <strong>{count} takım</strong> hazır. <Link to="/fixture">Fikstürü oluşturabilirsiniz →</Link>
    </p>
  )
}

export default TeamsPage
