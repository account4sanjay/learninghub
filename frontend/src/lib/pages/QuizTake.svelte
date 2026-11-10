<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'
  import Icon from '../components/Icon.svelte'
  import QuizParabola from '../components/QuizParabola.svelte'

  let { id } = $props()

  let quiz = $state(null)
  let attempt = $state(null) // { attemptId, secondsLeft, resumed, timeLimitMinutes }
  let blocked = $state(null) // 409 reason: closed, no attempts left, assignment
  let current = $state(1)
  let answers = $state({})
  let flags = $state({})
  let secondsLeft = $state(null)
  let submitting = $state(false)
  let saving = $state(false)
  let savedAt = $state(null)
  let error = $state(null)
  let timeUp = $state(false)

  // Ask 20: start (or resume) the timed attempt on the server, then load the questions.
  // The clock runs from the server's secondsLeft; answers are autosaved; 0:00 auto-submits.
  $effect(() => {
    const cur = id
    blocked = null
    api
      .post(`/api/assessment/${cur}/start`)
      .then((a) => {
        attempt = a
        secondsLeft = a.secondsLeft
        answers = { ...(a.answers ?? {}) }
        return api.get(`/api/assessment/${cur}/questions`)
      })
      .then((v) => {
        quiz = v
        if (v.attempt?.secondsLeft != null) secondsLeft = v.attempt.secondsLeft
      })
      .catch((e) => (blocked = e.message || 'This assessment cannot be started'))
  })

  $effect(() => {
    if (secondsLeft == null) return
    const t = setInterval(() => {
      if (secondsLeft > 0) secondsLeft -= 1
      else if (!timeUp) {
        timeUp = true
        submit(true)
      }
    }, 1000)
    return () => clearInterval(t)
  })

  const clock = $derived(
    secondsLeft == null
      ? '--:--'
      : `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`
  )
  const lastMinute = $derived(secondsLeft != null && secondsLeft <= 60)

  const questions = $derived(quiz?.questions ?? [])
  const q = $derived(questions[current - 1])
  const answeredCount = $derived(Object.keys(answers).length)

  // autosave 1.2s after the last change
  let saveTimer = null
  function scheduleSave() {
    clearTimeout(saveTimer)
    saveTimer = setTimeout(save, 1200)
  }
  async function save() {
    if (!attempt || submitting || timeUp) return
    saving = true
    try {
      const r = await api.put(`/api/assessment/${id}/answers`, { answers })
      if (r?.secondsLeft != null) secondsLeft = Math.min(secondsLeft ?? r.secondsLeft, r.secondsLeft)
      savedAt = new Date()
    } catch (e) {
      // "Time is up" → the server already finalised the attempt
      if (/time is up/i.test(e.message || '')) navigate(`#/assessments/${id}/review`)
    } finally {
      saving = false
    }
  }

  function choose(key) {
    answers = { ...answers, [current]: key }
    scheduleSave()
  }
  function toggleFlag() {
    flags = { ...flags, [current]: !flags[current] }
  }
  function navClass(n) {
    if (n === current) return 'cur'
    if (answers[n]) return 'ans'
    return ''
  }

  async function submit(auto = false) {
    if (submitting) return
    if (!auto) {
      const left = questions.length - answeredCount
      const msg = left > 0 ? `${left} question${left === 1 ? '' : 's'} not answered. Submit anyway?` : 'Submit your answers now?'
      if (!confirm(msg)) return
    }
    clearTimeout(saveTimer)
    submitting = true
    error = null
    try {
      await api.post(`/api/assessment/${id}/submit`, { answers })
      navigate(`#/assessments/${id}/review`)
    } catch (e) {
      error = e.message || 'Could not submit your answers'
    } finally {
      submitting = false
    }
  }

  function jumpTo(n) {
    current = Math.min(Math.max(1, n), questions.length)
  }
</script>

