import type { Position } from '../api/types'
import { basePosition, CENTER, clamp, distance, PITCH_H, PITCH_W, relPoint, shapeTarget, toRel, type Point } from './geometry'
import { between, createRng, pickWeighted, seedOf, type Rng } from './random'

/**
 * Olay güdümlü maç animasyonu. Skor ve olaylar backend'de belli; motor yeni bir simülasyon yapmaz.
 * - Olay dakikasında "yönetmen" bir sahne kurar: gol (paslaşma → asist → şut), penaltı, kendi kalesine gol, VAR,
 *   faul + kart, sakatlık, değişiklik. Sahne bitince onMinuteShown çağrılır; ekran sayacı bu sırada bekler.
 * - Olaysız zamanda tohumlu rastgele sayıyla paslaşma, top kaybı ve gol olmayan şutlar üretilir.
 * - Oyuncular dizilişteki yerlerinden topa göre kayan bir blok hâlinde koşar ("arrive" davranışı).
 */

export interface AnimPlayer {
  id: number
  name: string
  shirtNumber: number
  position: Position
  home: boolean
  minuteOn: number
  minuteOff: number | null
  replacedPlayerId: number | null
}

export interface AnimEvent {
  minute: number
  // MatchEventType ya da SUBSTITUTION
  type: string
  home: boolean
  playerId: number
  playerName: string
  // Golde asist yapan, değişiklikte çıkan oyuncu
  assistPlayerId: number | null
  penalty: boolean
}

export interface MatchScript {
  matchId: number
  players: AnimPlayer[]
  events: AnimEvent[]
  // Ev sahibinin topla oynama payı (0-1): top kaybı olasılığını belirler
  homeShare: number
}

export type OverlayTone = 'goal' | 'yellow' | 'red' | 'info'

export interface Overlay {
  text: string
  sub: string
  tone: OverlayTone
  until: number
}

export interface SnapshotPlayer {
  id: number
  x: number
  y: number
  home: boolean
  goalkeeper: boolean
  shirtNumber: number
  name: string
  alpha: number
  down: boolean
  mark: string | null
  label: boolean
}

export interface Snapshot {
  players: SnapshotPlayer[]
  ball: { x: number; y: number; z: number }
  overlay: Overlay | null
}

interface Actor {
  player: AnimPlayer
  x: number
  y: number
  base: Point
  target: Point | null
  sprint: boolean
  state: 'play' | 'down' | 'leaving'
  alpha: number
  mark: string | null
  markUntil: number
}

interface Step {
  duration: number
  begin?: (step: Step) => void
  tick?: (progress: number) => void
  end?: () => void
}

type ShotOutcome = 'goal' | 'saved' | 'wide'

const POSITIONS: Position[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD']
const RUN_SPEED = 7
const SPRINT_SPEED = 9.5
const DIVE_SPEED = 13
const DRIBBLE_SPEED = 6.5

// Pası kime atar: topu tutanın mevkisine göre alıcı mevkilerinin ağırlığı
const RECEIVER_WEIGHTS: Record<Position, Partial<Record<Position, number>>> = {
  GOALKEEPER: { DEFENDER: 1, MIDFIELDER: 0.3 },
  DEFENDER: { DEFENDER: 0.8, MIDFIELDER: 1.4, FORWARD: 0.2 },
  MIDFIELDER: { DEFENDER: 0.5, MIDFIELDER: 1, FORWARD: 1.2 },
  FORWARD: { MIDFIELDER: 1, FORWARD: 0.6, DEFENDER: 0.1 },
}

function lerp(a: Point, b: Point, t: number): Point {
  return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t }
}

function inside(point: Point): Point {
  return { x: clamp(point.x, 1.5, PITCH_W - 1.5), y: clamp(point.y, 1.5, PITCH_H - 1.5) }
}

function onPitchAt(player: AnimPlayer, minute: number) {
  return player.minuteOn <= minute && (player.minuteOff === null || minute < player.minuteOff)
}

export class MatchEngine {
  private script: MatchScript
  private readonly actors = new Map<number, Actor>()
  private readonly ball = { x: CENTER.x, y: CENTER.y, z: 0, owner: null as number | null }
  private queue: Step[] = []
  private current: Step | null = null
  private elapsed = 0
  private now = 0
  private ambientNo = 0
  private minute: number
  private homeInPossession = true
  private overlay: Overlay | null = null
  private readonly labelled = new Set<number>()
  private readonly onMinuteShown: (minute: number) => void

  constructor(script: MatchScript, minute: number, onMinuteShown: (minute: number) => void) {
    this.script = script
    this.minute = minute
    this.onMinuteShown = onMinuteShown
    this.reset(minute)
  }

  /** Olaylar veya oyuncular değişti (ör. ikinci yarı oynandı): oyuncu nesneleri güncellenir, sahne bozulmaz. */
  setScript(script: MatchScript) {
    this.script = script
    for (const player of script.players) {
      const actor = this.actors.get(player.id)
      if (actor) {
        actor.player = player
      }
    }
  }

