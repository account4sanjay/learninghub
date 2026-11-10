<script>
  import { api } from '../api.js'
  import { auth } from '../auth.svelte.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'
  import Donut from '../components/Donut.svelte'

  let dash = $state(null)
  let today = $state(null)

  $effect(() => {
    api.get('/api/dashboard/analytics').then((d) => (dash = d)).catch(() => {})
    api.get('/api/dashboard/today').then((d) => (today = d)).catch(() => {})
  })

  // dashboard.json icon names -> Icon.svelte names
  const STAT_ICONS = { classes: 'classes', progress: 'chart', assignments: 'doc', completed: 'check' }

  // Recent assessments: the next open items from assessment-service; upcoming classes: next meetings from schedule-service
  let assessments = $state(null)
  let week = $state(null)
  $effect(() => {
    api.get('/api/assessment/list').then((d) => (assessments = d)).catch(() => {})
    api.get('/api/schedule/week').then((d) => (week = d)).catch(() => {})
  })
  const TYPE_ICON = { Quiz: 'edit', Test: 'doc', Assignment: 'calc', Practice: 'practice' }
  const STATUS_PILL = { Upcoming: 'orange', Overdue: 'red', Completed: 'green' }
  const recentAssessments = $derived(
    (assessments ?? [])
      .filter((a) => a.status !== 'Completed')
      .slice(0, 3)
      .map((a) => ({ title: a.title, subject: a.subject, due: a.due, status: a.status === 'Upcoming' ? 'Pending' : a.status, pill: STATUS_PILL[a.status] ?? 'gray', icon: TYPE_ICON[a.type] ?? 'doc', color: a.color })),
  )
  const upcomingClasses = $derived(
    (week?.upcoming ?? []).map((u) => ({ date: u.when, time: u.time, subject: u.subject, color: u.color, teacher: u.teacher })),
  )
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Good morning, {auth.user?.firstName}! 👋</h1>
      <p class="page-sub">Here's your learning progress and upcoming activities.</p>
    </div>
    <div class="date">{dash?.date ?? 'Mon, Sep 15, 2026'}</div>
  </div>

  {#if dash}
    <div class="stats">
      {#each dash.stats as s}
        <StatCard icon={STAT_ICONS[s.icon] ?? s.icon} color={s.color} value={s.value} label={s.label} />
      {/each}
    </div>
  {/if}

  <div class="cols">
    <div class="main">
      <!-- Learning Progress -->
      <div class="card section">
        <div class="card-head">
          <h3>Learning Progress</h3>
          <a class="link" href="#/learning">View Details</a>
        </div>
        {#if dash}
          <div class="progress-body">
            <Donut percent={dash.learningProgress.overall} size={150} stroke={16} label="Overall Progress" />
            <div class="bars">
              {#each dash.learningProgress.subjects as s}
                <div class="bar-row">
                  <div class="bar-name">{s.name}</div>
                  <div class="bar-track">
                    <div class="bar-fill" style="width:{s.percent}%; background:var(--c-{s.color})"></div>
                  </div>
                  <div class="bar-pct">{s.percent}%</div>
                </div>
              {/each}
            </div>
          </div>
        {/if}
      </div>

      <!-- Recent Assessments -->
      <div class="card section">
        <div class="card-head">
          <h3>Recent Assessments</h3>
          <a class="link" href="#/assessments">View All</a>
        </div>
        <div class="ra-list">
          {#each recentAssessments as a}
            <a class="ra-row" href="#/assessments">
              <div class="icon-sq" style="background:var(--t-{a.color}); color:var(--c-{a.color})">
                <Icon name={a.icon} size={18} />
              </div>
              <div class="ra-body">
                <div class="ra-title">{a.title}</div>
                <div class="ra-sub">{a.subject}</div>
              </div>
              <div class="ra-due">Due {a.due}</div>
              <span class="pill pill-{a.pill}">{a.status}</span>
              <span class="chev"><Icon name="chevron-right" size={16} /></span>
            </a>
          {/each}
        </div>
      </div>

      <!-- Upcoming Classes -->
      <div class="card section">
        <div class="card-head">
          <h3>Upcoming Classes</h3>
          <a class="link" href="#/schedule">View All</a>
        </div>
        <table class="uc-table">
          <thead>
            <tr><th>Date</th><th>Time</th><th>Subject</th><th>Teacher</th></tr>
          </thead>
          <tbody>
            {#each upcomingClasses as c}
              <tr>
                <td class="uc-date">{c.date}</td>
                <td>{c.time}</td>
                <td>
                  <span class="dot" style="background:var(--c-{c.color})"></span>
                  <span class="uc-subject">{c.subject}</span>
                </td>
                <td>{c.teacher}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    </div>

    <div class="rail">
      <!-- Today's Schedule -->
      <div class="card section">
        <div class="card-head">
          <h3>Today's Schedule</h3>
          <a class="link" href="#/schedule">View Calendar</a>
        </div>
        <div class="ts-list">
          {#if today}
            {#each today as t}
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
            {/each}
          {/if}
        </div>
      </div>


      <!-- Payment Status -->
      <div class="card section">
        <div class="card-head">
          <h3>Payment Status</h3>
          <a class="link" href="#/payments">View Details</a>
        </div>
        <div class="pay-banner">
          <span class="pay-check"><Icon name="check" size={13} /></span>
          <div>
            <div class="pay-ok">All payments up to date</div>
            <div class="pay-next">Next payment due: Oct 1, 2026</div>
          </div>
        </div>
        <div class="pay-stats">
          <div class="pay-stat">
            <div class="pay-val">$0.00</div>
            <div class="pay-lbl">Amount Due</div>
          </div>
          <div class="pay-stat">
            <div class="pay-val">$320.00</div>
            <div class="pay-lbl">Next Payment</div>
          </div>
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

  /* Learning progress */
  .progress-body { display: flex; gap: 2rem; align-items: center; }
  .bars { flex: 1; display: grid; gap: 0.75rem; }
  .bar-row { display: flex; align-items: center; gap: 0.9rem; }
  .bar-name { width: 130px; font-size: 0.82rem; font-weight: 600; flex-shrink: 0; }
  .bar-track { flex: 1; height: 7px; background: #eef1f6; border-radius: 4px; overflow: hidden; }
  .bar-fill { height: 100%; border-radius: 4px; }
  .bar-pct { width: 38px; text-align: right; font-size: 0.8rem; font-weight: 600; color: var(--text-2); }

  /* Recent assessments */
  .ra-list { display: grid; }
  .ra-row { display: flex; align-items: center; gap: 0.85rem; padding: 0.7rem 0.25rem; border-top: 1px solid var(--border); text-decoration: none; color: inherit; }
  .ra-row:first-child { border-top: none; }
  .ra-row:hover { background: #f8fafd; }
  .icon-sq { width: 38px; height: 38px; border-radius: 10px; display: grid; place-items: center; flex-shrink: 0; }
  .ra-body { flex: 1; min-width: 0; }
  .ra-title { font-weight: 600; font-size: 0.88rem; }
  .ra-sub { color: var(--text-2); font-size: 0.76rem; margin-top: 1px; }
  .ra-due { color: var(--text-2); font-size: 0.78rem; white-space: nowrap; }
  .chev { color: var(--text-3); display: inline-flex; }

  /* Upcoming classes */
  .uc-table { width: 100%; border-collapse: collapse; font-size: 0.83rem; }
  .uc-table th { text-align: left; color: var(--text-2); font-size: 0.72rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.03em; padding: 0.4rem 0.5rem 0.55rem; border-bottom: 1px solid var(--border); }
  .uc-table td { padding: 0.65rem 0.5rem; border-bottom: 1px solid var(--border); color: var(--text-2); }
  .uc-table tbody tr:last-child td { border-bottom: none; }
  .uc-date { font-weight: 600; color: var(--text) !important; }
  .uc-subject { font-weight: 600; color: var(--text); }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 7px; vertical-align: 1px; }

  /* Today's schedule */
  .ts-list { display: grid; gap: 0.6rem; }
  .ts-row { display: flex; align-items: center; gap: 0.6rem; }
  .ts-time { width: 52px; flex-shrink: 0; font-size: 0.68rem; font-weight: 600; color: var(--text-2); }
  .ts-end { color: var(--text-3); }
  .ts-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
  .ts-item { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: space-between; gap: 0.4rem; border-radius: 10px; padding: 0.6rem 0.7rem; text-decoration: none; color: inherit; }
  .ts-sub { font-weight: 700; font-size: 0.83rem; }
  .ts-det { font-size: 0.7rem; color: var(--text-2); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .ts-info { min-width: 0; }

  /* Payment status */
  .pay-banner { display: flex; gap: 0.65rem; align-items: flex-start; background: var(--t-green); border-radius: 10px; padding: 0.75rem 0.85rem; }
  .pay-check { width: 20px; height: 20px; border-radius: 50%; background: var(--c-green); color: #fff; display: grid; place-items: center; flex-shrink: 0; margin-top: 1px; }
  .pay-ok { font-weight: 700; font-size: 0.83rem; }
  .pay-next { font-size: 0.74rem; color: var(--text-2); margin-top: 2px; }
  .pay-stats { display: flex; gap: 0.8rem; margin-top: 0.8rem; }
  .pay-stat { flex: 1; border: 1px solid var(--border); border-radius: 10px; padding: 0.65rem; text-align: center; }
  .pay-val { font-weight: 700; font-size: 1rem; }
  .pay-lbl { color: var(--text-2); font-size: 0.72rem; margin-top: 2px; }

  @media (max-width: 1000px) {
    .cols { flex-direction: column; }
    .rail { width: 100%; }
  }
</style>
