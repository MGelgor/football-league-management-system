import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Player, PlayerRequest, Position, Season, SplitStats, Team, TeamSeasonStats } from '../api/types'
import FormBadges from '../components/FormBadges'
import LineChart from '../components/LineChart'
import PlayerStatus from '../components/PlayerStatus'
import TeamLogo from '../components/TeamLogo'
import Trend from '../components/Trend'
import { POSITION_LABELS, POSITIONS } from '../labels'

const COLUMN_COUNT = 13
// Sıralama grafiğinin alt sınırı (ligdeki takım sayısı)
const LEAGUE_SIZE = 18

function sortSquad(players: Player[]) {
  return players.toSorted(
    (a, b) => POSITIONS.indexOf(a.position) - POSITIONS.indexOf(b.position) || a.shirtNumber - b.shirtNumber,
  )
}

function TeamDetailPage() {
  const teamId = Number(useParams().teamId)
  const [team, setTeam] = useState<Team | null>(null)
  const [players, setPlayers] = useState<Player[] | null>(null)
  const [seasons, setSeasons] = useState<Season[]>([])
  const [error, setError] = useState<string | null>(null)
  // Düzenlenen oyuncunun id'si ya da yeni oyuncu satırı için 'new'
  const [editingId, setEditingId] = useState<number | 'new' | null>(null)

  useEffect(() => {
    Promise.all([api.getTeam(teamId), api.getPlayers(teamId), api.getSeasons()])
      .then(([teamData, squad, seasonList]) => {
        setTeam(teamData)
        setPlayers(sortSquad(squad))
        setSeasons(seasonList)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [teamId])

  function handleSaved(saved: Player) {
    setPlayers((current) => {
      if (!current) {
        return current
      }
      const exists = current.some((p) => p.id === saved.id)
      return sortSquad(exists ? current.map((p) => (p.id === saved.id ? saved : p)) : [...current, saved])
    })
    setEditingId(null)
  }

  const nextShirtNumber = players ? firstFreeShirtNumber(players) : 1
  const editable = team?.active ?? false

  return (
    <section>
      <p>
        <Link to="/teams">← Takımlar</Link>
      </p>
      {error && <p className="alert alert-error">{error}</p>}

      {team && !team.active && (
        <p className="alert alert-warning">
          Bu takım artık ligde değil (küme düştü ya da silindi). Geçmiş istatistikleri görüntülenebilir.
        </p>
      )}

      {team && (
        <div className="card team-hero">
          <TeamLogo name={team.name} logoUrl={team.logoUrl} large />
          <div>
            <h1>{team.name}</h1>
            <div className="muted">
              Kuruluş {team.foundedYear} · {team.colors}
            </div>
            <Link to={`/compare?a=${team.id}`} className="hero-link">
              Başka bir takımla karşılaştır →
            </Link>
          </div>
          <dl className="hero-stats">
            <div>
              <dt>Güç</dt>
              <dd>
                {team.strength} <Trend value={team.lastStrengthChange} title="Son değişim" />
              </dd>
            </div>
            <div>
              <dt>Sezon</dt>
              <dd>{team.seasonStrengthChange === 0 ? '–' : <Trend value={team.seasonStrengthChange} />}</dd>
            </div>
            <div>
              <dt>Moral</dt>
              <dd>{team.morale}</dd>
            </div>
          </dl>
        </div>
      )}

      {team && <TeamSeasonPanel teamId={teamId} seasons={seasons} />}

      {players && (
        <div className="table-wrap card">
          <table className="table squad">
            <thead>
              <tr>
                <th className="num">No</th>
                <th>Oyuncu</th>
                <th>Mevki</th>
                <th className="num">Yaş</th>
                <th className="num" title="Oyuncu gücü: mevkiyle birlikte gol atma olasılığını belirler">
                  Güç
                </th>
                <th className="num" title="Bu sezon oynadığı lig maçı">MS</th>
                <th className="num" title="Bu sezon gol">G</th>
                <th className="num" title="Bu sezon asist">A</th>
                <th className="num" title="Bu sezon sarı / kırmızı kart">Kart</th>
                <th className="num" title="Bu sezon ortalama reyting">Ort</th>
                <th className="num" title="Bu sezon maçın oyuncusu seçilme">⭐</th>
                <th className="num" title="Tüm sezonlar: maç / gol / asist">Kariyer</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {players.map((player) =>
                editingId === player.id ? (
                  <PlayerEditRow
                    key={player.id}
                    player={player}
                    onSubmit={(request) => api.updatePlayer(player.id, request)}
                    onSaved={handleSaved}
                    onCancel={() => setEditingId(null)}
                  />
                ) : (
                  <tr key={player.id}>
                    <td className="num strong">{player.shirtNumber}</td>
                    <td className="player-cell">
                      <Link to={`/players/${player.id}`} className="team-link">
                        {player.name}
                      </Link>{' '}
                      <PlayerStatus
                        suspendedMatches={player.suspendedMatches}
                        injuredMatches={player.injuredMatches}
                      />
                    </td>
                    <td>
                      <span className={`pos pos-${player.position.toLowerCase()}`}>
                        {POSITION_LABELS[player.position]}
                      </span>
                    </td>
                    <td className="num">{player.age}</td>
                    <td className="num">
                      {player.strength} <Trend value={player.lastStrengthChange} title="Sezon sonu gelişimi" />
                    </td>
                    <td className="num">{player.appearances || ''}</td>
                    <td className="num">{player.goals || ''}</td>
                    <td className="num">{player.assists || ''}</td>
                    <td className="num">
                      {player.yellowCards > 0 && `🟨${player.yellowCards}`}
                      {player.redCards > 0 && ` 🟥${player.redCards}`}
                    </td>
                    <td className="num">{player.averageRating?.toFixed(1) ?? ''}</td>
                    <td className="num">{player.playerOfTheMatch || ''}</td>
                    <td className="num muted">
                      {player.careerAppearances > 0 &&
                        `${player.careerAppearances} / ${player.careerGoals} / ${player.careerAssists}`}
                    </td>
                    <td className="row-actions">
                      {editable && (
                        <button
                          className="btn btn-sm"
                          onClick={() => setEditingId(player.id)}
                          disabled={editingId !== null}
                        >
                          Düzenle
                        </button>
                      )}
                    </td>
                  </tr>
                ),
              )}
              {editingId === 'new' && (
                <PlayerEditRow
                  player={null}
                  defaultShirtNumber={nextShirtNumber}
                  onSubmit={(request) => api.addPlayer(teamId, request)}
                  onSaved={handleSaved}
                  onCancel={() => setEditingId(null)}
                />
              )}
            </tbody>
          </table>
          {editable && (
            <div className="table-footer">
              <button className="btn btn-sm" onClick={() => setEditingId('new')} disabled={editingId !== null}>
                + Oyuncu ekle
              </button>
            </div>
          )}
        </div>
      )}
      <p className="legend muted">
        MS, G, A, kart, Ort ve ⭐ güncel sezonun lig maçlarına; Kariyer (maç / gol / asist) tüm sezon ve kupa maçlarına
        aittir. Kırmızı kart 1 maç, sezonda her 4 sarı kart 1 maç ceza getirir. Oyuncular lig bitince bir yaş büyür; gençler
        gelişir, yaşlılar geriler, 35 yaşından sonra emeklilik başlar.
      </p>
    </section>
  )
}

/** Seçilen sezonun takım istatistikleri ve haftalık sıra / güç grafikleri. */
function TeamSeasonPanel({ teamId, seasons }: { teamId: number; seasons: Season[] }) {
  const [seasonId, setSeasonId] = useState<number | undefined>(undefined)
  const [stats, setStats] = useState<TeamSeasonStats | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getTeamStats(teamId, seasonId)
      .then(setStats)
      .catch((e) => setError(errorMessage(e)))
  }, [teamId, seasonId])

  if (error) {
    return <p className="alert alert-error">{error}</p>
  }
  if (!stats || stats.seasonId === null) {
    return null
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>
          Sezon {stats.seasonNumber} istatistikleri
          {stats.rank !== null && stats.overall.played > 0 && <span className="muted"> · {stats.rank}. sırada</span>}
        </h2>
        {seasons.length > 1 && (
          <select
            className="select"
            value={stats.seasonId}
            onChange={(e) => setSeasonId(Number(e.target.value))}
            aria-label="Sezon seç"
          >
            {seasons.map((season) => (
              <option key={season.id} value={season.id}>
                Sezon {season.seasonNumber}
              </option>
            ))}
          </select>
        )}
      </div>

      {stats.overall.played === 0 ? (
        <p className="muted">Bu sezonda henüz lig maçı oynamadı.</p>
      ) : (
        <>
          <div className="stats-grid">
            <table className="table compact split-table">
              <thead>
                <tr>
                  <th />
                  <th className="num">O</th>
                  <th className="num">G</th>
                  <th className="num">B</th>
                  <th className="num">M</th>
                  <th className="num">A</th>
                  <th className="num">Y</th>
                  <th className="num">P</th>
                </tr>
              </thead>
              <tbody>
                <SplitRow label="Toplam" split={stats.overall} />
                <SplitRow label="İç saha" split={stats.home} />
                <SplitRow label="Deplasman" split={stats.away} />
              </tbody>
            </table>
            <dl className="stat-list">
              <div>
                <dt>Form</dt>
                <dd>
                  <FormBadges form={stats.form} />
                </dd>
              </div>
              <div>
                <dt>Topla oynama ort.</dt>
                <dd>%{stats.averagePossession}</dd>
              </div>
              <div>
                <dt>Şut isabeti</dt>
                <dd>
                  %{stats.shotAccuracy ?? 0}{' '}
                  <span className="muted">
                    ({stats.shotsOnTarget} / {stats.shots})
                  </span>
                </dd>
              </div>
              <div>
                <dt>Gol yemediği maç</dt>
                <dd>{stats.cleanSheets}</dd>
              </div>
              <div>
                <dt>Kartlar</dt>
                <dd>
                  🟨{stats.yellowCards} 🟥{stats.redCards}
                </dd>
              </div>
              <div>
                <dt>En golcü</dt>
                <dd>
                  {stats.topScorer ? (
                    <Link to={`/players/${stats.topScorer.playerId}`}>
                      {stats.topScorer.name} ({stats.topScorer.value})
                    </Link>
                  ) : (
                    '–'
                  )}
                </dd>
              </div>
              <div>
                <dt>En çok asist</dt>
                <dd>
                  {stats.topAssister ? (
                    <Link to={`/players/${stats.topAssister.playerId}`}>
                      {stats.topAssister.name} ({stats.topAssister.value})
                    </Link>
                  ) : (
                    '–'
                  )}
                </dd>
              </div>
            </dl>
          </div>

          <div className="chart-grid-2">
            <LineChart
              title="Haftalık sıralama"
              points={stats.weeks.map((w) => ({ x: w.week, y: w.rank }))}
              invert
              yMin={1}
              yMax={Math.max(LEAGUE_SIZE, ...stats.weeks.map((w) => w.rank))}
              formatY={(value) => `${value}.`}
            />
            <LineChart
              title="Güç geçmişi"
              points={stats.weeks.filter((w) => w.strength !== null).map((w) => ({ x: w.week, y: w.strength! }))}
            />
          </div>
        </>
      )}
    </div>
  )
}

function SplitRow({ label, split }: { label: string; split: SplitStats }) {
  return (
    <tr>
      <td className="strong">{label}</td>
      <td className="num">{split.played}</td>
      <td className="num">{split.won}</td>
      <td className="num">{split.drawn}</td>
      <td className="num">{split.lost}</td>
      <td className="num">{split.goalsFor}</td>
      <td className="num">{split.goalsAgainst}</td>
      <td className="num strong">{split.points}</td>
    </tr>
  )
}

function firstFreeShirtNumber(players: Player[]) {
  const used = new Set(players.map((p) => p.shirtNumber))
  let number = 1
  while (used.has(number) && number < 99) {
    number++
  }
  return number
}

interface PlayerEditRowProps {
  player: Player | null
  defaultShirtNumber?: number
  onSubmit: (request: PlayerRequest) => Promise<Player>
  onSaved: (player: Player) => void
  onCancel: () => void
}

function PlayerEditRow({ player, defaultShirtNumber = 1, onSubmit, onSaved, onCancel }: PlayerEditRowProps) {
  const [name, setName] = useState(player?.name ?? '')
  const [position, setPosition] = useState<Position>(player?.position ?? 'FORWARD')
  const [shirtNumber, setShirtNumber] = useState(String(player?.shirtNumber ?? defaultShirtNumber))
  const [strength, setStrength] = useState(String(player?.strength ?? 70))
  const [age, setAge] = useState(String(player?.age ?? 24))
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const formId = `player-form-${player?.id ?? 'new'}`

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      onSaved(
        await onSubmit({
          name: name.trim(),
          position,
          shirtNumber: Number(shirtNumber),
          strength: Number(strength),
          age: Number(age),
        }),
      )
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <tr className="editing-row">
        <td className="num">
          <form id={formId} onSubmit={handleSubmit} />
          <input
            form={formId}
            className="input-sm input-num"
            type="number"
            min={1}
            max={99}
            required
            value={shirtNumber}
            onChange={(e) => setShirtNumber(e.target.value)}
            aria-label="Forma numarası"
          />
        </td>
        <td>
          <input
            form={formId}
            className="input-sm"
            required
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Oyuncu adı"
            aria-label="Oyuncu adı"
            autoFocus={player === null}
          />
        </td>
        <td>
          <select
            form={formId}
            className="input-sm"
            value={position}
            onChange={(e) => setPosition(e.target.value as Position)}
            aria-label="Mevki"
          >
            {POSITIONS.map((p) => (
              <option key={p} value={p}>
                {POSITION_LABELS[p]}
              </option>
            ))}
          </select>
        </td>
        <td className="num">
          <input
            form={formId}
            className="input-sm input-num"
            type="number"
            min={16}
            max={45}
            required
            value={age}
            onChange={(e) => setAge(e.target.value)}
            aria-label="Yaş"
          />
        </td>
        <td className="num">
          <input
            form={formId}
            className="input-sm input-num"
            type="number"
            min={1}
            max={100}
            required
            value={strength}
            onChange={(e) => setStrength(e.target.value)}
            aria-label="Oyuncu gücü"
          />
        </td>
        <td colSpan={7} />
        <td className="row-actions">
          <button form={formId} type="submit" className="btn btn-sm btn-primary" disabled={saving}>
            {player ? 'Kaydet' : 'Ekle'}
          </button>
          <button type="button" className="btn btn-sm" onClick={onCancel} disabled={saving}>
            Vazgeç
          </button>
        </td>
      </tr>
      {error && (
        <tr className="editing-row">
          <td colSpan={COLUMN_COUNT}>
            <span className="alert-inline">{error}</span>
          </td>
        </tr>
      )}
    </>
  )
}

export default TeamDetailPage
