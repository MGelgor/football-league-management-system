import { useEffect, useRef } from 'react'
import { MatchEngine, type MatchScript } from '../match2d/engine'
import { AWAY_KIT, drawMatch, HOME_KIT } from '../match2d/render'

interface MatchCanvasProps {
  script: MatchScript
  // Ekrandaki maç dakikası; bir dakikanın olay sahnesi bitince onMinuteShown(dakika) çağrılır
  minute: number
  playing: boolean
  // Ekran hızı (1, 3, 10): animasyon da hızlanır ama daha az
  speed: number
  onMinuteShown: (minute: number) => void
  homeName: string
  awayName: string
}

function playbackRate(speed: number) {
  return speed >= 10 ? 3 : speed >= 3 ? 1.6 : 1
}

/**
 * Maçın 2D animasyonu: oyuncular ve top tuvalde (canvas) çizilir. Dakika sayacı ebeveyndedir; olaylı dakikada
 * ebeveyn sayaç, sahne bitip onMinuteShown gelene kadar bekler (bkz. match2d/engine.ts).
 */
function MatchCanvas({ script, minute, playing, speed, onMinuteShown, homeName, awayName }: MatchCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const engineRef = useRef<MatchEngine | null>(null)
  const shownRef = useRef(onMinuteShown)
  const playingRef = useRef(playing)
  const speedRef = useRef(speed)
  const scriptRef = useRef(script)
  const minuteRef = useRef(minute)
  const matchId = script.matchId

  useEffect(() => {
    shownRef.current = onMinuteShown
    playingRef.current = playing
    speedRef.current = speed
    scriptRef.current = script
    minuteRef.current = minute
  })

  // Maç değişince motor baştan kurulur; aynı maçta yalnızca olaylar / oyuncular güncellenir
  useEffect(() => {
    engineRef.current = new MatchEngine(scriptRef.current, minuteRef.current, (shown) => shownRef.current(shown))
    // Tuval sonradan açıldıysa (görünüm değişti) bu dakika gösterilmiş sayılır; sayaç beklemez
    shownRef.current(minuteRef.current)
    return () => {
      engineRef.current = null
    }
  }, [matchId])

  useEffect(() => {
    engineRef.current?.setScript(script)
  }, [script])

  useEffect(() => {
    engineRef.current?.setMinute(minute)
  }, [minute])

  useEffect(() => {
    let frame = 0
    let last = performance.now()
    const loop = (time: number) => {
      const dt = Math.min(0.05, (time - last) / 1000)
      last = time
      const engine = engineRef.current
      const canvas = canvasRef.current
      if (engine && canvas) {
        if (playingRef.current) {
          engine.update(dt * playbackRate(speedRef.current))
        }
        drawMatch(canvas, engine.snapshot())
      }
      frame = requestAnimationFrame(loop)
    }
    frame = requestAnimationFrame(loop)
    return () => cancelAnimationFrame(frame)
  }, [])

  return (
    <div className="match-canvas">
      <div className="pitch-legend">
        <span>
          <span className="kit-dot" style={{ background: HOME_KIT }} /> {homeName} →
        </span>
        <span>
          ← {awayName} <span className="kit-dot" style={{ background: AWAY_KIT }} />
        </span>
      </div>
      <canvas ref={canvasRef} role="img" aria-label="Maçın sahadaki animasyonu" />
    </div>
  )
}

export default MatchCanvas
