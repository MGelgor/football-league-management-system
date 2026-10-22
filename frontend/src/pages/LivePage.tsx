import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { api } from '../api/client'
import { inRoom, notifyRoomChanged } from '../api/rooms'
import type { Team } from '../api/types'
import { useActiveRoom } from '../api/useActiveRoom'
import LiveBroadcast from '../components/LiveBroadcast'
import { roomStatusText } from '../roomTime'

/** Odanın canlı maç günü: maçlar saatinde oynanır, herkes aynı dakikayı izler. */
function LivePage() {
  const { room, error } = useActiveRoom()
  const [teams, setTeams] = useState<Team[]>([])
  const navigate = useNavigate()

  useEffect(() => {
    if (inRoom()) {
      // Takım listesi canlı yayın sırasında da açık; kendi takımımın maçı öne alınır
      api
        .getTeams()
        .then(setTeams)
        .catch(() => setTeams([]))
    }
  }, [])

  if (!inRoom()) {
    return (
      <section>
        <h1>Canlı</h1>
        <p className="alert alert-info">
          Canlı maç günleri odalarda oynanır. <Link to="/rooms">Bir odaya katıl</Link>.
        </p>
      </section>
    )
  }
  if (error) {
    return <p className="alert alert-error">{error}</p>
  }
  if (!room) {
    return <p className="muted">Yükleniyor…</p>
  }

  const myTeamName = room.members.find((member) => member.id === room.you)?.teamName
  const myTeamId = teams.find((team) => team.name === myTeamName)?.id
  return (
    <section>
      <h1>Canlı</h1>
      {room.liveWeek ? (
        <LiveBroadcast
          key={room.liveWeek}
          weekNumber={room.liveWeek}
          title={room.liveTitle ?? 'Maç günü'}
          highlightTeamIds={myTeamId ? [myTeamId] : []}
          realTime
          onFinish={notifyRoomChanged}
          onClose={() => navigate('/fixture')}
        />
      ) : (
        <div className="card">
          <p>Şu an canlı maç yok.</p>
          <p className="muted">{roomStatusText(room)}</p>
          <p className="muted legend">
            Maç günü saatinde bu sayfayı aç: maçlar 10 dakikada oynanır, herkes aynı dakikayı izler. Yayın sürerken puan
            durumu ve fikstür kilitlidir; bittiğinde sonuçlar her sayfada görünür.
          </p>
        </div>
      )}
    </section>
  )
}

export default LivePage
