<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'
  import Icon from '../components/Icon.svelte'

  // id is set when editing (#/assessments/{id}/edit); absent for #/assessments/new
  let { id = null } = $props()
  const isEdit = $derived(!!id)

  const TYPES = ['Quiz', 'Test', 'Assignment', 'Practice']
  const KEYS = ['A', 'B', 'C', 'D']
  const COLORS = ['blue', 'purple', 'green', 'orange', 'red', 'teal', 'amber']

  let classes = $state([]) // subjects I teach: { name, gradeLevel, color }
  let loading = $state(true)
  let saving = $state(false)
  let error = $state('')
  let fieldErrors = $state([])

  // form model
  let form = $state({
    title: '',
    subtitle: '',
    courseId: '',
    subject: '',
    gradeLevel: '',
    type: 'Quiz',
    dueDate: '',
    dueTime: '23:59',
    points: 100,
    attemptsAllowed: 1,
    timeLimitMinutes: 30,
    instructionsIntro: '',
    instructionsSteps: '',
    color: 'blue',
    questions: [],
  })

  function blankQuestion(n) {
    return {
      number: n,
      text: '',
      options: KEYS.map((key) => ({ key, text: '' })),
      correct: 'A',
      points: 1,
      topic: '',
      section: 'Multiple Choice',
    }
  }

  function splitIso(iso) {
    // "2026-10-02T23:59:00" -> ["2026-10-02", "23:59"]
    if (!iso) return ['', '23:59']
    const [d, t = '23:59:00'] = String(iso).split('T')
    return [d, t.slice(0, 5)]
  }

  async function load() {
    loading = true
    error = ''
    try {
      const mc = await api.get('/api/enrollment/my-classes').catch(() => null)
      const seen = new Set()
      classes = (mc?.classes ?? []).filter((c) => {
        const k = c.id
        if (seen.has(k)) return false
        seen.add(k)
        return true
      })
      if (isEdit) {
        const a = await api.get(`/api/assessment/teacher/assessments/${id}`)
        const [dueDate, dueTime] = splitIso(a.dueAt)
        const qs = (a.questions ?? []).map((q, i) => ({
          number: q.number ?? i + 1,
          text: q.text ?? '',
          options: KEYS.map((key) => ({ key, text: (q.options ?? []).find((o) => o.key === key)?.text ?? '' })),
          correct: q.correct ?? 'A',
          points: q.points ?? 1,
          topic: q.topic ?? '',
          section: q.section ?? 'Multiple Choice',
        }))
        form = {
          title: a.title ?? '',
          subtitle: a.subtitle ?? '',
          courseId: a.courseId ?? '',
          subject: a.subject ?? '',
          gradeLevel: a.gradeLevel ?? '',
          type: a.type ?? 'Quiz',
          dueDate,
          dueTime,
          points: a.points ?? 100,
          attemptsAllowed: a.attemptsAllowed ?? 1,
          timeLimitMinutes: a.timeLimitMinutes ?? 30,
          instructionsIntro: a.instructionsIntro ?? '',
          instructionsSteps: (a.instructionsSteps ?? []).join('\n'),
          color: a.color ?? 'blue',
          questions: qs,
        }
      } else if (classes.length) {
        form.courseId = classes[0].id
        form.subject = classes[0].name
        form.gradeLevel = classes[0].gradeLevel ?? ''
        form.color = classes[0].color ?? 'blue'
        form.questions = [blankQuestion(1)]
      } else {
        form.questions = [blankQuestion(1)]
      }
    } catch (e) {
      error = e.message
    } finally {
      loading = false
    }
  }
  load()

  function onSubjectChange() {
    const c = classes.find((x) => x.id === form.courseId)
    if (c) {
      form.subject = c.name
      form.gradeLevel = c.gradeLevel ?? form.gradeLevel
      form.color = c.color ?? form.color
    }
  }

  // Ask 45: assignments and practice sets are handed in as files, so they need no questions.
  // Quizzes and tests are taken online and get one blank question to start from.
  const needsQuestions = $derived(form.type === 'Quiz' || form.type === 'Test')
  const isBlank = (q) => !q.text.trim() && !q.options.some((o) => o.text.trim())
  function onTypeChange() {
    if (!needsQuestions) form.questions = form.questions.filter((q) => !isBlank(q))
    else if (form.questions.length === 0) form.questions = [blankQuestion(1)]
    if (form.type === 'Assignment') { form.timeLimitMinutes = 0; form.attemptsAllowed = 1 }
  }
  const effectiveQuestions = () => (needsQuestions ? form.questions : form.questions.filter((q) => !isBlank(q)))

  function addQuestion() {
    form.questions.push(blankQuestion(form.questions.length + 1))
  }
  function removeQuestion(i) {
    form.questions.splice(i, 1)
    form.questions.forEach((q, n) => (q.number = n + 1))
  }
  function moveQuestion(i, delta) {
    const j = i + delta
    if (j < 0 || j >= form.questions.length) return
    const [q] = form.questions.splice(i, 1)
    form.questions.splice(j, 0, q)
    form.questions.forEach((x, n) => (x.number = n + 1))
  }

  function validate() {
    const errs = []
    if (!form.title.trim()) errs.push('Title is required.')
    if (!form.courseId) errs.push('Class is required.')
    if (!form.dueDate) errs.push('Due date is required.')
    if (!(Number(form.points) > 0)) errs.push('Points must be greater than 0.')
    if (!(Number(form.attemptsAllowed) > 0)) errs.push('Attempts allowed must be at least 1.')
    if (Number(form.timeLimitMinutes) < 0) errs.push('Time limit cannot be negative.')
    effectiveQuestions().forEach((q, i) => {
      if (!q.text.trim()) errs.push(`Question ${i + 1}: text is required.`)
      const filled = q.options.filter((o) => o.text.trim())
      if (filled.length < 2) errs.push(`Question ${i + 1}: at least two options need text.`)
      if (!q.options.find((o) => o.key === q.correct)?.text.trim()) errs.push(`Question ${i + 1}: the correct option (${q.correct}) has no text.`)
    })
    return errs
  }

  function body() {
    return {
      title: form.title.trim(),
      subtitle: form.subtitle.trim(),
      courseId: form.courseId,
      type: form.type,
      dueAt: `${form.dueDate}T${form.dueTime || '23:59'}:00`,
      points: Number(form.points),
      attemptsAllowed: Number(form.attemptsAllowed),
      timeLimitMinutes: Number(form.timeLimitMinutes),
      instructionsIntro: form.instructionsIntro.trim(),
      instructionsSteps: form.instructionsSteps.split('\n').map((s) => s.trim()).filter(Boolean),
      questions: effectiveQuestions().map((q, i) => ({
        number: i + 1,
        section: q.section || 'Multiple Choice',
        text: q.text.trim(),
        options: q.options.filter((o) => o.text.trim()).map((o) => ({ key: o.key, text: o.text.trim() })),
        correct: q.correct,
        points: Number(q.points) || 1,
        topic: q.topic.trim(),
      })),
    }
  }

  async function save(e) {
    e.preventDefault()
    fieldErrors = validate()
    error = ''
    if (fieldErrors.length) return
    saving = true
    try {
      if (isEdit) await api.put(`/api/assessment/teacher/assessments/${id}`, body())
      else await api.post('/api/assessment/teacher/assessments', body())
      navigate('#/assessments')
    } catch (err) {
      error = err.message
    } finally {
      saving = false
    }
  }

  const totalQuestionPoints = $derived(form.questions.reduce((n, q) => n + (Number(q.points) || 0), 0))
