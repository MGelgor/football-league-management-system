import type { MatchWeek, SeasonResult, Standing, Team, TeamRequest } from './types'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, init)
  } catch {
    throw new ApiError(0, 'Sunucuya ulaşılamadı. Backend çalışıyor mu?')
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
  const fallback = `İstek başarısız oldu (HTTP ${response.status})`
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

export const api = {
  getTeams: () => request<Team[]>('/api/teams'),
  createTeam: (team: TeamRequest) => request<Team>('/api/teams', jsonRequest('POST', team)),
  updateTeam: (id: number, team: TeamRequest) => request<Team>(`/api/teams/${id}`, jsonRequest('PUT', team)),
  deleteTeam: (id: number) => request<void>(`/api/teams/${id}`, { method: 'DELETE' }),
  uploadLogo: (id: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<Team>(`/api/teams/${id}/logo`, { method: 'POST', body: form })
  },

  getFixture: () => request<MatchWeek[]>('/api/fixtures'),
  generateFixture: () => request<MatchWeek[]>('/api/fixtures/generate', { method: 'POST' }),
  resetFixture: () => request<void>('/api/fixtures', { method: 'DELETE' }),
  playWeek: (weekNumber: number) => request<MatchWeek>(`/api/weeks/${weekNumber}/play`, { method: 'POST' }),

  getStandings: () => request<Standing[]>('/api/standings'),
  playSeason: () => request<SeasonResult>('/api/season/play-all', { method: 'POST' }),
}
