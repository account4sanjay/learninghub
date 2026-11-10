<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import Avatar from '../components/Avatar.svelte'
  import { primaryRole } from '../auth.svelte.js'

  let data = $state(null)
  const isTeacher = $derived(primaryRole() === 'teacher')

  $effect(() => {
    api.get('/api/enrollment/my-classes').then((d) => (data = d)).catch(() => {})
  })
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">My Classes</h1>
      {#if data}
        <p class="page-sub">{data.term.name} &middot; {data.term.start} – {data.term.end}{#if isTeacher} &middot; classes you teach{/if}</p>
      {/if}
    </div>
    {#if data}
      <div class="credits-box">
        {#if isTeacher}
          <div class="credits-val">{data.classes.length}</div>
          <div class="credits-lbl">Classes</div>
        {:else}
          <div class="credits-val">{data.totalCredits}</div>
          <div class="credits-lbl">Credits</div>
        {/if}
      </div>
    {/if}
  </div>

  {#if data}
    <div class="grid">
      {#each data.classes as c}
        <div class="card cls" style="border-left: 4px solid var(--c-{c.color})">
          <div class="cls-top">
            <div class="icon-sq" style="background:var(--t-{c.color}); color:var(--c-{c.color})">
              <Icon name="book" size={20} />
            </div>
            <div class="cls-title">
              <div class="cls-name">{c.name}</div>
              <div class="cls-code">{c.gradeLevel}</div>
            </div>
            {#if !isTeacher}<span class="pill pill-green">{c.status}</span>{/if}
          </div>
          <div class="cls-rows">
            {#if isTeacher}
              <div class="cls-row">
                <span class="cls-ic"><Icon name="grad" size={15} /></span>
                {c.studentCount ?? 0} students enrolled
              </div>
            {:else}
              <div class="cls-row">
                <span class="cls-ic"><Avatar name={c.teacher} username={c.teacherUsername} size={18} /></span>
                {c.teacher}
              </div>
            {/if}
            <div class="cls-row">
              <span class="cls-ic"><Icon name="calendar" size={15} /></span>
              {c.days.join(', ')}
            </div>
            <div class="cls-row">
              <span class="cls-ic"><Icon name="clock" size={15} /></span>
              {c.time}
            </div>
          </div>
          <div class="cls-foot">
            <div class="cls-meta">
              <span class="cls-credits">{c.credits} Credits</span>
            </div>
            <a class="link sched-link" href="#/schedule">View Schedule <Icon name="chevron-right" size={13} /></a>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.3rem; }
  .credits-box { background: var(--card); border: 1px solid var(--border); border-radius: var(--radius); padding: 0.55rem 1.1rem; text-align: center; box-shadow: var(--shadow); }
  .credits-val { font-weight: 700; font-size: 1.05rem; }
  .credits-lbl { color: var(--text-2); font-size: 0.72rem; }

  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1.1rem; }

  .cls { padding: 1.1rem 1.2rem; display: flex; flex-direction: column; gap: 0.9rem; }
  .cls-top { display: flex; align-items: flex-start; gap: 0.8rem; }
  .icon-sq { width: 42px; height: 42px; border-radius: 11px; display: grid; place-items: center; flex-shrink: 0; }
  .cls-title { flex: 1; min-width: 0; }
  .cls-name { font-weight: 700; font-size: 0.98rem; }
  .cls-code { color: var(--text-2); font-size: 0.76rem; margin-top: 1px; }

  .cls-rows { display: grid; gap: 0.45rem; }
  .cls-row { display: flex; align-items: center; gap: 0.55rem; color: var(--text-2); font-size: 0.83rem; }
  .cls-ic { color: var(--text-3); display: inline-flex; }

  .cls-foot { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border); padding-top: 0.8rem; margin-top: auto; }
  .cls-meta { display: flex; align-items: center; gap: 0.6rem; }
  .cls-credits { color: var(--text-2); font-size: 0.78rem; font-weight: 600; }
  .sched-link { display: inline-flex; align-items: center; gap: 2px; }
</style>
