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
  formation: Formation
  playStyle: PlayStyle
  // Avro
  budget: number | null
  // Teknik direktörü olmayan (eski) takımlarda null
  manager: ManagerRef | null
}

export type Formation = 'F442' | 'F433' | 'F352' | 'F532' | 'F451'
export type PlayStyle = 'ATTACKING' | 'BALANCED' | 'DEFENSIVE'

export interface ManagerRef {
  id: number
  name: string
  tacticalSkill: number
  preferredFormation: Formation
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
  // Form çarpanı 0.9-1.1 (son 5 maç reytinginden), yorgunluk (efektif güçten düşülen)
  form: number
  fatigue: number
  injurySeverity: InjurySeverity | null
  // Avro; haftalık maaş, sözleşmenin bittiği sezon
  marketValue: number
  wage: number | null
  contractUntil: number | null
}

export type InjurySeverity = 'MINOR' | 'MODERATE' | 'SERIOUS'

export interface InjuryLine {
  matchId: number
  seasonNumber: number
  competition: Competition
  weekNumber: number
  cupRound: CupRound | null
  minute: number
  // Bu özellikten önceki sakatlıklarda null
  matches: number | null
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
  // Kendi kalesine attığı goller
  ownGoals: number
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
  // Serbest oyuncuda null
  teamId: number | null
  teamName: string | null
  teamActive: boolean
  recentRatings: number[]
  form: number
  injurySeverity: InjurySeverity | null
  seasons: PlayerSeasonLine[]
  goals: PlayerGoalLine[]
  injuries: InjuryLine[]
  transfers: TransferEntry[]
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
  // Bu özellikten önceki maçlarda null
  formation: Formation | null
  playStyle: PlayStyle | null
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
  // OWN_GOAL: oyuncu (home tarafının oyuncusu) kendi kalesine attı, gol rakibe yazılır
  type: MatchEventType
  home: boolean
  playerId: number
  playerName: string
  position: Position
  shirtNumber: number
  assistName: string | null
  // Maç anlatımı cümlesi
  commentary: string
  // Penaltı golü / kaçan penaltı
  penalty: boolean
}

export type MatchEventType = 'GOAL' | 'YELLOW_CARD' | 'RED_CARD' | 'INJURY' | 'OWN_GOAL' | 'PENALTY_MISSED' | 'VAR_DISALLOWED'

export interface RefereeRef {
  id: number
  name: string
  strictness: number
}