  /** Ekrandaki dakika ilerledi. Sırayla gelinmediyse (atlama, devam etme) sahne sıfırlanır. */
  setMinute(minute: number) {
    if (minute === this.minute) {
      return
    }
    const sequential = minute === this.minute + 1
    this.minute = minute
    if (!sequential) {
      this.reset(minute)
      this.onMinuteShown(minute)
      return
    }
    const events = this.script.events.filter((event) => event.minute === minute)
    const secondHalf = minute === 46
    this.enter(minute)
    if (events.length === 0 && !secondHalf) {
      this.leave(minute)
      this.onMinuteShown(minute)
      return
    }
    this.interrupt()
    if (secondHalf) {
      this.queue.push(this.kickoffStep(false))
    }
    events.forEach((event, index) => this.queue.push(...this.scene(event, index)))
    this.queue.push({
      duration: 0,
      end: () => {
        this.leave(minute)
        this.labelled.clear()
        this.onMinuteShown(minute)
      },
    })
  }

  update(dt: number) {
    this.now += dt
    this.advanceSteps(dt)
    this.moveActors(dt)
    this.moveBall(dt)
    if (this.overlay && this.now > this.overlay.until) {
      this.overlay = null
    }
  }

  snapshot(): Snapshot {
    return {
      players: [...this.actors.values()].map((actor) => ({
        id: actor.player.id,
        x: actor.x,
        y: actor.y,
        home: actor.player.home,
        goalkeeper: actor.player.position === 'GOALKEEPER',
        shirtNumber: actor.player.shirtNumber,
        name: actor.player.name,
        alpha: clamp(actor.alpha, 0, 1),
        down: actor.state === 'down',
        mark: actor.mark && this.now < actor.markUntil ? actor.mark : null,
        label: this.labelled.has(actor.player.id),
      })),
      ball: { x: this.ball.x, y: this.ball.y, z: this.ball.z },
      overlay: this.overlay,
    }
  }

  // ---- Kadro ve diziliş

  private reset(minute: number) {
    this.queue = []
    this.current = null
    this.overlay = null
    this.labelled.clear()
    this.actors.clear()
    for (const player of this.script.players.filter((candidate) => onPitchAt(candidate, minute))) {
      this.actors.set(player.id, this.newActor(player, CENTER, 1))
    }
    this.recomputeBases()
    for (const actor of this.actors.values()) {
      actor.x = actor.base.x
      actor.y = actor.base.y
    }
    const homeKicks = minute < 46
    this.ball.x = CENTER.x
    this.ball.y = CENTER.y
    this.ball.z = 0
    this.ball.owner = this.kicker(homeKicks)?.player.id ?? null
    this.homeInPossession = homeKicks
  }

  private newActor(player: AnimPlayer, at: Point, alpha: number): Actor {
    return { player, x: at.x, y: at.y, base: at, target: null, sprint: false, state: 'play', alpha, mark: null, markUntil: 0 }
  }

  /** Bu dakikada oyuna girenler kenardan gelir. */
  private enter(minute: number) {
    let changed = false
    for (const player of this.script.players) {
      if (player.minuteOn === minute && minute > 0 && !this.actors.has(player.id)) {
        this.actors.set(player.id, this.newActor(player, { x: CENTER.x + (player.home ? -4 : 4), y: -1.5 }, 0))
        changed = true
      }
    }
    if (changed) {
      this.recomputeBases()
    }
  }

  /** Bu dakikada çıkanlar (değişiklik, kırmızı kart, sakatlık) en yakın taç çizgisine yürür ve kaybolur. */
  private leave(minute: number) {
    let changed = false
    for (const actor of this.actors.values()) {
      if (actor.player.minuteOff === minute && actor.state !== 'leaving') {
        actor.state = 'leaving'
        actor.sprint = false
        actor.target = { x: actor.x, y: actor.y < PITCH_H / 2 ? -2.5 : PITCH_H + 2.5 }
        if (this.ball.owner === actor.player.id) {
          this.ball.owner = null
        }
        changed = true
      }
    }
    if (changed) {
      this.recomputeBases()
    }
  }

  /** Değişiklikle giren, çıkanın sırasını alır (aynı mevkideyse); böylece hat düzeni bozulmaz. */
  private slotKey(player: AnimPlayer, depth = 0): number {
    const replaced = this.script.players.find((candidate) => candidate.id === player.replacedPlayerId)
    if (replaced && replaced.position === player.position && depth < 5) {
      return this.slotKey(replaced, depth + 1)
    }
    return player.shirtNumber
  }

  private recomputeBases() {
    const active = [...this.actors.values()].filter((actor) => actor.state !== 'leaving')
    for (const home of [true, false]) {
      for (const position of POSITIONS) {
        const line = active
          .filter((actor) => actor.player.home === home && actor.player.position === position)
          .sort((a, b) => this.slotKey(a.player) - this.slotKey(b.player))
        line.forEach((actor, index) => {
          actor.base = basePosition(position, index, line.length, home)
        })
      }
    }
  }

