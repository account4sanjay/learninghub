<script>
  import { api } from '../api.js'
  import { auth } from '../auth.svelte.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'

  let dash = $state(null)
  let today = $state(null)

  $effect(() => {
    api.get('/api/dashboard/analytics').then((d) => (dash = d)).catch(() => {})
    api.get('/api/dashboard/today').then((d) => (today = d)).catch(() => {})
  })

  const STAT_ICONS = { students: 'grad', teachers: 'profile', subjects: 'book', enrollments: 'classes' }
  const STAT_FALLBACK = ['grad', 'profile', 'book', 'classes']
  const statIcon = (s, i) => STAT_ICONS[s.icon] ?? s.icon ?? STAT_FALLBACK[i % STAT_FALLBACK.length]
  // tiles are links: Students / Teachers → Users (filtered), Subjects → Subjects, Enrollment → Enrollment view
  const TILE_LINKS = { students: '#/admin/users?role=student', teachers: '#/admin/users?role=teacher', subjects: '#/admin/subjects', enrollment: '#/admin/enrollment', enrollments: '#/admin/enrollment' }
  const tileHref = (s) => TILE_LINKS[(s.label ?? '').toLowerCase()] ?? null
  const subjects = $derived(dash?.subjects ?? [])
  const perf = $derived(dash?.performance ?? [])
  const roles = $derived.by(() => {
    const u = dash?.usersByRole ?? {}
    const rows = [
      { label: 'Students', n: u.student ?? 0, color: 'blue' },
      { label: 'Teachers', n: u.teacher ?? 0, color: 'purple' },
      { label: 'Admins', n: u.admin ?? 0, color: 'red' },
    ]
    const max = Math.max(1, ...rows.map((r) => r.n))
    return rows.map((r) => ({ ...r, pct: Math.round((100 * r.n) / max) }))
  })
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Good morning, {dash?.greetingName ?? auth.user?.firstName}! 👋</h1>
      <p class="page-sub">School-wide overview: people, subjects, enrolments and performance.</p>
    </div>
    <div class="date">{dash?.date ?? ''}</div>
  </div>

  {#if dash}
    <div class="stats">
      {#each dash.stats as s, i}
        <StatCard icon={statIcon(s, i)} color={s.color ?? 'blue'} value={s.value} label={s.label} sub={s.sub ?? ''} href={tileHref(s)} />
      {/each}
    </div>
  {/if}

  <div class="cols">
    <div class="main">
      <!-- Performance by subject -->
      <div class="card section">
        <div class="card-head">
          <h3>Performance by Subject</h3>
          <a class="link" href="#/assessments">All Assessments</a>
        </div>
        {#if perf.length}
          <div class="bars">
            {#each perf as p}
              <div class="bar-row">
                <div class="bar-name">{p.subject}</div>
                <div class="bar-track"><div class="bar-fill" style="width:{p.percent}%; background:var(--c-{p.color})"></div></div>
                <div class="bar-pct">{p.percent}%</div>
              </div>
            {/each}
          </div>
        {:else}
          <div class="empty">No completed assessments yet.</div>
        {/if}
      </div>
      <!-- Subjects -->
      <div class="card section">
        <div class="card-head">
          <h3>Subjects</h3>
          <a class="link" href="#/admin/subjects">Manage Subjects</a>
        </div>
        <table class="tbl">
          <thead>
            <tr><th>Subject</th><th>Grade level</th><th>Teacher</th><th class="num">Students</th></tr>
          </thead>
          <tbody>
            {#each subjects as s}
              <tr>
                <td><span class="dot" style="background:var(--c-{s.color ?? 'blue'})"></span><span class="strong">{s.name}</span></td>
                <td class="muted">{s.gradeLevel}</td>
                <td class="muted">{s.teacher}</td>
                <td class="num strong">{s.studentCount ?? 0}</td>
              </tr>
            {:else}
              <tr><td colspan="4" class="empty">{dash ? 'No subjects yet.' : 'Loading…'}</td></tr>
            {/each}
          </tbody>
        </table>
      </div>

    </div>

    <div class="rail">
      <!-- Users by role -->
      <div class="card section">
        <div class="card-head">
          <h3>Users by Role</h3>
          <a class="link" href="#/admin/users">Manage Users</a>
        </div>
        <div class="bars">
          {#each roles as r}
            <div class="bar-row">
              <div class="bar-name">{r.label}</div>
              <div class="bar-track"><div class="bar-fill" style="width:{r.pct}%; background:var(--c-{r.color})"></div></div>
              <div class="bar-pct">{r.n}</div>
            </div>
          {/each}
        </div>
      </div>

      <!-- Today across the school -->
      <div class="card section">
        <div class="card-head">
          <h3>Today's Classes</h3>
          <a class="link" href="#/schedule">All Schedule</a>
        </div>
        <div class="ts-list">
          {#each today ?? [] as t}
            <div class="ts-row">
              <div class="ts-time">
                <div>{t.start}</div>
                <div class="ts-end">{t.end}</div>
              </div>
              <div class="ts-dot" style="background:var(--c-{t.color})"></div>
              <a class="ts-item" style="background:var(--t-{t.color})" href="#/schedule">
                <div class="ts-info">
                  <div class="ts-sub">{t.subject}</div>
                  <div class="ts-det">{t.teacher ? `${t.teacher} · ` : ''}{t.detail}</div>
                </div>
                <span class="chev"><Icon name="chevron-right" size={14} /></span>
              </a>
            </div>
          {:else}
            <div class="empty">{today ? 'No classes today.' : 'Loading…'}</div>
          {/each}
        </div>
      </div>

    </div>
  </div>
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.2rem; }
  .date { color: var(--text-2); font-size: 0.85rem; font-weight: 600; margin-top: 0.35rem; }
  .stats { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.2rem; }
  .cols { display: flex; gap: 1.2rem; align-items: flex-start; }
  .main { flex: 1; min-width: 0; }
  .rail { width: 320px; flex-shrink: 0; }
  .section { padding: 1.1rem 1.25rem; margin-bottom: 1.2rem; }
  .card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.9rem; }
  .card-head h3 { font-size: 1rem; font-weight: 700; }
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.83rem; }
  .tbl th { text-align: left; color: var(--text-2); font-size: 0.72rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.03em; padding: 0.4rem 0.5rem 0.55rem; border-bottom: 1px solid var(--border); }
  .tbl td { padding: 0.65rem 0.5rem; border-bottom: 1px solid var(--border); }
  .tbl tbody tr:last-child td { border-bottom: none; }
  .num { text-align: right; }
  .strong { font-weight: 600; color: var(--text); }
  .muted { color: var(--text-2); }
  .empty { color: var(--text-3); font-size: 0.8rem; padding: 0.6rem 0.25rem; }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 7px; vertical-align: 1px; }
  .bars { display: grid; gap: 0.75rem; }
  .bar-row { display: flex; align-items: center; gap: 0.9rem; }
  .bar-name { width: 110px; font-size: 0.82rem; font-weight: 600; flex-shrink: 0; }
  .bar-track { flex: 1; height: 7px; background: #eef1f6; border-radius: 4px; overflow: hidden; }
  .bar-fill { height: 100%; border-radius: 4px; }
  .bar-pct { width: 38px; text-align: right; font-size: 0.8rem; font-weight: 600; color: var(--text-2); }
  .ts-list { display: grid; gap: 0.6rem; }
  .ts-row { display: flex; align-items: center; gap: 0.6rem; }
  .ts-time { width: 52px; flex-shrink: 0; font-size: 0.68rem; font-weight: 600; color: var(--text-2); }
  .ts-end { color: var(--text-3); }
  .ts-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
  .ts-item { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: space-between; gap: 0.4rem; border-radius: 10px; padding: 0.6rem 0.7rem; text-decoration: none; color: inherit; }
  .ts-sub { font-weight: 700; font-size: 0.83rem; }
  .ts-det { font-size: 0.7rem; color: var(--text-2); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .ts-info { min-width: 0; }
  .chev { color: var(--text-3); display: inline-flex; }
  @media (max-width: 1000px) { .cols { flex-direction: column; } .rail { width: 100%; } }
</style>
