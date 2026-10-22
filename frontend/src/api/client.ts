import type {
  AcademyPlayer,
  ActionResult,
  Career,
  Cup,
  Finances,
  Formation,
  HeadToHead,
  InboxMessage,
  Lineup,
  LineupRequest,
  LiveSession,
  MarketPlayer,
  MyTeam,
  OfferResult,
  OpponentReport,
  MatchDetail,
  MatchWeek,
  Player,
  PlayerProfile,
  PlayerRequest,
  PlayerStats,
  PlayStyle,
  RecordEntry,
  Room,
  RoomJoin,
  RoomSettings,
  Referee,
  SaveSlot,
  Season,
  SeasonResult,
  Standing,
  Team,
  TeamRequest,
  TeamSeasonStats,
  TeamTalk,
  TransferEntry,
  TransferWindow,
} from './types'
import { apiUrl } from './config'
import { roomHeaders, withRoom } from './rooms'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const UNREACHABLE_MESSAGE = 'Sunucuya ulaşılamadı. Backend çalışıyor mu?'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    // Aktif odanın başlıkları; isteğin kendi başlıkları (ör. başka bir odanın token'ı) öncelikli
    response = await fetch(apiUrl(path), {
      ...init,
      headers: { ...roomHeaders(), ...(init?.headers as Record<string, string> | undefined) },
    })
  } catch {
    throw new ApiError(0, UNREACHABLE_MESSAGE)
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

async function readErrorMessage(response: Response): Promise<string> {
  // Vite proxy backend'e ulaşamazsa gövdesiz 502 döner
  const fallback = response.status === 502 ? UNREACHABLE_MESSAGE : `İstek başarısız oldu (HTTP ${response.status})`
  try {
    const body: Record<string, unknown> = await response.json()
    if (typeof body.message === 'string' && body.message) {
      return body.message
    }
    // Validasyon hatası: { alanAdı: mesaj } (Spring'in varsayılan hata gövdesinde "status" alanı olur)
    if (typeof body.status !== 'number') {
      return Object.values(body).join(' ')
    }
    return fallback
  } catch {
    return fallback
  }
}

function jsonRequest(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Beklenmeyen bir hata oluştu'
}

function withSeason(path: string, seasonId?: number) {
  return seasonId === undefined ? path : `${path}?seasonId=${seasonId}`
}

/** Belirli bir odanın token'ıyla istek (aktif oda olmayabilir); isteğin diğer başlıkları korunur. */
function withToken(token: string, init: RequestInit = {}): RequestInit {
  return { ...init, headers: { ...(init.headers as Record<string, string> | undefined), 'X-Room-Token': token } }
}

export const api = {
  createRoom: (roomName: string, nickname: string) =>
    request<RoomJoin>('/api/rooms', jsonRequest('POST', { roomName, nickname })),
  joinRoom: (code: string, nickname: string) =>
    request<RoomJoin>(`/api/rooms/${encodeURIComponent(code)}/join`, jsonRequest('POST', { nickname })),
  getRoom: (code: string, token: string) => request<Room>(`/api/rooms/${code}`, withToken(token)),
  updateRoomSettings: (code: string, token: string, settings: RoomSettings) =>
    request<Room>(`/api/rooms/${code}/settings`, withToken(token, jsonRequest('PUT', settings))),
  startRoom: (code: string, token: string) =>
    request<Room>(`/api/rooms/${code}/start`, withToken(token, { method: 'POST' })),
  setReady: (code: string, token: string, ready: boolean) =>
    request<Room>(`/api/rooms/${code}/ready`, withToken(token, jsonRequest('POST', { ready }))),
  runRoomNow: (code: string, token: string) =>
    request<Room>(`/api/rooms/${code}/run-now`, withToken(token, { method: 'POST' })),
  leaveRoom: (code: string, token: string) =>
    request<void>(`/api/rooms/${code}/members/me`, withToken(token, { method: 'DELETE' })),
  deleteRoom: (code: string, token: string) =>
    request<void>(`/api/rooms/${code}`, withToken(token, { method: 'DELETE' })),

  getTeams: () => request<Team[]>('/api/teams'),
  getTeam: (id: number) => request<Team>(`/api/teams/${id}`),
  createTeam: (team: TeamRequest) => request<Team>('/api/teams', jsonRequest('POST', team)),
  createRandomTeams: (count: number) => request<Team[]>('/api/teams/random', jsonRequest('POST', { count })),
  updateTeam: (id: number, team: TeamRequest) => request<Team>(`/api/teams/${id}`, jsonRequest('PUT', team)),
  updateTactics: (id: number, formation: Formation, playStyle: PlayStyle) =>
    request<Team>(`/api/teams/${id}/tactics`, jsonRequest('PUT', { formation, playStyle })),
  deleteTeam: (id: number) => request<void>(`/api/teams/${id}`, { method: 'DELETE' }),
  uploadLogo: (id: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<Team>(`/api/teams/${id}/logo`, { method: 'POST', body: form })
  },

  getPlayers: (teamId: number) => request<Player[]>(`/api/teams/${teamId}/players`),
  addPlayer: (teamId: number, player: PlayerRequest) =>
    request<Player>(`/api/teams/${teamId}/players`, jsonRequest('POST', player)),
  updatePlayer: (id: number, player: PlayerRequest) =>
    request<Player>(`/api/players/${id}`, jsonRequest('PUT', player)),
  getPlayerStats: (seasonId?: number) => request<PlayerStats[]>(withSeason('/api/players/stats', seasonId)),
  getPlayerProfile: (id: number) => request<PlayerProfile>(`/api/players/${id}`),
  getTeamStats: (teamId: number, seasonId?: number) =>
    request<TeamSeasonStats>(withSeason(`/api/teams/${teamId}/stats`, seasonId)),
  getFinances: (teamId: number, seasonId?: number) =>
    request<Finances>(withSeason(`/api/teams/${teamId}/finances`, seasonId)),
  getHeadToHead: (teamA: number, teamB: number) =>
    request<HeadToHead>(`/api/teams/head-to-head?teamA=${teamA}&teamB=${teamB}`),

  getFixture: (seasonId?: number) => request<MatchWeek[]>(withSeason('/api/fixtures', seasonId)),
  generateFixture: () => request<MatchWeek[]>('/api/fixtures/generate', { method: 'POST' }),
  resetFixture: () => request<void>('/api/fixtures', { method: 'DELETE' }),
  playWeek: (weekNumber: number, auto = false) =>
    request<MatchWeek>(`/api/weeks/${weekNumber}/play${auto ? '?auto=true' : ''}`, { method: 'POST' }),
  getMatch: (id: number) => request<MatchDetail>(`/api/matches/${id}`),

  getStandings: (seasonId?: number) => request<Standing[]>(withSeason('/api/standings', seasonId)),
  getSeasons: () => request<Season[]>('/api/seasons'),
  playSeason: (auto = false) =>
    request<SeasonResult>(`/api/seasons/play-all${auto ? '?auto=true' : ''}`, { method: 'POST' }),

  getCup: (seasonId?: number) => request<Cup>(withSeason('/api/cup', seasonId)),
  startCup: () => request<Cup>('/api/cup/start', { method: 'POST' }),
  playCupRound: (auto = false) =>
    request<Cup>(`/api/cup/play-round${auto ? '?auto=true' : ''}`, { method: 'POST' }),
  playCupAll: (auto = false) => request<Cup>(`/api/cup/play-all${auto ? '?auto=true' : ''}`, { method: 'POST' }),

  getRecords: () => request<RecordEntry[]>('/api/records'),
  getReferees: () => request<Referee[]>('/api/referees'),

  getTransferWindow: () => request<TransferWindow>('/api/transfers/window'),
  getMarket: () => request<MarketPlayer[]>('/api/transfers/market'),
  getTransfers: () => request<TransferEntry[]>('/api/transfers'),
  makeOffer: (playerId: number, buyerTeamId: number, fee: number) =>
    request<OfferResult>('/api/transfers/offers', jsonRequest('POST', { playerId, buyerTeamId, fee })),
  signFreeAgent: (playerId: number, teamId: number) =>
    request<TransferEntry>(`/api/transfers/free-agents/${playerId}/sign`, jsonRequest('POST', { teamId })),

  getMyTeam: () => request<MyTeam>('/api/my-team'),
  startMyTeam: (managerName: string, teamId: number) =>
    request<MyTeam>('/api/my-team', jsonRequest('POST', { managerName, teamId })),
  stopMyTeam: () => request<void>('/api/my-team', { method: 'DELETE' }),
  getLineup: () => request<Lineup>('/api/my-team/lineup'),
  saveLineup: (lineup: LineupRequest) => request<Lineup>('/api/my-team/lineup', jsonRequest('PUT', lineup)),
  quickPlay: () => request<MyTeam>('/api/my-team/play', { method: 'POST' }),
  getOpponentReport: () => request<OpponentReport>('/api/my-team/opponent'),
  // Devre arasında bekleyen canlı maç; yoksa undefined (204)
  getLiveSession: () => request<LiveSession | undefined>('/api/my-team/live'),
  startLive: () => request<LiveSession>('/api/my-team/live/start', { method: 'POST' }),
  playSecondHalf: (
    substitutions: { outId: number; inId: number }[],
    playStyle: PlayStyle,
    talk: TeamTalk,
  ) => request<LiveSession>('/api/my-team/live/second-half', jsonRequest('POST', { substitutions, playStyle, talk })),

  getInbox: () => request<InboxMessage[]>('/api/my-team/inbox'),
  readAllInbox: () => request<void>('/api/my-team/inbox/read-all', { method: 'POST' }),
  acceptMessage: (id: number) => request<ActionResult>(`/api/my-team/inbox/${id}/accept`, { method: 'POST' }),
  rejectMessage: (id: number) => request<ActionResult>(`/api/my-team/inbox/${id}/reject`, { method: 'POST' }),
  renewContract: (playerId: number, weeklyWage: number, seasons: number) =>
    request<ActionResult>(`/api/my-team/contracts/${playerId}`, jsonRequest('POST', { weeklyWage, seasons })),
  listForSale: (playerId: number) => request<ActionResult>(`/api/my-team/sell/${playerId}`, { method: 'POST' }),
  getAcademy: () => request<AcademyPlayer[]>('/api/my-team/academy'),
  getCareer: () => request<Career>('/api/my-team/career'),

  getSaves: () => request<SaveSlot[]>('/api/data/saves'),
  createSave: (name: string) => request<SaveSlot>('/api/data/saves', jsonRequest('POST', { name })),
  loadSave: (name: string) =>
    request<void>(`/api/data/saves/${encodeURIComponent(name)}/load`, { method: 'POST' }),
  deleteSave: (name: string) => request<void>(`/api/data/saves/${encodeURIComponent(name)}`, { method: 'DELETE' }),
  importBackup: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<void>('/api/data/import', { method: 'POST', body: form })
  },
}

/** Tarayıcının doğrudan indirdiği dosyalar (JSON yedek, CSV); başlık gönderilemediği için oda linkte. */
export const downloadUrls = {
  backup: () => apiUrl(withRoom('/api/data/export')),
  standingsCsv: (seasonId?: number) => apiUrl(withRoom(withSeason('/api/data/csv/standings', seasonId))),
  fixtureCsv: (seasonId?: number) => apiUrl(withRoom(withSeason('/api/data/csv/fixture', seasonId))),
  playersCsv: (seasonId?: number) => apiUrl(withRoom(withSeason('/api/data/csv/players', seasonId))),
}