  // ---- Adım kuyruğu

  private interrupt() {
    this.queue = []
    this.current = null
    if (this.ball.owner === null) {
      this.ball.z = 0
    }
    this.clearTargets()
  }

  private advanceSteps(dt: number) {
    let budget = dt
    for (let guard = 0; guard < 25 && budget > 0; guard++) {
      if (!this.current) {
        if (this.queue.length === 0) {
          this.queue.push(...this.ambient())
        }
        const next = this.queue.shift()
        if (!next) {
          return
        }
        this.current = next
        this.elapsed = 0
        next.begin?.(next)
      }
      const step = this.current
      const left = step.duration - this.elapsed
      if (budget < left) {
        this.elapsed += budget
        step.tick?.(this.elapsed / step.duration)
        return
      }
      budget -= Math.max(0, left)
      step.tick?.(1)
      step.end?.()
      this.current = null
    }
  }

  // ---- Hareket

  private moveActors(dt: number) {
    const ball = { x: this.ball.x, y: this.ball.y }
    const presser = this.ball.owner !== null ? this.presser() : undefined
    for (const actor of [...this.actors.values()]) {
      if (actor.state === 'leaving') {
        actor.alpha -= dt * 0.6
        if (actor.alpha <= 0) {
          this.actors.delete(actor.player.id)
          continue
        }
      } else if (actor.alpha < 1) {
        actor.alpha = Math.min(1, actor.alpha + dt * 1.2)
      }
      if (actor.state === 'down') {
        continue
      }
      let target = actor.target
      if (!target) {
        if (this.ball.owner === actor.player.id) {
          target = { x: actor.x, y: actor.y }
        } else if (presser === actor) {
          const gap = distance(actor, ball)
          target = gap > 1.4 ? lerp(actor, ball, (gap - 1.2) / gap) : { x: actor.x, y: actor.y }
        } else {
          const shape = shapeTarget(actor.base, actor.player.position, actor.player.home, ball, actor.player.home === this.homeInPossession)
          // Bloğun içinde herkes biraz kendi başına oynar: yavaş, oyuncuya özgü bir salınım
          const sway = actor.player.position === 'GOALKEEPER' ? 0.3 : 1
          const seed = actor.player.id
          target = {
            x: shape.x + Math.sin(this.now * 0.6 + seed) * 1.8 * sway,
            y: shape.y + Math.cos(this.now * 0.45 + seed * 1.7) * 2.4 * sway,
          }
        }
      }
      const speed =
        actor.state === 'leaving' ? 3 : actor.sprint ? (actor.player.position === 'GOALKEEPER' ? DIVE_SPEED : SPRINT_SPEED) : RUN_SPEED
      const gap = distance(actor, target)
      if (gap > 0.01) {
        const move = Math.min(gap, Math.min(speed, gap * 2.4) * dt)
        actor.x += ((target.x - actor.x) / gap) * move
        actor.y += ((target.y - actor.y) / gap) * move
      }
    }
  }

  /** Topa sahip olmayan takımın topa en yakın saha oyuncusu baskı yapar. */
  private presser(): Actor | undefined {
    const owner = this.ownerActor()
    if (!owner) {
      return undefined
    }
    const candidate = this.nearest(!owner.player.home, this.ball, false)
    return candidate && distance(candidate, this.ball) < 25 ? candidate : undefined
  }

  private moveBall(dt: number) {
    const owner = this.ownerActor()
    if (this.ball.owner !== null && !owner) {
      this.ball.owner = null
    }
    if (!owner) {
      return
    }
    const foot = { x: owner.x + (owner.player.home ? 0.8 : -0.8), y: owner.y + 0.3 }
    const gap = distance(this.ball, foot)
    if (gap > 0.01) {
      const move = Math.min(gap, 25 * dt)
      this.ball.x += ((foot.x - this.ball.x) / gap) * move
      this.ball.y += ((foot.y - this.ball.y) / gap) * move
    }
    this.ball.z = 0
  }

  // ---- Yardımcılar

  private ownerActor(): Actor | undefined {
    const owner = this.ball.owner !== null ? this.actors.get(this.ball.owner) : undefined
    return owner && owner.state === 'play' ? owner : undefined
  }

  private team(home: boolean, keeper = true): Actor[] {
    return [...this.actors.values()].filter(
      (actor) =>
        actor.player.home === home && actor.state === 'play' && actor.alpha > 0.3 && (keeper || actor.player.position !== 'GOALKEEPER'),
    )
  }

  private nearest(home: boolean, point: Point, keeper = true): Actor | undefined {
    return this.team(home, keeper).reduce<Actor | undefined>(
      (best, actor) => (!best || distance(actor, point) < distance(best, point) ? actor : best),
      undefined,
    )
  }

  private keeper(home: boolean): Actor | undefined {
    return this.team(home).find((actor) => actor.player.position === 'GOALKEEPER')
  }

  private kicker(home: boolean): Actor | undefined {
    const outfield = this.team(home, false)
    return outfield.find((actor) => actor.player.position === 'FORWARD') ?? outfield[0]
  }

