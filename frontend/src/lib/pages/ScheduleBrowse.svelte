<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import TermPicker from '../components/TermPicker.svelte'
  import { CURRENT_TERM } from '../terms.js'
  import Avatar from '../components/Avatar.svelte'

  let data = $state(null)
  let gradeLevels = $state([])
  let selectedTerm = $state(CURRENT_TERM)
  $effect(() => {
    api.get('/api/enrollment/classes').then((d) => (data = d)).catch(() => {})
    api.get('/api/enrollment/grade-levels').then((g) => (gradeLevels = g)).catch(() => {})
  })
  // grade-level names for the Class filter: from the catalog's filters, else the grade-levels endpoint
  const gradeNames = $derived(
    (data?.filters?.gradeLevels ?? gradeLevels).map((g) => (typeof g === 'string' ? g : g.name)),
  )

  const TABS = ['Browse Classes', 'My Classes', 'Weekly View', 'Term Plan']

  // filters
  let fSubjects = $state([])
  let fDays = $state([])
  let fTeachers = $state([])
  let fGrades = $state([])
  let fStart = $state('Any')
  let fEnd = $state('Any')
  let sortKey = $state('Subject')

  const TIME_OPTS = ['Any', '8:00 AM', '9:00 AM', '10:00 AM', '11:00 AM', '12:00 PM', '1:00 PM', '2:00 PM', '3:00 PM', '4:00 PM', '5:00 PM']

  function toMin(t) {
    const [hm, mer] = t.split(' ')
    let [h, m] = hm.split(':').map(Number)
    if (mer === 'PM' && h !== 12) h += 12
    if (mer === 'AM' && h === 12) h = 0
    return h * 60 + m
  }
  function toggle(list, v) {
    const i = list.indexOf(v)
    if (i >= 0) list.splice(i, 1)
    else list.push(v)
  }
  function resetFilters() {
    fSubjects = []
    fDays = []
    fTeachers = []
    fGrades = []
    fStart = 'Any'
    fEnd = 'Any'
  }

  const filtered = $derived.by(() => {
    if (!data) return []
    return data.available.filter(
      (c) =>
        (fSubjects.length === 0 || fSubjects.includes(c.name)) &&
        (fDays.length === 0 || c.days.some((d) => fDays.includes(d))) &&
        (fTeachers.length === 0 || fTeachers.includes(c.teacher)) &&
        (fGrades.length === 0 || fGrades.includes(c.gradeLevel)) &&
        (fStart === 'Any' || toMin(c.start) >= toMin(fStart)) &&
        (fEnd === 'Any' || toMin(c.end) <= toMin(fEnd))
    )
  })
  const sorted = $derived.by(() => {
    const arr = [...filtered]
    if (sortKey === 'Teacher') arr.sort((a, b) => a.teacher.localeCompare(b.teacher))
    else if (sortKey === 'Time') arr.sort((a, b) => toMin(a.start) - toMin(b.start))
    else arr.sort((a, b) => a.name.localeCompare(b.name))
    return arr
  })
  const scheduled = $derived(data ? data.available.filter((c) => c.status === 'added') : [])

  async function add(c) {
    try {
      await api.post('/api/enrollment/enroll/' + c.id)
    } catch (e) {
      console.error(e)
    }
    c.status = 'added'
  }
  async function drop(c) {
    try {
      await api.del('/api/enrollment/drop/' + c.id)
    } catch (e) {
      console.error(e)
    }
    c.status = 'available'
  }

  const ICONS = { Mathematics: 'calc', Physics: 'target', Chemistry: 'flask', English: 'book', 'Computer Science': 'video' }
  const SHORT = {
    'AP Pre Calculus': 'AP PreCalc', 'AP Statistics': 'AP Stats', 'AP Computer Science': 'AP CS', 'AP English Language': 'AP Eng Lang',
    'AP English Literature': 'AP Eng Lit', 'Regular Chemistry': 'Reg Chem', 'Hons Chemistry': 'Hons Chem', 'AP Chemistry': 'AP Chem',
    'Hons Biology': 'Hons Bio', 'AP Biology': 'AP Bio', Trigonometry: 'Trig',
  }
  const fmtRange = (s, e) => (s.slice(-2) === e.slice(-2) ? `${s.slice(0, -3)} – ${e}` : `${s} – ${e}`)
  const shortName = (n) => SHORT[n] ?? n
  const shortRange = (s, e) => `${s.split(':')[0]}–${e.split(':')[0]} ${e.slice(-2)}`

  // compact weekly grid
  // current week Mon–Sun
  const MINI_DAYS = (() => {
    const now = new Date()
    const mon = new Date(now)
    mon.setDate(now.getDate() - ((now.getDay() + 6) % 7))
    return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((name, i) => {
      const d = new Date(mon)
      d.setDate(mon.getDate() + i)
      return { name, date: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) }
    })
  })()
  const MINI_HOURS = ['8:00 AM', '9:00 AM', '10:00 AM', '11:00 AM', '12:00 PM', '1:00 PM', '2:00 PM', '3:00 PM', '4:00 PM']
  function miniBlocks(dayName, hour) {
    return scheduled.filter((c) => c.start === hour && c.days.includes(dayName))
  }
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Schedule Classes</h1>
      <p class="page-sub">Plan and manage your classes for the {selectedTerm.name} semester.</p>
    </div>
    <div class="head-right">
      <TermPicker bind:selected={selectedTerm} />
    </div>
  </div>

  <div class="tabbar card">
    {#each TABS as t, i}
      <button class="tab" class:active={i === 0}>{t}</button>
    {/each}
  </div>

  <div class="layout">
    <div class="card filters">
      <div class="f-head">
        <strong>Filters</strong>
        <button class="link" onclick={resetFilters}>Reset</button>
      </div>
      {#if data}
        <div class="f-sec">
          <div class="f-title">Subject</div>
          {#each data.filters.subjects as s}
            <label class="f-check">
              <input type="checkbox" checked={fSubjects.includes(s.name)} onchange={() => toggle(fSubjects, s.name)} />
              <span class="dot" style="background:var(--c-{s.color})"></span>
              {s.name}
            </label>
          {/each}
        </div>
        <div class="f-sec">
          <div class="f-title">Class</div>
          {#each gradeNames as g}
            <label class="f-check">
              <input type="checkbox" checked={fGrades.includes(g)} onchange={() => toggle(fGrades, g)} />
              {g}
            </label>
          {/each}
        </div>
        <div class="f-sec">
          <div class="f-title">Days</div>
          <div class="f-days">
            {#each data.filters.days as d}
              <label class="f-check">
                <input type="checkbox" checked={fDays.includes(d)} onchange={() => toggle(fDays, d)} />
                {d}
              </label>
            {/each}
          </div>
        </div>
        <div class="f-sec">
          <div class="f-title">Time</div>
          <label class="f-sel-lbl">Start
            <select class="f-sel" bind:value={fStart}>
              {#each TIME_OPTS as t}<option>{t}</option>{/each}
            </select>
          </label>
          <label class="f-sel-lbl">End
            <select class="f-sel" bind:value={fEnd}>
              {#each TIME_OPTS as t}<option>{t}</option>{/each}
            </select>
          </label>
        </div>
        <div class="f-sec">
          <div class="f-title">Teacher</div>
          {#each data.filters.teachers as t}
            <label class="f-check">
              <input type="checkbox" checked={fTeachers.includes(t)} onchange={() => toggle(fTeachers, t)} />
              {t}
            </label>
          {/each}
        </div>
      {:else}
        <p class="loading">Loading…</p>
      {/if}
    </div>

    <div class="card avail">
      <div class="toolbar">
        <strong>Available Classes (Fall 2026)</strong>
        <div class="tb-right">
          <span class="count">{sorted.length} classes</span>
          <label class="sort-lbl">Sort by:
            <select class="f-sel sort-sel" bind:value={sortKey}>
              <option>Subject</option>
              <option>Teacher</option>
              <option>Time</option>
            </select>
          </label>
        </div>
      </div>
      {#if data}
        <table class="tbl">
          <thead>
            <tr>
              <th>Subject / Class</th><th>Teacher</th><th>Days</th><th>Time</th><th>Duration</th>
              <th>Dates</th><th>Credits</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {#each sorted as c (c.id)}
              <tr>
                <td>
                  <div class="subj-cell">
                    <span class="chip" style="background:var(--t-{c.color});color:var(--c-{c.color})"><Icon name={ICONS[c.name] ?? 'book'} size={15} /></span>
                    <div>
                      <div class="s-name">{c.name}</div>
                      <div class="s-code">{c.gradeLevel}</div>
                    </div>
                  </div>
                </td>
                <td><div class="teach-cell"><Avatar name={c.teacher} username={c.teacherUsername} size={24} /> {c.teacher}</div></td>
                <td>
                  <div class="days-cell">
                    {#each c.days as d}<span class="pill pill-blue day-pill">{d}</span>{/each}
                  </div>
                </td>
                <td class="nowrap">{fmtRange(c.start, c.end)}</td>
                <td>1 hr</td>
                <td class="nowrap">{c.dates}</td>
                <td class="nowrap">{c.credits} cr</td>
                <td>
                  {#if c.status === 'added'}
                    <button class="btn btn-ghost sm added" disabled><Icon name="check" size={12} /> Added</button>
                  {:else}
                    <button class="btn btn-primary sm" onclick={() => add(c)}>Add</button>
                  {/if}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
        <div class="pager">
          <button class="pg"><Icon name="chevron-left" size={13} /></button>
          <button class="pg on">1</button>
          <button class="pg">2</button>
          <button class="pg">3</button>
          <button class="pg"><Icon name="chevron-right" size={13} /></button>
        </div>
      {:else}
        <p class="loading">Loading classes…</p>
      {/if}
    </div>

    <div class="card sched">
      <div class="toolbar">
        <strong>My Scheduled Classes (Fall 2026)</strong>
        <span class="count">Total: {scheduled.length} classes</span>
      </div>
      <table class="tbl">
        <thead>
          <tr><th>Subject / Class</th><th>Time</th><th>Dates</th><th>Actions</th></tr>
        </thead>
        <tbody>
          {#each scheduled as c (c.id)}
            <tr>
              <td>
                <div class="subj-cell">
                  <span class="chip" style="background:var(--t-{c.color});color:var(--c-{c.color})"><Icon name={ICONS[c.name] ?? 'book'} size={15} /></span>
                  <div>
                    <div class="s-name">{c.name}</div>
                    <div class="s-code">{c.gradeLevel}</div>
                  </div>
                </div>
              </td>
              <td class="nowrap">{fmtRange(c.start, c.end)}</td>
              <td class="nowrap">{c.dates}</td>
              <td><button class="btn btn-danger-outline sm" onclick={() => drop(c)}>Drop</button></td>
            </tr>
          {:else}
            <tr><td colspan="4" class="empty">No classes scheduled yet.</td></tr>
          {/each}
        </tbody>
      </table>
    </div>

    <div class="card wk">
      <div class="toolbar wk-bar">
        <strong>Weekly Schedule (My Classes)</strong>
        <div class="tb-right">
          <button class="nav-btn"><Icon name="chevron-left" size={13} /></button>
          <button class="nav-btn"><Icon name="chevron-right" size={13} /></button>
          <span class="pill pill-blue">Current Week</span>
        </div>
      </div>
      <div class="mini-grid">
        <div class="mc mh"></div>
        {#each MINI_DAYS as d}
          <div class="mc mh">
            <span class="mh-name">{d.name}</span>
            <span class="mh-date">{d.date}</span>
          </div>
        {/each}
        {#each MINI_HOURS as h}
          <div class="mc mtime">{h.replace(':00 ', ' ')}</div>
          {#each MINI_DAYS as d}
            <div class="mc mslot">
              {#each miniBlocks(d.name, h) as c}
                <div class="mblock" style="background:var(--t-{c.color});border-left:2px solid var(--c-{c.color})">
                  <div class="mb-name">{shortName(c.name)}</div>
                  <div class="mb-time">{shortRange(c.start, c.end)}</div>
                </div>
              {/each}
            </div>
          {/each}
        {/each}
      </div>
    </div>
  </div>
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1.1rem; flex-wrap: wrap; }
  .head-right { margin-left: auto; }

  .tabbar { display: flex; gap: 0.25rem; padding: 0.35rem; margin-bottom: 1.1rem; }
  .tab { background: none; border: none; padding: 0.5rem 1rem; border-radius: 8px; font-size: 0.85rem; font-weight: 600; color: var(--text-2); }
  .tab.active { background: var(--t-blue); color: var(--primary); }

  .layout { display: grid; grid-template-columns: 218px 1fr 330px; gap: 1.1rem; align-items: start; }
  .filters { grid-row: 1 / 3; padding-bottom: 0.5rem; }
  .avail { grid-column: 2 / 4; grid-row: 1; }
  .sched { grid-column: 2 / 3; grid-row: 2; }
  .wk { grid-column: 3 / 4; grid-row: 2; }

  .f-head { display: flex; justify-content: space-between; align-items: center; padding: 0.9rem 1rem 0.4rem; }
  .f-sec { padding: 0.55rem 1rem; border-top: 1px solid var(--border); }
  .f-sec:first-of-type { border-top: none; }
  .f-title { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; color: var(--text-3); margin-bottom: 0.4rem; }
  .f-check { display: flex; align-items: center; gap: 0.45rem; font-size: 0.8rem; padding: 0.18rem 0; cursor: pointer; }
  .f-check input { accent-color: var(--primary); margin: 0; }
  .f-days { display: grid; grid-template-columns: 1fr 1fr; }
  .f-sel-lbl { display: flex; flex-direction: column; gap: 0.2rem; font-size: 0.72rem; color: var(--text-2); margin-bottom: 0.45rem; }
  .f-sel { border: 1px solid var(--border); border-radius: 8px; padding: 0.32rem 0.5rem; font-size: 0.78rem; background: #fff; color: var(--text); }
  .dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }

  .toolbar { display: flex; justify-content: space-between; align-items: center; padding: 0.85rem 1.1rem; border-bottom: 1px solid var(--border); flex-wrap: wrap; gap: 0.5rem; }
  .tb-right { display: flex; align-items: center; gap: 0.55rem; }
  .count { font-size: 0.76rem; color: var(--text-2); font-weight: 600; }
  .sort-lbl { font-size: 0.76rem; color: var(--text-2); display: flex; align-items: center; gap: 0.35rem; }
  .sort-sel { padding: 0.28rem 0.45rem; }

  .tbl { width: 100%; border-collapse: collapse; font-size: 0.78rem; }
  .tbl th { text-align: left; font-size: 0.66rem; text-transform: uppercase; letter-spacing: 0.02em; color: var(--text-3); font-weight: 600; padding: 0.55rem 0.65rem; border-bottom: 1px solid var(--border); background: #fafbfd; }
  .tbl td { padding: 0.55rem 0.65rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .tbl tr:last-child td { border-bottom: none; }
  .subj-cell { display: flex; align-items: center; gap: 0.5rem; }
  .chip { width: 30px; height: 30px; border-radius: 8px; display: grid; place-items: center; flex-shrink: 0; }
  .s-name { font-weight: 700; font-size: 0.8rem; white-space: nowrap; }
  .s-code { font-size: 0.68rem; color: var(--text-2); white-space: nowrap; }
  .teach-cell { display: flex; align-items: center; gap: 0.4rem; white-space: nowrap; }
  .days-cell { display: flex; gap: 0.2rem; flex-wrap: wrap; }
  .day-pill { padding: 0.1rem 0.4rem; font-size: 0.62rem; }
  .nowrap { white-space: nowrap; }
  .btn.sm { padding: 0.32rem 0.8rem; font-size: 0.75rem; }
  .added { color: var(--c-green); display: inline-flex; align-items: center; gap: 0.25rem; cursor: default; }
  .empty { color: var(--text-2); text-align: center; padding: 1rem; }
  .loading { padding: 1.25rem; color: var(--text-2); }

  .pager { display: flex; justify-content: center; gap: 0.35rem; padding: 0.8rem; }
  .pg { width: 28px; height: 28px; border: 1px solid var(--border); background: #fff; border-radius: 8px; font-size: 0.78rem; font-weight: 600; color: var(--text-2); display: grid; place-items: center; }
  .pg.on { background: var(--primary); border-color: var(--primary); color: #fff; }

  .wk-bar { gap: 0.3rem; }
  .wk-bar strong { font-size: 0.85rem; }
  .nav-btn { background: #fff; border: 1px solid var(--border); border-radius: 6px; width: 24px; height: 24px; display: grid; place-items: center; color: var(--text-2); }
  .mini-grid { display: grid; grid-template-columns: 44px repeat(7, 1fr); padding: 0.5rem 0.6rem 0.75rem; }
  .mc { border-bottom: 1px solid var(--border); min-height: 30px; }
  .mh { border-bottom: 1px solid var(--border); display: flex; flex-direction: column; align-items: center; padding: 0.25rem 0; min-height: auto; }
  .mh-name { font-size: 0.68rem; font-weight: 700; }
  .mh-date { font-size: 0.6rem; color: var(--text-3); }
  .mtime { font-size: 0.58rem; color: var(--text-3); font-weight: 600; text-align: right; padding: 2px 5px 0 0; }
  .mslot { padding: 1px; border-left: 1px solid var(--border); }
  .mblock { border-radius: 4px; padding: 1px 3px; height: 100%; overflow: hidden; }
  .mb-name { font-size: 0.58rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .mb-time { font-size: 0.54rem; color: var(--text-2); white-space: nowrap; }
</style>
