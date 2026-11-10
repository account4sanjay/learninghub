import { auth, logout } from './auth.svelte.js'
import { markServerDown } from './server.svelte.js'

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(path, {
      ...options,
      headers: Object.fromEntries(
        Object.entries({
          'Content-Type': 'application/json',
          ...(auth.token ? { Authorization: `Bearer ${auth.token}` } : {}),
          ...options.headers,
        }).filter(([, v]) => v !== undefined),
      ),
    })
  } catch (e) {
    // network-level failure: edge/gateway not reachable yet
    markServerDown()
    throw new Error('Server unavailable')
  }
  if (res.status === 501) {
    // stub build: the service exists but is not implemented yet — surface it, never treat it as an outage
    const err = await res.json().catch(() => ({}))
    const msg = err.message || `Not Implemented: ${options.method || 'GET'} ${path}`
    window.dispatchEvent(new CustomEvent('lh:notimplemented', { detail: { ...err, message: msg } }))
    throw new Error(msg)
  }
  if (res.status >= 500) {
    // 503/504: service not registered/ready yet; 500/502 is also what the
    // gateway returns when a registered instance refuses connections
    markServerDown()
    throw new Error('Server starting')
  }
  if (res.status === 401) {
    logout()
    throw new Error('Session expired')
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || err.error || `${res.status} ${res.statusText}`)
  }
  // stub build: an implemented endpoint names its service and owner in a response header — show it on the page
  const by = res.headers.get('x-implemented-by')
  if (by) window.dispatchEvent(new CustomEvent('lh:implemented', { detail: { endpoint: `${options.method || 'GET'} ${path.split('?')[0]}`, by } }))
  return res.status === 204 ? null : res.json()
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: (path, body) => request(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  del: (path) => request(path, { method: 'DELETE' }),
  // multipart upload: no JSON content-type so the browser sets the boundary
  upload: (path, formData, method = 'POST') => request(path, { method, body: formData, headers: { 'Content-Type': undefined } }),
}