  private setTarget(actor: Actor | undefined, point: Point, sprint = false) {
    if (actor) {
      actor.target = inside(point)
      actor.sprint = sprint
    }
  }

  private clearTargets() {
    for (const actor of this.actors.values()) {
      if (actor.state !== 'leaving') {
        actor.target = null
        actor.sprint = false
      }
    }
  }

  private nameOf(id: number | null) {
    return this.script.players.find((player) => player.id === id)?.name ?? ''
  }

  private receivePoint(receiver: Actor, rng: Rng): Point {
    const shape = shapeTarget(receiver.base, receiver.player.position, receiver.player.home, this.ball, true)
    const forward = receiver.player.home ? 1 : -1
    return inside({ x: shape.x + forward * between(rng, 2, 6), y: shape.y + between(rng, -3, 3) })
  }

  private pickTeammate(rng: Rng, owner: Actor, exclude: number[] = []): Actor | undefined {
    const weights = RECEIVER_WEIGHTS[owner.player.position]
    const ownerRel = toRel(owner.x, owner.player.home)
    const candidates = this.team(owner.player.home).filter(
      (actor) => actor !== owner && !exclude.includes(actor.player.id) && (weights[actor.player.position] ?? 0) > 0,
    )
    return pickWeighted(rng, candidates, (actor) => {
      const gain = toRel(actor.x, actor.player.home) - ownerRel
      const gap = distance(actor, owner)
      return (weights[actor.player.position] ?? 0) * (1 + Math.max(0, gain) / 30) * (gap > 6 && gap < 40 ? 1 : 0.3)
    })
  }

  // ---- Adımlar

  private waitStep(duration: number): Step {
    return { duration }
  }

  private overlayStep(text: string, sub: string, tone: OverlayTone, duration: number): Step {
    return {
      duration,
      begin: () => {
        this.overlay = { text, sub, tone, until: this.now + duration }
      },
    }
  }

  private labelStep(ids: (number | null)[]): Step {
    return {
      duration: 0,
      begin: () => ids.forEach((id) => id !== null && this.labelled.add(id)),
    }
  }

  /**
   * Top sahipsizse veya rakipteyse takımın en yakın oyuncusu topu alır. nearId: topu bu oyuncunun yakınındaki
   * oyuncu alır (faul sahnesinde faulü yapacak oyuncu yetişebilsin diye).
   */
  private recoverStep(home: boolean, nearId?: number): Step {
    let taker: Actor | undefined
    let from: Point | null = null
    return {
      duration: 0,
      begin: (step) => {
        const owner = this.ownerActor()
        const near = nearId !== undefined ? this.actors.get(nearId) : undefined
        if (owner && owner.player.home === home && (!near || distance(owner, near) < 12)) {
          return
        }
        taker = near ? this.nearest(home, near, false) : (this.nearest(home, this.ball, false) ?? this.nearest(home, this.ball))
        if (!taker) {
          return
        }
        this.ball.owner = null
        this.ball.z = 0
        if (near) {
          // Top, faulü yapacak oyuncunun bölgesindeki rakibe gider (seken / kesilen top)
          from = { x: this.ball.x, y: this.ball.y }
          step.duration = clamp(distance(from, taker) / 19, 0.3, 2)
        } else {
          this.setTarget(taker, this.ball, true)
          step.duration = clamp(distance(taker, this.ball) / SPRINT_SPEED, 0.3, 1.4)
        }
      },
      tick: (progress) => {
        if (from && taker) {
          const at = lerp(from, taker, progress)
          this.ball.x = at.x
          this.ball.y = at.y
        }
      },
      end: () => {
        if (taker) {
          taker.target = null
          taker.sprint = false
          this.ball.owner = taker.player.id
          this.homeInPossession = home
        }
      },
    }
  }


  /** Pas: top havada / yerde alıcının koştuğu noktaya gider. intercepted: rakip araya girer. */
  private passStep(toId: number, options: { rng?: Rng; point?: Point; lofted?: boolean; intercepted?: boolean } = {}): Step {
    let from: Point = { x: this.ball.x, y: this.ball.y }
    let to: Point = from
    let height = 0
    let catcher: Actor | undefined
    let runner: Actor | undefined
    return {
      duration: 0,
      begin: (step) => {
        const receiver = this.actors.get(toId)
        if (!receiver || receiver.state !== 'play') {
          return
        }
        const rng = options.rng ?? createRng(seedOf(this.script.matchId, this.ambientNo, toId))
        from = { x: this.ball.x, y: this.ball.y }
        to = inside(options.point ?? this.receivePoint(receiver, rng))
        catcher = receiver
        runner = receiver
        if (options.intercepted) {
          const thief = this.nearest(!receiver.player.home, lerp(from, to, 0.55), false)
          if (thief) {
            to = lerp(from, to, between(rng, 0.4, 0.7))
            catcher = thief
            this.setTarget(thief, to, true)
          }
        }
        const length = distance(from, to)
        height = options.lofted ? 2 + length * 0.12 : 0
        // Top, alıcının koşusuyla aynı anda varır (ara pası): alıcı uzaktaysa pas yavaşlar
        const run = distance(catcher, to) / (options.point !== undefined || catcher !== receiver ? SPRINT_SPEED : RUN_SPEED)
        step.duration = clamp(Math.max(length / (options.lofted ? 15 : 19), run), 0.3, 3)
        this.ball.owner = null
        this.setTarget(receiver, to, options.point !== undefined)
      },
      tick: (progress) => {
        const point = lerp(from, to, progress)
        this.ball.x = point.x
        this.ball.y = point.y
        this.ball.z = height * 4 * progress * (1 - progress)
      },
      end: () => {
        this.ball.z = 0
        if (catcher && catcher.state === 'play') {
          this.ball.owner = catcher.player.id
          this.homeInPossession = catcher.player.home
        }
        for (const actor of [runner, catcher]) {
          if (actor && actor.state !== 'leaving') {
            actor.target = null
            actor.sprint = false
          }
        }
      },
    }
  }

