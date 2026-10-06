interface LineChartProps {
  title: string
  // x: hafta, y: değer
  points: { x: number; y: number }[]
  // Sıralama grafiğinde 1. sıra üstte olsun diye eksen ters çevrilir
  invert?: boolean
  yMin?: number
  yMax?: number
  formatY?: (value: number) => string
}

const WIDTH = 520
const HEIGHT = 180
const PAD = { top: 12, right: 12, bottom: 24, left: 36 }

/** Bağımlılıksız, basit SVG çizgi grafik (haftalık sıra / güç geçmişi). */
function LineChart({ title, points, invert = false, yMin, yMax, formatY = String }: LineChartProps) {
  if (points.length === 0) {
    return (
      <figure className="chart">
        <figcaption>{title}</figcaption>
        <p className="muted">Henüz veri yok.</p>
      </figure>
    )
  }

  const xs = points.map((p) => p.x)
  const ys = points.map((p) => p.y)
  const minX = Math.min(...xs)
  const maxX = Math.max(...xs)
  const minY = yMin ?? Math.min(...ys)
  const maxY = yMax ?? Math.max(...ys)
  const spanY = maxY - minY || 1
  const spanX = maxX - minX || 1

  const x = (value: number) => PAD.left + ((value - minX) / spanX) * (WIDTH - PAD.left - PAD.right)
  const y = (value: number) => {
    const ratio = (value - minY) / spanY
    return PAD.top + (invert ? ratio : 1 - ratio) * (HEIGHT - PAD.top - PAD.bottom)
  }

  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(p.x).toFixed(1)},${y(p.y).toFixed(1)}`).join(' ')
  const yTicks = [minY, Math.round((minY + maxY) / 2), maxY]
  const last = points[points.length - 1]

  return (
    <figure className="chart">
      <figcaption>{title}</figcaption>
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`${title}: son değer ${formatY(last.y)}`}>
        {yTicks.map((tick) => (
          <g key={tick}>
            <line className="chart-grid" x1={PAD.left} x2={WIDTH - PAD.right} y1={y(tick)} y2={y(tick)} />
            <text className="chart-label" x={PAD.left - 6} y={y(tick) + 4} textAnchor="end">
              {formatY(tick)}
            </text>
          </g>
        ))}
        <text className="chart-label" x={x(minX)} y={HEIGHT - 6} textAnchor="start">
          Hafta {minX}
        </text>
        <text className="chart-label" x={x(maxX)} y={HEIGHT - 6} textAnchor="end">
          Hafta {maxX}
        </text>
        <path className="chart-line" d={path} />
        <circle className="chart-dot" cx={x(last.x)} cy={y(last.y)} r={4}>
          <title>
            Hafta {last.x}: {formatY(last.y)}
          </title>
        </circle>
      </svg>
    </figure>
  )
}

export default LineChart
