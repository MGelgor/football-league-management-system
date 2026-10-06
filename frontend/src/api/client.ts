import type {
  Cup,
  HeadToHead,
  MatchDetail,
  MatchWeek,
  Player,
  PlayerProfile,
  PlayerRequest,
  PlayerStats,
  RecordEntry,
  Season,
  SeasonResult,
  Standing,
  Team,
  TeamRequest,
  TeamSeasonStats,
} from './types'

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
    response = await fetch(path, init)
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

export const api = {
  getTeams: () => request<Team[]>('/api/teams'),
  getTeam: (id: number) => request<Team>(`/api/teams/${id}`),
  createTeam: (team: TeamRequest) => request<Team>('/api/teams', jsonRequest('POST', team)),
  createRandomTeams: (count: number) => request<Team[]>('/api/teams/random', jsonRequest('POST', { count })),
  updateTeam: (id: number, team: TeamRequest) => request<Team>(`/api/teams/${id}`, jsonRequest('PUT', team)),
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
  getHeadToHead: (teamA: number, teamB: number) =>
    request<HeadToHead>(`/api/teams/head-to-head?teamA=${teamA}&teamB=${teamB}`),

  getFixture: (seasonId?: number) => request<MatchWeek[]>(withSeason('/api/fixtures', seasonId)),
  generateFixture: () => request<MatchWeek[]>('/api/fixtures/generate', { method: 'POST' }),
  resetFixture: () => request<void>('/api/fixtures', { method: 'DELETE' }),
  playWeek: (weekNumber: number) => request<MatchWeek>(`/api/weeks/${weekNumber}/play`, { method: 'POST' }),
  getMatch: (id: number) => request<MatchDetail>(`/api/matches/${id}`),

  getStandings: (seasonId?: number) => request<Standing[]>(withSeason('/api/standings', seasonId)),
  getSeasons: () => request<Season[]>('/api/seasons'),
  playSeason: () => request<SeasonResult>('/api/seasons/play-all', { method: 'POST' }),

  getCup: (seasonId?: number) => request<Cup>(withSeason('/api/cup', seasonId)),
  startCup: () => request<Cup>('/api/cup/start', { method: 'POST' }),
  playCupRound: () => request<Cup>('/api/cup/play-round', { method: 'POST' }),
  playCupAll: () => request<Cup>('/api/cup/play-all', { method: 'POST' }),

  getRecords: () => request<RecordEntry[]>('/api/records'),
}
