<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'
  import { primaryRole } from '../auth.svelte.js'

  const role = primaryRole()
  const isStudent = role === 'student'
  let summary = $state(null)
  let list = $state(null)
  let perf = $state(null)

  let tab = $state('All Assessments')
  let statusFilter = $state('All Status')
  let typeFilter = $state('All Types')
  let search = $state('')
  // Ask 21 (student): weekly / monthly period window over the due dates
  let period = $state('week')
  let anchor = $state(startOfDay(new Date()))

  function startOfDay(d) {
    const x = new Date(d)
    x.setHours(0, 0, 0, 0)
    return x
  }
  function weekStart(d) {
    const x = startOfDay(d)
    x.setDate(x.getDate() - ((x.getDay() + 6) % 7)) // Monday
    return x
  }
  const range = $derived.by(() => {
    if (period === 'week') {
      const from = weekStart(anchor)
      const to = new Date(from)
      to.setDate(from.getDate() + 7)
      return { from, to }
    }
    const from = new Date(anchor.getFullYear(), anchor.getMonth(), 1)
    return { from, to: new Date(anchor.getFullYear(), anchor.getMonth() + 1, 1) }
  })
  const rangeLabel = $derived.by(() => {
    const { from, to } = range
    if (period === 'month') return from.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
    const last = new Date(to)
    last.setDate(to.getDate() - 1)
    const a = from.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    const b = last.toLocaleDateString('en-US', from.getMonth() === last.getMonth() ? { day: 'numeric' } : { month: 'short', day: 'numeric' })
    return `${a} - ${b}, ${last.getFullYear()}`
  })
  function shift(n) {
    const d = new Date(anchor)
    if (period === 'week') d.setDate(d.getDate() + 7 * n)
    else d.setMonth(d.getMonth() + n)
    anchor = d
  }
  const today = startOfDay(new Date())
  const isToday = (iso) => startOfDay(new Date(iso)).getTime() === today.getTime()
  const dueDow = (iso) => new Date(iso).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })

  const TABS = ['All Assessments', 'Quizzes', 'Tests', 'Assignments', 'Practice']
  const TAB_TYPE = { Quizzes: 'Quiz', Tests: 'Test', Assignments: 'Assignment', Practice: 'Practice' }
  const SUBJECT_PILL = { Mathematics: 'blue', Chemistry: 'amber', Physics: 'green', English: 'purple', 'SAT Prep': 'red' }
  const TYPE_PILL = { Quiz: 'purple', Test: 'red', Assignment: 'blue', Practice: 'green' }
  const STATUS_PILL = { Upcoming: 'blue', 'Due Today': 'red', 'In Progress': 'amber', Submitted: 'purple', Completed: 'green', Overdue: 'red' }
  // Ask 21: the row button — Start Now / Resume / Submit only when it is due today or in progress; otherwise View Details (start from there) or Review
  function rowButton(r) {
    if (r.action === 'Resume') return { label: 'Resume', primary: true }
    if (r.status === 'Due Today' && (r.action === 'Start' || r.action === 'Submit')) return { label: r.action === 'Start' ? 'Start Now' : 'Submit', primary: true }
    if (r.action === 'Review') return { label: 'Review', primary: false }
    return { label: 'View Details', primary: false }
  }
  const STAT_ICON = { total: 'assessments', score: 'chart', trophy: 'trophy', target: 'target' }
  const TYPE_ICON = { Quiz: 'edit', Test: 'doc', Assignment: 'assessments', Practice: 'practice' }

  ;(async () => {
    // stub build: load each part independently so an endpoint that is implemented still renders when a sibling answers 501
    const [s, l, a] = await Promise.allSettled([
      api.get('/api/assessment/summary'),
      api.get('/api/assessment/list'),
      role === 'teacher' ? api.get('/api/dashboard/analytics') : Promise.resolve(null), // performance chart: teachers only (admin: too many subjects)
    ])
    summary = s.status === 'fulfilled' ? s.value : null
    list = l.status === 'fulfilled' ? l.value : null
    perf = a.status === 'fulfilled' ? (a.value?.performance ?? []) : []
  })()

  const filtered = $derived.by(() => {
    if (!list) return []
    const q = search.trim().toLowerCase()
    const rows = list.filter((r) => {
      if (TAB_TYPE[tab] && r.type !== TAB_TYPE[tab]) return false
      if (statusFilter !== 'All Status' && r.status !== statusFilter) return false
      if (typeFilter !== 'All Types' && r.type !== typeFilter) return false
      if (q && !`${r.title} ${r.subtitle} ${r.subject}`.toLowerCase().includes(q)) return false
      if (r.dueAt) {
        const t = new Date(r.dueAt).getTime()
        if (t < range.from.getTime() || t >= range.to.getTime()) return false
      }
      return true
    })
    return rows.sort((a, b) => (a.dueAt ?? '').localeCompare(b.dueAt ?? ''))
  })
  const inPeriod = $derived(list ? list.filter((r) => { const t = new Date(r.dueAt).getTime(); return t >= range.from.getTime() && t < range.to.getTime() }).length : 0)

  // Start / Resume → timed attempt page; Review → results; Submit / View Submission / View Details → details page
  function act(row) {
    const b = rowButton(row)
    if (b.label === 'Start Now' || b.label === 'Resume') navigate(`#/assessments/${row.id}/take`)
    else if (b.label === 'Review') navigate(`#/assessments/${row.id}/review`)
    else navigate('#/assessments/' + row.id)
  }
  const mmss = (s) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`

  async function remove(row) {
    if (!confirm(`Delete "${row.title}"? Its questions and all student submissions are removed.`)) return
    try {
      await api.del(`/api/assessment/teacher/assessments/${row.id}`)
      list = await api.get('/api/assessment/list')
      summary = await api.get('/api/assessment/summary')
    } catch (e) {
      alert(e.message)
    }
  }

  function scoreColor(score) {
    const n = parseInt(score, 10)
    return n >= 80 ? 'var(--c-green)' : 'var(--c-amber)'
  }
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Assessments</h1>
      <p class="page-sub">
        {#if role === 'admin'}Every teacher's quizzes, tests and assignments with submissions and averages.
        {:else if role === 'teacher'}Your quizzes, tests and assignments with student submissions and averages.
        {:else}Track your quizzes, tests, and assignments. See your performance and stay on top of what's due.{/if}
      </p>
    </div>
    {#if role === 'teacher'}
      <a class="btn btn-primary new-btn" href="#/assessments/new"><Icon name="plus" size={14} /> New assessment</a>
    {/if}
    {#if summary}
      <div class="term card">
        <div class="term-ic"><Icon name="grad" size={18} /></div>
        <div>
          <div class="term-lbl">Term</div>
          <div class="term-name">{summary.term.name} <Icon name="chevron-down" size={13} /></div>
          <div class="term-dates">{summary.term.start} – {summary.term.end}</div>
        </div>
      </div>
    {/if}
  </div>

  {#if summary}
    <div class="stats">
      {#each summary.stats as s}
        <StatCard icon={STAT_ICON[s.icon] || 'assessments'} color={s.color} value={s.value} label={s.label} sub={s.sub} clickable={false} />
      {/each}
    </div>
  {/if}

  <div class="cols" class:full={isStudent}>
    <div class="main">
      <div class="card list-card">
        <div class="tabs">
          {#each TABS as t}
            <button class="tab" class:active={tab === t} onclick={() => (tab = t)}>{t}</button>
          {/each}
        </div>

        <div class="toolbar">
          <div class="seg">
            <button class:on={period === 'week'} onclick={() => (period = 'week')}>Weekly View</button>
            <button class:on={period === 'month'} onclick={() => (period = 'month')}>Monthly View</button>
          </div>
          <button class="btn btn-outline sm" onclick={() => (anchor = startOfDay(new Date()))}>Today</button>
          <div class="pager">
            <button class="pg" onclick={() => shift(-1)} aria-label="Previous"><Icon name="chevron-left" size={14} /></button>
            <button class="pg" onclick={() => shift(1)} aria-label="Next"><Icon name="chevron-right" size={14} /></button>
          </div>
          <div class="range">{rangeLabel}</div>
          <select class="tb-select" bind:value={statusFilter}>
            <option>All Status</option>
            {#if isStudent}
              <option>Due Today</option>
              <option>Upcoming</option>
              <option>In Progress</option>
              <option>Submitted</option>
              <option>Completed</option>
              <option>Overdue</option>
            {:else}
              <option>Open</option>
              <option>Closed</option>
            {/if}
          </select>
          <select class="tb-select" bind:value={typeFilter}>
            <option>All Types</option>
            <option>Quiz</option>
            <option>Test</option>
            <option>Assignment</option>
            <option>Practice</option>
          </select>
          <div class="search tb-search">
            <Icon name="search" size={15} />
            <input placeholder="Search assessments..." bind:value={search} />
          </div>
        </div>

        <div class="thead" class:staff={!isStudent} class:admin={role === 'admin'}>
          <div>Title</div>
          <div>Subject</div>
          <div>Type</div>
          {#if isStudent}
            <div class="sorted">Due Date <Icon name="arrow-up" size={11} /></div>
            <div>Status</div>
            <div>Score</div>
            <div>Actions</div>
          {:else}
            {#if role === 'admin'}<div>Teacher</div>{/if}
            <div class="sorted">Due Date <Icon name="arrow-up" size={11} /></div>
            <div>Submissions</div>
            <div>Average</div>
            <div>Actions</div>
          {/if}
          <div></div>
        </div>

        {#if list}
          {#each filtered as row (row.id)}
            <div class="trow" class:staff={!isStudent} class:admin={role === 'admin'}>
              <div class="title-cell">
                <div class="sq" style="background: var(--c-{row.color})"><Icon name={TYPE_ICON[row.type] || 'assessments'} size={15} /></div>
                <div class="tt">
                  <div class="t1">{row.title}</div>
                  <div class="t2">{row.subtitle}</div>
                </div>
              </div>
              <div><span class="pill pill-{SUBJECT_PILL[row.subject] || 'gray'}">{row.subject}</span></div>
              <div><span class="pill pill-{TYPE_PILL[row.type] || 'gray'}">{row.type}</span></div>
              {#if isStudent}
                {@const b = rowButton(row)}
                <div class="due">
                  <div class="d1" class:today={isToday(row.dueAt)}>{isToday(row.dueAt) ? 'Today' : dueDow(row.dueAt)}</div>
                  <div class="d2">{row.dueTime}</div>
                </div>
                <div>
                  <span class="pill pill-{STATUS_PILL[row.status] || 'gray'}">{row.status}</span>
                  {#if row.status === 'In Progress' && row.secondsLeft != null}<div class="d2">{mmss(row.secondsLeft)} left</div>{/if}
                  {#if row.status === 'Overdue'}<div class="d2">Closed</div>{/if}
                </div>
                <div class="score">{row.score ?? '—'}</div>
                <div>
                  <button class="btn {b.primary ? 'btn-primary' : 'btn-outline'} act" onclick={() => act(row)}>{b.label}</button>
                </div>
              {:else}
                {#if role === 'admin'}<div class="teacher">{row.teacher ?? '—'}</div>{/if}
                <div class="due">
                  <div class="d1" class:today={isToday(row.dueAt)}>{isToday(row.dueAt) ? 'Today' : dueDow(row.dueAt)}</div>
                  <div class="d2">{row.dueTime}</div>
                </div>
                <div class="score">{row.submissions ?? 0}</div>
                <div class="score">{row.average ?? '—'}</div>
                <div class="actions">
                  {#if role === 'teacher'}
                    <a class="btn btn-ghost xs" href="#/assessments/{row.id}/edit" title="Edit"><Icon name="edit" size={13} /> Edit</a>
                  {/if}
                  <a class="btn btn-outline xs" href="#/assessments/submissions?assessment={row.id}" title="Submissions">Submissions</a>
                  {#if role === 'teacher'}
                    <button class="btn btn-danger-outline xs" onclick={() => remove(row)} title="Delete">Delete</button>
                  {/if}
                </div>
              {/if}
              <a class="chev" href="#/assessments/{row.id}" aria-label="View details"><Icon name="chevron-right" size={15} /></a>
            </div>
          {:else}
            <div class="empty">{inPeriod === 0 ? `Nothing due ${period === 'week' ? 'this week' : 'this month'} (${rangeLabel}). Use the arrows to move to another period.` : 'No assessments match your filters.'}</div>
          {/each}
        {:else}
          <div class="empty">Loading…</div>
        {/if}
      </div>
    </div>

    {#if !isStudent}
    <div class="rail">
      {#if role === 'teacher' && perf && perf.length}
      <div class="card rail-card">
        <div class="rail-head">
          <h3>{isStudent ? 'Performance Overview' : 'Performance by Subject'}</h3>
          <select class="mini-select"><option>This Term</option></select>
        </div>
        {#if perf}
          <div class="chart">
            <div class="yaxis">
              {#each [100, 75, 50, 25, 0] as y}<div>{y}</div>{/each}
            </div>
            <div class="bars">
              {#each perf as p}
                <div class="bar-col">
                  <div class="bar-track">
                    <div class="bar" style="height:{p.percent}%; background: var(--c-{p.color})"></div>
                  </div>
                  <div class="bar-subj">{p.subject}</div>
                  <div class="bar-pct">{p.percent}%</div>
                </div>
              {/each}
            </div>
          </div>
        {/if}
      </div>
      {/if}

      <div class="card rail-card">
        <div class="rail-head">
          <h3>{isStudent ? 'Recent Results' : 'Recent Submissions'}</h3>
          {#if isStudent}<button class="link">View All</button>{:else}<a class="link" href="#/assessments/submissions">View All</a>{/if}
        </div>
        {#if summary}
          {#each summary.recentResults as r}
            <div class="rr">
              <div class="rr-ic" style="background: var(--t-{r.color}); color: var(--c-{r.color})"><Icon name="assessments" size={16} /></div>
              <div class="rr-body">
                <div class="rr-t">{r.title}</div>
                <div class="rr-s">{r.subject}{#if r.student} · {r.student}{/if}</div>
              </div>
              <div class="rr-right">
                <div class="rr-score" style="color:{scoreColor(r.score)}">{r.score}</div>
                <div class="rr-date">{r.date}</div>
              </div>
              <div class="chev"><Icon name="chevron-right" size={14} /></div>
            </div>
          {/each}
        {/if}
      </div>

    </div>
    {/if}
  </div>
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .term { display: flex; gap: 0.7rem; align-items: center; padding: 0.6rem 0.9rem; min-width: 210px; }
  .term-ic { width: 36px; height: 36px; border-radius: 10px; background: var(--t-blue); color: var(--c-blue); display: grid; place-items: center; }
  .term-lbl { font-size: 0.68rem; color: var(--text-3); }
  .term-name { font-weight: 700; font-size: 0.9rem; display: flex; align-items: center; gap: 4px; }
  .term-dates { font-size: 0.7rem; color: var(--text-2); }

  .stats { display: flex; gap: 0.9rem; margin-top: 1.1rem; flex-wrap: wrap; }

  .cols { display: grid; grid-template-columns: 1fr 300px; gap: 1rem; margin-top: 1.1rem; align-items: start; }
  .cols.full { grid-template-columns: 1fr; }
  .toolbar { display: flex; align-items: center; gap: 0.6rem; padding: 0.8rem 0 0.6rem; flex-wrap: wrap; }
  .seg { display: inline-flex; background: #eef1f6; border-radius: 9px; padding: 3px; }
  .seg button { border: none; background: none; border-radius: 7px; padding: 0.38rem 0.8rem; font-size: 0.78rem; font-weight: 600; color: var(--text-2); }
  .seg button.on { background: #fff; color: var(--primary); box-shadow: 0 1px 2px rgba(0,0,0,0.08); border: 1px solid #c7d7f5; }
  .btn.sm { padding: 0.38rem 0.75rem; font-size: 0.76rem; }
  .pager { display: inline-flex; gap: 4px; }
  .pg { width: 30px; height: 30px; border-radius: 8px; border: 1px solid var(--border); background: #fff; color: var(--text-2); display: grid; place-items: center; }
  .range { font-weight: 700; font-size: 0.86rem; margin-right: auto; white-space: nowrap; }
  .tb-select { border: 1px solid var(--border); border-radius: 8px; padding: 0.42rem 0.5rem; font-size: 0.78rem; color: var(--text); background: #fff; }
  .tb-search { min-width: 200px; }
  .sorted { display: inline-flex; align-items: center; gap: 3px; color: var(--text); }
  .due .d1.today { color: var(--c-red); }

  .list-card { padding: 0.4rem 1rem 0.6rem; }
  .tabs { display: flex; gap: 0.35rem; border-bottom: 1px solid var(--border); padding: 0.5rem 0; flex-wrap: wrap; }
  .tab { border: none; background: none; padding: 0.55rem 0.8rem; font-size: 0.82rem; font-weight: 600; color: var(--text-2); border-bottom: 2px solid transparent; margin-bottom: -1px; }
  .tab.active { color: var(--primary); border-bottom-color: var(--primary); }
  .tab:not(.active):hover { color: var(--text); }

  .search { display: flex; align-items: center; gap: 6px; border: 1px solid var(--border); border-radius: 8px; padding: 0.4rem 0.6rem; color: var(--text-3); background: #fff; }
  .search input { border: none; outline: none; flex: 1; font-size: 0.78rem; min-width: 0; }

  .thead, .trow { display: grid; grid-template-columns: 1.7fr 0.9fr 0.8fr 0.85fr 0.85fr 0.5fr 0.9fr 18px; gap: 0.5rem; align-items: center; }
  .thead.staff, .trow.staff { grid-template-columns: 1.6fr 0.9fr 0.7fr 0.85fr 0.7fr 0.6fr 1.5fr 18px; }
  .new-btn { display: inline-flex; align-items: center; gap: 0.35rem; align-self: center; margin-left: auto; }
  .actions { display: flex; gap: 0.3rem; flex-wrap: wrap; }
  .btn.xs { padding: 0.28rem 0.5rem; font-size: 0.7rem; white-space: nowrap; display: inline-flex; align-items: center; gap: 0.25rem; }
  .teacher { font-size: 0.78rem; color: var(--text-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .thead.staff.admin, .trow.staff.admin { grid-template-columns: 1.6fr 0.9fr 0.7fr 0.9fr 0.85fr 0.7fr 0.6fr 1fr 18px; }
  a.chev { display: inline-flex; }
  .thead { font-size: 0.7rem; color: var(--text-3); font-weight: 600; padding: 0.5rem 0.25rem; border-bottom: 1px solid var(--border); }
  .trow { padding: 0.6rem 0.25rem; border-bottom: 1px solid var(--border); }
  .trow:last-child { border-bottom: none; }
  .title-cell { display: flex; gap: 0.6rem; align-items: center; min-width: 0; }
  .sq { width: 32px; height: 32px; border-radius: 8px; color: #fff; display: grid; place-items: center; flex-shrink: 0; }
  .tt { min-width: 0; }
  .t1 { font-weight: 600; font-size: 0.82rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .t2 { font-size: 0.7rem; color: var(--text-3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .due .d1 { font-size: 0.76rem; font-weight: 600; }
  .due .d2 { font-size: 0.68rem; color: var(--text-3); }
  .score { font-weight: 700; font-size: 0.82rem; }
  .act { padding: 0.35rem 0.7rem; font-size: 0.74rem; white-space: nowrap; }
  .chev { color: var(--text-3); }
  .empty { padding: 1.2rem; color: var(--text-3); text-align: center; font-size: 0.82rem; }

  .rail { display: flex; flex-direction: column; gap: 1rem; }
  .rail-card { padding: 1rem 1.1rem; }
  .rail-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.8rem; }
  .rail-head h3 { font-size: 0.92rem; }
  .mini-select { border: 1px solid var(--border); border-radius: 7px; font-size: 0.72rem; padding: 0.25rem 0.35rem; color: var(--text-2); background: #fff; }

  .chart { display: flex; gap: 0.4rem; }
  .yaxis { display: flex; flex-direction: column; justify-content: space-between; height: 131px; font-size: 0.62rem; color: var(--text-3); text-align: right; }
  .bars { flex: 1; display: flex; justify-content: space-between; gap: 0.35rem; }
  .bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; }
  .bar-track { height: 130px; width: 100%; display: flex; align-items: flex-end; justify-content: center; border-bottom: 1px solid var(--border); }
  .bar { width: 22px; border-radius: 6px 6px 0 0; }
  .bar-subj { font-size: 0.64rem; color: var(--text-2); margin-top: 4px; font-weight: 600; }
  .bar-pct { font-size: 0.64rem; color: var(--text-3); }

  .rr { display: flex; align-items: center; gap: 0.6rem; padding: 0.5rem 0; border-bottom: 1px solid var(--border); }
  .rr:last-child { border-bottom: none; }
  .rr-ic { width: 32px; height: 32px; border-radius: 8px; display: grid; place-items: center; flex-shrink: 0; }
  .rr-body { flex: 1; min-width: 0; }
  .rr-t { font-weight: 600; font-size: 0.8rem; }
  .rr-s { font-size: 0.7rem; color: var(--text-3); }
  .rr-right { text-align: right; }
  .rr-score { font-weight: 700; font-size: 0.95rem; }
  .rr-date { font-size: 0.66rem; color: var(--text-3); }

  .area { display: flex; gap: 0.6rem; padding: 0.5rem 0; align-items: flex-start; border-bottom: 1px solid var(--border); }
  .area:last-child { border-bottom: none; }
  .area-note { font-size: 0.72rem; color: var(--text-2); margin-top: 2px; line-height: 1.35; }

  @media (max-width: 1100px) { .cols { grid-template-columns: 1fr; } }
</style>