  /** Topa sahip olmadan topa vuruş: orta, sekme. */
  private kickStep(point: Point, lofted: boolean, speed: number): Step {
    let from: Point = { x: this.ball.x, y: this.ball.y }
    let height = 0
    return {
      duration: 0,
      begin: (step) => {
        from = { x: this.ball.x, y: this.ball.y }
        const length = distance(from, point)
        height = lofted ? 2.5 + length * 0.1 : 0
        step.duration = clamp(length / speed, 0.25, 1.8)
        this.ball.owner = null
      },
      tick: (progress) => {
        const at = lerp(from, point, progress)
        this.ball.x = at.x
        this.ball.y = at.y
        this.ball.z = height * 4 * progress * (1 - progress)
      },
      end: () => {
        this.ball.z = 0
      },
    }
  }

  /** Topu tutan oyuncu ileri sürer. */
  private carryStep(length: number, rng: Rng): Step {
    let owner: Actor | undefined
    return {
      duration: 0,
      begin: (step) => {
        owner = this.ownerActor()
        if (!owner) {
          return
        }
        const forward = owner.player.home ? 1 : -1
        this.setTarget(owner, { x: owner.x + forward * length, y: owner.y + between(rng, -4, 4) })
        step.duration = length / DRIBBLE_SPEED
      },
      end: () => {
        if (owner) {
          owner.target = null
        }
      },
    }
  }

  private carryToStep(point: Point): Step {
    let owner: Actor | undefined
    return {
      duration: 0,
      begin: (step) => {
        owner = this.ownerActor()
        if (!owner) {
          return
        }
        this.setTarget(owner, point)
        step.duration = clamp(distance(owner, point) / DRIBBLE_SPEED, 0.3, 2.5)
      },
      end: () => {
        if (owner) {
          owner.target = null
        }
      },
    }
  }

  /** Oyuncu bir noktaya koşmaya başlar (hedef, pas gelene kadar kalır). */
  private runStep(id: number, point: Point, lead: number): Step {
    return {
      duration: lead,
      begin: (step) => {
        const actor = this.actors.get(id)
        this.setTarget(actor, point, true)
        // Ortadan önce koşunun çoğu bitsin: top havadayken son metreler alınır
        if (actor) {
          step.duration = clamp(distance(actor, point) / SPRINT_SPEED - 0.8, lead, 3)
        }
      },
    }
  }

  /** Oyuncu noktaya varana kadar bekler (penaltıda topa koşu). */
  private approachStep(id: number, point: Point): Step {
    return {
      duration: 0.4,
      begin: (step) => {
        const actor = this.actors.get(id)
        this.setTarget(actor, point, true)
        if (actor) {
          step.duration = clamp(distance(actor, point) / (SPRINT_SPEED * 0.8), 0.4, 4)
        }
      },
    }
  }

  private shotTarget(rng: Rng, home: boolean, outcome: ShotOutcome): Point {
    const side = rng() < 0.5 ? -1 : 1
    if (outcome === 'goal') {
      return relPoint(PITCH_W + 0.8, PITCH_H / 2 + side * between(rng, 0.5, 3.1), home)
    }
    if (outcome === 'saved') {
      return relPoint(PITCH_W - 1.2, PITCH_H / 2 + side * between(rng, 0, 2.6), home)
    }
    return relPoint(PITCH_W + 1.5, PITCH_H / 2 + side * between(rng, 4.5, 8), home)
  }

