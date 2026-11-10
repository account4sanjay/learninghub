// Global backend-readiness state. During stack cold start the gateway needs
// ~a minute before Eureka registrations propagate; APIs return 502/503 (or
// the connection fails) until then. Any such failure flips `starting` on,
// an overlay explains it, and we poll until the backend answers — then
// reload so every page refetches its data.
export const server = $state({
  starting: false,
  attempts: 0,
})

let polling = false

export function markServerDown() {
  if (!server.starting) {
    server.starting = true
    pollUntilReady()
  }
}

async function probe() {
  try {
    // unauthenticated probe through gateway -> eureka -> service:
    // 401 means the whole chain is up (request reached service security);
    // 5xx / network failure means still starting
    const res = await fetch('/api/users/me', { cache: 'no-store' })
    return res.status < 500
  } catch {
    return false
  }
}

async function pollUntilReady() {
  if (polling) return
  polling = true
  server.attempts = 0
  // eslint-disable-next-line no-constant-condition
  while (true) {
    server.attempts += 1
    if (await probe()) {
      window.location.reload()
      return
    }
    await new Promise((r) => setTimeout(r, 5000))
  }
}
