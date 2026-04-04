import type { AuthResponse } from "./types"

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost"

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  })
  if (!response.ok) throw new Error("Invalid credentials")
  return response.json()
}

export async function refresh(refreshToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) throw new Error("Refresh failed")
  return response.json()
}

export async function logout(accessToken: string): Promise<void> {
  await fetch(`${API_URL}/api/auth/logout`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` },
  })
}
