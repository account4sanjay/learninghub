<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'
  import { primaryRole } from '../auth.svelte.js'
  import Icon from '../components/Icon.svelte'
  import Avatar from '../components/Avatar.svelte'

  let { id } = $props()

  const isStudent = primaryRole() === 'student'
  let d = $state(null)
  let review = $state(null) // the student's own submission in the teacher-review shape (GET /{id}/review)
  // Ask 20: live countdown to the due date (assignments) / to the attempt deadline (in-progress exam)
  let secondsToDue = $state(null)
  let attemptLeft = $state(null)
  // assignment submission form
  let text = $state('')
  let files = $state([])
  let sending = $state(false)
  let sent = $state(null)
  let error = $state('')

  async function load() {
    const v = await api.get('/api/assessment/' + id)
    d = v
    secondsToDue = v.status?.secondsToDue ?? null
    attemptLeft = v.attempt?.secondsLeft ?? null
    text = v.submission?.text ?? ''
    files = (v.submission?.files ?? []).map((f) => ({ ...f }))
    review = isStudent && v.submission?.submitted ? await api.get(`/api/assessment/${id}/review`).catch(() => null) : null
  }
  const RESULT_PILL = { correct: 'green', incorrect: 'red', skipped: 'gray' }
  const optionText = (q, key) => (q.options ?? []).find((o) => o.key === key)?.text ?? key
  const scoreColor = (p) => (p >= 80 ? 'var(--c-green)' : p >= 60 ? 'var(--c-amber)' : 'var(--c-red)')
  $effect(() => {
    void id
    load()
  })
  $effect(() => {
    const t = setInterval(() => {
      if (secondsToDue != null && secondsToDue > 0) secondsToDue -= 1
      if (attemptLeft != null && attemptLeft > 0) attemptLeft -= 1
    }, 1000)
    return () => clearInterval(t)
  })

  const isAssignment = $derived(d?.type === 'Assignment')
  const st = $derived(d?.status ?? {})
  const sub = $derived(d?.submission ?? {})
  const dueClock = $derived.by(() => {
    if (secondsToDue == null) return ''
    if (secondsToDue <= 0) return 'Closed'
    const dd = Math.floor(secondsToDue / 86400), h = Math.floor((secondsToDue % 86400) / 3600)
    const m = Math.floor((secondsToDue % 3600) / 60), sec = secondsToDue % 60
    const p = (n) => String(n).padStart(2, '0')
    return (dd > 0 ? `${dd}d ` : '') + `${p(h)}:${p(m)}:${p(sec)}`
  })
  const attemptClock = $derived(attemptLeft == null ? '' : `${Math.floor(attemptLeft / 60)}:${String(attemptLeft % 60).padStart(2, '0')}`)
  const STATUS_PILL = { Upcoming: 'blue', 'In Progress': 'amber', Submitted: 'purple', Completed: 'green', Overdue: 'red' }
  const pill = $derived(STATUS_PILL[st.status] || 'gray')

  // header button: what the student can do right now
  const headAction = $derived.by(() => {
    if (!isStudent || !d) return null
    switch (st.action) {
      case 'Start':
        return { label: d.type === 'Test' ? 'Start Exam' : `Start ${d.type}`, primary: true, go: () => navigate(`#/assessments/${id}/take`) }
      case 'Resume':
        return { label: 'Resume Attempt', primary: true, go: () => navigate(`#/assessments/${id}/take`) }
      case 'Review':
        return st.canRetake
          ? { label: 'Retake', primary: true, go: () => navigate(`#/assessments/${id}/take`), alt: { label: 'Review Results', go: () => navigate(`#/assessments/${id}/review`) } }
          : { label: 'Review Results', primary: false, go: () => navigate(`#/assessments/${id}/review`) }
      case 'Submit':
        return { label: 'Submit Assignment', primary: true, go: () => document.getElementById('submission')?.scrollIntoView({ behavior: 'smooth' }) }
      case 'View Submission':
        return { label: 'View Submission', primary: false, go: () => document.getElementById('submission')?.scrollIntoView({ behavior: 'smooth' }) }
      default:
        return null
    }
  })

  function pickFiles(e) {
    const picked = [...(e.target.files ?? [])].map((f) => ({ name: f.name, size: f.size, type: f.type }))
    const names = new Set(files.map((f) => f.name))
    files = [...files, ...picked.filter((f) => !names.has(f.name))]
    e.target.value = ''
  }
  function dropFiles(e) {
    e.preventDefault()
    const picked = [...(e.dataTransfer?.files ?? [])].map((f) => ({ name: f.name, size: f.size, type: f.type }))
    const names = new Set(files.map((f) => f.name))
    files = [...files, ...picked.filter((f) => !names.has(f.name))]
  }
  const removeFile = (name) => (files = files.filter((f) => f.name !== name))
  const kb = (n) => (n >= 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB')

  async function submitAssignment() {
    if (sending) return
    if (!text.trim() && files.length === 0) {
      error = 'Write a response or attach at least one file.'
      return
    }
    if (sub.submitted && !confirm('Replace your previous submission?')) return
    sending = true
    error = ''
    try {
      sent = await api.post(`/api/assessment/${id}/assignment`, { text: text.trim(), files })
      await load()
    } catch (e) {
      error = e.message
    } finally {
      sending = false
    }
  }

  const FILE_COLOR = { pdf: 'red', doc: 'blue', video: 'purple', link: 'teal' }
</script>

<div class="page">
  <button class="back link" onclick={() => navigate('#/assessments')}>
    <Icon name="chevron-left" size={14} /> Back to Assessments
  </button>

  {#if d}
    <div class="card head-card">
      <div class="head-ic"><Icon name="doc" size={24} /></div>
      <div class="head-body">
        <div class="head-title">
          <h1>{d.title}</h1>
          <span class="pill pill-{pill}">{st.status}</span>
          {#if st.status === 'Overdue'}<span class="pill pill-red">Closed</span>{/if}
        </div>
        <div class="head-sub">{d.subject} · {d.type} · {d.gradeLevel ?? d.clazz} · {d.teacher}</div>
        <div class="head-due">
          <Icon name="clock" size={13} /> Due {d.due}
          {#if !isAssignment}<span class="sep">·</span> {st.timeLimit} time limit{/if}
          {#if st.status === 'In Progress'}<span class="sep">·</span> <b class="warn">{attemptClock} left on your attempt</b>{/if}
        </div>
      </div>
      {#if headAction}
        <div class="head-actions">
          {#if headAction.alt}<button class="btn btn-outline" onclick={headAction.alt.go}>{headAction.alt.label}</button>{/if}
          <button class="btn {headAction.primary ? 'btn-primary' : 'btn-outline'}" onclick={headAction.go}>{headAction.label}</button>
        </div>
      {:else if isStudent && st.status === 'Overdue'}
        <div class="closed-note"><Icon name="clock" size={14} /> The due date has passed. This {isAssignment ? 'assignment can no longer be submitted' : d.type.toLowerCase() + ' can no longer be started'}.</div>
      {/if}
    </div>

      <div class="cols">
        <div class="main">
          <div class="card sec">
            <h3>Instructions</h3>
            <p class="intro">{d.instructions.intro}</p>
            <div class="steps">
              {#each d.instructions.steps as s, i}
                <div class="step">
                  <div class="num">{i + 1}</div>
                  <div class="step-txt">{s}</div>
                </div>
              {/each}
            </div>
          </div>

          <div class="card sec">
            <div class="sec-head">
              <h3>Files and Resources</h3>
              <button class="link"><Icon name="download" size={13} /> Download All</button>
            </div>
            {#each d.files as f}
              <div class="frow">
                <div class="f-ic" style="background: var(--t-{FILE_COLOR[f.icon] || 'blue'}); color: var(--c-{FILE_COLOR[f.icon] || 'blue'})">
                  <Icon name={f.icon} size={16} />
                </div>
                <div class="f-body">
                  <div class="f-name">{f.name}</div>
                  <div class="f-meta">{f.meta}</div>
                </div>
                <button class="link">{f.action}</button>
              </div>
            {/each}
          </div>

          <div class="card sec">
            <div class="sec-head">
              <h3>Questions Preview</h3>
            </div>
            {#each d.questionsPreview as q, i}
              <div class="qrow">
                <div class="qnum">{i + 1}</div>
                <div class="q-txt">{q}</div>
                <div class="chev"><Icon name="chevron-right" size={14} /></div>
              </div>
            {/each}
          </div>
          <div class="card sec" id="submission">
            {#if !isStudent}
              <h3>Submissions</h3>
              <div class="sub-note">Student attempts and assignment submissions are reviewed and graded from the Submissions page.</div>
              <a class="btn btn-outline wide" href="#/assessments/submissions?assessment={id}">Open Submissions</a>
            {:else}
              <div class="sec-head">
                <h3>My Submission</h3>
                {#if sub.submitted}
                  <span class="pill pill-{sub.status === 'Submitted' ? 'purple' : 'green'}">{sub.status === 'Submitted' ? 'Submitted · awaiting grade' : sub.status === 'Graded' ? 'Graded' : 'Completed'}</span>
                {:else if isAssignment && !st.closed}
                  <span class="countdown" class:late={secondsToDue != null && secondsToDue < 3600}><Icon name="clock" size={12} /> {dueClock}</span>
                {/if}
              </div>

              {#if sub.submitted}
                <div class="muted rv-when">Submitted {sub.submittedAt}{#if sub.autoSubmitted} · submitted automatically when the time ran out{/if}</div>
                {#if !review}
                  <p class="muted">Loading your submission…</p>
                {:else}
                  {#if review.results && !isAssignment}
                    <div class="rv-stats">
                      <div class="rs"><b style="color:{scoreColor(review.results.percent ?? 0)}">{review.results.score}</b><span>score</span></div>
                      <div class="rs"><b>{review.results.correct}</b><span>correct</span></div>
                      <div class="rs"><b>{review.results.incorrect}</b><span>incorrect</span></div>
                      <div class="rs"><b>{review.results.skipped}</b><span>skipped</span></div>
                      <div class="rs"><b>{review.results.timeTaken ?? '—'}</b><span>time</span></div>
                    </div>
                  {/if}
                  {#if review.submission}
                    <div class="subm">
                      {#if review.submission.text}<p class="subm-text">{review.submission.text}</p>{:else}<p class="muted">No written response.</p>{/if}
                      {#each review.submission.files as f}
                        <div class="sub-file"><Icon name="doc" size={13} /> {f.name} <span class="muted">{kb(f.size)}</span></div>
                      {/each}
                      <div class="grade">
                        {#if review.submission.status === 'Graded'}
                          <div class="grade-pts">{review.submission.pointsAwarded} <span>/ {review.submission.points} pts · {sub.percent}%</span></div>
                          {#if review.submission.feedback}<div class="grade-fb">"{review.submission.feedback}"</div>{/if}
                          <div class="muted">Graded by {review.submission.gradedBy} · {review.submission.gradedAt}</div>
                        {:else}
                          <div class="muted">Awaiting grade from {d.teacher} · {review.submission.points} points possible</div>
                        {/if}
                      </div>
                    </div>
                  {/if}
                  {#each review.reviewQuestions ?? [] as q (q.number)}
                    {@const res = (q.result ?? '').toLowerCase()}
                    <div class="rq">
                      <div class="rq-top">
                        <span class="rq-num">Q{q.number}</span>
                        <span class="pill pill-{RESULT_PILL[res] ?? 'gray'}">{q.result ?? '—'}</span>
                        <span class="muted">{q.pointsLabel ?? ''}</span>
                      </div>
                      <div class="rq-text">{q.text}</div>
                      <div class="rq-ans">
                        <div><span class="lbl">Yours</span> {q.chosen ? `${q.chosen} · ${optionText(q, q.chosen)}` : 'no answer'}</div>
                        <div><span class="lbl">Correct</span> {q.correct} · {optionText(q, q.correct)}</div>
                      </div>
                      {#if q.explanation}<div class="rq-exp">{q.explanation}</div>{/if}
                    </div>
                  {/each}
                {/if}
                {#if isAssignment && sub.open && !st.closed}
                  <div class="resub"><h4>Resubmit before the due date</h4>
                    {#if sent}<div class="ok">Submitted {sent.submittedAt}{sent.resubmitted ? ' (replaced your previous submission)' : ''}.</div>{/if}
                    <textarea class="resp" rows="5" placeholder="Write your response…" bind:value={text} disabled={sending}></textarea>
                    <label class="dropzone" ondragover={(e) => e.preventDefault()} ondrop={dropFiles}>
                      <input type="file" multiple hidden onchange={pickFiles} />
                      <div class="up-ic"><Icon name="upload" size={20} /></div>
                      <div class="up-t">{sub.submitted ? 'Attach more files' : 'Attach your files'}</div>
                      <div class="up-s">Drag and drop files here or click to browse</div>
                    </label>
                    {#each files as f (f.name)}
                      <div class="sub-file">
                        <Icon name="doc" size={13} /> {f.name} <span class="muted">{kb(f.size)}</span>
                        <button class="link rm" onclick={() => removeFile(f.name)} aria-label="Remove">✕</button>
                      </div>
                    {/each}
                    {#if error}<p class="error">{error}</p>{/if}
                    <button class="btn btn-primary wide" disabled={sending} onclick={submitAssignment}>{sending ? 'Submitting…' : sub.submitted ? 'Resubmit Assignment' : 'Submit Assignment'}</button>
                      </div>
                {/if}
              {:else if isAssignment}
                {#if st.closed}
                  <div class="sub-note red">The due date has passed — this assignment can no longer be submitted.</div>
                {:else}
                  {#if sent}<div class="ok">Submitted {sent.submittedAt}{sent.resubmitted ? ' (replaced your previous submission)' : ''}.</div>{/if}
                  <textarea class="resp" rows="5" placeholder="Write your response…" bind:value={text} disabled={sending}></textarea>
                  <label class="dropzone" ondragover={(e) => e.preventDefault()} ondrop={dropFiles}>
                    <input type="file" multiple hidden onchange={pickFiles} />
                    <div class="up-ic"><Icon name="upload" size={20} /></div>
                    <div class="up-t">{sub.submitted ? 'Attach more files' : 'Attach your files'}</div>
                    <div class="up-s">Drag and drop files here or click to browse</div>
                  </label>
                  {#each files as f (f.name)}
                    <div class="sub-file">
                      <Icon name="doc" size={13} /> {f.name} <span class="muted">{kb(f.size)}</span>
                      <button class="link rm" onclick={() => removeFile(f.name)} aria-label="Remove">✕</button>
                    </div>
                  {/each}
                  {#if error}<p class="error">{error}</p>{/if}
                  <button class="btn btn-primary wide" disabled={sending} onclick={submitAssignment}>{sending ? 'Submitting…' : sub.submitted ? 'Resubmit Assignment' : 'Submit Assignment'}</button>
                    <div class="sub-note">{sub.note}</div>
                {/if}
              {:else if st.status === 'In Progress'}
                <div class="krow"><span class="k">Time left</span><span class="v strong red">{attemptClock}</span></div>
                <button class="btn btn-primary wide" onclick={() => navigate(`#/assessments/${id}/take`)}>Resume Attempt</button>
              {:else if st.closed}
                <div class="sub-note red">The due date has passed — this {d.type.toLowerCase()} can no longer be started.</div>
              {:else}
                <div class="krow"><span class="k">Time limit</span><span class="v">{st.timeLimit}</span></div>
                <div class="krow"><span class="k">Questions</span><span class="v">{sub.total}</span></div>
                <div class="sub-note">The timer starts when you click Start; answers are saved as you go and submitted automatically when it reaches 0:00.</div>
                <button class="btn btn-primary wide" onclick={() => navigate(`#/assessments/${id}/take`)}>{d.type === 'Test' ? 'Start Exam' : `Start ${d.type}`}</button>
              {/if}
            {/if}
          </div>
        </div>

        <div class="rail">
          <div class="card sec">
            <h3>{isAssignment ? 'Assignment' : d.type} Status</h3>
            <div class="krow"><span class="k">Status</span><span class="pill pill-{pill}">{st.status}</span></div>
            <div class="krow"><span class="k">Due Date</span><span class="v">{st.dueDate}</span></div>
            <div class="krow"><span class="k">Time Remaining</span><span class="v strong {st.closed ? 'red' : 'blue'}">{st.timeRemaining}</span></div>
            {#if !isAssignment}<div class="krow"><span class="k">Time Limit</span><span class="v">{st.timeLimit}</span></div>{/if}
            <div class="krow"><span class="k">Points</span><span class="v strong">{st.points}</span></div>
            <div class="krow"><span class="k">Attempts</span><span class="v">{st.attempts}</span></div>
            {#if sub.percent != null}<div class="krow"><span class="k">Score</span><span class="v strong green">{sub.percent}%{#if sub.autoSubmitted} · auto-submitted{/if}</span></div>{/if}
          </div>

          <div class="card sec">
            <h3>Class Information</h3>
            <div class="krow"><span class="k">Subject</span><span class="pill pill-amber">{d.classInfo.subject}</span></div>
            <div class="krow"><span class="k">Class</span><span class="v">{d.classInfo.gradeLevel ?? d.classInfo.clazz}</span></div>
            <div class="krow">
              <span class="k">Teacher</span>
              <span class="teacher"><Avatar name={d.classInfo.teacher} size={24} /> <span class="v">{d.classInfo.teacher}</span></span>
            </div>
          </div>

          <div class="card sec">
            <div class="sec-head">
              <h3>Grading Rubric</h3>
              <button class="link">View Full Rubric</button>
            </div>
            {#each d.rubric as r}
              <div class="rub">
                <div class="rub-row"><span>{r.label}</span><span class="rub-pct">{r.percent}%</span></div>
                <div class="rub-track"><div class="rub-fill" style="width:{r.percent}%"></div></div>
              </div>
            {/each}
          </div>

        </div>
      </div>
  {:else}
    <p class="page-sub">Loading…</p>
  {/if}
</div>

<style>
  .back { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 0.8rem; }

  .head-card { display: flex; align-items: center; gap: 1rem; padding: 1.1rem 1.25rem; }
  .head-ic { width: 52px; height: 52px; border-radius: 12px; background: var(--t-orange); color: var(--c-orange); display: grid; place-items: center; flex-shrink: 0; }
  .head-body { flex: 1; min-width: 0; }
  .head-title { display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap; }
  .head-title h1 { font-size: 1.15rem; font-weight: 700; }
  .head-sub { color: var(--text-2); font-size: 0.8rem; margin-top: 3px; }
  .head-due { color: var(--text-3); font-size: 0.74rem; margin-top: 3px; display: flex; align-items: center; gap: 4px; }

  .cols { display: grid; grid-template-columns: 1fr 320px; gap: 1rem; margin-top: 1rem; align-items: start; }
  .rv-when { margin: -0.3rem 0 0.7rem; }
  .rv-stats { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.8rem; }
  .rs { flex: 1; min-width: 70px; border: 1px solid var(--border); border-radius: 10px; padding: 0.45rem 0.6rem; display: flex; flex-direction: column; align-items: center; }
  .rs b { font-size: 1rem; }
  .rs span { color: var(--text-3); font-size: 0.68rem; }
  .subm { border: 1px solid var(--border); border-radius: 10px; padding: 0.7rem 0.8rem; background: var(--bg); margin-bottom: 0.6rem; }
  .subm-text { font-size: 0.84rem; line-height: 1.5; white-space: pre-wrap; margin: 0 0 0.4rem; }
  .rq { border-top: 1px solid var(--border); padding: 0.7rem 0; }
  .rq-top { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.3rem; }
  .rq-num { font-weight: 700; font-size: 0.78rem; color: var(--primary); }
  .rq-text { font-size: 0.84rem; font-weight: 600; margin-bottom: 0.35rem; }
  .rq-ans { display: grid; gap: 0.2rem; font-size: 0.8rem; }
  .lbl { display: inline-block; width: 60px; color: var(--text-3); font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; }
  .rq-exp { margin-top: 0.35rem; font-size: 0.76rem; color: var(--text-2); background: var(--bg); border-radius: 8px; padding: 0.4rem 0.6rem; }
  .resub { border-top: 1px solid var(--border); margin-top: 0.8rem; padding-top: 0.7rem; }
  .resub h4 { font-size: 0.82rem; margin-bottom: 0.5rem; }
  .main, .rail { display: flex; flex-direction: column; gap: 1rem; }

  .sec { padding: 1.1rem 1.25rem; }
  .sec h3 { font-size: 0.95rem; margin-bottom: 0.7rem; }
  .sec-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
  .sec-head h3 { margin-bottom: 0; }
  .sec-head .link { display: inline-flex; align-items: center; gap: 4px; }

  .intro { color: var(--text-2); font-size: 0.84rem; line-height: 1.55; margin-bottom: 0.9rem; }
  .steps { display: flex; flex-direction: column; gap: 0.6rem; }
  .step { display: flex; gap: 0.7rem; align-items: flex-start; }
  .num { width: 24px; height: 24px; border-radius: 50%; background: var(--t-blue); color: var(--c-blue); font-size: 0.72rem; font-weight: 700; display: grid; place-items: center; flex-shrink: 0; }
  .step-txt { font-size: 0.84rem; padding-top: 3px; }

  .frow { display: flex; align-items: center; gap: 0.7rem; padding: 0.6rem 0; border-bottom: 1px solid var(--border); }
  .frow:last-child { border-bottom: none; }
  .f-ic { width: 36px; height: 36px; border-radius: 9px; display: grid; place-items: center; flex-shrink: 0; }
  .f-body { flex: 1; min-width: 0; }
  .f-name { font-weight: 600; font-size: 0.82rem; }
  .f-meta { font-size: 0.7rem; color: var(--text-3); }

  .qrow { display: flex; align-items: center; gap: 0.7rem; padding: 0.6rem 0; border-bottom: 1px solid var(--border); }
  .qrow:last-child { border-bottom: none; }
  .qnum { width: 24px; height: 24px; border-radius: 7px; background: #eef1f6; color: var(--text-2); font-size: 0.72rem; font-weight: 700; display: grid; place-items: center; flex-shrink: 0; }
  .q-txt { flex: 1; font-size: 0.82rem; }
  .chev { color: var(--text-3); }

  .krow { display: flex; justify-content: space-between; align-items: center; padding: 0.45rem 0; border-bottom: 1px solid var(--border); }
  .krow:last-child { border-bottom: none; }
  .k { color: var(--text-2); font-size: 0.78rem; }
  .v { font-size: 0.8rem; font-weight: 600; }
  .strong { font-weight: 700; }
  .blue { color: var(--primary); }
  .teacher { display: flex; align-items: center; gap: 0.4rem; }

  .rub { margin-bottom: 0.65rem; }
  .rub:last-child { margin-bottom: 0; }
  .rub-row { display: flex; justify-content: space-between; font-size: 0.76rem; margin-bottom: 4px; }
  .rub-pct { font-weight: 700; }
  .rub-track { height: 5px; background: #eef1f6; border-radius: 3px; }
  .rub-fill { height: 100%; border-radius: 3px; background: var(--primary); }

  .head-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }
  .closed-note { display: flex; align-items: center; gap: 6px; color: #991b1b; background: var(--t-red); border-radius: 10px; padding: 0.5rem 0.8rem; font-size: 0.78rem; font-weight: 600; max-width: 300px; }
  .sep { margin: 0 4px; }
  .warn { color: var(--c-red); }
  .red { color: var(--c-red); }
  .green { color: var(--c-green); }
  .countdown { display: inline-flex; align-items: center; gap: 4px; font-size: 0.74rem; font-weight: 700; color: var(--primary); background: var(--t-blue); border-radius: 999px; padding: 0.2rem 0.6rem; font-variant-numeric: tabular-nums; }
  .countdown.late { color: var(--c-red); background: var(--t-red); }
  .sub-box { border: 1px solid var(--border); border-radius: 10px; padding: 0.7rem 0.8rem; margin-bottom: 0.8rem; background: var(--bg); }
  .sub-box.graded { border-color: var(--c-green); }
  .sub-row { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.4rem; }
  .sub-when { font-size: 0.72rem; color: var(--text-3); }
  .sub-text { font-size: 0.8rem; line-height: 1.5; margin-bottom: 0.4rem; white-space: pre-wrap; }
  .sub-file { display: flex; align-items: center; gap: 6px; font-size: 0.78rem; padding: 0.25rem 0; }
  .sub-file .rm { margin-left: auto; color: var(--text-3); }
  .muted { color: var(--text-3); font-size: 0.72rem; }
  .grade { margin-top: 0.6rem; border-top: 1px solid var(--border); padding-top: 0.5rem; }
  .grade-pts { font-size: 1.3rem; font-weight: 800; color: var(--c-green); }
  .grade-pts span { font-size: 0.78rem; color: var(--text-2); font-weight: 600; }
  .grade-fb { font-size: 0.8rem; font-style: italic; margin: 0.3rem 0; }
  .resp { width: 100%; border: 1px solid var(--border); border-radius: 10px; padding: 0.6rem 0.7rem; font: inherit; font-size: 0.82rem; resize: vertical; margin-bottom: 0.6rem; }
  .ok { background: var(--t-green); color: #166534; border-radius: 8px; padding: 0.4rem 0.7rem; font-size: 0.76rem; margin-bottom: 0.6rem; }
  .error { color: var(--c-red); font-size: 0.76rem; margin: 0.4rem 0; }
  .wide { width: 100%; margin-top: 0.6rem; }
  .sub-note.red { color: var(--c-red); }
  .dropzone { display: block; border: 2px dashed #c9d5e6; border-radius: 10px; padding: 1.3rem 1rem; text-align: center; cursor: pointer; }
  .dropzone:hover { border-color: var(--primary); }
  .up-ic { color: var(--primary); margin-bottom: 6px; }
  .up-t { font-weight: 600; font-size: 0.82rem; }
  .up-s { font-size: 0.72rem; color: var(--text-3); margin-top: 2px; }
  .sub-note { font-size: 0.72rem; color: var(--text-3); margin-top: 0.6rem; text-align: center; }


  @media (max-width: 1100px) { .cols { grid-template-columns: 1fr; } }
</style>