  /** Şut: kaleci hamle yapar; kurtarırsa topu alır, gol olursa top ağlarda kalır. */
  private shotStep(shooterId: number, target: Point, outcome: ShotOutcome): Step {
    let from: Point = { x: this.ball.x, y: this.ball.y }
    let keeper: Actor | undefined
    return {
      duration: 0,
      begin: (step) => {
        const shooter = this.actors.get(shooterId)
        keeper = shooter ? this.keeper(!shooter.player.home) : undefined
        from = { x: this.ball.x, y: this.ball.y }
        this.ball.owner = null
        step.duration = clamp(distance(from, target) / 27, 0.35, 1.3)
        if (keeper) {
          const reach = outcome === 'saved' ? 1 : outcome === 'goal' ? 0.45 : 0.25
          this.setTarget(keeper, lerp(keeper, target, reach), true)
        }
      },
      tick: (progress) => {
        const at = lerp(from, target, progress)
        this.ball.x = at.x
        this.ball.y = at.y
        this.ball.z = 0.6 * 4 * progress * (1 - progress)
      },
      end: () => {
        this.ball.z = 0
        if (outcome === 'saved' && keeper) {
          this.ball.owner = keeper.player.id
          this.homeInPossession = keeper.player.home
        }
        this.clearTargets()
      },
    }
  }

  /** Top auta çıktı: kaleci kendi altı pasına koyar ve oyunu başlatır. */
  private goalKickStep(home: boolean): Step {
    let from: Point = { x: this.ball.x, y: this.ball.y }
    const spot = relPoint(5.5, PITCH_H / 2, home)
    let keeper: Actor | undefined
    return {
      duration: 1.1,
      begin: () => {
        from = { x: this.ball.x, y: this.ball.y }
        this.ball.owner = null
        keeper = this.keeper(home)
        this.setTarget(keeper, spot)
      },
      tick: (progress) => {
        const at = lerp(from, spot, progress)
        this.ball.x = at.x
        this.ball.y = at.y
      },
      end: () => {
        this.clearTargets()
        if (keeper) {
          this.ball.owner = keeper.player.id
        }
        this.homeInPossession = home
      },
    }
  }

  /** Santra: top ortaya döner, herkes başlama yerine yürür, topu kicker alır. */
  private kickoffStep(homeKicks: boolean): Step {
    let from: Point = { x: this.ball.x, y: this.ball.y }
    return {
      duration: 1.8,
      begin: () => {
        from = { x: this.ball.x, y: this.ball.y }
        this.ball.owner = null
        for (const actor of this.actors.values()) {
          if (actor.state === 'down') {
            actor.state = 'play'
          }
          if (actor.state === 'play') {
            actor.target = actor.base
            actor.sprint = false
          }
        }
      },
      tick: (progress) => {
        const at = lerp(from, CENTER, progress)
        this.ball.x = at.x
        this.ball.y = at.y
        this.ball.z = 0
      },
      end: () => {
        this.clearTargets()
        this.ball.owner = this.kicker(homeKicks)?.player.id ?? null
        this.homeInPossession = homeKicks
      },
    }
  }

  /** Golcü köşe bayrağına koşar, takım arkadaşları peşinden gider. */
  private celebrateStep(scorerId: number, text: string, sub: string): Step {
    const duration = 2.2
    return {
      duration,
      begin: () => {
        this.overlay = { text, sub, tone: 'goal', until: this.now + duration }
        const scorer = this.actors.get(scorerId)
        if (!scorer) {
          return
        }
        const corner = relPoint(PITCH_W - 3, scorer.y < PITCH_H / 2 ? 3 : PITCH_H - 3, scorer.player.home)
        this.setTarget(scorer, corner, true)
        const rng = createRng(seedOf(this.script.matchId, scorerId, Math.round(this.now)))
        for (const mate of this.team(scorer.player.home, false)) {
          if (mate !== scorer) {
            this.setTarget(mate, { x: corner.x + between(rng, -9, 9), y: corner.y + between(rng, -8, 8) })
          }
        }
      },
      end: () => this.clearTargets(),
    }
  }

  /** Penaltı: top noktaya, herkes ceza sahası dışına, kaleci çizgiye. */
  private penaltySetupStep(home: boolean, takerId: number): Step {
    let from: Point = { x: this.ball.x, y: this.ball.y }
    const spot = relPoint(PITCH_W - 11, PITCH_H / 2, home)
    return {
      duration: 1.6,
      begin: (step) => {
        from = { x: this.ball.x, y: this.ball.y }
        this.ball.owner = null
        const keeper = this.keeper(!home)
        const others = [...this.actors.values()].filter(
          (actor) => actor.state === 'play' && actor.player.id !== takerId && actor !== keeper,
        )
        others.forEach((actor, index) => {
          actor.target = relPoint(80 + (index % 3) * 1.5, 12 + (index * 44) / Math.max(1, others.length - 1), home)
          actor.sprint = false
        })
        if (keeper) {
          keeper.target = relPoint(PITCH_W - 0.8, PITCH_H / 2, home)
        }
        this.setTarget(this.actors.get(takerId), relPoint(PITCH_W - 13, PITCH_H / 2 - 1, home), true)
        const slowest = Math.max(
          ...[...this.actors.values()].filter((actor) => actor.target).map((actor) => distance(actor, actor.target as Point)),
        )
        step.duration = clamp(slowest / (RUN_SPEED * 0.9), 1.6, 4.5)
      },
      tick: (progress) => {
        const at = lerp(from, spot, progress)
        this.ball.x = at.x
        this.ball.y = at.y
        this.ball.z = 0
      },
    }
  }

