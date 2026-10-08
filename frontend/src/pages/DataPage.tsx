import { useEffect, useRef, useState } from 'react'
import { api, downloadUrls, errorMessage } from '../api/client'
import type { SaveSlot, Season } from '../api/types'

function formatSize(bytes: number) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('tr-TR', { dateStyle: 'medium', timeStyle: 'short' })
}

/** Yedek / geri yükleme, adlandırılmış kayıt noktaları ve CSV indirme. */
function DataPage() {
  const [saves, setSaves] = useState<SaveSlot[] | null>(null)
  const [seasons, setSeasons] = useState<Season[]>([])
  const [seasonId, setSeasonId] = useState<number | undefined>(undefined)
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  // Geri yükleme mevcut tüm veriyi sildiği için onay ister: kayıt adı ya da seçilen dosya
  const [pending, setPending] = useState<{ kind: 'slot'; name: string } | { kind: 'file'; file: File } | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  useEffect(() => {
    Promise.all([api.getSaves(), api.getSeasons()])
      .then(([saveList, seasonList]) => {
        setSaves(saveList)
        setSeasons(seasonList)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  async function run(action: () => Promise<string>) {
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      setMessage(await action())
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const handleSave = () =>
    run(async () => {
      const slot = await api.createSave(name)
      setName('')
      setSaves(await api.getSaves())
      return `"${slot.name}" kaydedildi.`
    })

  const handleRestore = () =>
    run(async () => {
      if (!pending) {
        return ''
      }
      if (pending.kind === 'slot') {
        await api.loadSave(pending.name)
      } else {
        await api.importBackup(pending.file)
      }
      const label = pending.kind === 'slot' ? `"${pending.name}" kaydı` : pending.file.name
      setPending(null)
      setSeasons(await api.getSeasons())
      setSeasonId(undefined)
      return `${label} yüklendi. Diğer sayfalar yeni veriyi gösterir.`
    })

  const handleDelete = (slotName: string) =>
    run(async () => {
      await api.deleteSave(slotName)
      setSaves(await api.getSaves())
      return `"${slotName}" silindi.`
    })

  return (
    <section>
      <h1>Veri</h1>
      {error && <p className="alert alert-error">{error}</p>}
      {message && <p className="alert alert-success">{message}</p>}

      {pending && (
        <div className="alert alert-warning confirm-box" role="alertdialog">
          <span>
            <strong>{pending.kind === 'slot' ? `"${pending.name}"` : pending.file.name}</strong> yüklenince mevcut tüm
            takımlar, sezonlar ve maçlar silinip yerine yedektekiler gelecek. Devam edilsin mi?
          </span>
          <span className="confirm-actions">
            <button className="btn btn-danger" onClick={handleRestore} disabled={busy}>
              {busy ? 'Yükleniyor…' : 'Evet, yükle'}
            </button>
            <button className="btn" onClick={() => setPending(null)} disabled={busy}>
              Vazgeç
            </button>
          </span>
        </div>
      )}

      <div className="card">
        <h2>Kayıt noktaları</h2>
        <p className="muted legend">
          Ligin o anki hâli sunucuda bir dosyaya kaydedilir; sezonun ortasında bile geri dönülebilir. Kayıtlar uygulama
          yeniden başlasa da kalır.
        </p>
        <form
          className="inline-form"
          onSubmit={(event) => {
            event.preventDefault()
            handleSave()
          }}
        >
          <input
            className="input-sm"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Kayıt adı (ör. Sezon 3 ortası)"
            maxLength={40}
            aria-label="Kayıt adı"
          />
          <button className="btn btn-primary" disabled={busy || name.trim() === ''}>
            Kaydet
          </button>
        </form>

        {saves && saves.length === 0 && <p className="muted">Henüz kayıt yok.</p>}
        {saves && saves.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Ad</th>
                  <th>Tarih</th>
                  <th>İçerik</th>
                  <th className="num">Boyut</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {saves.map((slot) => (
                  <tr key={slot.name}>
                    <td className="strong">{slot.name}</td>
                    <td>{formatDate(slot.savedAt)}</td>
                    <td className="muted">
                      {slot.summary.teamCount} takım
                      {slot.summary.seasonNumber !== null &&
                        ` · Sezon ${slot.summary.seasonNumber} · ${slot.summary.playedMatches} maç`}
                    </td>
                    <td className="num">{formatSize(slot.sizeBytes)}</td>
                    <td className="row-actions">
                      <button
                        className="btn btn-sm"
                        onClick={() => setPending({ kind: 'slot', name: slot.name })}
                        disabled={busy}
                      >
                        Yükle
                      </button>
                      <button
                        className="btn btn-sm btn-danger-outline"
                        onClick={() => handleDelete(slot.name)}
                        disabled={busy}
                      >
                        Sil
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Yedek dosyası</h2>
        <p className="muted legend">
          Tüm lig (takımlar, logolar, oyuncular, sezonlar, maçlar) tek bir JSON dosyasına indirilir. Bu dosya başka bir
          bilgisayarda ya da veritabanı sıfırlandıktan sonra geri yüklenebilir.
        </p>
        <div className="page-actions">
          <a className="btn btn-primary" href={downloadUrls.backup} download>
            Yedeği indir
          </a>
          <button className="btn" onClick={() => fileInput.current?.click()} disabled={busy}>
            Yedek yükle…
          </button>
          <input
            ref={fileInput}
            type="file"
            accept="application/json,.json"
            hidden
            onChange={(event) => {
              const file = event.target.files?.[0]
              if (file) {
                setPending({ kind: 'file', file })
              }
              event.target.value = ''
            }}
          />
        </div>
      </div>

      <div className="card">
        <h2>CSV olarak indir</h2>
        <p className="muted legend">Excel ile açılabilir (noktalı virgül ayraçlı, UTF-8).</p>
        {seasons.length > 0 ? (
          <div className="page-actions">
            <select
              className="select"
              value={seasonId ?? ''}
              onChange={(event) => setSeasonId(event.target.value ? Number(event.target.value) : undefined)}
              aria-label="Sezon"
            >
              <option value="">Güncel sezon</option>
              {seasons.map((season) => (
                <option key={season.id} value={season.id}>
                  Sezon {season.seasonNumber}
                </option>
              ))}
            </select>
            <a className="btn" href={downloadUrls.standingsCsv(seasonId)} download>
              Puan durumu
            </a>
            <a className="btn" href={downloadUrls.fixtureCsv(seasonId)} download>
              Fikstür
            </a>
            <a className="btn" href={downloadUrls.playersCsv(seasonId)} download>
              Oyuncu istatistikleri
            </a>
          </div>
        ) : (
          <p className="muted">Henüz sezon yok.</p>
        )}
      </div>
    </section>
  )
}

export default DataPage
