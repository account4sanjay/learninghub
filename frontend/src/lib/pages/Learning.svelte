<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'
  import Avatar from '../components/Avatar.svelte'
  import { primaryRole } from '../auth.svelte.js'

  // Ask 25: every subject has a term lesson plan (one item per scheduled class). Students see their enrolled
  // subjects' plans; teachers (own courses) and admins mark classes complete and the students see it at once.
  const role = primaryRole()
  const isStudent = role === 'student'
  let overview = $state(null)
  let classes = $state(null)
  let plan = $state(null)
  let selectedId = $state(null)
  let filter = $state('All')
  let planFilter = $state('All')
  let busy = $state(null) // item id being updated
  let error = $state('')

  async function loadTop() {
    // stub build: load each part independently so an endpoint that is implemented still renders when a sibling answers 501
    const [o, c] = await Promise.allSettled([api.get('/api/learning/overview'), api.get('/api/learning/classes')])
    overview = o.status === 'fulfilled' ? o.value : null
    classes = c.status === 'fulfilled' ? c.value : []
    if (!selectedId && classes.length) selectedId = classes[0].id
  }
  loadTop()

  $effect(() => {
    const id = selectedId
    if (!id) return
    plan = null
    api.get(`/api/learning/plan/${id}`).then((p) => (plan = p)).catch((e) => (error = e.message))
  })

  async function setCompleted(item, completed) {
    busy = item.id
    error = ''
    try {
      const r = await api.put(`/api/learning/plan/items/${item.id}`, { completed })
      // reflect at once: the item, the plan counters, the course row and the stat tiles
      plan = {
        ...plan,
        items: plan.items.map((i) => (i.id === item.id ? r.item : i)),
        classesCompleted: r.completed,
        classesRemaining: r.total - r.completed,
        percent: r.coursePercent,
        status: r.courseStatus,
      }
      await loadTop()
    } catch (e) {
      error = e.message
    } finally {
      busy = null
    }
  }

  const STAT_ICON = { classes: 'classes', lessons: 'book', check: 'check', schedule: 'schedule', hours: 'clock' }
  const CLASS_ICON = { math: 'calc', physics: 'target', english: 'book', cs: 'doc', chemistry: 'flask' }
  const FILTERS = ['All', 'In Progress', 'Completed']
  const PLAN_FILTERS = ['All', 'Completed', 'Remaining']
  const STATUS_PILL = { Completed: 'green', 'Not completed': 'red', Today: 'amber', Upcoming: 'blue' }

  const filteredClasses = $derived(
    (classes || []).filter((c) => {
      if (filter === 'In Progress') return c.status === 'in-progress'
      if (filter === 'Completed') return c.status === 'completed'
      return true
    })
  )
  const avgProgress = $derived(classes && classes.length ? Math.round(classes.reduce((a, c) => a + (c.percent ?? 0), 0) / classes.length) : 0)
  const planItems = $derived(
    (plan?.items ?? []).filter((i) => (planFilter === 'Completed' ? i.completed : planFilter === 'Remaining' ? !i.completed : true))
  )
</script>