  /** Faul: kart gören oyuncu topu süren rakibe kayarak müdahale eder. */
  private tackleStep(foulerId: number, mark: string): Step {
    const fouler = () => this.actors.get(foulerId)
    return {
      duration: 1.1,
      begin: (step) => {
        const owner = this.ownerActor()
        const actor = fouler()
        if (owner && actor) {
          // İki oyuncu birbirine doğru koşar: topu süren rakibe doğru gelir, faulü yapan ona müdahale eder
          this.setTarget(owner, lerp(owner, actor, 0.45))
          this.setTarget(actor, owner, true)
          step.duration = clamp(distance(actor, owner) / (SPRINT_SPEED + DRIBBLE_SPEED) + 0.5, 0.8, 3)
        }
      },
      tick: () => {
        const owner = this.ownerActor()
        const actor = fouler()
        if (owner && actor) {
          actor.target = { x: owner.x, y: owner.y }
        }
      },
      end: () => {
        const owner = this.ownerActor()
        if (owner) {
          owner.target = null
        }
        const actor = fouler()
        if (actor) {
          actor.target = null
          actor.sprint = false
          actor.mark = mark
          actor.markUntil = this.now + 5
        }
      },
    }
  }

  // ---- Sahneler

  private scene(event: AnimEvent, index: number): Step[] {
    const rng = createRng(seedOf(this.script.matchId, event.minute, index, event.playerId))
    if (!this.actors.has(event.playerId)) {
      return [this.overlayStep(event.playerName, '', 'info', 1)]
    }
    switch (event.type) {
      case 'GOAL':
        return event.penalty ? this.penaltyScene(event, rng, 'goal') : this.goalScene(event, rng, false)
      case 'VAR_DISALLOWED':
        return this.goalScene(event, rng, true)
      case 'PENALTY_MISSED':
        return this.penaltyScene(event, rng, rng() < 0.6 ? 'saved' : 'wide')
      case 'OWN_GOAL':
        return this.ownGoalScene(event, rng)
      case 'YELLOW_CARD':
      case 'RED_CARD':
        return this.foulScene(event, rng)
      case 'INJURY':
        return this.injuryScene(event)
      case 'SUBSTITUTION':
        return [this.overlayStep('DEĞİŞİKLİK', `${event.playerName} ↔ ${this.nameOf(event.assistPlayerId)}`, 'info', 1.3)]
      default:
        return []
    }
  }

  /** Gol: topu kazan → 1-2 pas → asist yapan kanattan / ortadan pas → golcü şut. */
  private goalScene(event: AnimEvent, rng: Rng, disallowed: boolean): Step[] {
    const home = event.home
    const scorer = event.playerId
    const assist = event.assistPlayerId !== null && this.actors.has(event.assistPlayerId) ? event.assistPlayerId : null
    const side = rng() < 0.5 ? -1 : 1
    const steps: Step[] = [this.labelStep([scorer, assist]), this.recoverStep(home)]
    const builders = this.team(home, false).filter(
      (actor) => actor.player.id !== scorer && actor.player.id !== assist && actor.player.position !== 'FORWARD',
    )
    const passes = 1 + Math.floor(rng() * 2)
    for (let i = 0; i < passes && builders.length > 0; i++) {
      const builder = builders[Math.floor(rng() * builders.length)]
      steps.push(this.passStep(builder.player.id, { rng }))
    }
    const shotPoint = relPoint(between(rng, 87, 94), PITCH_H / 2 + between(rng, -7, 7), home)
    if (assist !== null) {
      const wing = relPoint(between(rng, 76, 85), PITCH_H / 2 + side * between(rng, 8, 22), home)
      steps.push(this.passStep(assist, { point: wing }))
      steps.push(this.runStep(scorer, shotPoint, 0.5))
      steps.push(this.passStep(scorer, { point: shotPoint, lofted: Math.abs(wing.y - PITCH_H / 2) > 14 }))
    } else {
      steps.push(this.passStep(scorer, { point: relPoint(between(rng, 72, 80), PITCH_H / 2 + side * between(rng, 4, 14), home) }))
      steps.push(this.carryToStep(shotPoint))
    }
    steps.push(this.shotStep(scorer, this.shotTarget(rng, home, 'goal'), 'goal'))
    if (disallowed) {
      steps.push(this.overlayStep('VAR', 'Gol inceleniyor…', 'info', 1.6))
      steps.push(this.overlayStep('GOL İPTAL', event.playerName, 'info', 1.2))
      steps.push(this.goalKickStep(!home))
      return steps
    }
    const assistName = assist !== null ? ` · asist ${this.nameOf(assist)}` : ''
    steps.push(this.celebrateStep(scorer, 'GOL!', `${event.playerName}${assistName}`))
    steps.push(this.kickoffStep(!home))
    return steps
  }

