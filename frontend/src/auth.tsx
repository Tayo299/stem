import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, type User } from './api'

type AuthValue = { token: string | null; user: User | null; loading: boolean; signIn: (email: string, password: string) => Promise<User>; refreshUser: () => Promise<User>; signOut: () => void }
const AuthContext = createContext<AuthValue | null>(null)
const TOKEN_KEY = 'hssc_access_token'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(Boolean(token))

  useEffect(() => {
    if (!token) { setUser(null); setLoading(false); return }
    let active = true
    api<User>('/me', {}, token).then(profile => {
      if (active) setUser(profile)
    }).catch(() => {
      if (!active) return
      sessionStorage.removeItem(TOKEN_KEY)
      setToken(null)
      setUser(null)
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [token])

  const value = useMemo<AuthValue>(() => ({
    token, user, loading,
    async refreshUser() {
      if (!token) throw new Error('Sign in before refreshing your account.')
      const profile = await api<User>('/me', {}, token)
      setUser(profile)
      return profile
    },
    async signIn(email, password) {
      const result = await api<{ accessToken: string; role: User['role'] }>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      const profile = await api<User>('/me', {}, result.accessToken)
      sessionStorage.setItem(TOKEN_KEY, result.accessToken)
      setUser(profile)
      setToken(result.accessToken)
      return profile
    },
    signOut() { sessionStorage.removeItem(TOKEN_KEY); setToken(null); setUser(null) },
  }), [token, user, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
