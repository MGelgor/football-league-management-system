import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../api/client'
import type { Position, Team } from '../api/types'
import { POSITION_LABELS, POSITIONS } from '../labels'

const MAX_LOGO_BYTES = 5 * 1024 * 1024
const MIN_FOUNDED_YEAR = 1850
const CURRENT_YEAR = new Date().getFullYear()
const MAX_PLAYERS = 30
const DEFAULT_PLAYER_STRENGTH = 70
const DEFAULT_PLAYER_AGE = 24

interface DraftPlayer {
  key: number
  name: string
  position: Position
  shirtNumber: string
  strength: string
  age: string
}

function nextFreeShirtNumber(players: DraftPlayer[]) {
  const used = new Set(players.map((p) => Number(p.shirtNumber)))
  let number = 1
  while (used.has(number) && number < 99) {
    number++
  }
  return String(number)
}

interface TeamFormProps {
  team: Team | null
  onSaved: () => void
  onCancel: () => void
}

function TeamForm({ team, onSaved, onCancel }: TeamFormProps) {
  const [name, setName] = useState(team?.name ?? '')
  const [foundedYear, setFoundedYear] = useState(team ? String(team.foundedYear) : '')
  const [colors, setColors] = useState(team?.colors ?? '')
  const [logo, setLogo] = useState<File | null>(null)
  // Takım oluşturulup logo yüklenemezse, tekrar "Kaydet" yeni takım değil güncelleme yapsın diye tutulur
  const [savedId, setSavedId] = useState(team?.id ?? null)
  const [players, setPlayers] = useState<DraftPlayer[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const creating = savedId === null

  function addPlayer() {
    setPlayers((current) => [
      ...current,
      {
        key: Math.max(0, ...current.map((p) => p.key)) + 1,
        name: '',
        position: 'FORWARD',
        shirtNumber: nextFreeShirtNumber(current),
        strength: String(DEFAULT_PLAYER_STRENGTH),
        age: String(DEFAULT_PLAYER_AGE),
      },
    ])
  }

  function updatePlayer(key: number, changes: Partial<DraftPlayer>) {
    setPlayers((current) => current.map((p) => (p.key === key ? { ...p, ...changes } : p)))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (logo && logo.size > MAX_LOGO_BYTES) {
      setError('Logo dosyası en fazla 5MB olabilir')
      return
    }

    setSaving(true)
    setError(null)
    try {
      const request = { name: name.trim(), foundedYear: Number(foundedYear), colors: colors.trim() }
      const saved =
        savedId === null
          ? await api.createTeam({
              ...request,
              players: players.map((p) => ({
                name: p.name.trim(),
                position: p.position,
                shirtNumber: Number(p.shirtNumber),
                strength: Number(p.strength),
                age: Number(p.age),
              })),
            })
          : await api.updateTeam(savedId, request)
      setSavedId(saved.id)
      if (logo) {
        await api.uploadLogo(saved.id, logo)
      }
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>{team ? `Düzenle: ${team.name}` : 'Yeni takım'}</h2>
      {error && <p className="alert alert-error">{error}</p>}

      <div className="form-grid">
        <label className="field">
          <span>Takım adı</span>
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
        </label>
        <label className="field">
          <span>Kuruluş yılı</span>
          <input
            type="number"
            value={foundedYear}
            onChange={(e) => setFoundedYear(e.target.value)}
            required
            min={MIN_FOUNDED_YEAR}
            max={CURRENT_YEAR}
          />
        </label>
        <label className="field">
          <span>Renkler</span>
          <input
            value={colors}
            onChange={(e) => setColors(e.target.value)}
            placeholder="Örn. Sarı-Lacivert"
            required
            maxLength={100}
          />
        </label>
        <label className="field">
          <span>Logo {team?.logoUrl ? '(değiştirmek için seçin)' : '(isteğe bağlı)'}</span>
          <input type="file" accept="image/*" onChange={(e) => setLogo(e.target.files?.[0] ?? null)} />
        </label>
      </div>

      {creating && (
        <fieldset className="player-drafts">
          <legend>Oyuncular (isteğe bağlı)</legend>
          <p className="muted legend">
            Güç (1–100), mevkiyle birlikte golün o oyuncuya yazılma olasılığını belirler: aynı mevkide gücü iki katı
            olan oyuncu iki kat sık gol atar. Eksik mevkiler rastgele oyuncularla 18 kişiye tamamlanır.
          </p>
          {players.length > 0 && (
            <div className="player-draft-head muted" aria-hidden>
              <span>Ad</span>
              <span>Mevki</span>
              <span>No</span>
              <span>Güç</span>
              <span>Yaş</span>
              <span />
            </div>
          )}
          {players.map((player) => (
            <div key={player.key} className="player-draft">
              <input
                className="input-sm"
                value={player.name}
                onChange={(e) => updatePlayer(player.key, { name: e.target.value })}
                placeholder="Oyuncu adı"
                required
                maxLength={100}
                aria-label="Oyuncu adı"
              />
              <select
                className="input-sm"
                value={player.position}
                onChange={(e) => updatePlayer(player.key, { position: e.target.value as Position })}
                aria-label="Mevki"
              >
                {POSITIONS.map((p) => (
                  <option key={p} value={p}>
                    {POSITION_LABELS[p]}
                  </option>
                ))}
              </select>
              <input
                className="input-sm"
                type="number"
                min={1}
                max={99}
                required
                value={player.shirtNumber}
                onChange={(e) => updatePlayer(player.key, { shirtNumber: e.target.value })}
                aria-label="Forma numarası"
              />
              <input
                className="input-sm"
                type="number"
                min={1}
                max={100}
                required
                value={player.strength}
                onChange={(e) => updatePlayer(player.key, { strength: e.target.value })}
                aria-label="Oyuncu gücü"
              />
              <input
                className="input-sm"
                type="number"
                min={16}
                max={45}
                required
                value={player.age}
                onChange={(e) => updatePlayer(player.key, { age: e.target.value })}
                aria-label="Yaş"
              />
              <button
                type="button"
                className="btn btn-sm btn-danger-outline"
                onClick={() => setPlayers((current) => current.filter((p) => p.key !== player.key))}
                aria-label="Oyuncuyu çıkar"
              >
                ×
              </button>
            </div>
          ))}
          <button type="button" className="btn btn-sm" onClick={addPlayer} disabled={players.length >= MAX_PLAYERS}>
            + Oyuncu ekle
          </button>
        </fieldset>
      )}

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Kaydediliyor…' : 'Kaydet'}
        </button>
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>
          Vazgeç
        </button>
      </div>
    </form>
  )
}

export default TeamForm
