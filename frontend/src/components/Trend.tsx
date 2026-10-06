interface TrendProps {
  value: number
  title?: string
}

/** Pozitif değişimde yeşil ▲, negatifte kırmızı ▼; değişim yoksa hiçbir şey göstermez. */
function Trend({ value, title }: TrendProps) {
  if (value === 0) {
    return null
  }
  const up = value > 0
  return (
    <span className={`trend ${up ? 'trend-up' : 'trend-down'}`} title={title}>
      {up ? '▲' : '▼'}
      {Math.abs(value)}
    </span>
  )
}

export default Trend
