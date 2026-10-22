/**
 * Yayında frontend (GitHub Pages) ile backend (Render) farklı adreslerde: istekler VITE_API_URL ile başlar.
 * Geliştirmede boştur, istekler Vite proxy'sinden geçer. Uygulama bir alt yolda yayınlanır (BASE_URL).
 */
const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export function apiUrl(path: string): string {
  return API_BASE + path
}

/** Uygulama içi mutlak yol: '/teams' → '/football-league-management-system/teams' */
export function appPath(path: string): string {
  return import.meta.env.BASE_URL.replace(/\/$/, '') + path
}