  private penaltyScene(event: AnimEvent, rng: Rng, outcome: ShotOutcome): Step[] {
    const home = event.home
    const steps: Step[] = [
      this.labelStep([event.playerId]),
      this.overlayStep('PENALTI', event.playerName, 'info', 1),
      this.penaltySetupStep(home, event.playerId),
      this.waitStep(0.3),
      this.approachStep(event.playerId, relPoint(PITCH_W - 11.6, PITCH_H / 2 - 0.3, home)),
      this.shotStep(event.playerId, this.shotTarget(rng, home, outcome), outcome),
    ]
    if (outcome === 'goal') {
      steps.push(this.celebrateStep(event.playerId, 'GOL!', `${event.playerName} · penaltı`))
      steps.push(this.kickoffStep(!home))
    } else if (outcome === 'saved') {
      steps.push(this.overlayStep('PENALTI KAÇTI', 'Kaleci kurtardı', 'info', 1.3))
    } else {
      steps.push(this.overlayStep('PENALTI KAÇTI', 'Top auta gitti', 'info', 1.2))
      steps.push(this.goalKickStep(!home))
    }
    return steps
  }

  /** Kendi kalesine gol: rakip kanattan orta yapar, savunmacının dokunuşu topu kendi kalesine sokar. */
  private ownGoalScene(event: AnimEvent, rng: Rng): Step[] {
    const attack = !event.home
    const side = rng() < 0.5 ? -1 : 1
    const winger =
      this.team(attack, false).find((actor) => actor.player.position === 'MIDFIELDER') ?? this.team(attack, false)[0]
    const nearPost = relPoint(PITCH_W - 4, PITCH_H / 2 + side * 1.5, attack)
    const steps: Step[] = [this.labelStep([event.playerId]), this.recoverStep(attack)]
    if (winger) {
      steps.push(this.passStep(winger.player.id, { point: relPoint(between(rng, 84, 90), PITCH_H / 2 + side * 24, attack) }))
      steps.push(this.carryToStep(relPoint(97, PITCH_H / 2 + side * 26, attack)))
    }
    steps.push(this.runStep(event.playerId, nearPost, 0.4))
    steps.push(this.kickStep(nearPost, true, 16))
    steps.push(this.kickStep(relPoint(PITCH_W + 0.8, PITCH_H / 2 + side * between(rng, 0.5, 2.5), attack), false, 20))
    const celebrate = winger?.player.id ?? event.playerId
    steps.push(this.celebrateStep(celebrate, 'KENDİ KALESİNE GOL', event.playerName))
    steps.push(this.kickoffStep(event.home))
    return steps
  }

  private foulScene(event: AnimEvent, rng: Rng): Step[] {
    const red = event.type === 'RED_CARD'
    return [
      this.labelStep([event.playerId]),
      this.recoverStep(!event.home, event.playerId),
      this.carryStep(between(rng, 3, 6), rng),
      this.tackleStep(event.playerId, red ? '🟥' : '🟨'),
      this.overlayStep(red ? 'KIRMIZI KART' : 'SARI KART', event.playerName, red ? 'red' : 'yellow', 1.6),
      this.waitStep(0.3),
    ]
  }

  private injuryScene(event: AnimEvent): Step[] {
    return [
      this.labelStep([event.playerId]),
      {
        duration: 2,
        begin: () => {
          const actor = this.actors.get(event.playerId)
          if (actor) {
            actor.state = 'down'
          }
          this.overlay = { text: 'SAKATLIK', sub: event.playerName, tone: 'info', until: this.now + 2 }
        },
      },
    ]
  }

  // ---- Olaysız oyun

  private ambient(): Step[] {
    const rng = createRng(seedOf(this.script.matchId, 7, this.ambientNo++))
    const owner = this.ownerActor()
    if (!owner) {
      return [this.recoverStep(this.homeInPossession)]
    }
    const home = owner.player.home
    if (owner.player.position === 'GOALKEEPER') {
      const target = this.pickTeammate(rng, owner)
      return target ? [this.waitStep(0.4), this.passStep(target.player.id, { rng, lofted: rng() < 0.4 })] : [this.waitStep(0.5)]
    }
    const rel = toRel(this.ball.x, home)
    if (rel > 76 && rng() < 0.25) {
      const outcome: ShotOutcome = rng() < 0.55 ? 'saved' : 'wide'
      const steps = [this.shotStep(owner.player.id, this.shotTarget(rng, home, outcome), outcome)]
      return outcome === 'saved' ? [...steps, this.waitStep(0.8)] : [...steps, this.waitStep(0.6), this.goalKickStep(!home)]
    }
    const steps: Step[] = []
    if (rng() < 0.35) {
      steps.push(this.carryStep(between(rng, 4, 10), rng))
    }
    const receiver = this.pickTeammate(rng, owner)
    if (!receiver) {
      return [...steps, this.waitStep(0.5)]
    }
    const share = home ? this.script.homeShare : 1 - this.script.homeShare
    const intercepted = rng() < clamp((0.13 * (1 - share)) / 0.5, 0.05, 0.25)
    steps.push(this.passStep(receiver.player.id, { rng, lofted: rel > 70 && rng() < 0.3, intercepted }))
    return steps
  }
}
