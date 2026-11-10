<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'
  import { primaryRole } from '../auth.svelte.js'
  import Icon from '../components/Icon.svelte'
  import TermPicker from '../components/TermPicker.svelte'
  import { CURRENT_TERM } from '../terms.js'
  import Avatar from '../components/Avatar.svelte'
  import MiniCalendar from '../components/MiniCalendar.svelte'

  let week = $state(null)
  let term = $state(null)
  let selectedTerm = $state(CURRENT_TERM)
  let enrolled = $state(null)
  const role = $derived(primaryRole())
  // The Schedule opens on List View for every role; admins additionally have no Weekly View
  let activeTab = $state('list')
  const tabs = $derived(TABS.filter((t) => !(role === 'admin' && t.id === 'weekly')))
  const isStudent = $derived(role === 'student')
  const pageTitle = $derived(role === 'admin' ? 'All Schedule' : 'My Schedule')
  const tableTitle = $derived(role === 'admin' ? 'All Classes' : role === 'teacher' ? 'Classes I Teach' : 'My Enrolled Classes')
  const STATUS_PILL = { Enrolled: 'pill-green', Teaching: 'pill-purple', Active: 'pill-blue' }
  const isLastDay = (d) => week && d === week.days[week.days.length - 1]

  const TABS = [
    { id: 'weekly', label: 'Weekly View' },
    { id: 'list', label: 'List View' },
    { id: 'calendar', label: 'Calendar View' },
    { id: 'term', label: 'Term View' },
  ]

  $effect(() => {
    api.get('/api/schedule/week').then((d) => (week = d)).catch(() => {})
    api.get('/api/schedule/term').then((d) => (term = d)).catch(() => {})
    api.get('/api/enrollment/my-classes').then((d) => (enrolled = d)).catch(() => {})
  })

  function fmtRange(s, e) {
    return s.slice(-2) === e.slice(-2) ? `${s.slice(0, -3)} – ${e}` : `${s} – ${e}`
  }
  function blocksAt(dayName, hour) {
    return week ? week.blocks.filter((b) => b.start === hour && b.days.includes(dayName)) : []
  }


  async function drop(id) {
    try {
      await api.del('/api/enrollment/drop/' + id)
    } catch (e) {
      console.error(e)
    }
    enrolled.classes = enrolled.classes.filter((c) => c.id !== id)
  }

  // Month grid (calendar view) from /api/schedule/month — navigable by month
  const DOW = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  const now = new Date()
  let calYear = $state(now.getFullYear())
  let calMonth = $state(now.getMonth() + 1)
  let month = $state(null)
  $effect(() => {
    const y = calYear, m = calMonth
    api.get(`/api/schedule/month?year=${y}&month=${m}`).then((d) => (month = d)).catch(() => {})
  })
  function shiftMonth(delta) {
    const d = new Date(calYear, calMonth - 1 + delta, 1)
    calYear = d.getFullYear()
    calMonth = d.getMonth() + 1
  }
  function goToday() {
    calYear = now.getFullYear()
    calMonth = now.getMonth() + 1
  }
  const calCells = $derived(
    month ? [...Array(month.firstDow).fill(null), ...Array.from({ length: month.days }, (_, i) => i + 1)] : [],
  )
  function classesOn(day) {
    return month ? month.classes.filter((c) => c.day === day) : []
  }
  const codeOf = (subject) => enrolled?.classes.find((c) => c.name === subject)?.gradeLevel ?? ''
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">{pageTitle}</h1>
      <p class="page-sub">
        {#if role === 'admin'}Every class across the school for the {selectedTerm.name} term.
        {:else if role === 'teacher'}The classes you teach in the {selectedTerm.name} term.
        {:else}View and manage your classes for the {selectedTerm.name} term. Manage and plan your schedule.{/if}
      </p>
    </div>
    <div class="head-right">
      <TermPicker bind:selected={selectedTerm} />
    </div>
  </div>

  <div class="tabbar card">
    {#each tabs as t}
      <button class="tab" class:active={activeTab === t.id} onclick={() => (activeTab = t.id)}>{t.label}</button>
    {/each}
  </div>

  {#if activeTab === 'weekly'}
    <div class="main-grid">
      <div class="card week-card">
        <div class="toolbar">
          <div class="tb-left">
            <button class="btn btn-ghost sm">Today</button>
            <button class="nav-btn"><Icon name="chevron-left" size={15} /></button>
            <button class="nav-btn"><Icon name="chevron-right" size={15} /></button>
            <strong class="week-label">{week?.weekLabel ?? ''}</strong>
          </div>
        </div>
        {#if week}
          <div class="tgrid" style="--days:{week.days.length}">
            <div class="tcell thead time-col">Time</div>
            {#each week.days as d}
              <div class="tcell thead day-head" class:last={isLastDay(d)} class:today={week.miniCalendar && d.date.endsWith(' ' + week.miniCalendar.highlight)}>
                <span class="dh-name">{d.name}</span>
                <span class="dh-date">{d.date}</span>
              </div>
            {/each}
            {#each week.hours as h}
              <div class="tcell time-col hour-lbl">{h.replace(':00', '')}</div>
              {#each week.days as d}
                {@const bs = blocksAt(d.name, h)}
                <div class="tcell slot" class:last={isLastDay(d)} class:multi={bs.length > 1}>
                  {#if bs.length > 1}
                    {#each bs as b}
                      <div class="chip" style="background:var(--t-{b.color});border-left:3px solid var(--c-{b.color})" title="{b.subject} · {b.gradeLevel ?? ''} · {fmtRange(b.start, b.end)} · {b.teacher}">
                        <span class="c-subj">{b.subject}</span>
                        {#if b.gradeLevel}<span class="c-grade">{b.gradeLevel}</span>{/if}
                      </div>
                    {/each}
                  {:else}
                    {#each bs as b}
                      <div class="block" style="background:var(--t-{b.color});border-left:3px solid var(--c-{b.color})">
                        <div class="b-subj">{b.subject}</div>
                        <div class="b-time">{fmtRange(b.start, b.end)}</div>
                        <div class="b-meta"><Icon name="profile" size={10} /> {b.teacher}</div>
                      </div>
                    {/each}
                  {/if}
                </div>
              {/each}
            {/each}
          </div>
        {:else}
          <p class="loading">Loading schedule…</p>
        {/if}
      </div>

      <div class="rail">
        {#if week}
          <MiniCalendar {...week.miniCalendar} />
        {/if}
      </div>
    </div>
  {:else if activeTab === 'term'}
    <div class="main-grid">
      <div class="card term-sched">
        <div class="toolbar">
          <strong>Term Schedule ({term?.term?.name ?? selectedTerm.name})</strong>
        </div>
        {#if term}
          <div class="gantt">
            <div class="g-row g-head">
              <div class="g-label sub-lbl">Subject / Class</div>
              <div class="g-track g-months">
                {#each term.months as m}<span class="g-month">{m}</span>{/each}
              </div>
            </div>
            {#each term.bars as b}
              <div class="g-row">
                <div class="g-label">
                  <span class="dot" style="background:var(--c-{b.color})"></span>
                  <div>
                    <div class="g-subj">{b.subject}</div>
                    <div class="g-code">{codeOf(b.subject)}</div>
                  </div>
                </div>
                <div class="g-track">
                  <div class="g-bar" style="left:{b.startPct}%;width:{b.endPct - b.startPct}%;background:var(--t-{b.color})">
                    <span class="dot sm-dot" style="background:var(--c-{b.color})"></span>
                    <span class="g-bar-lbl">{b.label}</span>
                  </div>
                </div>
              </div>
            {/each}
          </div>
        {:else}
          <p class="loading">Loading term schedule…</p>
        {/if}
      </div>

      <div class="rail">
        {#if week}
          <MiniCalendar {...week.miniCalendar} />
        {/if}
      </div>
    </div>
  {:else if activeTab === 'list'}
    <!-- list view: only the "My Enrolled Classes" table below -->
  {:else}
    <div class="card cal-card">
      <div class="toolbar">
        <div class="tb-left">
          <button class="btn btn-ghost sm" onclick={goToday}>Today</button>
          <button class="nav-btn" onclick={() => shiftMonth(-1)} aria-label="Previous month"><Icon name="chevron-left" size={15} /></button>
          <button class="nav-btn" onclick={() => shiftMonth(1)} aria-label="Next month"><Icon name="chevron-right" size={15} /></button>
          <strong>{month?.month ?? ''}</strong>
        </div>
      </div>
      <div class="month-grid">
        {#each DOW as d}<div class="mg-dow">{d}</div>{/each}
        {#each calCells as day}
          <div class="mg-cell" class:mg-sel={day != null && day === month?.today}>
            {#if day}
              <span class="mg-num">{day}</span>
              <div class="mg-dots">
                {#each classesOn(day) as c}<span class="mg-dot" style="background:var(--c-{c.color})" title="{c.subject} {c.start} – {c.end}"></span>{/each}
              </div>
            {/if}
          </div>
        {/each}
      </div>
    </div>
  {/if}

  <div class="card enr-card">
    <div class="toolbar">
      <strong>{tableTitle} ({selectedTerm.name})</strong>
      {#if isStudent}<button class="btn btn-primary" onclick={() => navigate('#/schedule/browse')}>+ Add / Change Classes</button>{/if}
    </div>
    {#if enrolled}
      <table class="tbl">
        <thead>
          <tr>
            <th>Subject / Class</th><th>Teacher</th><th>Days</th><th>Time</th><th>Duration</th>
            <th>Start Date</th><th>End Date</th>{#if !isStudent}<th>Students</th>{/if}<th>Status</th>{#if isStudent}<th>Actions</th>{/if}
          </tr>
        </thead>
        <tbody>
          {#each enrolled.classes as c (c.id)}
            <tr>
              <td>
                <div class="subj-cell">
                  <span class="dot" style="background:var(--c-{c.color})"></span>
                  <div>
                    <div class="up-subj">{c.name}</div>
                    <div class="up-meta">{c.gradeLevel}</div>
                  </div>
                </div>
              </td>
              <td><div class="teach-cell"><Avatar name={c.teacher} username={c.teacherUsername} size={26} /> {c.teacher}</div></td>
              <td>
                <div class="days-cell">
                  {#each c.days as d}<span class="pill pill-blue day-pill">{d}</span>{/each}
                </div>
              </td>
              <td class="nowrap">{c.time}</td>
              <td>{c.duration}</td>
              <td class="nowrap">{c.startDate}</td>
              <td class="nowrap">{c.endDate}</td>
              {#if !isStudent}<td class="nowrap">{c.studentCount ?? 0}</td>{/if}
              <td><span class="pill {STATUS_PILL[c.status] ?? 'pill-gray'}">{c.status}</span></td>
              {#if isStudent}<td><button class="btn btn-danger-outline sm" onclick={() => drop(c.id)}>Drop</button></td>{/if}
            </tr>
          {/each}
        </tbody>
      </table>
    {:else}
      <p class="loading">Loading classes…</p>
    {/if}
  </div>
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1.1rem; flex-wrap: wrap; }
  .head-right { margin-left: auto; }

  .tabbar { display: flex; gap: 0.25rem; padding: 0.35rem; margin-bottom: 1.1rem; }
  .tab { background: none; border: none; padding: 0.5rem 1rem; border-radius: 8px; font-size: 0.85rem; font-weight: 600; color: var(--text-2); }
  .tab.active { background: var(--t-blue); color: var(--primary); }

  .main-grid { display: grid; grid-template-columns: 1fr 296px; gap: 1.15rem; align-items: start; margin-bottom: 1.15rem; }
  .rail { display: flex; flex-direction: column; gap: 1rem; }

  .toolbar { display: flex; justify-content: space-between; align-items: center; padding: 0.9rem 1.1rem; border-bottom: 1px solid var(--border); flex-wrap: wrap; gap: 0.5rem; }
  .tb-left { display: flex; align-items: center; gap: 0.45rem; }
  .btn.sm { padding: 0.35rem 0.75rem; font-size: 0.78rem; }
  .nav-btn { background: #fff; border: 1px solid var(--border); border-radius: 8px; width: 28px; height: 28px; display: grid; place-items: center; color: var(--text-2); }
  .week-label { font-size: 0.95rem; margin-left: 0.35rem; }

  .tgrid { display: grid; grid-template-columns: 62px repeat(var(--days, 7), 1fr); }
  .tcell { border-bottom: 1px solid var(--border); border-right: 1px solid var(--border); min-height: 54px; }
  .tcell.last { border-right: none; }
  .thead { min-height: auto; padding: 0.5rem 0.4rem; text-align: center; }
  .time-col { font-size: 0.68rem; color: var(--text-3); text-align: right; padding: 0.35rem 0.5rem 0 0; font-weight: 600; }
  .day-head { display: flex; flex-direction: column; gap: 1px; }
  .dh-name { font-size: 0.7rem; color: var(--text-3); font-weight: 600; }
  .dh-date { font-size: 0.8rem; font-weight: 700; }
  .day-head.today { background: var(--t-blue); }
  .day-head.today .dh-date, .day-head.today .dh-name { color: var(--primary); }
  .slot { padding: 2px; }
  .block { border-radius: 6px; padding: 0.3rem 0.4rem; height: 100%; overflow: hidden; }
  .slot.multi { display: flex; flex-direction: column; gap: 2px; }
  .chip { border-radius: 5px; padding: 0.12rem 0.35rem; display: flex; justify-content: space-between; align-items: baseline; gap: 0.3rem; min-width: 0; }
  .c-subj { font-size: 0.66rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0; }
  .c-grade { font-size: 0.58rem; color: var(--text-2); flex-shrink: 0; }
  .b-subj { font-size: 0.72rem; font-weight: 700; }
  .b-time { font-size: 0.65rem; color: var(--text-2); }
  .b-meta { font-size: 0.62rem; color: var(--text-2); display: flex; align-items: center; gap: 0.2rem; }
  .loading { padding: 1.25rem; color: var(--text-2); }

  .dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; }
  .up-subj { font-weight: 700; font-size: 0.82rem; }
  .up-meta { font-size: 0.72rem; color: var(--text-2); }

  .gantt { padding: 0.75rem 1.1rem 1.1rem; }
  .g-row { display: grid; grid-template-columns: 165px 1fr; align-items: center; min-height: 52px; border-bottom: 1px solid var(--border); }
  .g-row:last-child { border-bottom: none; }
  .g-head { min-height: 34px; border-bottom: 1px solid var(--border); }
  .sub-lbl { font-size: 0.7rem; color: var(--text-3); font-weight: 600; }
  .g-label { display: flex; align-items: center; gap: 0.55rem; padding-right: 0.75rem; }
  .g-subj { font-weight: 700; font-size: 0.82rem; }
  .g-code { font-size: 0.7rem; color: var(--text-2); }
  .g-track { position: relative; height: 100%; min-height: 34px; background-image: linear-gradient(to right, var(--border) 1px, transparent 1px); background-size: 20% 100%; }
  .g-months { display: flex; align-items: center; background-image: none; }
  .g-month { flex: 1; font-size: 0.72rem; font-weight: 600; color: var(--text-2); padding-left: 0.5rem; }
  .g-bar { position: absolute; top: 50%; transform: translateY(-50%); height: 26px; border-radius: 999px; display: flex; align-items: center; gap: 0.5rem; padding: 0 0.6rem; }
  .sm-dot { width: 8px; height: 8px; margin-top: 0; }
  .g-bar-lbl { font-size: 0.7rem; font-weight: 600; color: var(--text-2); white-space: nowrap; margin: 0 auto; }

  .day-pill { padding: 0.12rem 0.45rem; font-size: 0.66rem; }

  .cal-card { margin-bottom: 1.15rem; }
  .month-grid { display: grid; grid-template-columns: repeat(7, 1fr); padding: 0.75rem 1.1rem 1.1rem; }
  .mg-dow { font-size: 0.7rem; color: var(--text-3); font-weight: 600; text-align: center; padding: 0.4rem 0; }
  .mg-cell { border: 1px solid var(--border); min-height: 72px; padding: 0.35rem 0.45rem; margin: -0.5px; }
  .mg-cell.mg-sel { background: var(--t-blue); }
  .mg-num { font-size: 0.75rem; font-weight: 600; }
  .mg-dots { display: flex; gap: 3px; margin-top: 4px; flex-wrap: wrap; }
  .mg-dot { width: 7px; height: 7px; border-radius: 50%; }

  .enr-card { overflow: hidden; }
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.8rem; }
  .tbl th { text-align: left; font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.02em; color: var(--text-3); font-weight: 600; padding: 0.6rem 0.75rem; border-bottom: 1px solid var(--border); background: #fafbfd; }
  .tbl td { padding: 0.6rem 0.75rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .tbl tr:last-child td { border-bottom: none; }
  .subj-cell { display: flex; align-items: center; gap: 0.55rem; }
  .subj-cell .dot { margin-top: 0; }
  .teach-cell { display: flex; align-items: center; gap: 0.45rem; white-space: nowrap; }
  .days-cell { display: flex; gap: 0.2rem; flex-wrap: wrap; }
  .nowrap { white-space: nowrap; }
</style>
