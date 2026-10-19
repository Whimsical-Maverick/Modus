import axios, { type InternalAxiosRequestConfig } from 'axios'

type AccessTokenResponse = { accessToken: string }
type RetryConfig = InternalAxiosRequestConfig & { _retried?: boolean }

const raw = axios.create({ baseURL: '/api/v1', withCredentials: true })
export const api = axios.create({ baseURL: '/api/v1', withCredentials: true })

let accessToken: string | null = null
let refreshInFlight: Promise<string> | null = null
const listeners = new Set<(token: string | null) => void>()

function setAccessToken(token: string | null) {
  accessToken = token
  listeners.forEach((listener) => listener(token))
}

export function getAccessToken() {
  return accessToken
}

export function subscribeToAuth(listener: (token: string | null) => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

export function refreshAccessToken(): Promise<string> {
  if (!refreshInFlight) {
    refreshInFlight = raw.post<AccessTokenResponse>('/auth/refresh')
      .then(({ data }) => {
        setAccessToken(data.accessToken)
        return data.accessToken
      })
      .catch((error: unknown) => {
        setAccessToken(null)
        throw error
      })
      .finally(() => { refreshInFlight = null })
  }
  return refreshInFlight
}

export async function login(email: string, password: string) {
  const { data } = await raw.post<AccessTokenResponse>('/auth/login', { email, password })
  setAccessToken(data.accessToken)
}

export async function register(username: string, email: string, password: string) {
  await raw.post('/auth/register', { username, email, password })
}

export async function logout() {
  await raw.post('/auth/logout')
  // Do not show a successful logout when the server could not revoke the cookie.
  setAccessToken(null)
}

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

api.interceptors.response.use(undefined, async (error: unknown) => {
  if (!axios.isAxiosError(error)) return Promise.reject(error)
  const request = error.config as RetryConfig | undefined
  if (error.response?.status !== 401 || !request || request._retried) {
    return Promise.reject(error)
  }
  request._retried = true
  try {
    const token = await refreshAccessToken()
    request.headers.Authorization = `Bearer ${token}`
    return api(request)
  } catch {
    return Promise.reject(error)
  }
})

export function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const detail = (error.response?.data as { detail?: string } | undefined)?.detail
    return detail || (error.response ? `Request failed (${error.response.status})` : 'Cannot reach the server')
  }
  return error instanceof Error ? error.message : 'Something went wrong'
}
