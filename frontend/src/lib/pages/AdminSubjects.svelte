<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import Avatar from '../components/Avatar.svelte'

  const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  const COLORS = ['blue', 'purple', 'green', 'orange', 'red', 'teal', 'amber']
  // 1-hour slots, 8:00 AM … 4:00 PM
  const SLOTS = Array.from({ length: 9 }, (_, i) => `${String(8 + i).padStart(2, '0')}:00`)

  let subjects = $state(null)
  let teachers = $state([])
  let terms = $state([])
  let gradeLevels = $state([]) // from GET /api/enrollment/grade-levels only — no built-in list
  let gradeError = $state('') // the service's message when that call failed (e.g. Not Implemented)
  let gradeFilter = $state('') // grade level id, '' = all
  let error = $state('')
  let notice = $state('')
  let search = $state('')
  let busy = $state(false)
  let form = $state(null)

  async function load() {
    error = ''
    // each part loads on its own: a 501 from one endpoint must not hide what the others returned
    const [s, t, tm, gl] = await Promise.allSettled([
      api.get('/api/enrollment/subjects'),
      api.get('/api/users/teachers'),
      api.get('/api/schedule/terms'),
      api.get('/api/enrollment/grade-levels'),
    ])
    if (s.status === 'fulfilled') subjects = s.value
    else { error = s.reason?.message ?? 'subjects unavailable'; subjects = [] }
    teachers = t.status === 'fulfilled' ? t.value : []
    const sortedTerms = tm.status === 'fulfilled' ? tm.value : []
    terms = sortedTerms.slice().sort((a, b) => (a.start ?? a.startDate ?? '').localeCompare(b.start ?? b.startDate ?? ''))
    if (gl.status === 'fulfilled') { gradeLevels = Array.isArray(gl.value) ? gl.value : []; gradeError = '' }
    else { gradeLevels = []; gradeError = gl.reason?.message ?? 'grade levels unavailable' }
  }
  load()

  const to12 = (hhmm) => {
    if (!hhmm) return ''
    const [h, m] = hhmm.split(':').map(Number)
    const mer = h >= 12 ? 'PM' : 'AM'
    return `${((h + 11) % 12) + 1}:${String(m).padStart(2, '0')} ${mer}`
  }
  const plusHour = (hhmm) => {
    if (!hhmm) return ''
    const [h, m] = hhmm.split(':').map(Number)
    return `${String(h + 1).padStart(2, '0')}:${String(m).padStart(2, '0')}`
  }
  // subjects may carry startTime as "HH:mm" or "HH:mm:ss"
  const hm = (t) => (t ? t.slice(0, 5) : '')
  const termName = (id) => terms.find((t) => t.id === id)?.name ?? id ?? '—'
  const teacherName = (s) => s.teacher || teachers.find((t) => t.username === s.teacherUsername)?.displayName || s.teacherUsername || '—'
  const gradeName = (s) => s.gradeLevelName || gradeLevels.find((g) => g.id === s.gradeLevel || g.name === s.gradeLevel)?.name || s.gradeLevel || '—'

  const filtered = $derived.by(() => {
    if (!subjects) return []
    const q = search.trim().toLowerCase()
    return subjects
      .filter((s) => !gradeFilter || s.gradeLevel === gradeFilter || s.gradeLevelName === gradeLevels.find((g) => g.id === gradeFilter)?.name)
      .filter((s) => !q || `${s.name} ${gradeName(s)} ${teacherName(s)} ${termName(s.termId)}`.toLowerCase().includes(q))
  })

  function openCreate() {
    const cur = terms.find((t) => t.current) ?? terms[0]
    form = { mode: 'create', id: '', name: '', gradeLevel: gradeLevels[0]?.id ?? '', teacherUsername: teachers[0]?.username ?? '', termId: cur?.id ?? '', days: [], startTime: '09:00', credits: 3, color: 'blue' }
  }
  function openEdit(s) {
    form = {
      mode: 'edit',
      id: s.id,
      name: s.name ?? '',
      gradeLevel: gradeLevels.find((g) => g.id === s.gradeLevel || g.name === s.gradeLevel)?.id ?? s.gradeLevel ?? '',
      teacherUsername: s.teacherUsername ?? '',
      termId: s.termId ?? '',
      days: [...(s.days ?? [])],
      startTime: hm(s.startTime) || '09:00',
      credits: s.credits ?? 3,
      color: s.color ?? 'blue',
    }
  }
  function toggleDay(d) {
    form.days = form.days.includes(d) ? form.days.filter((x) => x !== d) : DAYS.filter((x) => x === d || form.days.includes(x))
  }

  async function run(label, fn) {
    busy = true
    error = ''
    notice = ''
    try {
      await fn()
      notice = label
      await load()
    } catch (e) {
      error = e.message
    } finally {
      busy = false
    }
  }

  async function save(e) {
    e.preventDefault()
    const f = form
    if (!f.name || !f.teacherUsername || !f.termId || !f.gradeLevel) {
      error = 'Name, grade level, teacher and term are required.'
      return
    }
    if (f.days.length === 0) {
      error = 'Pick at least one meeting day.'
      return
    }
    // Ask 46: the catalogue entry belongs to enrollment-service, the meeting slot to schedule-service.
    // The page writes each owner directly; neither service copies the other's data.
    const body = { name: f.name, gradeLevel: f.gradeLevel, teacherUsername: f.teacherUsername, termId: f.termId, credits: Number(f.credits), color: f.color }
    const slot = { termId: f.termId, days: DAYS.filter((d) => f.days.includes(d)), startTime: f.startTime }
    if (f.mode === 'create') {
      await run(`Subject ${f.name} created`, async () => {
        const created = await api.post('/api/enrollment/subjects', body)
        await api.put(`/api/schedule/sessions/by-course/${created.id}`, slot)
        form = null
      })
    } else {
      await run(`Subject ${f.name} updated`, async () => {
        await api.put(`/api/enrollment/subjects/${f.id}`, body)
        await api.put(`/api/schedule/sessions/by-course/${f.id}`, slot)
        form = null
      })
    }
  }

  function remove(s) {
    if (!confirm(`Delete subject ${s.name} (${gradeName(s)})? Enrolments and the schedule entry are removed too.`)) return
    run(`Subject ${s.name} deleted`, async () => {
      await api.del(`/api/enrollment/subjects/${s.id}`)
      await api.del(`/api/schedule/sessions/by-course/${s.id}`).catch(() => {}) // no slot is fine
    })
  }
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Subjects</h1>
      <p class="page-sub">Subjects offered per quarter to a grade level: teacher, meeting days and a one-hour time slot. Saving also updates the timetable.</p>
    </div>
    <button class="btn btn-primary new-btn" onclick={openCreate}><Icon name="plus" size={14} /> New subject</button>
  </div>

  {#if notice}<div class="banner ok">{notice}</div>{/if}
  {#if error}<div class="banner err">{error}</div>{/if}

  <div class="card toolbar">
    <div class="search">
      <Icon name="search" size={15} />
      <input placeholder="Search by name, grade level, teacher or term…" bind:value={search} />
    </div>
    <span class="count">{filtered.length} of {subjects?.length ?? 0}</span>
    <div class="grades" role="group" aria-label="Filter by grade level">
      <button class="chip" class:on={gradeFilter === ''} onclick={() => (gradeFilter = '')}>All grades</button>
      {#each gradeLevels as g (g.id)}
        <button class="chip" class:on={gradeFilter === g.id} onclick={() => (gradeFilter = gradeFilter === g.id ? '' : g.id)}>{g.name}{g.gradeNumber ? ` · grade ${g.gradeNumber}` : ''}</button>
      {/each}
      <span class="src" class:err={gradeError}>{gradeError ? gradeError : gradeLevels.length ? 'grade levels from GET /api/enrollment/grade-levels' : ''}</span>
    </div>
  </div>

  <div class="card list">
    {#if subjects === null}
      <p class="loading">Loading subjects…</p>
    {:else if filtered.length === 0}
      <p class="loading">No subjects match.</p>
    {:else}
      <table class="tbl">
        <thead>
          <tr><th>Subject</th><th>Teacher</th><th>Term</th><th>Days</th><th>Time</th><th class="right">Actions</th></tr>
        </thead>
        <tbody>
          {#each filtered as s (s.id)}
            <tr>
              <td>
                <div class="subj">
                  <span class="dot" style="background:var(--c-{s.color ?? 'blue'})"></span>
                  <div>
                    <div class="name">{s.name}</div>
                    <div class="code">{gradeName(s)}</div>
                  </div>
                </div>
              </td>
              <td><div class="who"><Avatar name={teacherName(s)} username={s.teacherUsername} size={26} /> {teacherName(s)}</div></td>
              <td class="nowrap">{termName(s.termId)}</td>
              <td><div class="days">{#each s.days ?? [] as d}<span class="pill pill-blue day-pill">{d}</span>{/each}</div></td>
              <td class="nowrap">{to12(hm(s.startTime))} – {to12(hm(s.endTime) || plusHour(hm(s.startTime)))}</td>
              <td class="right actions">
                <button class="btn btn-ghost sm" onclick={() => openEdit(s)} disabled={busy}><Icon name="edit" size={13} /> Edit</button>
                <button class="btn btn-danger-outline sm" onclick={() => remove(s)} disabled={busy}>Delete</button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>

{#if form}
  <div class="backdrop" onclick={() => (form = null)} role="presentation"></div>
  <form class="modal card" onsubmit={save}>
    <h2>{form.mode === 'create' ? 'New subject' : `Edit ${form.name}`}</h2>
    <div class="fields">
      <label>Name <input bind:value={form.name} placeholder="e.g. AP Biology" required /></label>
      <label>Grade level
        <select bind:value={form.gradeLevel} required>
          {#each gradeLevels as g (g.id)}<option value={g.id}>{g.name}{g.gradeNumber ? ` (grade ${g.gradeNumber})` : ''}</option>{/each}
        </select>
      </label>
      <label>Teacher
        <select bind:value={form.teacherUsername} required>
          {#if teachers.length === 0}<option value="">No teachers found</option>{/if}
          {#each teachers as t (t.username)}<option value={t.username}>{t.displayName}</option>{/each}
        </select>
      </label>
      <label>Term (quarter)
        <select bind:value={form.termId} required>
          {#each terms as t (t.id)}<option value={t.id}>{t.name}{t.current ? ' · current' : ''}</option>{/each}
        </select>
      </label>
      <div class="wide">
        <span class="lbl">Meeting days</span>
        <div class="daypick">
          {#each DAYS as d}
            <label class="daychk" class:on={form.days.includes(d)}>
              <input type="checkbox" checked={form.days.includes(d)} onchange={() => toggleDay(d)} /> {d}
            </label>
          {/each}
        </div>
      </div>
      <label>Start time
        <select bind:value={form.startTime}>
          {#each SLOTS as t}<option value={t}>{to12(t)}</option>{/each}
        </select>
      </label>
      <div>
        <span class="lbl">End time</span>
        <div class="static">{to12(plusHour(form.startTime))} <span class="muted">(1-hour class)</span></div>
      </div>
      <label>Color
        <select bind:value={form.color}>
          {#each COLORS as c}<option value={c}>{c}</option>{/each}
        </select>
      </label>
      <label>Credits <input type="number" min="1" max="6" bind:value={form.credits} /></label>
    </div>
    <div class="modal-actions">
      <button type="button" class="btn btn-ghost" onclick={() => (form = null)} disabled={busy}>Cancel</button>
      <button type="submit" class="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : form.mode === 'create' ? 'Create subject' : 'Save changes'}</button>
    </div>
  </form>
{/if}

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
  .new-btn { display: inline-flex; align-items: center; gap: 0.4rem; margin-left: auto; }
  .banner { padding: 0.6rem 0.9rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 0.8rem; }
  .banner.ok { background: var(--t-green); color: #166534; }
  .banner.err { background: var(--t-red); color: #991b1b; }

  .toolbar { display: flex; align-items: center; gap: 0.8rem; padding: 0.6rem 0.9rem; flex-wrap: wrap; margin-bottom: 1rem; }
  .search { display: flex; align-items: center; gap: 0.5rem; color: var(--text-3); background: var(--bg); border: 1px solid var(--border); border-radius: 9px; padding: 0.4rem 0.7rem; flex: 1 1 260px; }
  .search input { border: none; outline: none; background: none; width: 100%; font-size: 0.85rem; color: var(--text); }
  .count { color: var(--text-3); font-size: 0.78rem; margin-left: auto; }
  .grades { flex-basis: 100%; display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap; padding-top: 0.2rem; }
  .chip { border: 1px solid var(--border); background: var(--bg); color: var(--text-2); border-radius: 999px; padding: 0.25rem 0.7rem; font-size: 0.78rem; cursor: pointer; }
  .chip.on { background: var(--c-blue); border-color: var(--c-blue); color: #fff; }
  .src { color: var(--text-3); font-size: 0.72rem; font-family: ui-monospace, Menlo, monospace; margin-left: auto; }
  .src.err { color: #92400e; }

  .list { padding: 0; overflow-x: auto; }
  .loading { padding: 1.25rem; color: var(--text-2); }
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
  .tbl th { text-align: left; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-3); padding: 0.75rem 1rem; border-bottom: 1px solid var(--border); }
  .tbl td { padding: 0.65rem 1rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .tbl tr:last-child td { border-bottom: none; }
  .right { text-align: right; }
  .nowrap { white-space: nowrap; }
  .muted { color: var(--text-2); }
  .subj { display: flex; align-items: center; gap: 0.6rem; }
  .dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
  .name { font-weight: 600; }
  .code { color: var(--text-3); font-size: 0.75rem; }
  .who { display: flex; align-items: center; gap: 0.5rem; white-space: nowrap; }
  .days { display: flex; gap: 0.25rem; flex-wrap: wrap; }
  .day-pill { padding: 0.12rem 0.45rem; font-size: 0.66rem; }
  .actions { white-space: nowrap; display: flex; gap: 0.35rem; justify-content: flex-end; }
  .btn.sm { padding: 0.32rem 0.6rem; font-size: 0.76rem; display: inline-flex; align-items: center; gap: 0.3rem; }
  .btn:disabled { opacity: 0.5; cursor: not-allowed; }

  .backdrop { position: fixed; inset: 0; background: rgba(15, 30, 61, 0.35); z-index: 40; }
  .modal { position: fixed; z-index: 41; top: 50%; left: 50%; transform: translate(-50%, -50%); width: min(560px, 94vw); max-height: 92vh; overflow-y: auto; padding: 1.3rem 1.4rem; box-shadow: 0 24px 60px rgba(15, 30, 61, 0.25); }
  .modal h2 { font-size: 1.1rem; margin-bottom: 0.9rem; }
  .fields { display: grid; grid-template-columns: 1fr 1fr; gap: 0.7rem 0.9rem; }
  .fields label, .fields .lbl { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.78rem; font-weight: 600; color: var(--text-2); }
  .fields .wide { grid-column: 1 / -1; }
  .fields input, .fields select { border: 1px solid var(--border); border-radius: 9px; padding: 0.5rem 0.7rem; font: inherit; font-size: 0.86rem; color: var(--text); background: #fff; width: 100%; }
  .fields input:focus, .fields select:focus { outline: none; border-color: var(--primary); }
  .static { padding: 0.5rem 0; font-size: 0.86rem; color: var(--text); }
  .daypick { display: flex; gap: 0.4rem; flex-wrap: wrap; margin-top: 0.3rem; }
  .daychk { display: inline-flex !important; flex-direction: row !important; align-items: center; gap: 0.35rem; border: 1px solid var(--border); border-radius: 999px; padding: 0.3rem 0.75rem; cursor: pointer; font-weight: 600; font-size: 0.8rem; color: var(--text-2); }
  .daychk.on { background: var(--t-blue); color: var(--primary); border-color: var(--primary); }
  .daychk input { width: auto; margin: 0; }
  .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1.1rem; }
  @media (max-width: 600px) { .fields { grid-template-columns: 1fr; } }
</style>
