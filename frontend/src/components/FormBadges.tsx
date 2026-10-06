import type { FormResult } from '../api/types'

const TITLES: Record<FormResult, string> = { G: 'Galibiyet', B: 'Beraberlik', M: 'Mağlubiyet' }

/** Son maçların sonuçları, eskiden yeniye (en sağdaki en son maç). */
function FormBadges({ form }: { form: FormResult[] }) {
  return (
    <span className="form" aria-label={`Son ${form.length} maç: ${form.join(' ')}`}>
      {form.map((result, index) => (
        <span key={index} className={`form-badge form-${result}`} title={TITLES[result]}>
          {result}
        </span>
      ))}
    </span>
  )
}

export default FormBadges
