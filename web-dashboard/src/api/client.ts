const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost"
const METRICS_URL = import.meta.env.VITE_METRICS_URL ?? "http://localhost"

type TokenGetter = () => string | null
type RefreshFn = () => Promise<boolean>

let getToken: TokenGetter = () => null
let refreshToken: RefreshFn = async () => false

export function setAuthFunctions(getter: TokenGetter, refresh: RefreshFn) {
  getToken = getter
  refreshToken = refresh
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const baseUrl = path.startsWith("/api/metrics/") ? METRICS_URL : API_URL
  const url = `${baseUrl}${path}`

  const token = getToken()
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...Object.fromEntries(Object.entries(options?.headers ?? {})),
  }
  if (token) {
    headers["Authorization"] = `Bearer ${token}`
  }

  let response = await fetch(url, { ...options, headers })

  if (response.status === 401 && token) {
    const refreshed = await refreshToken()
    if (refreshed) {
      const newToken = getToken()
      if (newToken) {
        headers["Authorization"] = `Bearer ${newToken}`
      }
      response = await fetch(url, { ...options, headers })
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, response.statusText, await response.text().catch(() => ""))
  }

  if (response.status === 204) return undefined as T

  return response.json() as Promise<T>
}

export class ApiError extends Error {
  status: number
  statusText: string
  body: string

  constructor(status: number, statusText: string, body: string) {
    super(`API Error ${status}: ${statusText}`)
    this.status = status
    this.statusText = statusText
    this.body = body
  }
}
