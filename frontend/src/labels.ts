import type { Competition, CupRound, Position, Zone } from './api/types'

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
