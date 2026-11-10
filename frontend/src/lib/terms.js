// Academic terms shown by the TermPicker. Only Fall 2026 has data behind it
// today; the other entries let the picker behave like a real dropdown.
export const TERMS = [
  { id: 'spring-2026', name: 'Spring 2026', start: 'Jan 12, 2026', end: 'May 15, 2026' },
  { id: 'fall-2026', name: 'Fall 2026', start: 'Aug 24, 2026', end: 'Dec 18, 2026', current: true },
  { id: 'spring-2027', name: 'Spring 2027', start: 'Jan 11, 2027', end: 'May 14, 2027' },
]

export const CURRENT_TERM = TERMS.find((t) => t.current)
