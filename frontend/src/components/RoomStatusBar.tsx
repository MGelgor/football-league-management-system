import { Link } from 'react-router'
import { useActiveRoom } from '../api/useActiveRoom'
import { roomStatusText } from '../roomTime'

/** Aktif odanın takvimi: üst çubuğun altında. */
function RoomStatusBar() {
  const { room, error } = useActiveRoom()

  if (!room && !error) {
    return null
  }
  return (
    <div className="room-bar">
      <div className="room-bar-inner">
        <span>{error ?? (room && roomStatusText(room))}</span>
        {room?.liveWeek ? <Link to="/live">İzle →</Link> : <Link to="/rooms">Oda →</Link>}
      </div>
    </div>
  )
}

export default RoomStatusBar
