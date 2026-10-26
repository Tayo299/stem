export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function api<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(options.headers)
  if (!(options.body instanceof FormData) && options.body !== undefined) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, { ...options, headers })
  } catch {
    throw new ApiError('Unable to reach the HSSC server. Check your connection and try again.', 0)
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => null) as { message?: string; detail?: string; fields?: Record<string, string> } | null
    const message = payload?.message || payload?.detail || `Request failed (${response.status})`
    const fieldDetails = payload?.fields ? Object.entries(payload.fields).map(([field, reason]) => `${field}: ${reason}`).join('; ') : ''
    throw new ApiError(fieldDetails ? `${message} — ${fieldDetails}` : message, response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function downloadDocument(id: string, token: string): Promise<Blob> {
  let response: Response
  try {
    response = await fetch(`${API_URL}/documents/${id}`, { headers: { Authorization: `Bearer ${token}` } })
  } catch {
    throw new ApiError('Unable to reach the HSSC server. Check your connection and try again.', 0)
  }
  if (!response.ok) throw new ApiError(`Document download failed (${response.status})`, response.status)
  return response.blob()
}

export type District = { id: string; code: string; name: string; zones: { id: string; name: string }[] }
export type User = { id: string; email: string; role: 'ADMIN' | 'SCHOOL' | 'STUDENT'; status?: string; name?: string; studentName?: string; studentId?: string; schoolId?: string; schoolName?: string }
