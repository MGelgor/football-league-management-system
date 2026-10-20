import type { LineupEntry, LiveSession, MatchDetail } from '../api/types'
import type { AnimEvent, AnimPlayer, MatchScript } from './engine'
import { clamp } from './geometry'

interface EventLike {
  minute: number
  type: string
  home: boolean
  playerId: number
  playerName: string
  assistPlayerId: number | null
  penalty: boolean
}

function toAnimEvent(item: EventLike): AnimEvent {
  return {
    minute: item.minute,
    type: item.type,
    home: item.home,
    playerId: item.playerId,
    playerName: item.playerName,
    assistPlayerId: item.assistPlayerId,
    penalty: item.penalty,
  }
}

/** Menajerin canlı maçı: oturumdaki saha oyuncuları ve olaylar. Topla oynama payı iki ilk 11'in gücünden. */
export function scriptFromLive(session: LiveSession): MatchScript {
  const starters = session.pitch.filter((player) => player.minuteOn === 0)
  const strength = (home: boolean) =>
    starters.filter((player) => player.home === home).reduce((sum, player) => sum + player.effectiveStrength, 0)
  const home = strength(true)
  const away = strength(false)
  return {
    matchId: session.matchId,
    players: session.pitch.map((player) => ({
      id: player.id,
      name: player.name,
      shirtNumber: player.shirtNumber,
      position: player.position,
      home: player.home,
      minuteOn: player.minuteOn,
      minuteOff: player.minuteOff,
      replacedPlayerId: player.replacedPlayerId,
    })),
    events: session.events.map(toAnimEvent),
    homeShare: home + away > 0 ? clamp(0.5 + ((home - away) / (home + away)) * 1.5, 0.35, 0.65) : 0.5,
  }
}

function lineupPlayer(entry: LineupEntry, home: boolean): AnimPlayer {
  return {
    id: entry.playerId,
    name: entry.playerName,
    shirtNumber: entry.shirtNumber,
    position: entry.position,
    home,
    minuteOn: entry.minuteOn,
    // 90 = maç sonuna kadar oynadı
    minuteOff: entry.minuteOff >= 90 ? null : entry.minuteOff,
    replacedPlayerId: entry.replacedPlayerId,
  }
}

/** Oynanmış maç: kadrolar, olaylar ve kadrodan türetilen değişiklikler; topla oynama payı maç istatistiğinden. */
export function scriptFromDetail(detail: MatchDetail): MatchScript {
  const players = [
    ...detail.home.lineup.map((entry) => lineupPlayer(entry, true)),
    ...detail.away.lineup.map((entry) => lineupPlayer(entry, false)),
  ]
  const substitutions: AnimEvent[] = [
    ...detail.home.lineup.map((entry) => ({ entry, home: true })),
    ...detail.away.lineup.map((entry) => ({ entry, home: false })),
  ]
    .filter(({ entry }) => !entry.starter && entry.replacedPlayerId !== null)
    .map(({ entry, home }) => ({
      minute: entry.minuteOn,
      type: 'SUBSTITUTION',
      home,
      playerId: entry.playerId,
      playerName: entry.playerName,
      assistPlayerId: entry.replacedPlayerId,
      penalty: false,
    }))
  // Aynı dakikada önce olaylar, sonra değişiklik (sakatlanan oyuncunun yerine giren sonra gelir)
  const events = [...detail.events.map(toAnimEvent), ...substitutions].sort(
    (a, b) => a.minute - b.minute || Number(a.type === 'SUBSTITUTION') - Number(b.type === 'SUBSTITUTION'),
  )
  const possession = detail.home.stats?.possession
  return {
    matchId: detail.id,
    players,
    events,
    homeShare: possession ? possession / 100 : 0.5,
  }
}