</script>

<div class="page">
  <div class="head">
    <div>
      <a class="back" href="#/assessments"><Icon name="chevron-left" size={14} /> Assessments</a>
      <h1 class="page-title">{isEdit ? 'Edit assessment' : 'New assessment'}</h1>
      <p class="page-sub">{isEdit ? 'Changes apply immediately; existing submissions are kept.' : 'Create a quiz, test, assignment or practice for one of your classes. Students see it as soon as it is saved.'}</p>
    </div>
  </div>

  {#if loading}
    <div class="card"><p class="loading">Loading…</p></div>
  {:else}
    <form class="grid" onsubmit={save}>
      <div class="col">
        <div class="card section">
          <h3>Details</h3>
          <div class="fields">
            <label class="wide">Title <input bind:value={form.title} placeholder="e.g. Unit 3 Quiz" required /></label>
            <label class="wide">Subtitle <input bind:value={form.subtitle} placeholder="e.g. Trigonometric identities" /></label>
            <label class="wide">Class
              <select bind:value={form.courseId} onchange={onSubjectChange} required>
                {#if isEdit && form.courseId && !classes.some((c) => c.id === form.courseId)}<option value={form.courseId}>{form.subject} · {form.gradeLevel}</option>{/if}
                {#each classes as c (c.id)}<option value={c.id}>{c.name} · {c.gradeLevel}</option>{/each}
              </select>
            </label>
            <label>Type
              <select bind:value={form.type} onchange={onTypeChange}>{#each TYPES as t}<option>{t}</option>{/each}</select>
            </label>
            <label>Due date <input type="date" bind:value={form.dueDate} required /></label>
            <label>Due time <input type="time" bind:value={form.dueTime} /></label>
            <label>Points <input type="number" min="1" bind:value={form.points} /></label>
            {#if form.type !== 'Assignment'}
              <label>Attempts allowed <input type="number" min="1" bind:value={form.attemptsAllowed} /></label>
              <label>Time limit (min, 0 = none) <input type="number" min="0" bind:value={form.timeLimitMinutes} /></label>
            {/if}
          </div>
        </div>

        <div class="card section">
          <h3>Instructions</h3>
          <div class="fields">
            <label class="wide">Introduction <textarea rows="3" bind:value={form.instructionsIntro} placeholder="What students should know before they start"></textarea></label>
            <label class="wide">Steps (one per line) <textarea rows="4" bind:value={form.instructionsSteps} placeholder={'Read the chapter summary.\nAnswer every question.\nSubmit before the due time.'}></textarea></label>
          </div>
        </div>
      </div>

      <div class="col">
        <div class="card section">
          <div class="qhead">
            <h3>Questions <span class="muted">({form.questions.length} · {totalQuestionPoints} pts)</span></h3>
            <button type="button" class="btn btn-outline sm" onclick={addQuestion}><Icon name="plus" size={13} /> Add question</button>
          </div>
          {#if form.questions.length === 0}
            <p class="muted">{needsQuestions ? 'No questions yet. Quizzes and tests need at least one to be taken online.' : 'Assignments and practice sets need no questions: students hand in their work as files and the teacher grades it.'}</p>
          {/if}
          {#each form.questions as q, i (i)}
            <div class="q">
              <div class="q-top">
                <span class="q-num">Q{i + 1}</span>
                <div class="q-actions">
                  <button type="button" class="icon-btn" title="Move up" onclick={() => moveQuestion(i, -1)} disabled={i === 0}>↑</button>
                  <button type="button" class="icon-btn" title="Move down" onclick={() => moveQuestion(i, 1)} disabled={i === form.questions.length - 1}>↓</button>
                  <button type="button" class="icon-btn danger" title="Remove" onclick={() => removeQuestion(i)}><Icon name="x" size={13} /></button>
                </div>
              </div>
              <label class="wide">Question <textarea rows="2" bind:value={q.text} placeholder="Question text"></textarea></label>
              <div class="opts">
                {#each q.options as o (o.key)}
                  <label class="opt" class:correct={q.correct === o.key}>
                    <input type="radio" name="correct-{i}" value={o.key} bind:group={q.correct} title="Mark as correct" />
                    <span class="key">{o.key}</span>
                    <input class="opt-text" bind:value={o.text} placeholder={`Option ${o.key}`} />
                  </label>
                {/each}
              </div>
              <div class="q-meta">
                <label>Points <input type="number" min="1" bind:value={q.points} /></label>
                <label>Topic <input bind:value={q.topic} placeholder="e.g. Vertex form" /></label>
                <span class="hint">Select the radio next to the correct option (now {q.correct}).</span>
              </div>
            </div>
          {/each}
        </div>

        {#if fieldErrors.length}
          <div class="banner err">
            <ul>{#each fieldErrors as fe}<li>{fe}</li>{/each}</ul>
          </div>
        {/if}
        {#if error}<div class="banner err">{error}</div>{/if}

        <div class="actions">
          <a class="btn btn-ghost" href="#/assessments">Cancel</a>
          <button type="submit" class="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create assessment'}</button>
        </div>
      </div>
    </form>
  {/if}
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; }
  .back { display: inline-flex; align-items: center; gap: 0.2rem; font-size: 0.78rem; color: var(--text-2); margin-bottom: 0.3rem; }
  .loading { padding: 1.25rem; color: var(--text-2); }
  .grid { display: grid; grid-template-columns: 1fr 1.2fr; gap: 1rem; align-items: start; }
  .col { display: flex; flex-direction: column; gap: 1rem; }
  .section { padding: 1rem 1.1rem; }
  .section h3 { font-size: 0.95rem; font-weight: 700; margin-bottom: 0.7rem; }
  .fields { display: grid; grid-template-columns: 1fr 1fr; gap: 0.65rem 0.9rem; }
  .fields label, .q label, .q-meta label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.76rem; font-weight: 600; color: var(--text-2); }
  .fields .wide, .q .wide { grid-column: 1 / -1; }
  input:not([type='radio']), select, textarea { border: 1px solid var(--border); border-radius: 9px; padding: 0.48rem 0.7rem; font: inherit; font-size: 0.85rem; color: var(--text); background: #fff; width: 100%; }
  input:focus, select:focus, textarea:focus { outline: none; border-color: var(--primary); }
  textarea { resize: vertical; }
  .qhead { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.6rem; gap: 0.6rem; }
  .qhead h3 { margin: 0; }
  .btn.sm { padding: 0.32rem 0.6rem; font-size: 0.76rem; display: inline-flex; align-items: center; gap: 0.3rem; }
  .muted { color: var(--text-3); font-size: 0.8rem; font-weight: 500; }
  .q { border: 1px solid var(--border); border-radius: 10px; padding: 0.75rem 0.85rem; margin-bottom: 0.7rem; display: flex; flex-direction: column; gap: 0.55rem; background: var(--bg); }
  .q-top { display: flex; justify-content: space-between; align-items: center; }
  .q-num { font-weight: 700; font-size: 0.8rem; color: var(--primary); }
  .q-actions { display: flex; gap: 0.25rem; }
  .icon-btn { width: 26px; height: 26px; border-radius: 7px; border: 1px solid var(--border); background: #fff; color: var(--text-2); display: grid; place-items: center; cursor: pointer; font-size: 0.8rem; }
  .icon-btn:disabled { opacity: 0.4; cursor: default; }
  .icon-btn.danger { color: var(--c-red); }
  .opts { display: grid; grid-template-columns: 1fr 1fr; gap: 0.45rem; }
  .opt { display: flex; align-items: center; gap: 0.4rem; border: 1px solid var(--border); border-radius: 9px; padding: 0.3rem 0.5rem; background: #fff; flex-direction: row !important; }
  .opt.correct { border-color: var(--c-green); background: var(--t-green); }
  .opt .key { font-weight: 700; font-size: 0.78rem; width: 16px; }
  .opt input[type='radio'] { width: auto; }
  .opt .opt-text { border: none; padding: 0.2rem 0.3rem; background: transparent; }
  .opt .opt-text:focus { border: none; }
  .q-meta { display: grid; grid-template-columns: 110px 1fr; gap: 0.6rem; align-items: end; }
  .hint { grid-column: 1 / -1; font-size: 0.7rem; color: var(--text-3); }
  .banner { padding: 0.6rem 0.9rem; border-radius: 10px; font-size: 0.84rem; }
  .banner.err { background: var(--t-red); color: #991b1b; }
  .banner ul { margin: 0; padding-left: 1.1rem; }
  .actions { display: flex; justify-content: flex-end; gap: 0.5rem; }
  .btn:disabled { opacity: 0.5; cursor: not-allowed; }
  @media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
  @media (max-width: 600px) { .fields, .opts { grid-template-columns: 1fr; } }
</style>
