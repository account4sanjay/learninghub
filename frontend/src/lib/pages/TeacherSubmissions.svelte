<script>
  import { api } from '../api.js'
  import { router, query } from '../router.svelte.js'
  import { primaryRole } from '../auth.svelte.js'
  import Icon from '../components/Icon.svelte'
  import Avatar from '../components/Avatar.svelte'

  const role = primaryRole()
  let rows = $state(null)
  let directory = $state({})
  let error = $state('')
  let search = $state('')
  let assessmentFilter = $state('')
  let selected = $state(null) // { attemptId, ...review }
  let loadingReview = $state(false)

  async function load() {
    error = ''
    try {
      const [r, d] = await Promise.all([api.get('/api/assessment/submissions'), api.get('/api/users/directory').catch(() => [])])
      rows = r
      directory = Object.fromEntries((d ?? []).map((u) => [u.username, u]))
    } catch (e) {
      error = e.message
      rows = []
    }
  }
  load()

  // ?assessment=<id> pre-filters and follows hash changes
  $effect(() => {
    void router.hash
    assessmentFilter = query().get('assessment') ?? ''
  })

  const nameOf = (u) => directory[u]?.displayName ?? u
  const assessments = $derived.by(() => {
    const m = new Map()
    for (const r of rows ?? []) if (!m.has(r.assessmentId)) m.set(r.assessmentId, r.title)
    return [...m.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  })
  const filtered = $derived.by(() => {
    if (!rows) return []
    const q = search.trim().toLowerCase()
    return rows.filter(
      (r) =>
        (!assessmentFilter || r.assessmentId === assessmentFilter) &&
        (!q || `${r.student} ${nameOf(r.student)} ${r.title} ${r.subject}`.toLowerCase().includes(q)),
    )
  })
  const scored = $derived(filtered.filter((r) => r.percent != null))
  const avg = $derived.by(() => {
    if (!scored.length) return null
    return Math.round(scored.reduce((n, r) => n + r.percent, 0) / scored.length)
  })
  const pending = $derived(filtered.filter((r) => r.status === 'Submitted').length)

  // Ask 20: grade an assignment submission
  let gradePoints = $state('')
  let gradeFeedback = $state('')
  let grading = $state(false)
  async function saveGrade() {
    const pts = Number(gradePoints)
    const max = selected?.submission?.points ?? selected?.row?.points ?? 100
    if (!Number.isFinite(pts) || pts < 0 || pts > max) {
      error = `Points must be between 0 and ${max}`
      return
    }
    grading = true
    error = ''
    try {
      await api.post(`/api/assessment/submissions/${selected.attemptId}/grade`, { points: pts, feedback: gradeFeedback })
      const keep = selected.attemptId
      await load()
      const r = rows.find((x) => x.attemptId === keep)
      selected = null
      if (r) await open(r)
    } catch (e) {
      error = e.message
    } finally {
      grading = false
    }
  }

  async function open(r) {
    if (selected?.attemptId === r.attemptId) {
      selected = null
      return
    }
    loadingReview = true
    try {
      const rv = await api.get(`/api/assessment/submissions/${r.attemptId}`)
      selected = { attemptId: r.attemptId, row: r, ...rv }
      gradePoints = rv.submission?.pointsAwarded ?? ''
      gradeFeedback = rv.submission?.feedback ?? ''
    } catch (e) {
      error = e.message
    } finally {
      loadingReview = false
    }
  }

  const scoreColor = (p) => (p >= 80 ? 'var(--c-green)' : p >= 60 ? 'var(--c-amber)' : 'var(--c-red)')
  const RESULT_PILL = { correct: 'green', incorrect: 'red', skipped: 'gray' }
  const STATUS_PILL = { Completed: 'green', Submitted: 'purple', Graded: 'blue' }
  const kb = (n) => (n >= 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB')
  const optionText = (q, key) => (q.options ?? []).find((o) => o.key === key)?.text ?? key
</script>

<div class="page">
  <div class="head">
    <div>
      <a class="back" href="#/assessments"><Icon name="chevron-left" size={14} /> Assessments</a>
      <h1 class="page-title">Submissions</h1>
      <p class="page-sub">{role === 'admin' ? "Every student's completed attempt across all teachers." : 'Completed attempts on your assessments. Click a row to review it question by question.'}</p>
    </div>
    <div class="totals">
      <div class="tot"><b>{filtered.length}</b><span>submissions</span></div>
      <div class="tot"><b>{avg == null ? '—' : avg + '%'}</b><span>average</span></div>
      <div class="tot" class:hot={pending > 0}><b>{pending}</b><span>to grade</span></div>
    </div>
  </div>

  {#if error}<div class="banner err">{error}</div>{/if}

  <div class="card toolbar">
    <div class="search">
      <Icon name="search" size={15} />
      <input placeholder="Search by student, assessment or subject…" bind:value={search} />
    </div>
    <select bind:value={assessmentFilter}>
      <option value="">All assessments</option>
      {#each assessments as [aid, title] (aid)}<option value={aid}>{title}</option>{/each}
    </select>
    <span class="count">{filtered.length} of {rows?.length ?? 0}</span>
  </div>

  <div class="layout" class:with-review={!!selected}>
    <div class="card list">
      {#if rows === null}
        <p class="loading">Loading submissions…</p>
      {:else if filtered.length === 0}
        <p class="loading">No submissions yet.</p>
      {:else}
        <table class="tbl">
          <thead>
            <tr><th>Student</th><th>Assessment</th><th>Subject</th><th>Status</th><th>Submitted</th><th class="right">Score</th><th class="right">Correct</th><th></th></tr>
          </thead>
          <tbody>
            {#each filtered as r (r.attemptId)}
              <tr class:sel={selected?.attemptId === r.attemptId} onclick={() => open(r)}>
                <td><div class="who"><Avatar name={nameOf(r.student)} username={r.student} size={26} /> <span>{nameOf(r.student)}</span></div></td>
                <td><div class="name">{r.title}</div><div class="code">{r.gradeLevel ?? ''}</div></td>
                <td class="muted">{r.subject}</td>
                <td><span class="pill pill-{STATUS_PILL[r.status] ?? 'gray'}">{r.status === 'Submitted' ? 'To grade' : r.status}</span>{#if r.autoSubmitted}<div class="code">auto-submitted</div>{/if}</td>
                <td class="muted nowrap">{r.completedOn}</td>
                <td class="right">{#if r.percent == null}<span class="muted">Pending</span>{:else}<b style="color:{scoreColor(r.percent)}">{r.score}</b>{/if}</td>
                <td class="right muted">{#if r.type === 'Assignment'}{r.pointsAwarded ?? '—'} / {r.points} pts{:else}{r.correct} / {r.total}{/if}</td>
                <td class="chev"><Icon name="chevron-right" size={15} /></td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </div>

    {#if selected}
      <div class="card review">
        <div class="rv-head">
          <div>
            <div class="rv-title">{selected.title ?? selected.row.title}</div>
            <div class="muted">{nameOf(selected.student ?? selected.row.student)} · {selected.completedOn ?? selected.row.completedOn}</div>
          </div>
          <button class="icon-btn" onclick={() => (selected = null)} aria-label="Close"><Icon name="x" size={14} /></button>
        </div>
        {#if loadingReview}
          <p class="loading">Loading…</p>
        {:else}
          {#if selected.submission}
            <div class="subm">
              <div class="rq-top"><span class="pill pill-{STATUS_PILL[selected.submission.status] ?? 'gray'}">{selected.submission.status}</span><span class="muted">{selected.submission.submittedAt}</span></div>
              {#if selected.submission.text}<p class="subm-text">{selected.submission.text}</p>{:else}<p class="muted">No written response.</p>{/if}
              {#each selected.submission.files as f}
                <div class="subm-file"><Icon name="doc" size={13} /> {f.name} <span class="muted">{kb(f.size)}</span></div>
              {/each}
              <div class="grade-form">
                <div class="gf-row">
                  <label>Points <input type="number" min="0" max={selected.submission.points} bind:value={gradePoints} /> <span class="muted">/ {selected.submission.points}</span></label>
                </div>
                <textarea rows="3" placeholder="Feedback for the student…" bind:value={gradeFeedback}></textarea>
                <button class="btn btn-primary" disabled={grading} onclick={saveGrade}>{grading ? 'Saving…' : selected.submission.status === 'Graded' ? 'Update Grade' : 'Save Grade'}</button>
                {#if selected.submission.status === 'Graded'}<div class="muted">Graded by {selected.submission.gradedBy} · {selected.submission.gradedAt}</div>{/if}
              </div>
            </div>
          {:else if selected.results}
            <div class="rv-stats">
              <div class="rs"><b style="color:{scoreColor(selected.results.percent ?? 0)}">{selected.results.score}</b><span>score</span></div>
              <div class="rs"><b>{selected.results.correct}</b><span>correct</span></div>
              <div class="rs"><b>{selected.results.incorrect}</b><span>incorrect</span></div>
              <div class="rs"><b>{selected.results.skipped}</b><span>skipped</span></div>
              <div class="rs"><b>{selected.results.timeTaken ?? '—'}</b><span>time</span></div>
            </div>
          {/if}
          {#each selected.submission ? [] : selected.reviewQuestions ?? [] as q (q.number)}
            {@const res = (q.result ?? '').toLowerCase()}
            <div class="rq">
              <div class="rq-top">
                <span class="rq-num">Q{q.number}</span>
                <span class="pill pill-{RESULT_PILL[res] ?? 'gray'}">{q.result ?? '—'}</span>
                <span class="muted">{q.pointsLabel ?? ''}</span>
              </div>
              <div class="rq-text">{q.text}</div>
              <div class="rq-ans">
                <div><span class="lbl">Student</span> {q.chosen ? `${q.chosen} · ${optionText(q, q.chosen)}` : 'no answer'}</div>
                <div><span class="lbl">Correct</span> {q.correct} · {optionText(q, q.correct)}</div>
              </div>
              {#if q.explanation}<div class="rq-exp">{q.explanation}</div>{/if}
            </div>
          {:else}
            {#if !selected.submission}<p class="muted">No question detail available for this submission.</p>{/if}
          {/each}
        {/if}
      </div>
    {/if}
  </div>
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
  .back { display: inline-flex; align-items: center; gap: 0.2rem; font-size: 0.78rem; color: var(--text-2); margin-bottom: 0.3rem; }
  .totals { display: flex; gap: 0.6rem; margin-left: auto; }
  .tot { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 0.5rem 0.9rem; min-width: 96px; display: flex; flex-direction: column; }
  .tot b { font-size: 1.25rem; line-height: 1.1; }
  .tot span { color: var(--text-2); font-size: 0.75rem; }
  .banner { padding: 0.6rem 0.9rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 0.8rem; }
  .banner.err { background: var(--t-red); color: #991b1b; }

  .toolbar { display: flex; align-items: center; gap: 0.8rem; padding: 0.6rem 0.9rem; flex-wrap: wrap; margin-bottom: 1rem; }
  .search { display: flex; align-items: center; gap: 0.5rem; color: var(--text-3); background: var(--bg); border: 1px solid var(--border); border-radius: 9px; padding: 0.4rem 0.7rem; flex: 1 1 260px; }
  .search input { border: none; outline: none; background: none; width: 100%; font-size: 0.85rem; color: var(--text); }
  .toolbar select { border: 1px solid var(--border); border-radius: 9px; padding: 0.4rem 0.6rem; font: inherit; font-size: 0.82rem; color: var(--text); background: #fff; }
  .count { color: var(--text-3); font-size: 0.78rem; margin-left: auto; }

  .layout { display: grid; grid-template-columns: 1fr; gap: 1rem; align-items: start; }
  .layout.with-review { grid-template-columns: 1.15fr 1fr; }
  .list { padding: 0; overflow-x: auto; }
  .loading { padding: 1.25rem; color: var(--text-2); }
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
  .tbl th { text-align: left; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-3); padding: 0.65rem 1rem; border-bottom: 1px solid var(--border); }
  .tbl td { padding: 0.6rem 1rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .tbl tbody tr { cursor: pointer; }
  .tbl tbody tr:hover td { background: var(--bg); }
  .tbl tr.sel td { background: var(--t-blue); }
  .tbl tr:last-child td { border-bottom: none; }
  .right { text-align: right; }
  .nowrap { white-space: nowrap; }
  .muted { color: var(--text-2); font-size: 0.8rem; }
  .who { display: flex; align-items: center; gap: 0.5rem; white-space: nowrap; }
  .name { font-weight: 600; }
  .code { color: var(--text-3); font-size: 0.75rem; }
  .chev { color: var(--text-3); width: 24px; }

  .review { padding: 1rem 1.1rem; position: sticky; top: 4rem; max-height: calc(100vh - 5rem); overflow-y: auto; }
  .rv-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 0.6rem; margin-bottom: 0.7rem; }
  .rv-title { font-weight: 700; font-size: 0.95rem; }
  .icon-btn { width: 28px; height: 28px; border-radius: 8px; border: 1px solid var(--border); background: #fff; color: var(--text-2); display: grid; place-items: center; cursor: pointer; flex-shrink: 0; }
  .rv-stats { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.8rem; }
  .rs { flex: 1; min-width: 70px; border: 1px solid var(--border); border-radius: 10px; padding: 0.45rem 0.6rem; display: flex; flex-direction: column; align-items: center; }
  .rs b { font-size: 1rem; }
  .rs span { color: var(--text-3); font-size: 0.68rem; }
  .rq { border-top: 1px solid var(--border); padding: 0.7rem 0; }
  .rq-top { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.3rem; }
  .rq-num { font-weight: 700; font-size: 0.78rem; color: var(--primary); }
  .rq-text { font-size: 0.84rem; font-weight: 600; margin-bottom: 0.35rem; }
  .rq-ans { display: grid; gap: 0.2rem; font-size: 0.8rem; }
  .lbl { display: inline-block; width: 60px; color: var(--text-3); font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; }
  .tot.hot b { color: var(--c-purple, #7c3aed); }
  .subm { border-top: 1px solid var(--border); padding-top: 0.6rem; }
  .subm-text { font-size: 0.84rem; line-height: 1.5; white-space: pre-wrap; margin: 0.4rem 0; }
  .subm-file { display: flex; align-items: center; gap: 6px; font-size: 0.78rem; padding: 0.2rem 0; }
  .grade-form { margin-top: 0.8rem; border-top: 1px solid var(--border); padding-top: 0.7rem; display: flex; flex-direction: column; gap: 0.5rem; }
  .gf-row label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.8rem; font-weight: 600; }
  .gf-row input { width: 90px; border: 1px solid var(--border); border-radius: 8px; padding: 0.35rem 0.5rem; font: inherit; }
  .grade-form textarea { border: 1px solid var(--border); border-radius: 8px; padding: 0.45rem 0.6rem; font: inherit; font-size: 0.8rem; resize: vertical; }
  .rq-exp { margin-top: 0.35rem; font-size: 0.76rem; color: var(--text-2); background: var(--bg); border-radius: 8px; padding: 0.4rem 0.6rem; }
  @media (max-width: 1100px) { .layout.with-review { grid-template-columns: 1fr; } .review { position: static; max-height: none; } }
</style>
