import { useEffect, useState, type DragEvent } from 'react'
import { api, errorMessage } from '../api/client'
import type { Formation, Lineup, LineupSquadPlayer, PlayStyle, Position } from '../api/types'
import {
  FORMATION_COUNTS,
  FORMATION_LABELS,
  FORMATIONS,
  INJURY_LABELS,
  PLAY_STYLE_LABELS,
  PLAY_STYLES,
  POSITION_LABELS,
  POSITION_SHORT,
  POSITIONS,
} from '../labels'
import FormIndicator from './FormIndicator'
import { PitchLines } from './PitchFormation'

// Sahada mevki satırlarının dikey konumu (% üstten)
const ROWS: Record<Position, number> = { GOALKEEPER: 88, DEFENDER: 68, MIDFIELDER: 44, FORWARD: 20 }

type Slots = Record<Position, (number | null)[]>

/** Kadronun mevki slotlarına dağılımı: dizilişin her mevkisi için o kadar slot. */
function buildSlots(formation: Formation, starterIds: number[], squad: LineupSquadPlayer[]): Slots {
  const byId = new Map(squad.map((player) => [player.id, player]))
  const slots = {} as Slots
  POSITIONS.forEach((position) => {
    const ids = starterIds.filter((id) => byId.get(id)?.position === position)
    slots[position] = Array.from({ length: FORMATION_COUNTS[formation][position] }, (_, index) => ids[index] ?? null)
  })
  return slots
}

/** Diziliş değişince aynı mevkideki oyuncular korunur, boş kalan slotlar en güçlü uygun oyuncularla dolar. */
function refill(formation: Formation, current: Slots, squad: LineupSquadPlayer[]): Slots {
  const used = new Set(
    Object.values(current)
      .flat()
      .filter((id): id is number => id !== null),
  )
  const slots = {} as Slots
  POSITIONS.forEach((position) => {
    const kept = current[position]
      .filter((id): id is number => id !== null)
      .slice(0, FORMATION_COUNTS[formation][position])
    const extra = squad
      .filter((player) => player.position === position && player.available && !used.has(player.id))
      .toSorted((a, b) => b.effectiveStrength - a.effectiveStrength)
      .map((player) => player.id)
    const filled = [...kept, ...extra].slice(0, FORMATION_COUNTS[formation][position])
    slots[position] = Array.from({ length: FORMATION_COUNTS[formation][position] }, (_, index) => filled[index] ?? null)
  })
  return slots
}

interface LineupEditorProps {
  onSaved: () => void
  onCancel: () => void
}

/**
 * Sıradaki maçın kadrosu: oyuncular sağdaki listeden saha üzerindeki slotlara sürüklenir (ya da önce oyuncuya,
 * sonra slota tıklanır). Her slot dizilişteki bir mevkiye aittir; cezalı / sakat oyuncu seçilemez.
 */
