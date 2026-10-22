import { lazy, Suspense } from 'react'
import { Link, Navigate, NavLink, Route, Routes } from 'react-router'
import { activeRoom } from './api/rooms'
import ComparePage from './pages/ComparePage'
import CupPage from './pages/CupPage'
import DataPage from './pages/DataPage'
import FixturePage from './pages/FixturePage'
import MatchDetailPage from './pages/MatchDetailPage'
import MyTeamPage from './pages/MyTeamPage'
import PlayerPage from './pages/PlayerPage'
import RoomsPage from './pages/RoomsPage'
import RoomStatusBar from './components/RoomStatusBar'
import RefereesPage from './pages/RefereesPage'
import SeasonsPage from './pages/SeasonsPage'
import StandingsPage from './pages/StandingsPage'
import TeamDetailPage from './pages/TeamDetailPage'
import TeamsPage from './pages/TeamsPage'
import TransfersPage from './pages/TransfersPage'

// Rehber büyük olduğu için ayrı bir dosyada yüklenir (yalnızca açılınca)
const GuidePage = lazy(() => import('./pages/GuidePage'))

function App() {
  const room = activeRoom()
  const guide = (
    <Suspense fallback={<p className="muted">Yükleniyor…</p>}>
      <GuidePage />
    </Suspense>
  )

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header-inner">
          <span className="brand">⚽ Futbol Ligi</span>
          <Link className="room-chip" to="/rooms" title="Odalar">
            {room ? `${room.name} · ${room.owner ? 'sahip' : 'üye'}` : 'Tek kişilik'}
          </Link>
          <nav className="nav">
            <NavLink to="/rooms">Odalar</NavLink>
            <NavLink to="/my-team">Takımım</NavLink>
            <NavLink to="/teams">Takımlar</NavLink>
            <NavLink to="/fixture">Fikstür</NavLink>
            <NavLink to="/standings">Puan Durumu</NavLink>
            <NavLink to="/cup">Kupa</NavLink>
            <NavLink to="/seasons">Sezonlar</NavLink>
            <NavLink to="/transfers">Transferler</NavLink>
            <NavLink to="/compare">Karşılaştır</NavLink>
            <NavLink to="/data">Veri</NavLink>
            <NavLink to="/guide">Rehber</NavLink>
          </nav>
        </div>
      </header>
      {room && <RoomStatusBar />}

      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to={room ? '/teams' : '/rooms'} replace />} />
          <Route path="/rooms" element={<RoomsPage />} />
          <Route path="/join/:code" element={<RoomsPage />} />
          <Route path="/teams" element={<TeamsPage />} />
          <Route path="/teams/:teamId" element={<TeamDetailPage />} />
          <Route path="/fixture" element={<FixturePage />} />
          <Route path="/matches/:matchId" element={<MatchDetailPage />} />
          <Route path="/standings" element={<StandingsPage />} />
          <Route path="/seasons" element={<SeasonsPage />} />
          <Route path="/cup" element={<CupPage />} />
          <Route path="/compare" element={<ComparePage />} />
          <Route path="/players/:playerId" element={<PlayerPage />} />
          <Route path="/data" element={<DataPage />} />
          <Route path="/referees" element={<RefereesPage />} />
          <Route path="/transfers" element={<TransfersPage />} />
          <Route path="/my-team" element={<MyTeamPage />} />
          <Route path="/guide" element={guide} />
          <Route path="/guide/:moduleId" element={guide} />
          <Route path="*" element={<p className="muted">Sayfa bulunamadı.</p>} />
        </Routes>
      </main>
    </div>
  )
}

export default App
