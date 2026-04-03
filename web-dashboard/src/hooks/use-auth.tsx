import { createContext, useContext, useState, useEffect, useCallback, useRef, type ReactNode } from "react"

const safeStorage = {
  getItem(key: string): string | null {
    try { return localStorage.getItem(key) } catch { return null }
  },
  setItem(key: string, value: string) {
    try { localStorage.setItem(key, value) } catch { /* noop */ }
  },
  removeItem(key: string) {
    try { localStorage.removeItem(key) } catch { /* noop */ }
  },
}
import * as authApi from "@/api/auth"
import { setAuthFunctions } from "@/api/client"
import type { Role } from "@/api/types"

interface AuthState {
  user: string | null
  role: Role | null
  accessToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

function parseJwt(token: string): { sub: string; role: Role; exp: number } | null {
  try {
    const payload = token.split(".")[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<string | null>(null)
  const [role, setRole] = useState<Role | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout>>()

  const clearAuth = useCallback(() => {
    setUser(null)
    setRole(null)
    setAccessToken(null)
    safeStorage.removeItem("cb-refresh-token")
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current)
  }, [])

  const setAuthFromTokens = useCallback((access: string, refreshToken: string, expiresIn: number) => {
    const parsed = parseJwt(access)
    if (!parsed) return
    setUser(parsed.sub)
    setRole(parsed.role)
    setAccessToken(access)
    safeStorage.setItem("cb-refresh-token", refreshToken)

    // Schedule refresh at 80% of TTL
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current)
    const refreshMs = expiresIn * 0.8 * 1000
    refreshTimerRef.current = setTimeout(() => {
      doRefresh()
    }, refreshMs)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const doRefresh = useCallback(async (): Promise<boolean> => {
    const rt = safeStorage.getItem("cb-refresh-token")
    if (!rt) return false
    try {
      const res = await authApi.refresh(rt)
      setAuthFromTokens(res.accessToken, res.refreshToken, res.expiresIn)
      return true
    } catch {
      clearAuth()
      return false
    }
  }, [setAuthFromTokens, clearAuth])

  // Wire up the API client auth functions
  useEffect(() => {
    setAuthFunctions(
      () => accessToken,
      doRefresh,
    )
  }, [accessToken, doRefresh])

  // On mount, try silent refresh
  useEffect(() => {
    const rt = safeStorage.getItem("cb-refresh-token")
    if (rt) {
      doRefresh().finally(() => setIsLoading(false))
    } else {
      setIsLoading(false)
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    setAuthFromTokens(res.accessToken, res.refreshToken, res.expiresIn)
  }, [setAuthFromTokens])

  const logout = useCallback(async () => {
    if (accessToken) {
      try {
        await authApi.logout(accessToken)
      } catch { /* ignore */ }
    }
    clearAuth()
  }, [accessToken, clearAuth])

  return (
    <AuthContext.Provider value={{
      user,
      role,
      accessToken,
      isAuthenticated: !!accessToken,
      isLoading,
      login,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error("useAuth must be used within AuthProvider")
  return context
}
