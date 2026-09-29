import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../api/client'
import type { Team } from '../api/types'

const MAX_LOGO_BYTES = 5 * 1024 * 1024
const MIN_FOUNDED_YEAR = 1850
const CURRENT_YEAR = new Date().getFullYear()

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
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

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
      const saved = savedId === null ? await api.createTeam(request) : await api.updateTeam(savedId, request)
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
