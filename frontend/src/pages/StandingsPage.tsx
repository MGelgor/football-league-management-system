import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Standing } from '../api/types'

function StandingsPage() {
  const [standings, setStandings] = useState<Standing[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getStandings().then(setStandings).catch((e) => setError(errorMessage(e)))
  }, [])

  return (
    <section>
      <h1>Puan Durumu</h1>
      {error && <p className="alert alert-error">{error}</p>}
      {standings && <p className="muted">{standings.length} takım tabloda.</p>}
    </section>
  )
}

export default StandingsPage
