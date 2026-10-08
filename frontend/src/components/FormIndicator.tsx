interface FormIndicatorProps {
  // 0.9-1.1 arası form çarpanı
  form: number
  fatigue?: number
}

/** Form oku (iyi / kötü form) ve yorgunluk simgesi; nötr formda ve dinçken hiçbir şey göstermez. */
function FormIndicator({ form, fatigue = 0 }: FormIndicatorProps) {
  const percent = Math.round((form - 1) * 100)
  return (
    <>
      {percent >= 3 && (
        <span className="form-arrow form-up" title={`İyi formda (+%${percent})`}>
          ↗
        </span>
      )}
      {percent <= -3 && (
        <span className="form-arrow form-down" title={`Formsuz (%${percent})`}>
          ↘
        </span>
      )}
      {fatigue > 0 && (
        <span className="fatigue" title={`Yorgun: üst üste çok maç, güç −${fatigue}`}>
          💤
        </span>
      )}
    </>
  )
}

export default FormIndicator
