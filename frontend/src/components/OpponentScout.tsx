import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, errorMessage } from '../api/client'
import type { Formation, OpponentReport, ScoutPlayer } from '../api/types'
import { CUP_ROUND_LABELS, FORMATION_LABELS, PLAY_STYLE_LABELS, POSITION_SHORT, POSITIONS } from '../labels'
import FormBadges from './FormBadges'
import FormIndicator from './FormIndicator'
import TeamLogo from './TeamLogo'

// Bu kadar güç farkından sonra strateji ipucu verilir
const CLEAR_GAP = 8

/** Rakibin güç farkına ve beklenen stiline göre kısa öneri. */
function strategyHint(report: OpponentReport) {
  if (report.strengthGap >= CLEAR_GAP) {
    return 'Rakip belirgin şekilde güçlü: savunma stili ve dizilişine karşı avantajlı bir diziliş şansını artırır. Galibiyet ya da attığın her gol ekstra moral getirir.'
  }
  if (report.strengthGap <= -CLEAR_GAP) {
    return 'Rakip belirgin şekilde zayıf: hücum stiliyle gol beklentini artırabilirsin; kaybetmek moralini sert düşürür.'
  }
  if (report.expectedStyle === 'ATTACKING') {
    return 'Rakip hücuma çıkacak: savunma stili rakibin gol beklentisini azaltır.'
  }
  return 'Denk bir maç: diziliş avantajı ve en iyi 11 farkı belirleyebilir.'
}

interface OpponentScoutProps {
  // Seçilen diziliş verilirse rakibin dizilişine karşı durumu gösterilir
  myFormation?: Formation
}

