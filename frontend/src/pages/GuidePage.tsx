import { useEffect } from 'react'
import { Link, Navigate, useParams } from 'react-router'
import { GUIDE_MODULES } from '../guide/modules'
import { ArrowDefs } from '../guide/parts'
import '../guide/guide.css'

/** Sistemin nasıl çalıştığını şemalarla anlatan rehber: /guide/:moduleId. */
function GuidePage() {
  const { moduleId = GUIDE_MODULES[0].id } = useParams()
  const index = GUIDE_MODULES.findIndex((item) => item.id === moduleId)

  useEffect(() => {
    window.scrollTo(0, 0)
    // Mobilde modül şeridi yatay kayar; açık modül görünür kalsın
    document.querySelector('.g-modules a.active')?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  }, [moduleId])

  if (index < 0) {
    return <Navigate to="/guide" replace />
  }

  const current = GUIDE_MODULES[index]
  const previous = GUIDE_MODULES[index - 1]
  const next = GUIDE_MODULES[index + 1]
  const Content = current.Content

  return (
    <div className="guide">
      <ArrowDefs />
      <nav className="g-modules" aria-label="Rehber modülleri">
        {GUIDE_MODULES.map((item) => (
          <Link key={item.id} to={`/guide/${item.id}`} className={item.id === current.id ? 'active' : undefined}>
            <span>{item.code}</span>
            {item.title}
          </Link>
        ))}
      </nav>

      <article>
        <header className="g-dm">
          <div className="g-dm-code">
            <span>
              <b>{current.code}</b> {current.title.toLocaleUpperCase('tr')}
            </span>
            <span>{current.source}</span>
          </div>
          <h1>{current.title}</h1>
          <p className="g-lead">{current.lead}</p>
          <ul className="g-toc">
            {current.sections.map((section) => (
              <li key={section.id}>
                <a href={`#${section.id}`}>
                  <span className="g-no">{section.no}</span>
                  {section.title}
                </a>
              </li>
            ))}
          </ul>
          {current.pages && (
            <ul className="g-related">
              <li className="muted">Uygulamada:</li>
              {current.pages.map((page) => (
                <li key={page.to}>
                  <Link to={page.to}>{page.label} →</Link>
                </li>
              ))}
            </ul>
          )}
        </header>

        <Content />

        {index === 0 && (
          <section className="g-sec">
            <div className="g-sec-head">
              <span className="g-sec-no">0.7</span>
              <h2>Modüller</h2>
            </div>
            <div className="g-cards">
              {GUIDE_MODULES.slice(1).map((item) => (
                <Link key={item.id} className="g-card" to={`/guide/${item.id}`}>
                  <span>{item.code}</span>
                  <b>{item.title}</b>
                  <p>{item.summary}</p>
                </Link>
              ))}
            </div>
          </section>
        )}

        <nav className="g-pager" aria-label="Sayfa geçişi">
          {previous && (
            <Link to={`/guide/${previous.id}`}>
              <span>ÖNCEKİ · {previous.code}</span>
              {previous.title}
            </Link>
          )}
          {next && (
            <Link className="next" to={`/guide/${next.id}`}>
              <span>SONRAKİ · {next.code}</span>
              {next.title}
            </Link>
          )}
        </nav>
      </article>
    </div>
  )
}

export default GuidePage
