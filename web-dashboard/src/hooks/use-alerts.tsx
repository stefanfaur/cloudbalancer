import { createContext, useContext, useState, useCallback, type ReactNode } from "react"

interface Alert {
  severity: string
  message: string
  timestamp: string
}

interface AlertsState {
  alerts: Alert[]
  addAlert: (alert: Alert) => void
  dismiss: (index: number) => void
  clearAll: () => void
}

const AlertsContext = createContext<AlertsState | null>(null)

const MAX_ALERTS = 50

export function AlertsProvider({ children }: { children: ReactNode }) {
  const [alerts, setAlerts] = useState<Alert[]>([])

  const addAlert = useCallback((alert: Alert) => {
    setAlerts((prev) => [alert, ...prev].slice(0, MAX_ALERTS))
  }, [])

  const dismiss = useCallback((index: number) => {
    setAlerts((prev) => prev.filter((_, i) => i !== index))
  }, [])

  const clearAll = useCallback(() => setAlerts([]), [])

  return (
    <AlertsContext.Provider value={{ alerts, addAlert, dismiss, clearAll }}>
      {children}
    </AlertsContext.Provider>
  )
}

export function useAlerts() {
  const context = useContext(AlertsContext)
  if (!context) throw new Error("useAlerts must be used within AlertsProvider")
  return context
}
