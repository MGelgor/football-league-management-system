export interface Team {
  id: number
  name: string
  foundedYear: number
  colors: string
  logoUrl: string | null
  strength: number
  morale: number
}

export interface TeamRequest {
  name: string
  foundedYear: number
  colors: string
}

export interface Match {
  id: number
  homeTeamId: number
  homeTeamName: string
  awayTeamId: number
  awayTeamName: string
  homeScore: number | null
  awayScore: number | null
  played: boolean
}

export interface MatchWeek {
  weekNumber: number
  matches: Match[]
}

export interface Standing {
  rank: number
  teamId: number
  teamName: string
  played: number
  won: number
  drawn: number
  lost: number
  goalsFor: number
  goalsAgainst: number
  goalDifference: number
  points: number
}

export interface SeasonResult {
  championName: string
  championPoints: number
  finalStandings: Standing[]
}
