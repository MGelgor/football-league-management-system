export type Rng = () => number

/** Tohumlu rastgele sayı (mulberry32): aynı tohum her zaman aynı diziyi verir, aynı maç aynı görünür. */
export function createRng(seed: number): Rng {
  let state = seed >>> 0
  return () => {
    state = (state + 0x6d2b79f5) >>> 0
    let t = state
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export function seedOf(...parts: number[]) {
  let hash = 2166136261
  for (const part of parts) {
    hash = Math.imul(hash ^ (part | 0), 16777619)
  }
  return hash >>> 0
}

export function between(rng: Rng, min: number, max: number) {
  return min + rng() * (max - min)
}

export function pickWeighted<T>(rng: Rng, items: T[], weight: (item: T) => number): T | undefined {
  const total = items.reduce((sum, item) => sum + Math.max(0, weight(item)), 0)
  if (total <= 0) {
    return items[0]
  }
  let roll = rng() * total
  for (const item of items) {
    roll -= Math.max(0, weight(item))
    if (roll < 0) {
      return item
    }
  }
  return items.at(-1)
}
