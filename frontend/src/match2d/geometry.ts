import type { Position } from '../api/types'

// Saha metre cinsindendir: 105 × 68. Ev sahibi +x yönüne (sağa) hücum eder.
export const PITCH_W = 105
export const PITCH_H = 68
export const CENTER: Point = { x: PITCH_W / 2, y: PITCH_H / 2 }

export interface Point {
  x: number
  y: number
}

// Hatların kendi kale çizgisine uzaklığı (dinlenme / başlama vuruşu dizilişi)
const LINE_REL: Record<Position, number> = { GOALKEEPER: 5, DEFENDER: 20, MIDFIELDER: 35, FORWARD: 47 }

export function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

export function distance(a: Point, b: Point) {
  return Math.hypot(a.x - b.x, a.y - b.y)
}

/** Takıma göre derinlik: 0 kendi kale çizgisi, 105 rakip kale çizgisi. */
export function toRel(x: number, home: boolean) {
  return home ? x : PITCH_W - x
}

export function fromRel(rel: number, home: boolean) {
  return home ? rel : PITCH_W - rel
}

export function relPoint(rel: number, y: number, home: boolean): Point {
  return { x: fromRel(rel, home), y }
}

/** Hattaki sıraya göre dinlenme yeri; hatta ne kadar çok oyuncu varsa o kadar geniş yayılır. */
export function basePosition(position: Position, index: number, count: number, home: boolean): Point {
  const spread = position === 'GOALKEEPER' ? 0 : Math.min(54, (count - 1) * 16)
  const y = count <= 1 ? PITCH_H / 2 : PITCH_H / 2 - spread / 2 + (spread * index) / (count - 1)
  return relPoint(LINE_REL[position], y, home)
}

/**
 * Takım bloğu: oyuncu dizilişteki yerinden topa göre kayar. Top ileride ise blok öne çıkar, topa sahip takım daha
 * yukarıda durur; yanlara da biraz kayılır. Kaleci kalesinin önünde kalır.
 */
export function shapeTarget(base: Point, position: Position, home: boolean, ball: Point, inPossession: boolean): Point {
  const ballRel = toRel(ball.x, home)
  if (position === 'GOALKEEPER') {
    return relPoint(clamp(2.5 + ballRel * 0.09, 2, 12), PITCH_H / 2 + (ball.y - PITCH_H / 2) * 0.15, home)
  }
  const shift = (ballRel - PITCH_W / 2) * 0.55 + (inPossession ? 10 : -4)
  const rel = clamp(toRel(base.x, home) + shift, 8, inPossession ? 97 : 88)
  return relPoint(rel, clamp(base.y + (ball.y - PITCH_H / 2) * 0.3, 3, PITCH_H - 3), home)
}
