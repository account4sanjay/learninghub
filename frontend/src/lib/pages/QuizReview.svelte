<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'
  import Icon from '../components/Icon.svelte'
  import QuizParabola from '../components/QuizParabola.svelte'

  let { id } = $props()

  let d = $state(null)
  let tab = $state('Review')
  let selected = $state(1)
  let error = $state(null)

  $effect(() => {
    const cur = id
    api.get(`/api/assessment/${cur}/review`)
      .then((v) => {
        d = v
        selected = v.reviewQuestion?.number ?? 1
      })
      .catch((e) => (error = e.message))
  })

  const REL_COLOR = { video: 'red', practice: 'blue', lesson: 'purple' }
  const REL_ICON = { video: 'video', practice: 'practice', lesson: 'book' }
  const RESULT_PILL = { Correct: 'pill-green', Incorrect: 'pill-red', Skipped: 'pill-gray' }

  // the question being reviewed (all questions come back with chosen/correct/explanation)
  const rq = $derived(d?.reviewQuestions?.find((q) => q.number === selected) ?? d?.reviewQuestion)
</script>

<div class="page">
  {#if d}
    <button class="back link" onclick={() => navigate('#/assessments')}>
      <Icon name="chevron-left" size={14} /> Back to Assessments
    </button>

    <div class="card head-card">
      <div class="head-ic"><Icon name="check" size={22} /></div>
      <div class="head-body">
        <div class="head-title">
          <h1>{d.title}</h1>
          <span class="pill pill-green">Completed</span>
        </div>
        <div class="head-sub">{d.subtitle}</div>
        <div class="head-meta">Completed on {d.completedOn} · {d.questions} questions · {d.minutes} minutes</div>
      </div>
      <div class="head-actions">
        <button class="btn btn-outline" onclick={() => navigate(`#/assessments/${id}/take`)}>Retake Test</button>
        <button class="btn btn-primary">Review All</button>
      </div>
    </div>

    <div class="card tabs-card">
      {#each ['Review', 'Performance', 'Question Summary'] as t}
        <button class="tab" class:active={tab === t} onclick={() => (tab = t)}>{t}</button>
      {/each}
    </div>

    {#if tab === 'Review'}
      <div class="cols">
        <div class="card qlist">
          <div class="qlist-head">Questions</div>
          {#each d.questionTopics as t (t.n)}
            <button class="qitem" class:sel={selected === t.n} onclick={() => (selected = t.n)}>
              {#if t.result === 'correct'}
                <span class="ri ok"><Icon name="check" size={11} /></span>
              {:else if t.result === 'incorrect'}
                <span class="ri bad"><Icon name="x" size={11} /></span>
              {:else}
                <span class="ri skip">—</span>
              {/if}
              <span class="qitem-txt">{t.n}. {t.topic}</span>
            </button>
          {/each}
        </div>

        <div class="center">
          {#if rq}
            <div class="card qcard">
              <div class="qhead">
                <div class="qtitle">
                  Question {rq.number} of {rq.total}
                  <span class="pill {RESULT_PILL[rq.result] || 'pill-gray'}">{rq.result}</span>
                </div>
                <div class="qpts">{rq.pointsLabel}</div>
              </div>
              <p class="qtext">{rq.text}</p>
              {#if rq.kind === 'graph-choice'}
                <div class="graph-grid">
                  {#each rq.options as o}
                    <div class="graph-opt" class:correct={o.key === rq.correct} class:wrong={o.key === rq.chosen && o.key !== rq.correct}>
                      <span class="opt-key" class:ok={o.key === rq.correct}>{o.key}</span>
                      {#if o.key === rq.correct}
                        <span class="tag-ok"><Icon name="check" size={11} /> Correct answer</span>
                      {:else if o.key === rq.chosen}
                        <span class="tag-bad"><Icon name="x" size={11} /> Your answer</span>
                      {/if}
                      <QuizParabola graph={o.graph} width={155} height={110} />
                    </div>
                  {/each}
                </div>
              {:else}
                <div class="opts">
                  {#each rq.options as o}
                    <div class="opt" class:correct={o.key === rq.correct} class:wrong={o.key === rq.chosen && o.key !== rq.correct}>
                      <span class="opt-letter">{o.key}.</span>
                      <span class="opt-text">{o.text}</span>
                      {#if o.key === rq.correct}
                        <span class="tag-ok"><Icon name="check" size={11} /> Correct answer</span>
                      {:else if o.key === rq.chosen}
                        <span class="tag-bad"><Icon name="x" size={11} /> Your answer</span>
                      {/if}
                    </div>
                  {/each}
                </div>
                {#if !rq.chosen}<p class="ph">You skipped this question.</p>{/if}
              {/if}
              {#if rq.explanation}
                <div class="explain">
                  <div class="explain-h">💡 Explanation</div>
                  <p>{rq.explanation}</p>
                </div>
              {/if}
            </div>
          {/if}
        </div>

        <div class="rail">
          <div class="card sec results">
            <h3>Test Results</h3>
            <div class="big">{d.results.correct} / {d.results.total}</div>
            <div class="big-pct">{d.results.percent}%</div>
            <span class="pill pill-green">{d.results.status}</span>
            <div class="rrows">
              <div class="rrow"><span class="rk ok-t"><Icon name="check" size={13} /> Correct</span><span class="rv ok-t">{d.results.correct}</span></div>
              <div class="rrow"><span class="rk bad-t"><Icon name="x" size={13} /> Incorrect</span><span class="rv bad-t">{d.results.incorrect}</span></div>
              <div class="rrow"><span class="rk">— Skipped</span><span class="rv">{d.results.skipped}</span></div>
              <div class="rrow"><span class="rk">Time Taken</span><span class="rv">{d.results.timeTaken}</span></div>
              <div class="rrow"><span class="rk">Points Earned</span><span class="rv">{d.results.pointsEarned}</span></div>
              <div class="rrow"><span class="rk">Score</span><span class="rv">{d.results.score}</span></div>
              <div class="rrow"><span class="rk">Status</span><span class="pill pill-green">{d.results.status}</span></div>
            </div>
            <button class="btn btn-outline wide" onclick={() => navigate(`#/assessments/${id}/take`)}>Retake Test</button>
          </div>

          <div class="card sec">
            <h3>Related Learning</h3>
            {#each d.relatedLearning as r}
              <div class="rel">
                <span class="rel-ic" style="background: var(--t-{REL_COLOR[r.icon] || 'blue'}); color: var(--c-{REL_COLOR[r.icon] || 'blue'})">
                  <Icon name={REL_ICON[r.icon] || 'book'} size={15} />
                </span>
                <span class="rel-body">
                  <span class="rel-kind">{r.kind}</span>
                  <span class="rel-t">{r.title}</span>
                  <span class="rel-m">{r.meta}</span>
                </span>
                <span class="chev"><Icon name="chevron-right" size={14} /></span>
              </div>
            {/each}
          </div>
        </div>
      </div>
    {:else}
      <div class="card sec placeholder">
        <h3>{tab}</h3>
        <p class="ph">This section is not available in the demo.</p>
      </div>
    {/if}
  {:else if error}
    <button class="back link" onclick={() => navigate('#/assessments')}>
      <Icon name="chevron-left" size={14} /> Back to Assessments
    </button>
    <div class="card sec placeholder">
      <h3>No completed attempt yet</h3>
      <p class="ph">Take this assessment first, then come back to review your answers.</p>
      <button class="btn btn-primary" style="margin-top:0.8rem" onclick={() => navigate(`#/assessments/${id}/take`)}>Start</button>
    </div>
  {:else}
    <p class="page-sub">Loading…</p>
  {/if}
</div>

<style>
  .back { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 0.8rem; }

  .head-card { display: flex; align-items: center; gap: 1rem; padding: 1.1rem 1.25rem; }
  .head-ic { width: 46px; height: 46px; border-radius: 50%; background: var(--t-green); color: var(--c-green); display: grid; place-items: center; flex-shrink: 0; }
  .head-body { flex: 1; min-width: 0; }
  .head-title { display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap; }
  .head-title h1 { font-size: 1.15rem; font-weight: 700; }
  .head-sub { color: var(--text-2); font-size: 0.8rem; margin-top: 2px; }
  .head-meta { color: var(--text-3); font-size: 0.72rem; margin-top: 2px; }
  .head-actions { display: flex; gap: 0.6rem; }

  .tabs-card { display: flex; gap: 0.25rem; margin-top: 0.9rem; padding: 0.35rem 0.6rem; }
  .tab { border: none; background: none; padding: 0.5rem 0.85rem; font-size: 0.8rem; font-weight: 600; color: var(--text-2); border-bottom: 2px solid transparent; }
  .tab.active { color: var(--primary); border-bottom-color: var(--primary); }

  .cols { display: grid; grid-template-columns: 220px 1fr 290px; gap: 1rem; margin-top: 1rem; align-items: start; }

  .qlist { padding: 0.6rem 0.5rem; }
  .qlist-head { font-size: 0.78rem; font-weight: 700; padding: 0.4rem 0.6rem 0.6rem; }
  .qitem { display: flex; align-items: center; gap: 0.5rem; width: 100%; text-align: left; border: none; background: none; padding: 0.42rem 0.6rem; border-radius: 8px; font-size: 0.74rem; color: var(--text); }
  .qitem:hover { background: #f1f5fb; }
  .qitem.sel { background: var(--t-blue); color: var(--primary); font-weight: 600; }
  .qitem-txt { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .ri { width: 17px; height: 17px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0; font-size: 0.62rem; font-weight: 700; }
  .ri.ok { background: var(--t-green); color: var(--c-green); }
  .ri.bad { background: var(--t-red); color: var(--c-red); }
  .ri.skip { background: #eef1f6; color: var(--text-3); }

  .qcard { padding: 1.1rem 1.25rem; }
  .qhead { display: flex; justify-content: space-between; align-items: center; gap: 1rem; border-bottom: 1px solid var(--border); padding-bottom: 0.7rem; flex-wrap: wrap; }
  .qtitle { font-weight: 700; font-size: 0.95rem; display: flex; align-items: center; gap: 0.6rem; }
  .qpts { font-size: 0.76rem; color: var(--text-2); font-weight: 600; }
  .qtext { margin: 0.9rem 0; font-size: 0.9rem; font-weight: 600; }
  .ph { color: var(--text-2); font-size: 0.82rem; line-height: 1.5; }

  .graph-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.8rem; }
  .graph-opt { position: relative; background: #fff; border: 1.5px solid var(--border); border-radius: 10px; padding: 1.4rem 0.6rem 0.6rem; display: grid; place-items: center; }
  .graph-opt.correct { border-color: var(--c-green); box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.15); background: #f7fdfb; }
  .opt-key { position: absolute; top: 8px; left: 10px; width: 22px; height: 22px; border-radius: 6px; background: #eef1f6; color: var(--text-2); display: grid; place-items: center; font-size: 0.72rem; font-weight: 700; }
  .opt-key.ok { background: var(--c-green); color: #fff; }
  .graph-opt.wrong { border-color: var(--c-red); background: #fff8f8; }
  .opts { display: flex; flex-direction: column; gap: 0.55rem; }
  .opt { position: relative; display: flex; align-items: center; gap: 0.6rem; background: #fff; border: 1.5px solid var(--border); border-radius: 10px; padding: 0.7rem 8.5rem 0.7rem 0.9rem; font-size: 0.85rem; }
  .opt.correct { border-color: var(--c-green); background: #f7fdfb; }
  .opt.wrong { border-color: var(--c-red); background: #fff8f8; }
  .opt-letter { font-weight: 700; color: var(--text-2); }
  .opt-text { flex: 1; }
  .tag-bad { position: absolute; top: 10px; right: 10px; display: inline-flex; align-items: center; gap: 3px; font-size: 0.62rem; font-weight: 700; color: var(--c-red); background: var(--t-red); border-radius: 999px; padding: 2px 8px; }
  .tag-ok { position: absolute; top: 10px; right: 10px; display: inline-flex; align-items: center; gap: 3px; font-size: 0.62rem; font-weight: 700; color: var(--c-green); background: var(--t-green); border-radius: 999px; padding: 2px 8px; }

  .explain { margin-top: 1rem; background: var(--t-green); border: 1px solid #c9eeda; border-radius: 10px; padding: 0.85rem 1rem; }
  .explain-h { font-weight: 700; font-size: 0.8rem; color: #0b9666; margin-bottom: 4px; }
  .explain p { font-size: 0.8rem; color: var(--text); line-height: 1.5; }

  .rail { display: flex; flex-direction: column; gap: 1rem; }
  .sec { padding: 1rem 1.1rem; }
  .sec h3 { font-size: 0.92rem; margin-bottom: 0.7rem; }

  .results { text-align: center; }
  .results h3 { text-align: left; }
  .big { font-size: 2rem; font-weight: 800; line-height: 1.1; }
  .big-pct { font-size: 1.05rem; font-weight: 700; color: var(--c-green); margin: 2px 0 6px; }
  .rrows { margin-top: 0.9rem; text-align: left; }
  .rrow { display: flex; justify-content: space-between; align-items: center; padding: 0.42rem 0; border-bottom: 1px solid var(--border); font-size: 0.78rem; }
  .rrow:last-child { border-bottom: none; }
  .rk { color: var(--text-2); display: inline-flex; align-items: center; gap: 5px; }
  .rv { font-weight: 700; }
  .ok-t { color: var(--c-green); }
  .bad-t { color: var(--c-red); }
  .wide { width: 100%; margin-top: 0.7rem; }

  .rel { display: flex; align-items: center; gap: 0.6rem; padding: 0.5rem 0; border-bottom: 1px solid var(--border); }
  .rel:last-child { border-bottom: none; }
  .rel-ic { width: 32px; height: 32px; border-radius: 8px; display: grid; place-items: center; flex-shrink: 0; }
  .rel-body { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .rel-kind { font-size: 0.62rem; color: var(--text-3); font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; }
  .rel-t { font-size: 0.78rem; font-weight: 600; }
  .rel-m { font-size: 0.68rem; color: var(--text-3); }
  .chev { color: var(--text-3); }

  .placeholder { margin-top: 1rem; }

  @media (max-width: 1150px) { .cols { grid-template-columns: 1fr; } }
</style>
