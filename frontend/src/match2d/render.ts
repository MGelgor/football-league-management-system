import type { Snapshot } from './engine'
import { PITCH_H, PITCH_W } from './geometry'

// Sahanın çevresinde boşluk (m): oyuna girenler / çıkanlar taç çizgisinin dışında görünür
// Formalar temadan bağımsız: yeşil sahada iki takım da her temada seçilsin
export const HOME_KIT = '#d8433b'
export const AWAY_KIT = '#2f6fb3'
const MARGIN_X = 3
const MARGIN_Y = 4
export const VIEW_W = PITCH_W + MARGIN_X * 2
export const VIEW_H = PITCH_H + MARGIN_Y * 2

interface Palette {
  grass: string
  stripe: string
  gold: string
  danger: string
  accent: string
}

function cssVar(element: Element, name: string, fallback: string) {
  return getComputedStyle(element).getPropertyValue(name).trim() || fallback
}

function palette(canvas: HTMLCanvasElement): Palette {
  return {
    grass: cssVar(canvas, '--grass', '#3d8b55'),
    stripe: cssVar(canvas, '--grass-stripe', '#378050'),
    gold: cssVar(canvas, '--gold', '#a87800'),
    danger: cssVar(canvas, '--danger', '#c0392b'),
    accent: cssVar(canvas, '--accent', '#1d7a4c'),
  }
}

/** Tuvali kapsayıcısının genişliğine ve ekran yoğunluğuna göre boyutlar; ölçek (piksel / metre) döner. */
function fit(canvas: HTMLCanvasElement, ctx: CanvasRenderingContext2D) {
  const width = canvas.clientWidth
  const height = (width * VIEW_H) / VIEW_W
  const ratio = window.devicePixelRatio || 1
  if (canvas.width !== Math.round(width * ratio) || canvas.height !== Math.round(height * ratio)) {
    canvas.width = Math.round(width * ratio)
    canvas.height = Math.round(height * ratio)
    canvas.style.height = `${height}px`
  }
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  return width / VIEW_W
}