<div class="page">
  <div class="head-row">
    <div>
      <h1 class="page-title">Learning</h1>
      <p class="page-sub">
        {#if role === 'admin'}Every course's term lesson plan and how far each class has come.
        {:else if role === 'teacher'}Your courses' term lesson plans: click a course and mark each class complete as you teach it.
        {:else}Your subjects' lesson plans for the term: what has been covered and what is still to come.{/if}
      </p>
    </div>
    {#if overview}
      <div class="term-card card">
        <div class="term-top">
          <span class="term-icon"><Icon name="grad" size={18} /></span>
          <div>
            <div class="term-label">Term</div>
            <div class="term-name">{overview.term.name}</div>
          </div>
        </div>
        <div class="term-dates">{overview.term.start} &ndash; {overview.term.end}</div>
      </div>
    {/if}
  </div>

  {#if overview}
    <div class="stats-row">
      {#each overview.stats as s}
        <StatCard icon={STAT_ICON[s.icon] || s.icon} color={s.color} value={s.value} label={s.label} sub={s.sub || ''} progress={s.progress ?? null} clickable={false} />
      {/each}
    </div>
  {/if}

  {#if error}<div class="banner err">{error}</div>{/if}

  {#snippet planPanel()}
    <div class="main-col card">
      {#if plan}
        <div class="main-head">
          <Avatar name={plan.teacher} username={plan.teacherUsername} size={44} />
          <div class="main-head-txt">
            <h3 class="main-title">{plan.name} {#if plan.gradeLevel}<span class="title-sub">({plan.gradeLevel})</span>{/if}</h3>
            <div class="main-teacher">{plan.teacher} · {plan.classes} classes this term</div>
          </div>
          <div class="main-complete">
            <span class="pill pill-{plan.percent === 100 ? 'green' : 'blue'}">{plan.classesCompleted} / {plan.classes} completed · {plan.percent}%</span>
            <div class="track wide"><div class="fill" style="width:{plan.percent}%;background:var(--primary)"></div></div>
          </div>
        </div>
        <div class="plan-bar">
          <div class="filter-row">
            {#each PLAN_FILTERS as f}
              <button class="filter-pill" class:active={planFilter === f} onclick={() => (planFilter = f)}>{f}</button>
            {/each}
          </div>
          {#if plan.nextClass}
            <div class="next"><Icon name="schedule" size={13} /> Next class: <b>#{plan.nextClass.classNumber} · {plan.nextClass.dateLabel}</b> — {plan.nextClass.topic}</div>
          {:else}
            <div class="next"><Icon name="check" size={13} /> All classes this term are complete</div>
          {/if}
        </div>
        <div class="plan-table-wrap">
          <table class="plan">
            <thead>
              <tr><th class="num">#</th><th>Date</th><th>Unit · Topic</th><th>Status</th>{#if plan.canMark}<th class="right">Mark</th>{/if}</tr>
            </thead>
            <tbody>
              {#each planItems as i (i.id)}
                <tr class:done={i.completed} class:today={i.status === 'Today'}>
                  <td class="num">{i.classNumber}</td>
                  <td class="nowrap"><div class="d1">{i.dateLabel}</div>{#if i.status === 'Today'}<div class="d2 hot">Today</div>{/if}</td>
                  <td><div class="d1">{i.topic}</div><div class="d2">{i.title}</div></td>
                  <td>
                    <span class="pill pill-{STATUS_PILL[i.status] ?? 'gray'}">{i.status}</span>
                    {#if i.completed && i.completedAt}<div class="d2">{i.completedAt}</div>{/if}
                  </td>
                  {#if plan.canMark}
                    <td class="right">
                      {#if i.completed}
                        <button class="btn btn-ghost xs" disabled={busy === i.id} onclick={() => setCompleted(i, false)}>Undo</button>
                      {:else}
                        <button class="btn btn-primary xs" disabled={busy === i.id} onclick={() => setCompleted(i, true)}><Icon name="check" size={12} /> Mark complete</button>
                      {/if}
                    </td>
                  {/if}
                </tr>
              {:else}
                <tr><td colspan="5" class="empty-note">No classes match this filter.</td></tr>
              {/each}
            </tbody>
          </table>
        </div>
      {:else}
        <div class="placeholder">{selectedId ? 'Loading lesson plan…' : 'Select a course to see its lesson plan.'}</div>
      {/if}
    </div>
  {/snippet}

  {#if !isStudent}
    <div class="card course-table">
      <div class="ct-head">
        <h3>{role === 'admin' ? 'All Courses' : 'My Courses'}</h3>
        <span class="ct-avg">Average progress <strong>{avgProgress}%</strong> · click a course for its lesson plan</span>
      </div>
      <table class="ctbl">
        <thead>
          <tr><th>Course</th><th>Teacher</th><th class="num">Classes</th><th class="num">Completed</th><th>Next class</th><th>Progress</th></tr>
        </thead>
        <tbody>
          {#each classes ?? [] as c (c.id)}
            <tr class:sel={selectedId === c.id} onclick={() => (selectedId = c.id)}>
              <td>
                <div class="ct-course">
                  <span class="class-icon" style="background:var(--t-{c.color});color:var(--c-{c.color})"><Icon name={CLASS_ICON[c.id] || 'book'} size={15} /></span>
                  <div><div class="ct-name">{c.name}</div>{#if c.gradeLevel}<div class="ct-code">{c.gradeLevel}</div>{/if}</div>
                </div>
              </td>
              <td><div class="ct-teacher"><Avatar name={c.teacher} username={c.teacherUsername} size={22} /> {c.teacher}</div></td>
              <td class="num">{c.classes ?? 0}</td>
              <td class="num">{c.classesCompleted ?? 0}</td>
              <td class="muted nowrap">{c.nextClass ? `#${c.nextClass.classNumber} · ${c.nextClass.dateLabel}` : '—'}</td>
              <td>
                <div class="class-prog">
                  <div class="track"><div class="fill" style="width:{c.percent}%;background:var(--c-{c.color})"></div></div>
                  <span class="pct">{c.percent}%</span>
                </div>
              </td>
            </tr>
          {:else}
            <tr><td colspan="6" class="empty-note">{classes ? 'No courses.' : 'Loading…'}</td></tr>
          {/each}
        </tbody>
      </table>
    </div>
    <div class="staff-plan">{@render planPanel()}</div>
  {:else}
  <div class="cols">
    <div class="left-col">
      <h3 class="col-title">My Classes</h3>
      <div class="filter-row">
        {#each FILTERS as f}
          <button class="filter-pill" class:active={filter === f} onclick={() => (filter = f)}>{f}</button>
        {/each}
      </div>
      <div class="class-list">
        {#each filteredClasses as c (c.id)}
          <button class="class-card card" class:selected={selectedId === c.id} onclick={() => (selectedId = c.id)}>
            <div class="class-top">
              <span class="class-icon" style="background:var(--t-{c.color});color:var(--c-{c.color})">
                <Icon name={CLASS_ICON[c.id] || 'book'} size={16} />
              </span>
              <div class="class-names">
                <div class="class-name">{c.name}</div>
                <div class="class-teacher">{c.teacher}</div>
              </div>
              {#if selectedId === c.id}<span class="class-chev"><Icon name="chevron-right" size={14} /></span>{/if}
            </div>
            <div class="class-prog">
              <div class="track"><div class="fill" style="width:{c.percent}%;background:var(--c-{c.color})"></div></div>
              <span class="pct">{c.classesCompleted} / {c.classes}</span>
            </div>
          </button>
        {:else}
          <div class="empty-note">No classes.</div>
        {/each}
      </div>
    </div>
    {@render planPanel()}
  </div>
  {/if}
</div>

<style>
  .head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .term-card { padding: 0.7rem 1rem; min-width: 220px; }
  .term-top { display: flex; align-items: center; gap: 0.6rem; }
  .term-icon { color: var(--primary); display: grid; place-items: center; }
  .term-label { font-size: 0.7rem; color: var(--text-3); }
  .term-name { font-weight: 700; font-size: 0.9rem; color: var(--text); }
  .term-dates { color: var(--text-2); font-size: 0.75rem; margin-top: 0.45rem; padding-top: 0.45rem; border-top: 1px solid var(--border); }
  .stats-row { display: flex; gap: 1rem; margin-top: 1.25rem; flex-wrap: wrap; }
  .banner { padding: 0.6rem 0.9rem; border-radius: 10px; font-size: 0.85rem; margin-top: 1rem; }
  .banner.err { background: var(--t-red); color: #991b1b; }

  .course-table { margin-top: 1.25rem; overflow: hidden; }
  .ct-head { display: flex; justify-content: space-between; align-items: center; padding: 0.9rem 1.1rem; border-bottom: 1px solid var(--border); gap: 1rem; flex-wrap: wrap; }
  .ct-head h3 { font-size: 0.95rem; }
  .ct-avg { font-size: 0.8rem; color: var(--text-2); }
  .ctbl { width: 100%; border-collapse: collapse; font-size: 0.83rem; }
  .ctbl th { text-align: left; font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--text-3); font-weight: 600; padding: 0.6rem 1.1rem; border-bottom: 1px solid var(--border); background: #fafbfd; }
  .ctbl td { padding: 0.65rem 1.1rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .ctbl tbody tr { cursor: pointer; }
  .ctbl tbody tr:hover td { background: var(--bg); }
  .ctbl tr.sel td { background: var(--t-blue); }
  .ctbl tr:last-child td { border-bottom: none; }
  .ctbl .num, .plan .num { text-align: right; }
  .ct-course { display: flex; align-items: center; gap: 0.6rem; }
  .ct-name { font-weight: 700; }
  .ct-code { color: var(--text-3); font-size: 0.72rem; }
  .ct-teacher { display: flex; align-items: center; gap: 0.45rem; white-space: nowrap; }
  .ctbl .class-prog { margin-top: 0; min-width: 140px; }
  .staff-plan { margin-top: 1rem; }

  .cols { display: grid; grid-template-columns: 260px 1fr; gap: 1rem; margin-top: 1.25rem; align-items: start; }
  .col-title { font-size: 0.95rem; font-weight: 700; }
  .filter-row { display: flex; gap: 0.35rem; margin-top: 0.6rem; }
  .filter-pill { border: 1px solid var(--border); background: #fff; color: var(--text-2); border-radius: 999px; padding: 0.28rem 0.65rem; font-size: 0.72rem; font-weight: 600; }
  .filter-pill.active { background: var(--primary); border-color: var(--primary); color: #fff; }
  .class-list { display: flex; flex-direction: column; gap: 0.6rem; margin-top: 0.75rem; }
  .class-card { display: block; width: 100%; text-align: left; padding: 0.75rem 0.8rem; border-width: 1px; cursor: pointer; }
  .class-card.selected { border: 2px solid var(--primary); background: #f7faff; padding: calc(0.75rem - 1px) calc(0.8rem - 1px); }
  .class-top { display: flex; align-items: center; gap: 0.6rem; }
  .class-icon { width: 32px; height: 32px; border-radius: 9px; display: grid; place-items: center; flex-shrink: 0; }
  .class-names { flex: 1; min-width: 0; }
  .class-name { font-weight: 700; font-size: 0.83rem; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .class-teacher { color: var(--text-2); font-size: 0.72rem; }
  .class-chev { color: var(--text-3); }
  .class-prog { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.6rem; }
  .track { flex: 1; height: 5px; background: #eef1f6; border-radius: 3px; overflow: hidden; }
  .fill { height: 100%; border-radius: 3px; }
  .pct { font-size: 0.7rem; color: var(--text-2); font-weight: 600; white-space: nowrap; }
  .empty-note { color: var(--text-3); font-size: 0.8rem; padding: 0.75rem 0.2rem; }

  .main-col { padding: 1.1rem 1.2rem; }
  .main-head { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
  .main-head-txt { flex: 1; min-width: 0; }
  .main-title { font-size: 1.05rem; font-weight: 700; }
  .title-sub { color: var(--text-2); font-weight: 500; }
  .main-teacher { color: var(--text-2); font-size: 0.8rem; margin-top: 1px; }
  .main-complete { text-align: right; }
  .main-complete .track { width: 160px; margin-top: 0.45rem; margin-left: auto; }
  .plan-bar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-top: 0.9rem; flex-wrap: wrap; }
  .plan-bar .filter-row { margin-top: 0; }
  .next { font-size: 0.78rem; color: var(--text-2); display: flex; align-items: center; gap: 0.35rem; }
  .plan-table-wrap { margin-top: 0.8rem; overflow-x: auto; }
  .plan { width: 100%; border-collapse: collapse; font-size: 0.83rem; }
  .plan th { text-align: left; font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--text-3); font-weight: 600; padding: 0.5rem 0.6rem; border-bottom: 1px solid var(--border); }
  .plan td { padding: 0.55rem 0.6rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .plan tr:last-child td { border-bottom: none; }
  .plan tr.done td { color: var(--text-2); }
  .plan tr.today td { background: #fffbeb; }
  .d1 { font-weight: 600; }
  .plan tr.done .d1 { font-weight: 500; }
  .d2 { font-size: 0.7rem; color: var(--text-3); }
  .d2.hot { color: #b45309; font-weight: 700; }
  .nowrap { white-space: nowrap; }
  .right { text-align: right; }
  .muted { color: var(--text-2); }
  .btn.xs { padding: 0.28rem 0.55rem; font-size: 0.72rem; white-space: nowrap; display: inline-flex; align-items: center; gap: 0.25rem; }
  .placeholder { color: var(--text-3); text-align: center; padding: 2.5rem 1rem; font-size: 0.9rem; }

  @media (max-width: 1100px) { .cols { grid-template-columns: 1fr; } }
</style>
