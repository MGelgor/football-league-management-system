/** Süper Lig (1) / 2. Lig (2) seçici; Puan Durumu ve Fikstür sayfalarında. */
function LeagueSwitch({ division, onChange }: { division: number; onChange: (division: number) => void }) {
  return (
    <span className="segmented" role="group" aria-label="Lig">
      {[
        { value: 1, label: 'Süper Lig' },
        { value: 2, label: '2. Lig' },
      ].map(({ value, label }) => (
        <button
          key={value}
          className={`btn btn-sm${division === value ? ' is-selected' : ''}`}
          onClick={() => onChange(value)}
          aria-pressed={division === value}
        >
          {label}
        </button>
      ))}
    </span>
  )
}

export default LeagueSwitch
