interface ProbabilityBarProps {
  home: number
  draw: number
  away: number
}

function ProbabilityBar({ home, draw, away }: ProbabilityBarProps) {
  return (
    <div className="prob" aria-label={`Ev sahibi %${home}, beraberlik %${draw}, deplasman %${away}`}>
      <div className="prob-bar" aria-hidden>
        <span className="prob-home" style={{ width: `${home}%` }} />
        <span className="prob-draw" style={{ width: `${draw}%` }} />
        <span className="prob-away" style={{ width: `${away}%` }} />
      </div>
      <div className="prob-labels" aria-hidden>
        <span>1 · %{home}</span>
        <span>X · %{draw}</span>
        <span>2 · %{away}</span>
      </div>
    </div>
  )
}

export default ProbabilityBar
