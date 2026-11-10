// Minimal hash router: "#/assessments/unit2-quiz/take" -> segments
export const router = $state({ hash: window.location.hash || '#/dashboard' })

window.addEventListener('hashchange', () => {
  router.hash = window.location.hash || '#/dashboard'
})

export function navigate(hash) {
  window.location.hash = hash
}

/** Path part of the hash without a leading "#/" and without any "?query" */
export function path() {
  return router.hash.replace(/^#\/?/, '').split('?')[0]
}

export function segments() {
  return path().split('/').filter(Boolean)
}

/** Query part of the hash ("#/admin/users?role=teacher" → URLSearchParams{role=teacher}) */
export function query() {
  const i = router.hash.indexOf('?')
  return new URLSearchParams(i === -1 ? '' : router.hash.slice(i + 1))
}
