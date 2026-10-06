export interface Team {
  id: number
  name: string
  foundedYear: number
  colors: string
  logoUrl: string | null
  strength: number
  morale: number
  bigFour: boolean
  lastStrengthChange: number
  seasonStrengthChange: number
  // false: küme düştü ya da silindi (arşivde)
  active: boolean
}

export interface TeamRequest {
  name: string
  foundedYear: number
  colors: string
  // Yalnızca oluştururken, isteğe bağlı; eksik mevkiler rastgele oyuncularla 18'e tamamlanır
  players?: PlayerRequest[]
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
  homeWinProbability: number
  drawProbability: number
  awayWinProbability: number
  // Yalnızca beraberlikle biten kupa maçlarında
  homePenalties: number | null
  awayPenalties: number | null
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
  rankChange: number
  form: FormResult[]
  zone: Zone | null
}

export type FormResult = 'G' | 'B' | 'M'
export type Zone = 'CHAMPIONS_LEAGUE' | 'EUROPA_LEAGUE' | 'RELEGATION'

export interface SeasonResult {
  championName: string
  championPoints: number
  finalStandings: Standing[]
}

export interface Season {
  id: number
  seasonNumber: number
  finished: boolean
  championTeamId: number | null
  championName: string | null
  totalMatches: number
  playedMatches: number
  cupWinnerTeamId: number | null
  cupWinnerName: string | null
  relegated: TeamRef[]
  promoted: TeamRef[]
}

export interface TeamRef {
  id: number
  name: string
}

export type Competition = 'LEAGUE' | 'CUP'
export type CupRound = 'QUARTER_FINAL' | 'SEMI_FINAL' | 'FINAL'

export type Position = 'GOALKEEPER' | 'DEFENDER' | 'MIDFIELDER' | 'FORWARD'

/** Sezon alanları güncel sezonun lig maçları, career* tüm sezon ve turnuvalar. averageRating oynamadıysa null. */
export interface Player {
  id: number
  name: string
  position: Position
  shirtNumber: number
  strength: number
  age: number
  lastStrengthChange: number
  suspendedMatches: number
  injuredMatches: number
  appearances: number
  minutes: number
  goals: number
  assists: number
  yellowCards: number
  redCards: number
  averageRating: number | null
  playerOfTheMatch: number
  careerAppearances: number
  careerGoals: number
  careerAssists: number
}

export interface PlayerRequest {
  name: string
  position: Position
  shirtNumber: number
  strength: number
  age: number
}

export interface PlayerStats {
  playerId: number
  playerName: string
  position: Position
  teamId: number
  teamName: string
  appearances: number
  minutes: number
  goals: number
  assists: number
  yellowCards: number
  redCards: number
  averageRating: number | null
  playerOfTheMatch: number
}

export interface PlayerSeasonLine {
  seasonNumber: number
  competition: Competition
  appearances: number
  minutes: number
  goals: number
  assists: number
  yellowCards: number
  redCards: number
  averageRating: number | null
  playerOfTheMatch: number
}

export interface PlayerGoalLine {
  matchId: number
  seasonNumber: number
  competition: Competition
  weekNumber: number
  cupRound: CupRound | null
  opponentName: string
  home: boolean
  score: string
  minute: number
  type: 'GOAL' | 'ASSIST'
  partnerName: string | null
}

export interface PlayerProfile {
  id: number
  name: string
  position: Position
  shirtNumber: number
  strength: number
  age: number
  lastStrengthChange: number
  retired: boolean
  suspendedMatches: number
  injuredMatches: number
  teamId: number
  teamName: string
  teamActive: boolean
  seasons: PlayerSeasonLine[]
  goals: PlayerGoalLine[]
}

export interface MatchStats {
  possession: number
  shots: number
  shotsOnTarget: number
  corners: number
  fouls: number
  offsides: number
  saves: number
  yellowCards: number
  redCards: number
}

export interface LineupEntry {
  playerId: number
  playerName: string
  position: Position
  shirtNumber: number
  starter: boolean
  minuteOn: number
  minuteOff: number
  replacedPlayerName: string | null
  rating: number
  playerOfTheMatch: boolean
}

export interface MatchSide {
  teamId: number
  teamName: string
  logoUrl: string | null
  score: number | null
  penalties: number | null
  stats: MatchStats | null
  lineup: LineupEntry[]
}

export interface MatchEvent {
  minute: number
  type: 'GOAL' | 'YELLOW_CARD' | 'RED_CARD' | 'INJURY'
  home: boolean
  playerId: number
  playerName: string
  position: Position
  shirtNumber: number
  assistName: string | null
}

export interface MatchDetail {
  id: number
  seasonNumber: number
  weekNumber: number
  competition: Competition
  cupRound: CupRound | null
  played: boolean
  home: MatchSide
  away: MatchSide
  homeWinProbability: number
  drawProbability: number
  awayWinProbability: number
  events: MatchEvent[]
}

export type CupStatus = 'NO_SEASON' | 'LEAGUE_IN_PROGRESS' | 'NOT_STARTED' | 'IN_PROGRESS' | 'FINISHED'

export interface CupTie {
  match: Match
  homeSeed: number
  awaySeed: number
  winnerTeamId: number | null
}

export interface CupRoundState {
  round: CupRound
  played: boolean
  ties: CupTie[]
}

export interface Cup {
  seasonId: number | null
  seasonNumber: number | null
  status: CupStatus
  rounds: CupRoundState[]
  winnerTeamId: number | null
  winnerName: string | null
}

export interface RecordEntry {
  title: string
  holder: string
  value: string
  detail: string | null
  teamId: number | null
  playerId: number | null
  matchId: number | null
}

export interface SplitStats {
  played: number
  won: number
  drawn: number
  lost: number
  goalsFor: number
  goalsAgainst: number
  points: number
}

export interface PlayerRef {
  playerId: number
  name: string
  value: number
}

export interface WeekPoint {
  week: number
  rank: number
  points: number
  strength: number | null
}

export interface TeamSeasonStats {
  seasonId: number | null
  seasonNumber: number | null
  rank: number | null
  overall: SplitStats
  home: SplitStats
  away: SplitStats
  averagePossession: number | null
  shots: number
  shotsOnTarget: number
  shotAccuracy: number | null
  cleanSheets: number
  yellowCards: number
  redCards: number
  topScorer: PlayerRef | null
  topAssister: PlayerRef | null
  form: FormResult[]
  weeks: WeekPoint[]
}

export interface HeadToHeadMatch {
  matchId: number
  seasonNumber: number
  competition: Competition
  weekNumber: number
  cupRound: CupRound | null
  homeTeamId: number
  homeTeamName: string
  awayTeamName: string
  homeScore: number
  awayScore: number
  homePenalties: number | null
  awayPenalties: number | null
}

export interface HeadToHead {
  teamA: Team
  teamB: Team
  played: number
  winsA: number
  draws: number
  winsB: number
  goalsA: number
  goalsB: number
  matches: HeadToHeadMatch[]
}
