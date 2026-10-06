interface PlayerStatusProps {
  suspendedMatches: number
  injuredMatches: number
}

/** Cezalı / sakat oyuncu rozeti; ikisi de yoksa hiçbir şey göstermez. */
function PlayerStatus({ suspendedMatches, injuredMatches }: PlayerStatusProps) {
  return (
    <>
      {suspendedMatches > 0 && (
        <span className="status status-suspended" title={`${suspendedMatches} maç ceza`}>
          Cezalı {suspendedMatches}
        </span>
      )}
      {injuredMatches > 0 && (
        <span className="status status-injured" title={`${injuredMatches} maç sakat`}>
          Sakat {injuredMatches}
        </span>
      )}
    </>
  )
}

export default PlayerStatus
