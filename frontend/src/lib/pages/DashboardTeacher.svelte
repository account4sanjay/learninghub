<script>
  import { api } from '../api.js'
  import { auth } from '../auth.svelte.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'

  let dash = $state(null)
  let today = $state(null)
  let week = $state(null)

  $effect(() => {
    api.get('/api/dashboard/analytics').then((d) => (dash = d)).catch(() => {})
    api.get('/api/dashboard/today').then((d) => (today = d)).catch(() => {})
    api.get('/api/schedule/week').then((d) => (week = d)).catch(() => {})
  })

  const STAT_ICONS = { classes: 'classes', students: 'grad', assessments: 'assessments', submissions: 'doc' }
  const STAT_FALLBACK = ['classes', 'grad', 'assessments', 'doc']
  const statIcon = (s, i) => STAT_ICONS[s.icon] ?? s.icon ?? STAT_FALLBACK[i % STAT_FALLBACK.length]
  // tiles link to the matching page
  const TILE_LINKS = [
    [/class/i, '#/my-classes'],
    [/student/i, '#/students'],
    [/submission/i, '#/assessments/submissions'],
    [/assessment/i, '#/assessments'],
  ]
  const tileHref = (label) => TILE_LINKS.find(([re]) => re.test(label ?? ''))?.[1] ?? null
  const myClasses = $derived(dash?.myClasses ?? [])
  const upcoming = $derived((week?.upcoming ?? []).slice(0, 5))
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Good morning, {dash?.greetingName ?? auth.user?.firstName}! 👋</h1>
      <p class="page-sub">Your classes, students and assessments at a glance.</p>
    </div>
    <div class="date">{dash?.date ?? ''}</div>
  </div>

  {#if dash}
    <div class="stats">
      {#each dash.stats as s, i}
        <StatCard icon={statIcon(s, i)} color={s.color ?? 'blue'} value={s.value} label={s.label} sub={s.sub ?? ''} href={tileHref(s.label)} />
      {/each}
    </div>
  {/if}

  <div class="cols">
    <div class="main">
      <!-- Classes I teach -->
      <div class="card section">
        <div class="card-head">
          <h3>Classes I Teach</h3>
          <a class="link" href="#/my-classes">View All</a>
        </div>
        <table class="tbl">
          <thead>
            <tr><th>Class</th><th>Days</th><th>Time</th><th class="num">Students</th></tr>
          </thead>
          <tbody>
            {#each myClasses as c (c.id)}
              <tr>
                <td>
                  <span class="dot" style="background:var(--c-{c.color ?? 'blue'})"></span>
                  <span class="strong">{c.name}</span>
                  <span class="muted"> · {c.gradeLevel}</span>
                </td>
                <td class="muted">{(c.days ?? []).join(', ')}</td>
                <td class="muted nowrap">{c.time}</td>
                <td class="num strong">{c.studentCount ?? 0}</td>
              </tr>
            {:else}
              <tr><td colspan="4" class="empty">{dash ? 'No classes assigned yet.' : 'Loading…'}</td></tr>
            {/each}
          </tbody>
        </table>
      </div>

      <!-- Upcoming classes -->
      <div class="card section">
        <div class="card-head">
          <h3>Upcoming Classes</h3>
          <a class="link" href="#/schedule">View Schedule</a>
        </div>
        <table class="tbl">
          <thead>
            <tr><th>Date</th><th>Time</th><th>Subject</th></tr>
          </thead>
          <tbody>
            {#each upcoming as u}
              <tr>
                <td class="strong nowrap">{u.when}</td>
                <td class="muted nowrap">{u.time}</td>
                <td><span class="dot" style="background:var(--c-{u.color})"></span><span class="strong">{u.subject}</span></td>
              </tr>
            {:else}
              <tr><td colspan="3" class="empty">No upcoming classes.</td></tr>
            {/each}
          </tbody>
        </table>
      </div>
    </div>

    <div class="rail">
      <!-- Today's classes -->
      <div class="card section">
        <div class="card-head">
          <h3>Today's Classes</h3>
          <a class="link" href="#/schedule">View Calendar</a>
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
                  <div class="ts-det">{t.detail}</div>
                </div>
                <span class="chev"><Icon name="chevron-right" size={14} /></span>
              </a>
            </div>
          {:else}
            <div class="empty">{today ? 'No classes today.' : 'Loading…'}</div>
          {/each}
        </div>
      </div>


      <div class="card section quick">
        <div class="card-head"><h3>Quick Links</h3></div>
        <a class="ql" href="#/assessments"><Icon name="assessments" size={15} /> My assessments &amp; submissions</a>
        <a class="ql" href="#/learning"><Icon name="learning" size={15} /> My course content</a>
        <a class="ql" href="#/profile"><Icon name="profile" size={15} /> My profile</a>
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
  .nowrap { white-space: nowrap; }
  .empty { color: var(--text-3); font-size: 0.8rem; padding: 0.6rem 0.25rem; }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 7px; vertical-align: 1px; }
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
  .quick .ql { display: flex; align-items: center; gap: 0.5rem; padding: 0.45rem 0; font-size: 0.83rem; font-weight: 600; color: var(--text); text-decoration: none; border-top: 1px solid var(--border); }
  .quick .ql:first-of-type { border-top: none; }
  .quick .ql:hover { color: var(--primary); }
  @media (max-width: 1000px) { .cols { flex-direction: column; } .rail { width: 100%; } }
</style>