<div class="page">
  {#if quiz}
    <div class="topbar">
      <button class="back link" onclick={() => navigate('#/assessments')}>
        <Icon name="chevron-left" size={14} /> Back to Assessments
      </button>
      <div class="tb-center">
        <div class="tb-title">{quiz.title}</div>
        <div class="tb-sub">{quiz.subtitle}</div>
      </div>
      <div class="tb-right">
        <div class="timer" class:warn={lastMinute}>
          <div class="timer-lbl">{attempt?.resumed ? 'Resumed · time remaining' : 'Time Remaining'}</div>
          <div class="timer-val"><Icon name="clock" size={15} /> {clock}</div>
          <div class="timer-sub">{quiz.timeLimitMinutes} min limit · {saving ? 'saving…' : savedAt ? 'saved ' + savedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'answers autosave'}</div>
        </div>
        <button class="btn btn-danger-outline" disabled={submitting} onclick={() => submit(false)}>{quiz.type === 'Test' ? 'End Exam' : 'End ' + (quiz.type || 'Quiz')}</button>
      </div>
    </div>
    {#if timeUp}
      <div class="banner">Time is up — your answers are being submitted…</div>
    {:else if lastMinute}
      <div class="banner">Less than a minute left — your answers will be submitted automatically at 0:00.</div>
    {/if}

    <div class="cols">
      <div class="main">
        {#if questions.length === 0}
          <div class="card qcard"><p class="page-sub">This assessment has no questions yet.</p></div>
        {:else if q}
          <div class="card qcard">
            <div class="qhead">
              <div class="qtitle">Question {q.number} of {quiz.totalQuestions}</div>
              <div class="qmeta">
                <span>Section: {q.section} &nbsp;·&nbsp; Points: {q.points}</span>
                <button class="flag-btn" class:on={flags[current]} onclick={toggleFlag}>
                  <Icon name="bookmark" size={14} /> Bookmark
                </button>
              </div>
            </div>

            <p class="qtext">{q.text}</p>

            {#if q.kind === 'graph-choice'}
              <div class="graph-grid">
                {#each q.options as o}
                  <button class="graph-opt" class:sel={answers[current] === o.key} onclick={() => choose(o.key)}>
                    <span class="opt-key">{o.key}</span>
                    <QuizParabola graph={o.graph} />
                  </button>
                {/each}
              </div>
            {:else}
              <div class="opts">
                {#each q.options as o}
                  <button class="opt" class:sel={answers[current] === o.key} onclick={() => choose(o.key)}>
                    <span class="opt-radio" class:on={answers[current] === o.key}></span>
                    <span class="opt-letter">{o.key}.</span>
                    <span>{o.text}</span>
                  </button>
                {/each}
              </div>
            {/if}
          </div>

          {#if error}<p class="error">{error}</p>{/if}
          <div class="footer">
            <button class="btn btn-ghost" disabled={current === 1} onclick={() => jumpTo(current - 1)}>
              ‹ Previous
            </button>
            {#if current === questions.length}
              <button class="btn btn-primary" disabled={submitting} onclick={() => submit(false)}>{submitting ? 'Submitting…' : quiz.type === 'Test' ? 'Submit Exam' : 'Submit ' + (quiz.type || 'Quiz')}</button>
            {:else}
              <button class="btn btn-primary" onclick={() => jumpTo(current + 1)}>
                Next ›
              </button>
            {/if}
          </div>
        {/if}
      </div>

      <div class="rail">
        <div class="card sec">
          <h3>Question Navigator</h3>
          <div class="navgrid">
            {#each questions as _, i}
              <button
                class="navbtn {navClass(i + 1)}"
                class:flagged={flags[i + 1]}
                onclick={() => jumpTo(i + 1)}
              >{i + 1}</button>
            {/each}
          </div>
          <div class="legend">
            <div><span class="dot cur"></span> Current</div>
            <div><span class="dot ans"></span> Answered</div>
            <div><span class="dot"></span> Not answered</div>
            <div><span class="dot flg"></span> Flagged</div>
          </div>
        </div>

        <div class="card sec">
          <h3>Topics</h3>
          <div class="topics">
            {#each questions as tq, i}
              <button class="topic" class:cur={i + 1 === current} class:ans={answers[i + 1]} onclick={() => jumpTo(i + 1)}>
                <span class="topic-n">{i + 1}</span>
                <span class="topic-t">{tq.topic || `Question ${i + 1}`}</span>
              </button>
            {/each}
          </div>
        </div>

        <div class="card sec">
          <h3>Test Progress</h3>
          <div class="prog-lbl">{answeredCount} of {quiz.totalQuestions} answered</div>
          <div class="prog-track"><div class="prog-fill" style="width:{(answeredCount / quiz.totalQuestions) * 100}%"></div></div>
        </div>

        <div class="card sec">
          <h3>Tools</h3>
          <div class="tool"><span class="tool-ic"><Icon name="calc" size={15} /></span> Calculator</div>
          <div class="tool"><span class="tool-ic"><Icon name="edit" size={15} /></span> Scratch Pad</div>
          <div class="tool"><span class="tool-ic"><Icon name="flag" size={15} /></span> Flag for Review</div>
        </div>
      </div>
    </div>
  {:else if blocked}
    <button class="back link" onclick={() => navigate('#/assessments')}>
      <Icon name="chevron-left" size={14} /> Back to Assessments
    </button>
    <div class="card blocked">
      <div class="blocked-ic"><Icon name="clock" size={22} /></div>
      <h2>This assessment cannot be started</h2>
      <p>{blocked}</p>
      <a class="btn btn-outline" href="#/assessments/{id}">View Details</a>
    </div>
  {:else}
    <p class="page-sub">Loading…</p>
  {/if}
</div>

<style>
  .topbar { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }
  .back { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }
  .tb-center { text-align: center; flex: 1; }
  .tb-title { font-size: 1.15rem; font-weight: 700; }
  .tb-sub { font-size: 0.76rem; color: var(--text-2); }
  .tb-right { display: flex; align-items: center; gap: 0.8rem; }
  .timer { text-align: right; }
  .timer-lbl { font-size: 0.66rem; color: var(--text-3); }
  .timer-val { font-weight: 700; font-size: 1.05rem; display: flex; align-items: center; gap: 5px; justify-content: flex-end; }
  .timer-sub { font-size: 0.64rem; color: var(--text-3); }
  .timer.warn .timer-val { color: var(--c-red); animation: blink 1s steps(2) infinite; }
  @keyframes blink { 50% { opacity: 0.45; } }
  .banner { background: var(--t-red); color: #991b1b; border-radius: 10px; padding: 0.55rem 0.9rem; font-size: 0.82rem; font-weight: 600; margin-bottom: 1rem; }
  .blocked { padding: 2rem; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 0.6rem; margin-top: 1rem; }
  .blocked-ic { width: 48px; height: 48px; border-radius: 12px; background: var(--t-red); color: var(--c-red); display: grid; place-items: center; }
  .blocked h2 { font-size: 1.05rem; }
  .blocked p { color: var(--text-2); font-size: 0.86rem; max-width: 460px; }

  .cols { display: grid; grid-template-columns: 1fr 290px; gap: 1rem; align-items: start; }

  .qcard { padding: 1.2rem 1.4rem; }
  .qhead { display: flex; justify-content: space-between; align-items: center; gap: 1rem; border-bottom: 1px solid var(--border); padding-bottom: 0.7rem; flex-wrap: wrap; }
  .qtitle { font-weight: 700; font-size: 1rem; }
  .qmeta { display: flex; align-items: center; gap: 0.9rem; font-size: 0.74rem; color: var(--text-2); }
  .flag-btn { display: inline-flex; align-items: center; gap: 5px; border: 1px solid var(--border); background: #fff; border-radius: 8px; padding: 0.35rem 0.7rem; font-size: 0.74rem; font-weight: 600; color: var(--text-2); }
  .flag-btn.on { border-color: var(--c-amber); color: #d97706; background: var(--t-amber); }

  .qtext { margin: 1rem 0; font-size: 0.95rem; font-weight: 600; }

  .graph-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.9rem; }
  .graph-opt { position: relative; background: #fff; border: 1.5px solid var(--border); border-radius: 10px; padding: 0.9rem; display: grid; place-items: center; }
  .graph-opt:hover { border-color: #b9c8e2; }
  .graph-opt.sel { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15); background: #f8fbff; }
  .opt-key { position: absolute; top: 8px; left: 10px; width: 22px; height: 22px; border-radius: 6px; background: #eef1f6; color: var(--text-2); display: grid; place-items: center; font-size: 0.72rem; font-weight: 700; }
  .graph-opt.sel .opt-key { background: var(--primary); color: #fff; }

  .opts { display: flex; flex-direction: column; gap: 0.55rem; }
  .opt { display: flex; align-items: center; gap: 0.6rem; text-align: left; background: #fff; border: 1.5px solid var(--border); border-radius: 10px; padding: 0.75rem 0.9rem; font-size: 0.85rem; }
  .opt:hover { border-color: #b9c8e2; }
  .opt.sel { border-color: var(--primary); background: #f8fbff; }
  .opt-radio { width: 15px; height: 15px; border-radius: 50%; border: 2px solid #c3cddc; flex-shrink: 0; }
  .opt-radio.on { border-color: var(--primary); background: radial-gradient(circle, var(--primary) 45%, #fff 50%); }
  .opt-letter { font-weight: 700; color: var(--text-2); }

  .footer { display: flex; justify-content: space-between; margin-top: 1rem; }

  .rail { display: flex; flex-direction: column; gap: 1rem; }
  .sec { padding: 1rem 1.1rem; }
  .sec h3 { font-size: 0.92rem; margin-bottom: 0.7rem; }

  .navgrid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0.4rem; }
  .navbtn { height: 34px; border-radius: 8px; border: 1px solid var(--border); background: #fff; font-weight: 600; font-size: 0.78rem; color: var(--text-2); }
  .navbtn.ans { background: var(--c-green); border-color: var(--c-green); color: #fff; }
  .navbtn.cur { background: var(--primary); border-color: var(--primary); color: #fff; }
  .navbtn.flagged { box-shadow: 0 0 0 2px var(--c-amber); }

  .legend { display: grid; grid-template-columns: 1fr 1fr; gap: 0.3rem 0.6rem; margin-top: 0.8rem; font-size: 0.68rem; color: var(--text-2); }
  .legend > div { display: flex; align-items: center; gap: 5px; }
  .dot { width: 10px; height: 10px; border-radius: 3px; background: #fff; border: 1px solid var(--border); display: inline-block; }
  .dot.cur { background: var(--primary); border-color: var(--primary); }
  .dot.ans { background: var(--c-green); border-color: var(--c-green); }
  .dot.flg { border: 2px solid var(--c-amber); }

  .error { color: var(--c-red); font-size: 0.8rem; margin-top: 0.6rem; }
  .topics { display: flex; flex-direction: column; gap: 2px; max-height: 260px; overflow-y: auto; }
  .topic { display: flex; align-items: center; gap: 0.5rem; width: 100%; text-align: left; border: none; background: none; padding: 0.3rem 0.4rem; border-radius: 7px; font-size: 0.74rem; color: var(--text-2); }
  .topic:hover { background: #f1f5fb; }
  .topic.ans .topic-n { background: var(--c-green); color: #fff; border-color: var(--c-green); }
  .topic.cur { background: var(--t-blue); color: var(--primary); font-weight: 600; }
  .topic.cur .topic-n { background: var(--primary); color: #fff; border-color: var(--primary); }
  .topic-n { width: 20px; height: 20px; border-radius: 6px; border: 1px solid var(--border); display: grid; place-items: center; font-size: 0.66rem; font-weight: 700; flex-shrink: 0; }
  .topic-t { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .prog-lbl { font-size: 0.76rem; color: var(--text-2); margin-bottom: 6px; }
  .prog-track { height: 7px; background: #eef1f6; border-radius: 4px; }
  .prog-fill { height: 100%; border-radius: 4px; background: var(--primary); transition: width 0.2s; }

  .tool { display: flex; align-items: center; gap: 0.6rem; padding: 0.45rem 0; font-size: 0.8rem; font-weight: 600; color: var(--text-2); }
  .tool-ic { width: 28px; height: 28px; border-radius: 7px; background: #eef1f6; display: grid; place-items: center; color: var(--text-2); }

  @media (max-width: 1100px) { .cols { grid-template-columns: 1fr; } }
</style>
