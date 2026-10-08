import { Link } from 'react-router'

interface ErrorAlertProps {
  message: string
  // Yönetilen takımın maçı kadro bekliyorsa: "yapay zekâ seçsin" ile tekrar dene
  onAuto?: () => void
}

/** Hata mesajı; menajer modundaki "önce kadroyu belirleyin" hatasında Takımım bağlantısı ve otomatik seçenek. */
function ErrorAlert({ message, onAuto }: ErrorAlertProps) {
  const managed = message.includes('Takımım')
  return (
    <div className="alert alert-error confirm-box">
      <span>{message}</span>
      {managed && (
        <span className="confirm-actions">
          <Link to="/my-team" className="btn btn-sm">
            Takımım →
          </Link>
          {onAuto && (
            <button className="btn btn-sm" onClick={onAuto}>
              Kadroyu yapay zekâ seçsin
            </button>
          )}
        </span>
      )}
    </div>
  )
}

export default ErrorAlert
