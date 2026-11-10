<script>
  import Icon from './Icon.svelte'

  import { TERMS as FALLBACK, CURRENT_TERM } from '../terms.js'
  import { api } from '../api.js'

  // parents must bind an initialised value (Svelte forbids bind:undefined with a fallback)
  let { selected = $bindable(CURRENT_TERM) } = $props()

  let open = $state(false)
  let root = $state(null)
  // quarters from schedule-service (the term owner, Ask 46); terms.js is only the placeholder while loading
  let terms = $state(FALLBACK)

  const fmt = (iso) => {
    if (!iso) return ''
    const [y, m, d] = iso.split('-').map(Number)
    return new Date(y, m - 1, d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  }
  api
    .get('/api/schedule/terms')
    .then((list) => {
      if (!Array.isArray(list) || list.length === 0) return
      terms = list
        .slice()
        .sort((a, b) => (a.start ?? a.startDate ?? '').localeCompare(b.start ?? b.startDate ?? ''))
        .map((t) => ({ id: t.id, name: t.name, start: fmt(t.start ?? t.startDate), end: fmt(t.end ?? t.endDate), current: !!t.current, quarter: t.quarter, academicYear: t.academicYear }))
      const cur = terms.find((t) => t.current) ?? terms[0]
      if (!terms.some((t) => t.id === selected?.id)) selected = cur
    })
    .catch(() => {})

  function pick(t) {
    selected = t
    open = false
  }
  function onWindowClick(e) {
    if (open && root && !root.contains(e.target)) open = false
  }
  function onWindowKey(e) {
    if (e.key === 'Escape') open = false
  }
</script>

<svelte:window onclick={onWindowClick} onkeydown={onWindowKey} />

<div class="term-picker" bind:this={root}>
  <button class="term-card card" class:open onclick={() => (open = !open)} aria-haspopup="listbox" aria-expanded={open}>
    <span class="term-ico"><Icon name="grad" size={20} /></span>
    <span class="term-text">
      <span class="term-lbl">Term</span>
      <span class="term-name">{selected.name} <span class="caret" class:up={open}><Icon name="chevron-down" size={14} /></span></span>
      <span class="term-dates">{selected.start} – {selected.end}</span>
    </span>
  </button>

  {#if open}
    <ul class="menu card" role="listbox">
      {#each terms as t (t.id)}
        <li>
          <button class="item" class:active={t.id === selected.id} role="option" aria-selected={t.id === selected.id} onclick={() => pick(t)}>
            <span class="i-name">{t.name}{#if t.current}<span class="pill pill-blue cur">Current</span>{/if}</span>
            <span class="i-dates">{t.start} – {t.end}</span>
            {#if t.id === selected.id}<span class="check"><Icon name="check" size={14} /></span>{/if}
          </button>
        </li>
      {/each}
    </ul>
  {/if}
</div>

<style>
  .term-picker { position: relative; }
  .term-card {
    display: flex; align-items: center; gap: 0.65rem; padding: 0.55rem 0.9rem;
    cursor: pointer; text-align: left; color: var(--text); font: inherit;
  }
  .term-card:hover, .term-card.open { border-color: var(--primary); }
  .term-ico { color: var(--primary); display: grid; place-items: center; }
  .term-text { display: flex; flex-direction: column; line-height: 1.25; }
  .term-lbl { font-size: 0.68rem; color: var(--text-3); font-weight: 600; }
  .term-name { font-weight: 700; font-size: 0.9rem; display: flex; align-items: center; gap: 0.3rem; }
  .caret { display: inline-flex; color: var(--text-3); transition: transform 0.15s; }
  .caret.up { transform: rotate(180deg); }
  .term-dates { font-size: 0.72rem; color: var(--text-2); }

  .menu {
    position: absolute; right: 0; top: calc(100% + 6px); min-width: 250px; z-index: 30;
    list-style: none; margin: 0; padding: 0.35rem;
    box-shadow: var(--shadow), 0 12px 32px rgba(15, 30, 61, 0.12);
  }
  .item {
    display: grid; grid-template-columns: 1fr auto; align-items: center; column-gap: 0.6rem;
    width: 100%; background: none; border: none; text-align: left; color: var(--text); font: inherit;
    padding: 0.5rem 0.7rem; border-radius: 8px; cursor: pointer;
  }
  .item:hover { background: var(--bg); }
  .item.active { background: var(--t-blue); }
  .i-name { font-weight: 600; font-size: 0.85rem; display: flex; align-items: center; gap: 0.4rem; }
  .cur { font-size: 0.62rem; padding: 0.05rem 0.4rem; }
  .i-dates { grid-column: 1; font-size: 0.72rem; color: var(--text-2); }
  .check { grid-column: 2; grid-row: 1 / span 2; color: var(--primary); display: grid; place-items: center; }
</style>
