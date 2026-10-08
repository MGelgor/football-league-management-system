import type {
  Competition,
  CupRound,
  FinanceType,
  Formation,
  InjurySeverity,
  MatchEventType,
  PlayStyle,
  Position,
  Zone,
} from './api/types'

export const POSITIONS: Position[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD']

export const POSITION_LABELS: Record<Position, string> = {
  GOALKEEPER: 'Kaleci',
  DEFENDER: 'Defans',
  MIDFIELDER: 'Orta saha',
  FORWARD: 'Forvet',
}

export const POSITION_SHORT: Record<Position, string> = {
  GOALKEEPER: 'KL',
  DEFENDER: 'DEF',
  MIDFIELDER: 'OS',
  FORWARD: 'FV',
}

export const CUP_ROUND_LABELS: Record<CupRound, string> = {
  QUARTER_FINAL: 'Çeyrek final',
  SEMI_FINAL: 'Yarı final',
  FINAL: 'Final',
}

export const COMPETITION_LABELS: Record<Competition, string> = {
  LEAGUE: 'Lig',
  CUP: 'Kupa',
}

export const ZONE_LABELS: Record<Zone, string> = {
  CHAMPIONS_LEAGUE: 'Şampiyonlar Ligi',
  EUROPA_LEAGUE: 'Avrupa Ligi',
  RELEGATION: 'Küme düşme',
}

/** Lig haftası için "Hafta 5", kupa maçı için tur adı. */
export function roundLabel(competition: Competition, weekNumber: number, cupRound: CupRound | null) {
  return competition === 'CUP' && cupRound ? `Kupa · ${CUP_ROUND_LABELS[cupRound]}` : `Hafta ${weekNumber}`
}

/** "4-4-2" gibi diziliş etiketi (kaleci hariç defans-orta saha-forvet). */
export function formationLabel(players: { position: Position }[]) {
  return POSITIONS.filter((position) => position !== 'GOALKEEPER')
    .map((position) => players.filter((player) => player.position === position).length)
    .filter((count) => count > 0)
    .join('-')
}

export const EVENT_ICONS: Record<MatchEventType, string> = {
  GOAL: '⚽',
  YELLOW_CARD: '🟨',
  RED_CARD: '🟥',
  INJURY: '🩹',
  OWN_GOAL: '⚽',
  PENALTY_MISSED: '❌',
  VAR_DISALLOWED: '📺',
}

/** Skora yazılan olay mı; kendi kalesine gol rakibin hanesine sayılır. */
export function scoringSide(event: { type: string; home: boolean }): 'home' | 'away' | null {
  if (event.type === 'GOAL') {
    return event.home ? 'home' : 'away'
  }
  if (event.type === 'OWN_GOAL') {
    return event.home ? 'away' : 'home'
  }
  return null
}

/** Golcü listelerinde ad yanına: penaltı (P), kendi kalesine (KK). */
export function goalSuffix(event: { type: string; penalty: boolean }) {
  if (event.type === 'OWN_GOAL') {
    return ' (KK)'
  }
  return event.penalty ? ' (P)' : ''
}

export const FORMATIONS: Formation[] = ['F442', 'F433', 'F352', 'F532', 'F451']

// Backend'deki Formation enum'uyla aynı mevki sayıları
export const FORMATION_COUNTS: Record<Formation, Record<Position, number>> = {
  F442: { GOALKEEPER: 1, DEFENDER: 4, MIDFIELDER: 4, FORWARD: 2 },
  F433: { GOALKEEPER: 1, DEFENDER: 4, MIDFIELDER: 3, FORWARD: 3 },
  F352: { GOALKEEPER: 1, DEFENDER: 3, MIDFIELDER: 5, FORWARD: 2 },
  F532: { GOALKEEPER: 1, DEFENDER: 5, MIDFIELDER: 3, FORWARD: 2 },
  F451: { GOALKEEPER: 1, DEFENDER: 4, MIDFIELDER: 5, FORWARD: 1 },
}

export const FORMATION_LABELS: Record<Formation, string> = {
  F442: '4-4-2',
  F433: '4-3-3',
  F352: '3-5-2',
  F532: '5-3-2',
  F451: '4-5-1',
}

export const PLAY_STYLES: PlayStyle[] = ['ATTACKING', 'BALANCED', 'DEFENSIVE']

export const PLAY_STYLE_LABELS: Record<PlayStyle, string> = {
  ATTACKING: 'Hücum',
  BALANCED: 'Dengeli',
  DEFENSIVE: 'Savunma',
}

/** Diziliş döngüsü: her diziliş sıradaki bir sonraki ve üç sonraki dizilişi yener (backend Formation.advantageOver). */
export function formationBeats(formation: Formation): Formation[] {
  const index = FORMATIONS.indexOf(formation)
  return [FORMATIONS[(index + 1) % FORMATIONS.length], FORMATIONS[(index + 3) % FORMATIONS.length]]
}

export const INJURY_LABELS: Record<InjurySeverity, string> = {
  MINOR: 'Hafif',
  MODERATE: 'Orta',
  SERIOUS: 'Uzun süreli',
}

export const FINANCE_LABELS: Record<FinanceType, string> = {
  TICKETS: 'Bilet gelirleri',
  WAGES: 'Maaşlar',
  LEAGUE_PRIZE: 'Lig ödülü',
  CUP_PRIZE: 'Kupa ödülü',
  TRANSFER_SALE: 'Oyuncu satışı',
  TRANSFER_PURCHASE: 'Oyuncu alımı',
}

/** "€12,5M", "€850B", "€4.000" gibi kısa avro gösterimi (negatifte başta −). */
export function formatMoney(amount: number) {
  const sign = amount < 0 ? '−' : ''
  const value = Math.abs(amount)
  if (value >= 1_000_000) {
    return `${sign}€${(value / 1_000_000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })}M`
  }
  if (value >= 10_000) {
    return `${sign}€${Math.round(value / 1000).toLocaleString('tr-TR')}B`
  }
  return `${sign}€${value.toLocaleString('tr-TR')}`
}
