interface TeamLogoProps {
  name: string
  logoUrl: string | null
  large?: boolean
}

function TeamLogo({ name, logoUrl, large }: TeamLogoProps) {
  const className = `team-logo${large ? ' team-logo-lg' : ''}`
  if (logoUrl) {
    return <img className={className} src={logoUrl} alt={`${name} logosu`} />
  }
  return <span className={`${className} team-logo-placeholder`}>{name.charAt(0).toLocaleUpperCase('tr')}</span>
}

export default TeamLogo