export interface Referee {
  id: number
  name: string
  strictness: number
  matches: number
  yellowCards: number
  redCards: number
  penalties: number
  yellowCardsPerMatch: number
  redCardsPerMatch: number
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
  // null: hakem atanmadan oynanmış eski maçlar
  referee: RefereeRef | null
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
  // Canlı yayın bu hafta numarasıyla açılır (101+)
  weekNumber: number
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

export interface SnapshotSummary {
  teamCount: number
  seasonNumber: number | null
  playedMatches: number
}

export interface SaveSlot {
  name: string
  savedAt: string
  sizeBytes: number
  summary: SnapshotSummary
}

export interface LiveMatch {
  matchId: number
  homeTeamId: number
  homeTeamName: string
  awayTeamId: number
  awayTeamName: string
  homeScore: number
  awayScore: number
  homePenalties: number | null
  awayPenalties: number | null
}

export interface LiveItem {
  matchId: number
  minute: number
  // MatchEvent türü ya da SUBSTITUTION
  type: string
  home: boolean
  playerId: number
  playerName: string
  assistName: string | null
  text: string
  penalty: boolean
}

export interface LiveMinute {
  minute: number
  phase: 'KICK_OFF' | 'HALF_TIME' | 'FULL_TIME' | null
  items: LiveItem[]
  scores: { matchId: number; home: number; away: number }[]
}

export type FinanceType = 'TICKETS' | 'WAGES' | 'LEAGUE_PRIZE' | 'CUP_PRIZE' | 'TRANSFER_SALE' | 'TRANSFER_PURCHASE'

export interface Finances {
  teamId: number
  budget: number
  seasonNumber: number | null
  income: number
  expenses: number
  totals: { type: FinanceType; amount: number }[]
  weeklyWageBill: number
  squadValue: number
  contracts: {
    playerId: number
    name: string
    position: Position
    age: number
    strength: number
    marketValue: number
    wage: number
    contractUntil: number | null
  }[]
  recentEntries: { weekNumber: number | null; type: FinanceType; amount: number; description: string }[]
}

export interface TransferWindow {
  open: boolean
  upcomingSeasonNumber: number | null
  message: string
}

export interface MarketPlayer {
  playerId: number
  name: string
  position: Position
  age: number
  strength: number
  form: number
  teamId: number | null
  teamName: string | null
  freeAgent: boolean
  marketValue: number
  askingPrice: number
  expectedWage: number
  contractUntil: number | null
}

export interface TransferEntry {
  id: number
  playerId: number
  playerName: string
  position: Position
  // null: serbest oyuncu imzası
  fromTeamId: number | null
  fromTeamName: string | null
  toTeamId: number
  toTeamName: string
  fee: number
  seasonNumber: number
}

export interface OfferResult {
  status: 'ACCEPTED' | 'COUNTER' | 'REJECTED'
  askingPrice: number
  message: string
  transfer: TransferEntry | null
}

export interface MyTeam {
  active: boolean
  managerName: string | null
  team: Team | null
  seasonNumber: number | null
  status: string | null
  nextMatch: {
    match: Match
    competition: Competition
    weekNumber: number
    cupRound: CupRound | null
    home: boolean
    lineupSaved: boolean
  } | null
  standing: Standing | null
  standingsAround: Standing[]
  recentResults: { match: Match; competition: Competition; weekNumber: number; cupRound: CupRound | null; result: FormResult }[]
  unavailable: Player[]
  transferWindowOpen: boolean
  // Yönetim kurulu güveni (0-100) ve sezon hedefleri
  confidence: number
  targetRank: number | null
  cupTarget: CupRound | null
  unreadMessages: number
  // Kovuldu / istifa etti: gelen kutusunda iş teklifleri olabilir
  unemployed: boolean
}

export type InboxMessageType =
  | 'WELCOME'
  | 'BOARD'
  | 'TRANSFER_OFFER'
  | 'CONTRACT'
  | 'INJURY'
  | 'YOUTH'
  | 'SACKED'
  | 'JOB_OFFER'

export interface InboxMessage {
  id: number
  type: InboxMessageType
  title: string
  body: string
  seasonNumber: number
  read: boolean
  resolved: boolean
  actionable: boolean
  playerId: number | null
  playerName: string | null
  teamId: number | null
  teamName: string | null
  amount: number | null
}

export interface ActionResult {
  status: 'ACCEPTED' | 'COUNTER' | 'REJECTED' | 'DONE'
  amount: number | null
  message: string
}

export interface AcademyPlayer {
  id: number
  name: string
  position: Position
  age: number
  strength: number
}

export interface CareerSpell {
  teamId: number
  teamName: string
  startSeason: number
  endSeason: number | null
  endReason: string | null
  matches: number
  wins: number
  draws: number
  losses: number
  leagueTitles: number
  cups: number
}

export interface Career {
  managerName: string
  spells: CareerSpell[]
  matches: number
  wins: number
  draws: number
  losses: number
  leagueTitles: number
  cups: number
}

export interface LineupSquadPlayer {
  id: number
  name: string
  position: Position
  shirtNumber: number
  strength: number
  effectiveStrength: number
  form: number
  fatigue: number
  available: boolean
  suspendedMatches: number
  injuredMatches: number
  injurySeverity: InjurySeverity | null
}

export interface Lineup {
  matchId: number
  saved: boolean
  formation: Formation
  playStyle: PlayStyle
  starterIds: number[]
  captainId: number | null
  penaltyTakerId: number | null
  squad: LineupSquadPlayer[]
}

export interface LineupRequest {
  formation: Formation
  playStyle: PlayStyle
  starterIds: number[]
  captainId: number
  penaltyTakerId: number
}

export type TeamTalk = 'NONE' | 'CALM' | 'MOTIVATE' | 'CRITICIZE'

export interface SessionPlayer {
  id: number
  name: string
  position: Position
  shirtNumber: number
  strength: number
  form: number
  rating: number | null
}

export interface LiveSession {
  matchId: number
  minute: number
  finished: boolean
  match: LiveMatch
  userHome: boolean
  homeScore: number
  awayScore: number
  events: LiveItem[]
  onPitch: SessionPlayer[]
  bench: SessionPlayer[]
  substitutionsLeft: number
  formation: Formation
  playStyle: PlayStyle
  talkResult: string | null
  otherResults: Match[]
}
