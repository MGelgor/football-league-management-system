import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Team } from '../api/types'

function TeamsPage() {
  const [teams, setTeams] = useState<Team[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getTeams().then(setTeams).catch((e) => setError(errorMessage(e)))
  }, [])

  return (
    <section>
      <h1>Takımlar</h1>
      {error && <p className="alert alert-error">{error}</p>}
      {teams && <p className="muted">{teams.length} takım kayıtlı.</p>}
    </section>
  )
}

export default TeamsPage
