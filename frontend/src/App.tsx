import { Navigate, NavLink, Route, Routes } from 'react-router'
import FixturePage from './pages/FixturePage'
import StandingsPage from './pages/StandingsPage'
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
          </nav>
        </div>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/teams" replace />} />
          <Route path="/teams" element={<TeamsPage />} />
          <Route path="/fixture" element={<FixturePage />} />
          <Route path="/standings" element={<StandingsPage />} />
          <Route path="*" element={<p className="muted">Sayfa bulunamadı.</p>} />
        </Routes>
      </main>
    </div>
  )
}

export default App
