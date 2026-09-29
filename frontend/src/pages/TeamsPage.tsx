import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Team } from '../api/types'
import TeamForm from '../components/TeamForm'

const MIN_TEAM_COUNT = 18

function TeamsPage() {
  const [teams, setTeams] = useState<Team[] | null>(null)
  const [fixtureGenerated, setFixtureGenerated] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<Team | 'new' | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null)

  const load = useCallback(
    () =>
      Promise.all([api.getTeams(), api.getFixture()])
        .then(([teamList, fixture]) => {
          setTeams(teamList.toSorted((a, b) => a.name.localeCompare(b.name, 'tr')))
          setFixtureGenerated(fixture.length > 0)
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
        <button
          className="btn btn-primary"
          onClick={() => setEditing('new')}
          disabled={fixtureGenerated || editing !== null}
        >
          + Takım ekle
        </button>
      </div>

      {error && <p className="alert alert-error">{error}</p>}
      {teams && <TeamCountStatus count={teams.length} fixtureGenerated={fixtureGenerated} />}

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
                <th className="num">Güç</th>
                <th className="num">Moral</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {teams.map((team) => (
                <tr key={team.id}>
                  <td>
                    <TeamLogo team={team} />
                  </td>
                  <td className="strong">{team.name}</td>
                  <td>{team.foundedYear}</td>
                  <td>{team.colors}</td>
                  <td className="num">{team.strength}</td>
                  <td className="num">{team.morale}</td>
                  <td className="row-actions">
                    {confirmDeleteId === team.id ? (
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
                          disabled={fixtureGenerated}
                          title={fixtureGenerated ? 'Fikstür oluşturulduktan sonra takım silinemez' : undefined}
                        >
                          Sil
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function TeamCountStatus({ count, fixtureGenerated }: { count: number; fixtureGenerated: boolean }) {
  if (fixtureGenerated) {
    return (
      <p className="alert alert-info">
        Fikstür oluşturuldu, bu yüzden takım ekleme ve silme kapalı (düzenleme açık). Takım listesini değiştirmek için{' '}
        <Link to="/fixture">Fikstür</Link> sayfasından fikstürü sıfırlayın.
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

function TeamLogo({ team }: { team: Team }) {
  if (team.logoUrl) {
    return <img className="team-logo" src={team.logoUrl} alt={`${team.name} logosu`} />
  }
  return <span className="team-logo team-logo-placeholder">{team.name.charAt(0).toLocaleUpperCase('tr')}</span>
}

export default TeamsPage