export function drawMatch(canvas: HTMLCanvasElement, snapshot: Snapshot) {
  const ctx = canvas.getContext('2d')
  if (!ctx || canvas.clientWidth === 0) {
    return
  }
  const scale = fit(canvas, ctx)
  const colors = palette(canvas)
  const px = (x: number) => (x + MARGIN_X) * scale
  const py = (y: number) => (y + MARGIN_Y) * scale

  drawPitch(ctx, scale, colors, px, py)

  const radius = Math.max(5, 1.25 * scale)
  const players = [...snapshot.players].sort((a, b) => Number(a.label) - Number(b.label))
  for (const player of players) {
    ctx.globalAlpha = player.alpha
    const x = px(player.x)
    const y = py(player.y)
    const fill = player.goalkeeper ? (player.home ? colors.gold : '#7d5bc4') : player.home ? HOME_KIT : AWAY_KIT
    if (player.down) {
      ctx.fillStyle = 'rgba(0,0,0,0.25)'
      ctx.beginPath()
      ctx.ellipse(x, y, radius * 1.5, radius * 0.7, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = fill
      ctx.beginPath()
      ctx.ellipse(x, y, radius * 1.3, radius * 0.6, 0, 0, Math.PI * 2)
      ctx.fill()
    } else {
      ctx.fillStyle = 'rgba(0,0,0,0.22)'
      ctx.beginPath()
      ctx.ellipse(x + radius * 0.25, y + radius * 0.35, radius, radius * 0.75, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = fill
      ctx.strokeStyle = player.label ? '#ffffff' : 'rgba(255,255,255,0.75)'
      ctx.lineWidth = player.label ? 2.5 : 1.2
      ctx.beginPath()
      ctx.arc(x, y, radius, 0, Math.PI * 2)
      ctx.fill()
      ctx.stroke()
      if (radius >= 7) {
        ctx.fillStyle = '#ffffff'
        ctx.font = `700 ${Math.round(radius * 0.95)}px system-ui, sans-serif`
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText(String(player.shirtNumber), x, y + 0.5)
      }
    }
    if (player.mark) {
      ctx.font = `${Math.round(radius * 1.3)}px system-ui, sans-serif`
      ctx.textAlign = 'center'
      ctx.textBaseline = 'bottom'
      ctx.fillText(player.mark, x + radius, y - radius * 0.6)
    }
    if (player.label) {
      drawLabel(ctx, player.name.split(' ').at(-1) ?? player.name, x, y + radius + 3)
    }
  }
  ctx.globalAlpha = 1

  // Top: gölge yerde, top yüksekliği kadar yukarıda
  const ballX = px(snapshot.ball.x)
  const ballY = py(snapshot.ball.y)
  const lift = snapshot.ball.z * scale * 0.55
  const ballRadius = Math.max(3, 0.55 * scale) * (1 + snapshot.ball.z * 0.05)
  ctx.fillStyle = 'rgba(0,0,0,0.3)'
  ctx.beginPath()
  ctx.ellipse(ballX + 1, ballY + 1, ballRadius, ballRadius * 0.6, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.strokeStyle = '#1b2330'
  ctx.lineWidth = 1.2
  ctx.beginPath()
  ctx.arc(ballX, ballY - lift, ballRadius, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()

  if (snapshot.overlay) {
    drawOverlay(ctx, snapshot.overlay.text, snapshot.overlay.sub, snapshot.overlay.tone, colors, VIEW_W * scale, scale)
  }
}

function drawPitch(
  ctx: CanvasRenderingContext2D,
  scale: number,
  colors: Palette,
  px: (x: number) => number,
  py: (y: number) => number,
) {
  ctx.fillStyle = colors.grass
  ctx.fillRect(0, 0, VIEW_W * scale, VIEW_H * scale)
  ctx.fillStyle = colors.stripe
  for (let i = 0; i < 14; i += 2) {
    ctx.fillRect(px((PITCH_W / 14) * i), py(0), (PITCH_W / 14) * scale, PITCH_H * scale)
  }
  ctx.strokeStyle = 'rgba(255,255,255,0.7)'
  ctx.lineWidth = Math.max(1, 0.12 * scale)
  const rect = (x: number, y: number, w: number, h: number) => ctx.strokeRect(px(x), py(y), w * scale, h * scale)
  rect(0, 0, PITCH_W, PITCH_H)
  ctx.beginPath()
  ctx.moveTo(px(PITCH_W / 2), py(0))
  ctx.lineTo(px(PITCH_W / 2), py(PITCH_H))
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(px(PITCH_W / 2), py(PITCH_H / 2), 9.15 * scale, 0, Math.PI * 2)
  ctx.stroke()
  for (const left of [true, false]) {
    const edge = left ? 0 : PITCH_W
    const dir = left ? 1 : -1
    rect(left ? 0 : PITCH_W - 16.5, PITCH_H / 2 - 20.16, 16.5, 40.32)
    rect(left ? 0 : PITCH_W - 5.5, PITCH_H / 2 - 9.16, 5.5, 18.32)
    rect(left ? -2 : PITCH_W, PITCH_H / 2 - 3.66, 2, 7.32)
    ctx.fillStyle = 'rgba(255,255,255,0.8)'
    ctx.beginPath()
    ctx.arc(px(edge + dir * 11), py(PITCH_H / 2), Math.max(1.2, 0.25 * scale), 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.beginPath()
  ctx.arc(px(PITCH_W / 2), py(PITCH_H / 2), Math.max(1.2, 0.25 * scale), 0, Math.PI * 2)
  ctx.fill()
}

function drawLabel(ctx: CanvasRenderingContext2D, text: string, x: number, y: number) {
  ctx.font = '600 11px system-ui, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'top'
  const width = ctx.measureText(text).width + 8
  ctx.fillStyle = 'rgba(12,20,16,0.78)'
  ctx.beginPath()
  ctx.roundRect(x - width / 2, y, width, 15, 4)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.fillText(text, x, y + 2)
}

function drawOverlay(
  ctx: CanvasRenderingContext2D,
  text: string,
  sub: string,
  tone: string,
  colors: Palette,
  width: number,
  scale: number,
) {
  const background =
    tone === 'goal' ? colors.accent : tone === 'yellow' ? '#e0b100' : tone === 'red' ? colors.danger : 'rgba(12,20,16,0.82)'
  const big = Math.max(16, Math.min(34, scale * 4))
  ctx.font = `800 ${big}px system-ui, sans-serif`
  const textWidth = ctx.measureText(text).width
  ctx.font = `500 ${Math.round(big * 0.45)}px system-ui, sans-serif`
  const subWidth = sub ? ctx.measureText(sub).width : 0
  const boxWidth = Math.max(textWidth, subWidth) + big * 1.4
  const boxHeight = big * (sub ? 2.1 : 1.5)
  const x = (width - boxWidth) / 2
  const y = scale * 8
  ctx.fillStyle = background
  ctx.globalAlpha = 0.92
  ctx.beginPath()
  ctx.roundRect(x, y, boxWidth, boxHeight, 8)
  ctx.fill()
  ctx.globalAlpha = 1
  ctx.fillStyle = tone === 'yellow' ? '#1b2330' : '#ffffff'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'top'
  ctx.font = `800 ${big}px system-ui, sans-serif`
  ctx.fillText(text, width / 2, y + big * 0.25)
  if (sub) {
    ctx.font = `500 ${Math.round(big * 0.45)}px system-ui, sans-serif`
    ctx.fillText(sub, width / 2, y + big * 1.4)
  }
}