function LineupEditor({ onSaved, onCancel }: LineupEditorProps) {
  const [lineup, setLineup] = useState<Lineup | null>(null)
  const [formation, setFormation] = useState<Formation>('F442')
  const [playStyle, setPlayStyle] = useState<PlayStyle>('BALANCED')
  const [slots, setSlots] = useState<Slots | null>(null)
  const [captainId, setCaptainId] = useState<number | null>(null)
  const [penaltyTakerId, setPenaltyTakerId] = useState<number | null>(null)
  const [selected, setSelected] = useState<
    { kind: 'player'; id: number } | { kind: 'slot'; position: Position; index: number } | null
  >(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  function apply(data: Lineup) {
    setLineup(data)
    setFormation(data.formation)
    setPlayStyle(data.playStyle)
    setSlots(buildSlots(data.formation, data.starterIds, data.squad))
    setCaptainId(data.captainId)
    setPenaltyTakerId(data.penaltyTakerId)
  }

  useEffect(() => {
    api
      .getLineup()
      .then(apply)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  if (!lineup || !slots) {
    return error ? <p className="alert alert-error">{error}</p> : <p className="muted">Kadro yükleniyor…</p>
  }

  const byId = new Map(lineup.squad.map((player) => [player.id, player]))
  const starterIds = Object.values(slots)
    .flat()
    .filter((id): id is number => id !== null)
  const starters = starterIds.map((id) => byId.get(id)!)
  const complete = starterIds.length === 11

  function place(playerId: number, position: Position, index: number) {
    const player = byId.get(playerId)
    if (!player || !slots) {
      return
    }
    if (!player.available) {
      setError(`${player.name} cezalı ya da sakat`)
      return
    }
    if (player.position !== position) {
      setError(
        `${player.name} ${POSITION_LABELS[player.position].toLocaleLowerCase('tr')}; bu slot ${POSITION_LABELS[position].toLocaleLowerCase('tr')} için`,
      )
      return
    }
    setError(null)
    const next = { ...slots, [position]: [...slots[position]] }
    // Oyuncu başka slottaysa oradan çıkar (aynı mevkide yer değiştirir)
    const from = next[position].indexOf(playerId)
    if (from >= 0) {
      next[position][from] = next[position][index]
    }
    next[position][index] = playerId
    setSlots(next)
    setSelected(null)
  }

  function clearSlot(position: Position, index: number) {
    if (!slots) {
      return
    }
    const removed = slots[position][index]
    setSlots({ ...slots, [position]: slots[position].map((id, i) => (i === index ? null : id)) })
    if (removed === captainId) {
      setCaptainId(null)
    }
    if (removed === penaltyTakerId) {
      setPenaltyTakerId(null)
    }
  }

  function handleSlotClick(position: Position, index: number) {
    if (selected?.kind === 'player') {
      place(selected.id, position, index)
    } else {
      setSelected({ kind: 'slot', position, index })
    }
  }

  function handlePlayerClick(player: LineupSquadPlayer) {
    if (selected?.kind === 'slot') {
      place(player.id, selected.position, selected.index)
      return
    }
    if (!slots) {
      return
    }
    // Boş slot varsa doğrudan oraya, yoksa seçili bırak (sonra slota tıklanır)
    const empty = slots[player.position].indexOf(null)
    if (empty >= 0 && !starterIds.includes(player.id)) {
      place(player.id, player.position, empty)
    } else {
      setSelected({ kind: 'player', id: player.id })
    }
  }

  function handleDrop(event: DragEvent, position: Position, index: number) {
    event.preventDefault()
    const id = Number(event.dataTransfer.getData('text/plain'))
    if (id) {
      place(id, position, index)
    }
  }

  async function handleSave() {
    if (!complete || captainId === null || penaltyTakerId === null) {
      setError('11 oyuncu, kaptan ve penaltıcı seçilmeli')
      return
    }
    setBusy(true)
    setError(null)
    try {
      apply(await api.saveLineup({ formation, playStyle, starterIds, captainId, penaltyTakerId }))
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>
          Kadro {lineup.saved ? <span className="muted">· kaydedildi</span> : <span className="muted">· öneri</span>}
        </h2>
        <div className="page-actions">
          <button
            className="btn btn-sm"
            onClick={() =>
              setSlots(refill(formation, { GOALKEEPER: [], DEFENDER: [], MIDFIELDER: [], FORWARD: [] }, lineup.squad))
            }
          >
            En iyi 11
          </button>
          <button className="btn btn-sm" onClick={onCancel}>
            Kapat
          </button>
        </div>
      </div>
      {error && <p className="alert alert-error">{error}</p>}
      <div className="tactics">
        <label className="field">
          Diziliş
          <select
            value={formation}
            onChange={(event) => {
              const next = event.target.value as Formation
              setFormation(next)
              setSlots(refill(next, slots, lineup.squad))
            }}
          >
            {FORMATIONS.map((option) => (
              <option key={option} value={option}>
                {FORMATION_LABELS[option]}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          Stil
          <select value={playStyle} onChange={(event) => setPlayStyle(event.target.value as PlayStyle)}>
            {PLAY_STYLES.map((option) => (
              <option key={option} value={option}>
                {PLAY_STYLE_LABELS[option]}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          Kaptan
          <select value={captainId ?? ''} onChange={(event) => setCaptainId(Number(event.target.value))}>
            <option value="">Seçin</option>
            {starters.map((player) => (
              <option key={player.id} value={player.id}>
                {player.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          Penaltıcı
          <select value={penaltyTakerId ?? ''} onChange={(event) => setPenaltyTakerId(Number(event.target.value))}>
            <option value="">Seçin</option>
            {starters
              .filter((player) => player.position !== 'GOALKEEPER')
              .map((player) => (
                <option key={player.id} value={player.id}>
                  {player.name}
                </option>
              ))}
          </select>
        </label>
        <button className="btn btn-primary" onClick={handleSave} disabled={busy || !complete}>
          {busy ? 'Kaydediliyor…' : 'Kadroyu kaydet'}
        </button>
      </div>

      <div className="lineup-editor">
        <div className="pitch lineup-pitch">
          <PitchLines />
          {POSITIONS.map((position) =>
            slots[position].map((playerId, index) => {
              const player = playerId !== null ? byId.get(playerId) : undefined
              const isSelected = selected?.kind === 'slot' && selected.position === position && selected.index === index
              return (
                <div
                  key={`${position}-${index}`}
                  className={`lineup-slot${player ? ' filled' : ''}${isSelected ? ' selected' : ''}`}
                  style={{
                    left: `${((index + 1) / (slots[position].length + 1)) * 100}%`,
                    top: `${ROWS[position]}%`,
                  }}
                  onClick={() => handleSlotClick(position, index)}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => handleDrop(event, position, index)}
                  role="button"
                  tabIndex={0}
                  aria-label={
                    player ? `${player.name}, ${POSITION_LABELS[position]}` : `Boş ${POSITION_LABELS[position]} slotu`
                  }
                >
                  <span className="pitch-shirt">{player ? player.shirtNumber : POSITION_SHORT[position]}</span>
                  {player?.id === captainId && (
                    <span className="captain-badge" title="Kaptan">
                      C
                    </span>
                  )}
                  {player && (
                    <>
                      <span className="lineup-slot-name">{player.name.split(' ').at(-1)}</span>
                      <span className="lineup-slot-meta">
                        {Math.round(player.effectiveStrength)}
                        <button
                          className="lineup-slot-remove"
                          onClick={(event) => {
                            event.stopPropagation()
                            clearSlot(position, index)
                          }}
                          aria-label={`${player.name} kadrodan çıkar`}
                        >
                          ×
                        </button>
                      </span>
                    </>
                  )}
                </div>
              )
            }),
          )}
        </div>

        <div className="lineup-squad">
          {POSITIONS.map((position) => (
            <div key={position}>
              <div className="lineup-squad-title">{POSITION_LABELS[position]}</div>
              <ul>
                {lineup.squad
                  .filter((player) => player.position === position)
                  .toSorted((a, b) => b.effectiveStrength - a.effectiveStrength)
                  .map((player) => {
                    const starting = starterIds.includes(player.id)
                    const isSelected = selected?.kind === 'player' && selected.id === player.id
                    return (
                      <li
                        key={player.id}
                        className={`lineup-chip${starting ? ' starting' : ''}${!player.available ? ' unavailable' : ''}${isSelected ? ' selected' : ''}`}
                        draggable={player.available}
                        onDragStart={(event) => event.dataTransfer.setData('text/plain', String(player.id))}
                        onClick={() => player.available && handlePlayerClick(player)}
                        title={
                          player.available
                            ? `Güç ${player.strength}, form ve yorgunlukla ${player.effectiveStrength}`
                            : player.injuredMatches > 0
                              ? `${player.injurySeverity ? INJURY_LABELS[player.injurySeverity] : ''} sakatlık, ${player.injuredMatches} maç`
                              : `Cezalı, ${player.suspendedMatches} maç`
                        }
                      >
                        <span className="lineup-chip-number">{player.shirtNumber}</span>
                        <span className="lineup-chip-name">{player.name}</span>
                        <FormIndicator form={player.form} fatigue={player.fatigue} />
                        <span className="lineup-chip-strength">
                          {player.available ? Math.round(player.effectiveStrength) : '✚'}
                        </span>
                      </li>
                    )
                  })}
              </ul>
            </div>
          ))}
        </div>
      </div>
      <p className="legend muted">
        Oyuncuyu sahadaki aynı mevkiden bir slota sürükleyin ya da önce oyuncuya, sonra slota tıklayın. Sayı: form ve
        yorgunlukla efektif güç. Zayıf bir 11 seçmek takımın gol beklentisini düşürür.
      </p>
    </div>
  )
}

export default LineupEditor
