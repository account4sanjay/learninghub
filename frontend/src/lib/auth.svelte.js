// Auth store — embedded login form using Keycloak's direct-access grant
// (enabled on the learninghub-frontend client). Tokens live in sessionStorage;
// roles/identity are read from the JWT.
const KC_BASE = (import.meta.env.VITE_KEYCLOAK_URL || window.location.origin + '/auth') + '/realms/learninghub/protocol/openid-connect'

export const auth = $state({
  user: null, // { username, name, firstName, roles }
  token: null,
  refreshToken: null,
  error: null,
  loading: false,
})

function parseJwt(token) {
  try {
    return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return {}
  }
}

function applyTokens(data) {
  const claims = parseJwt(data.access_token)
  auth.token = data.access_token
  auth.refreshToken = data.refresh_token
  auth.user = {
    username: claims.preferred_username,
    firstName: claims.given_name || claims.preferred_username,
    name: [claims.given_name, claims.family_name].filter(Boolean).join(' ') || claims.preferred_username,
    roles: (claims.realm_access?.roles || []).filter((r) => ['admin', 'teacher', 'student'].includes(r)),
  }
  sessionStorage.setItem('lh.tokens', JSON.stringify({ access_token: data.access_token, refresh_token: data.refresh_token }))
}

async function tokenRequest(body) {
  let res
  try {
    res = await fetch(`${KC_BASE}/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ client_id: 'learninghub-frontend', ...body }),
    })
  } catch (e) {
    const { markServerDown } = await import('./server.svelte.js')
    markServerDown()
    throw new Error('Server is starting, please wait…')
  }
  if (res.status >= 500) {
    const { markServerDown } = await import('./server.svelte.js')
    markServerDown()
    throw new Error('Server is starting, please wait…')
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error_description || 'Sign in failed')
  }
  return res.json()
}

export async function login(username, password) {
  auth.loading = true
  auth.error = null
  try {
    applyTokens(await tokenRequest({ grant_type: 'password', username, password, scope: 'openid' }))
  } catch (e) {
    auth.error = e.message === 'Invalid user credentials' ? 'Invalid username or password' : e.message
    throw e
  } finally {
    auth.loading = false
  }
}

export async function refresh() {
  if (!auth.refreshToken) return
  try {
    applyTokens(await tokenRequest({ grant_type: 'refresh_token', refresh_token: auth.refreshToken }))
  } catch {
    logout()
  }
}

/** admin > teacher > student, from the JWT realm roles */
export function primaryRole() {
  const r = auth.user?.roles ?? []
  return r.includes('admin') ? 'admin' : r.includes('teacher') ? 'teacher' : 'student'
}

export function logout() {
  auth.user = null
  auth.token = null
  auth.refreshToken = null
  sessionStorage.removeItem('lh.tokens')
}

export async function restoreSession() {
  const saved = sessionStorage.getItem('lh.tokens')
  if (!saved) return
  const { refresh_token } = JSON.parse(saved)
  auth.refreshToken = refresh_token
  await refresh()
}

// keep the access token fresh while logged in
setInterval(() => {
  if (auth.refreshToken) refresh()
}, 60_000)