/** Sıradaki rakibin analizi: güç, form, taktik, muhtemel 11, öne çıkanlar ve eksikler. */
function OpponentScout({ myFormation }: OpponentScoutProps) {
  const [report, setReport] = useState<OpponentReport | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .getOpponentReport()
      .then(setReport)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  if (!report) {
    return error ? <p className="alert alert-error">{error}</p> : <p className="muted">Rakip analizi yükleniyor…</p>
  }

  const { team, standing } = report
  const matchup = myFormation
    ? report.strongAgainst.includes(myFormation)
      ? { className: 'scout-good', text: `${FORMATION_LABELS[myFormation]} bu dizilişe karşı avantajlı (+2)` }
      : report.weakAgainst.includes(myFormation)
        ? { className: 'scout-bad', text: `${FORMATION_LABELS[myFormation]} bu dizilişe karşı dezavantajlı (−2)` }
        : { className: 'muted', text: `${FORMATION_LABELS[myFormation]} ile nötr` }
    : null

  return (
    <div className="card scout">
      <div className="scout-head">
        <TeamLogo name={team.name} logoUrl={team.logoUrl} />
        <div>
          <div className="muted">Rakip analizi · {report.opponentHome ? 'deplasmandasın' : 'iç sahadasın'}</div>
          <h2 className="scout-title">
            <Link to={`/teams/${team.id}`} className="team-link">
              {team.name}
            </Link>
          </h2>
        </div>
        <dl className="hero-stats scout-stats">
          <div>
            <dt>Güç</dt>
            <dd>
              {team.strength}{' '}
              <span className={report.strengthGap > 0 ? 'scout-bad' : report.strengthGap < 0 ? 'scout-good' : 'muted'}>
                ({report.strengthGap > 0 ? '+' : ''}
                {report.strengthGap})
              </span>
            </dd>
          </div>
          <div>
            <dt>Moral</dt>
            <dd>{team.morale}</dd>
          </div>
          <div>
            <dt>Sıra</dt>
            <dd>{standing ? `${standing.rank}.` : '–'}</dd>
          </div>
          {team.manager && (
            <div>
              <dt>Hoca</dt>
              <dd className="scout-manager" title={`Taktik ustalığı ${team.manager.tacticalSkill}`}>
                {team.manager.name} ({team.manager.tacticalSkill})
              </dd>
            </div>
          )}
        </dl>
      </div>

      <p className="alert alert-info scout-hint">{strategyHint(report)}</p>

      <div className="scout-grid">
        <div>
          <h3>Taktik</h3>
          <p>
            Diziliş <strong>{FORMATION_LABELS[report.formation]}</strong> · beklenen stil{' '}
            <strong>{PLAY_STYLE_LABELS[report.expectedStyle]}</strong>
          </p>
          <p className="muted">
            Karşı avantajlı: {report.strongAgainst.map((formation) => FORMATION_LABELS[formation]).join(', ')} ·
            dezavantajlı: {report.weakAgainst.map((formation) => FORMATION_LABELS[formation]).join(', ')}
          </p>
          {matchup && <p className={matchup.className}>{matchup.text}</p>}
          {standing && (
            <p className="muted">
              {standing.played} maç · {standing.points} puan · {standing.goalsFor}-{standing.goalsAgainst}{' '}
              <FormBadges form={standing.form} />
            </p>
          )}
        </div>

        <div>
          <h3>Muhtemel 11</h3>
          <ul className="scout-eleven">
            {POSITIONS.map((position) => {
              const players = report.probableEleven.filter((player) => player.position === position)
              return (
                players.length > 0 && (
                  <li key={position}>
                    <span className="muted scout-pos">{POSITION_SHORT[position]}</span>
                    <span>
                      {players.map((player, index) => (
                        <span key={player.id}>
                          {index > 0 && ', '}
                          <ScoutName player={player} />
                        </span>
                      ))}
                    </span>
                  </li>
                )
              )
            })}
          </ul>
        </div>

        <div>
          <h3>Öne çıkanlar</h3>
          <ul className="scout-list">
            {report.keyPlayers.map((player) => (
              <li key={player.id}>
                <span className="muted scout-pos">{POSITION_SHORT[player.position]}</span>
                <ScoutName player={player} />
                {player.age !== null && <span className="muted"> · {player.age} yaş</span>}
              </li>
            ))}
          </ul>
          <h3>Eksikler</h3>
          {report.unavailable.length === 0 ? (
            <p className="muted">Eksik yok.</p>
          ) : (
            <ul className="scout-list">
              {report.unavailable.map((player) => (
                <li key={player.id}>
                  <span className="muted scout-pos">{POSITION_SHORT[player.position]}</span>
                  <Link to={`/players/${player.id}`}>{player.name}</Link>{' '}
                  <span className="muted">
                    ({player.strength}) · {player.note}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div>
          <h3>Son maçları</h3>
          {report.recentResults.length === 0 ? (
            <p className="muted">Henüz maç yok.</p>
          ) : (
            <ul className="recent-results">
              {report.recentResults.map((recent) => (
                <li key={recent.match.id}>
                  <span className={`form-badge form-${recent.result}`}>{recent.result}</span>
                  <Link to={`/matches/${recent.match.id}`}>
                    {recent.match.homeTeamName} {recent.match.homeScore} - {recent.match.awayScore}{' '}
                    {recent.match.awayTeamName}
                  </Link>
                  <span className="muted">
                    {recent.competition === 'CUP' && recent.cupRound
                      ? CUP_ROUND_LABELS[recent.cupRound]
                      : `H${recent.weekNumber}`}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}

function ScoutName({ player }: { player: ScoutPlayer }) {
  return (
    <>
      <Link
        to={`/players/${player.id}`}
        title={`Güç ${player.strength}, form ve yorgunlukla ${player.effectiveStrength}`}
      >
        {player.name.split(' ').at(-1)}
      </Link>{' '}
      <span className="muted">{Math.round(player.effectiveStrength)}</span>
      <FormIndicator form={player.form} fatigue={player.fatigue} />
    </>
  )
}

export default OpponentScout
