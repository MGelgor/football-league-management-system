import { Children, type ReactNode } from 'react'

export type Tone = 'plain' | 'acc' | 'sig' | 'dan' | 'away' | 'ghost' | 'gold'
type EdgeTone = 'acc' | 'sig' | 'dan' | 'away'
type TextTone = 'acc' | 'sig' | 'dan' | 'away' | 'mut'

export type Line = string | { text: string; mono?: boolean; tone?: TextTone; bold?: boolean }

function cx(...names: (string | false | undefined)[]) {
  return names.filter(Boolean).join(' ')
}

/** Tüm şemaların kullandığı ok başları; aynı belgedeki her SVG bu id'lere başvurur. */
export function ArrowDefs() {
  return (
    <svg className="g-defs" width="0" height="0" aria-hidden>
      <defs>
        {(['', 'acc', 'sig', 'dan', 'away'] as const).map((tone) => (
          <marker
            key={tone}
            id={tone ? `g-arrow-${tone}` : 'g-arrow'}
            viewBox="0 0 10 10"
            refX="9"
            refY="5"
            markerWidth="7"
            markerHeight="7"
            orient="auto-start-reverse"
          >
            <path className={cx('g-ah', tone)} d="M0,0L10,5L0,10z" />
          </marker>
        ))}
      </defs>
    </svg>
  )
}

interface EdgeProps {
  d?: string
  from?: [number, number]
  to?: [number, number]
  tone?: EdgeTone
  dash?: boolean
  thin?: boolean
  both?: boolean
  head?: boolean
}

export function Edge({ d, from, to, tone, dash, thin, both, head = true }: EdgeProps) {
  const path = d ?? (from && to ? `M${from[0]},${from[1]} L${to[0]},${to[1]}` : '')
  const marker = `url(#${tone ? `g-arrow-${tone}` : 'g-arrow'})`
  return (
    <path
      className={cx('g-ed', tone, dash && 'dash', thin && 'thin')}
      d={path}
      markerEnd={head ? marker : undefined}
      markerStart={both ? marker : undefined}
    />
  )
}

interface TxtProps {
  x: number
  y: number
  children: ReactNode
  cls?: string
  anchor?: 'start' | 'middle' | 'end'
}

export function Txt({ x, y, children, cls, anchor }: TxtProps) {
  return (
    <text x={x} y={y} className={cls} textAnchor={anchor}>
      {children}
    </text>
  )
}

interface BoxProps {
  x: number
  y: number
  w: number
  h: number
  tone?: Tone
  tag?: string
  title?: string
  lines?: Line[]
  center?: boolean
  rx?: number
  strong?: boolean
}

/** Kenarlıklı kutu: üstte küçük etiket, başlık ve satırlar dikeyde ortalanır. */
export function Box({ x, y, w, h, tone = 'plain', tag, title, lines = [], center = false, rx = 4, strong }: BoxProps) {
  const tagH = tag ? 15 : 0
  const titleH = title ? 18 : 0
  const total = tagH + titleH + lines.length * 16
  const top = y + (h - total) / 2
  const tx = center ? x + w / 2 : x + 12
  const anchor = center ? 'middle' : undefined
  return (
    <g>
      <rect className={cx('g-box', tone, strong && 'strong')} x={x} y={y} width={w} height={h} rx={rx} />
      {tag && (
        <text className="g-ms" x={tx} y={top + 10} textAnchor={anchor}>
          {tag}
        </text>
      )}
      {title && (
        <text className="g-t" x={tx} y={top + tagH + 13} textAnchor={anchor}>
          {title}
        </text>
      )}
      {lines.map((line, i) => {
        const item = typeof line === 'string' ? { text: line } : line
        return (
          <text
            key={i}
            className={cx(item.mono ? 'g-m' : 'g-s', item.tone && `t-${item.tone}`, item.bold && 'g-b')}
            x={tx}
            y={top + tagH + titleH + 12 + i * 16}
            textAnchor={anchor}
          >
            {item.text}
          </text>
        )
      })}
    </g>
  )
}

interface FigureProps {
  no: string
  caption: ReactNode
  label: string
  width?: number
  height: number
  minWidth?: number
  children: ReactNode
}

export function Figure({ no, caption, label, width = 960, height, minWidth = 720, children }: FigureProps) {
  return (
    <figure className="g-fig">
      <div className="g-scroll">
        <svg className="g-dg" viewBox={`0 0 ${width} ${height}`} style={{ minWidth }} role="img" aria-label={label}>
          {children}
        </svg>
      </div>
      <figcaption>
        <b>ŞEKİL {no}</b>
        <span>{caption}</span>
      </figcaption>
    </figure>
  )
}

interface SectionProps {
  id: string
  no: string
  title: string
  children: ReactNode
}

export function Section({ id, no, title, children }: SectionProps) {
  return (
    <section className="g-sec" id={id}>
      <div className="g-sec-head">
        <span className="g-sec-no">{no}</span>
        <h2>{title}</h2>
      </div>
      {children}
    </section>
  )
}

export function Prose({ children }: { children: ReactNode }) {
  return <div className="g-prose">{children}</div>
}

interface NoteProps {
  kind?: 'note' | 'caution'
  label?: string
  children: ReactNode
}

export function Note({ kind = 'note', label, children }: NoteProps) {
  return (
    <div className={cx('g-note', kind)}>
      <span className="g-note-k">{label ?? (kind === 'caution' ? 'DİKKAT' : 'NOT')}</span>
      <div>{children}</div>
    </div>
  )
}

export function Steps({ children }: { children: ReactNode }) {
  return (
    <ol className="g-steps">
      {Children.toArray(children).map((child, i) => (
        <li key={i}>
          <div>{child}</div>
        </li>
      ))}
    </ol>
  )
}

export function Cmd({ children }: { children: string }) {
  return <pre className="g-cmd">{children}</pre>
}

export function GTable({ head, rows }: { head: string[]; rows: ReactNode[][] }) {
  return (
    <div className="table-wrap">
      <table className="table g-table">
        <thead>
          <tr>
            {head.map((h) => (
              <th key={h}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              {row.map((cell, j) => (
                <td key={j}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export interface SectionMeta {
  id: string
  no: string
  title: string
}

export interface ModuleMeta {
  id: string
  code: string
  title: string
  summary: string
  lead: string
  source: string
  sections: SectionMeta[]
  pages?: { to: string; label: string }[]
}
