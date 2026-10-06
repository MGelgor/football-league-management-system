import { Navigate, NavLink, Route, Routes } from 'react-router'
import ComparePage from './pages/ComparePage'
import CupPage from './pages/CupPage'
import FixturePage from './pages/FixturePage'
import MatchDetailPage from './pages/MatchDetailPage'
import PlayerPage from './pages/PlayerPage'
import SeasonsPage from './pages/SeasonsPage'
import StandingsPage from './pages/StandingsPage'
import TeamDetailPage from './pages/TeamDetailPage'
import TeamsPage from './pages/TeamsPage'

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header-inner">
          <span className="brand">⚽ Futbol Ligi</span>
          <nav className="nav">
            <NavLink to="/teams">Takımlar</NavLink>
            <NavLink to="/fixture">Fikstür</NavLink>
            <NavLink to="/standings">Puan Durumu</NavLink>
            <NavLink to="/cup">Kupa</NavLink>
            <NavLink to="/seasons">Sezonlar</NavLink>
            <NavLink to="/compare">Karşılaştır</NavLink>
          </nav>
        </div>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/teams" replace />} />
          <Route path="/teams" element={<TeamsPage />} />
          <Route path="/teams/:teamId" element={<TeamDetailPage />} />
          <Route path="/fixture" element={<FixturePage />} />
          <Route path="/matches/:matchId" element={<MatchDetailPage />} />
          <Route path="/standings" element={<StandingsPage />} />
          <Route path="/seasons" element={<SeasonsPage />} />
          <Route path="/cup" element={<CupPage />} />
          <Route path="/compare" element={<ComparePage />} />
          <Route path="/players/:playerId" element={<PlayerPage />} />
          <Route path="*" element={<p className="muted">Sayfa bulunamadı.</p>} />
        </Routes>
      </main>
    </div>
  )
}

export default App
