import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { HeadToHead, Team } from '../api/types'
import TeamLogo from '../components/TeamLogo'
import { roundLabel } from '../labels'

/** İki takımın aralarındaki tüm maçlar (lig + kupa, tüm sezonlar). Takımlar URL'de: ?a=ID&b=ID */
function ComparePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const teamA = searchParams.get('a') ? Number(searchParams.get('a')) : null
  const teamB = searchParams.get('b') ? Number(searchParams.get('b')) : null

  const [teams, setTeams] = useState<Team[]>([])
  const [result, setResult] = useState<HeadToHead | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getTeams()
      .then((list) => setTeams(list.toSorted((x, y) => x.name.localeCompare(y.name, 'tr'))))
      .catch((e) => setError(errorMessage(e)))
  }, [])

  useEffect(() => {
    if (teamA === null || teamB === null || teamA === teamB) {
      return
    }
    api
      .getHeadToHead(teamA, teamB)
      .then(setResult)
      .catch((e) => setError(errorMessage(e)))
  }, [teamA, teamB])

  function select(key: 'a' | 'b', value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) {
      next.set(key, value)
    } else {
      next.delete(key)
    }
    setSearchParams(next)
  }

  const ready = teamA !== null && teamB !== null && teamA !== teamB
  const shown = ready && result && result.teamA.id === teamA && result.teamB.id === teamB ? result : null

  return (
    <section>
      <h1>Karşılaştır</h1>
      {error && <p className="alert alert-error">{error}</p>}

      <div className="card compare-pickers">
        <TeamSelect label="1. takım" value={teamA} teams={teams} onChange={(v) => select('a', v)} />
        <span className="muted">vs</span>
        <TeamSelect label="2. takım" value={teamB} teams={teams} onChange={(v) => select('b', v)} />
      </div>

      {!ready && <p className="muted">Aralarındaki maçları görmek için iki farklı takım seçin.</p>}

      {shown && (
        <>
          <div className="card compare-summary">
            <CompareTeam team={shown.teamA} />
            <div className="compare-numbers">
              <div className="compare-record">
                <span className="strong">{shown.winsA}</span>
                <span className="muted">{shown.draws} beraberlik</span>
                <span className="strong">{shown.winsB}</span>
              </div>
              <div className="muted">
                {shown.played} maç · goller {shown.goalsA} - {shown.goalsB}
              </div>
            </div>
            <CompareTeam team={shown.teamB} />
          </div>

          <div className="card">
            <h2>Maçlar</h2>
            {shown.matches.length === 0 ? (
              <p className="muted">Bu iki takım henüz karşılaşmadı.</p>
            ) : (
              <ul className="match-list">
                {shown.matches.toReversed().map((match) => (
                  <li key={match.matchId}>
                    <Link to={`/matches/${match.matchId}`} className="match match-link">
                      <span className="match-team match-home">{match.homeTeamName}</span>
                      <span className="score">
                        {match.homeScore} - {match.awayScore}
                      </span>
                      <span className="match-team">
                        {match.awayTeamName}
                        <span className="muted compare-when">
                          {' '}
                          · Sezon {match.seasonNumber}, {roundLabel(match.competition, match.weekNumber, match.cupRound)}
                          {match.homePenalties !== null && ` (pen. ${match.homePenalties}-${match.awayPenalties})`}
                        </span>
                      </span>
                      <span className="match-more" aria-hidden>
                        ›
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </section>
  )
}

interface TeamSelectProps {
  label: string
  value: number | null
  teams: Team[]
  onChange: (value: string) => void
}

function TeamSelect({ label, value, teams, onChange }: TeamSelectProps) {
  return (
    <label className="field">
      <span>{label}</span>
      <select className="select" value={value ?? ''} onChange={(e) => onChange(e.target.value)}>
        <option value="">Takım seçin</option>
        {teams.map((team) => (
          <option key={team.id} value={team.id}>
            {team.name}
          </option>
        ))}
      </select>
    </label>
  )
}

function CompareTeam({ team }: { team: Team }) {
  return (
    <Link to={`/teams/${team.id}`} className="scoreboard-team">
      <TeamLogo name={team.name} logoUrl={team.logoUrl} large />
      <span>{team.name}</span>
      <span className="muted">Güç {team.strength}</span>
    </Link>
  )
}

export default ComparePage
