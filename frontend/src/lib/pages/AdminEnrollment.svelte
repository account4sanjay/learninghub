<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import Avatar from '../components/Avatar.svelte'

  const GRADE_ORDER = ['Freshman', 'Sophomore', 'Junior', 'Senior']

  let roster = $state(null)
  let directory = $state({})
  let error = $state('')
  let search = $state('')

  async function load() {
    error = ''
    try {
      const [r, d] = await Promise.all([api.get('/api/enrollment/roster'), api.get('/api/users/directory').catch(() => [])])
      roster = r
      directory = Object.fromEntries((d ?? []).map((u) => [u.username, u]))
    } catch (e) {
      error = e.message
      roster = []
    }
  }
  load()

  const nameOf = (username) => directory[username]?.displayName ?? username
  const gradeName = (s) => s.gradeLevelName ?? s.gradeLevel ?? ''
  const teacherName = (s) => s.teacher ?? nameOf(s.teacherUsername)
  const timeOf = (s) => s.time ?? (s.startTime ? `${s.startTime} – ${s.endTime ?? ''}` : '')

  const filtered = $derived.by(() => {
    if (!roster) return []
    const q = search.trim().toLowerCase()
    if (!q) return roster
    return roster
      .map((s) => {
        const subjectHit = `${s.name} ${gradeName(s)} ${teacherName(s)}`.toLowerCase().includes(q)
        const students = (s.students ?? []).filter((st) => `${st.username} ${nameOf(st.username)}`.toLowerCase().includes(q))
        return subjectHit ? s : students.length ? { ...s, students } : null
      })
      .filter(Boolean)
  })

  const groups = $derived.by(() => {
    const byGrade = new Map()
    for (const s of filtered) {
      const g = gradeName(s)
      if (!byGrade.has(g)) byGrade.set(g, [])
      byGrade.get(g).push(s)
    }
    const order = (g) => {
      const i = GRADE_ORDER.indexOf(g)
      return i === -1 ? 99 : i
    }
    return [...byGrade.entries()]
      .sort((a, b) => order(a[0]) - order(b[0]) || a[0].localeCompare(b[0]))
      .map(([grade, subjects]) => ({ grade, subjects: subjects.slice().sort((a, b) => a.name.localeCompare(b.name)) }))
  })

  const totalEnrolments = $derived((roster ?? []).reduce((n, s) => n + (s.studentCount ?? s.students?.length ?? 0), 0))
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Enrollment</h1>
      <p class="page-sub">Current enrolment by subject: who is enrolled in each subject, grouped by grade level.</p>
    </div>
    <div class="totals">
      <div class="tot"><b>{roster?.length ?? 0}</b><span>subjects</span></div>
      <div class="tot"><b>{totalEnrolments}</b><span>enrolments</span></div>
    </div>
  </div>

  {#if error}<div class="banner err">{error}</div>{/if}

  <div class="card toolbar">
    <div class="search">
      <Icon name="search" size={15} />
      <input placeholder="Search by subject, teacher or student…" bind:value={search} />
    </div>
    <span class="count">{filtered.length} of {roster?.length ?? 0} subjects</span>
  </div>

  {#if roster === null}
    <div class="card list"><p class="loading">Loading enrolment…</p></div>
  {:else if groups.length === 0}
    <div class="card list"><p class="loading">No subjects match.</p></div>
  {:else}
    {#each groups as g (g.grade)}
      <div class="card group">
        <div class="group-head">
          <h3>{g.grade}</h3>
          <span class="muted">{g.subjects.length} subject{g.subjects.length === 1 ? '' : 's'} · {g.subjects.reduce((n, s) => n + (s.students?.length ?? 0), 0)} enrolments</span>
        </div>
        <table class="tbl">
          <thead>
            <tr><th>Subject</th><th>Teacher</th><th>When</th><th class="right">Students</th><th>Enrolled students</th></tr>
          </thead>
          <tbody>
            {#each g.subjects as s (s.id)}
              <tr>
                <td>
                  <div class="subj">
                    <span class="dot" style="background:var(--c-{s.color ?? 'blue'})"></span>
                    <div><div class="name">{s.name}</div><div class="code">{gradeName(s)}</div></div>
                  </div>
                </td>
                <td><div class="who"><Avatar name={teacherName(s)} username={s.teacherUsername} size={26} /> {teacherName(s)}</div></td>
                <td class="nowrap muted">
                  <div class="days">{#each s.days ?? [] as d}<span class="pill pill-blue day-pill">{d}</span>{/each}</div>
                  <div class="time">{timeOf(s)}</div>
                </td>
                <td class="right"><span class="pill pill-gray">{s.studentCount ?? s.students?.length ?? 0}</span></td>
                <td>
                  {#if (s.students ?? []).length === 0}
                    <span class="muted">No students enrolled</span>
                  {:else}
                    <div class="chips">
                      {#each s.students as st (st.username)}
                        <span class="chip" title={st.enrolledAt ? `Enrolled ${st.enrolledAt}` : st.username}>
                          <Avatar name={nameOf(st.username)} username={st.username} size={20} />
                          {nameOf(st.username)}
                        </span>
                      {/each}
                    </div>
                  {/if}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/each}
  {/if}
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
  .totals { display: flex; gap: 0.6rem; margin-left: auto; }
  .tot { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 0.5rem 0.9rem; min-width: 96px; display: flex; flex-direction: column; }
  .tot b { font-size: 1.25rem; line-height: 1.1; }
  .tot span { color: var(--text-2); font-size: 0.75rem; }
  .banner { padding: 0.6rem 0.9rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 0.8rem; }
  .banner.err { background: var(--t-red); color: #991b1b; }

  .toolbar { display: flex; align-items: center; gap: 0.8rem; padding: 0.6rem 0.9rem; flex-wrap: wrap; margin-bottom: 1rem; }
  .search { display: flex; align-items: center; gap: 0.5rem; color: var(--text-3); background: var(--bg); border: 1px solid var(--border); border-radius: 9px; padding: 0.4rem 0.7rem; flex: 1 1 260px; }
  .search input { border: none; outline: none; background: none; width: 100%; font-size: 0.85rem; color: var(--text); }
  .count { color: var(--text-3); font-size: 0.78rem; margin-left: auto; }

  .list { padding: 0; }
  .loading { padding: 1.25rem; color: var(--text-2); }
  .group { padding: 0; overflow-x: auto; margin-bottom: 1rem; }
  .group-head { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; padding: 0.8rem 1rem; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
  .group-head h3 { font-size: 0.95rem; font-weight: 700; }
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
  .tbl th { text-align: left; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-3); padding: 0.65rem 1rem; border-bottom: 1px solid var(--border); }
  .tbl td { padding: 0.65rem 1rem; border-bottom: 1px solid var(--border); vertical-align: top; }
  .tbl tr:last-child td { border-bottom: none; }
  .right { text-align: right; }
  .nowrap { white-space: nowrap; }
  .muted { color: var(--text-2); font-size: 0.8rem; }
  .subj { display: flex; align-items: center; gap: 0.6rem; }
  .dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
  .name { font-weight: 600; }
  .code { color: var(--text-3); font-size: 0.75rem; }
  .who { display: flex; align-items: center; gap: 0.5rem; white-space: nowrap; }
  .days { display: flex; gap: 0.25rem; flex-wrap: wrap; }
  .day-pill { padding: 0.12rem 0.45rem; font-size: 0.66rem; }
  .time { font-size: 0.78rem; margin-top: 0.2rem; }
  .chips { display: flex; flex-wrap: wrap; gap: 0.3rem; }
  .chip { display: inline-flex; align-items: center; gap: 0.35rem; background: var(--bg); border: 1px solid var(--border); border-radius: 999px; padding: 0.1rem 0.55rem 0.1rem 0.15rem; font-size: 0.76rem; white-space: nowrap; }
</style>
