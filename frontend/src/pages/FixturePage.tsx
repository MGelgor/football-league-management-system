import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { MatchWeek } from '../api/types'

function FixturePage() {
  const [weeks, setWeeks] = useState<MatchWeek[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getFixture().then(setWeeks).catch((e) => setError(errorMessage(e)))
  }, [])

  return (
    <section>
      <h1>Fikstür</h1>
      {error && <p className="alert alert-error">{error}</p>}
      {weeks && <p className="muted">{weeks.length} hafta.</p>}
    </section>
  )
}

export default FixturePage
