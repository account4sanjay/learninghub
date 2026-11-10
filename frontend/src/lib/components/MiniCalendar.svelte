<script>
  import Icon from './Icon.svelte'
  let { month = 'September 2026', selected = 15, highlight = 14, dotDays = [] } = $props()
  // September 2026: starts on Tuesday, 30 days
  const firstDay = 2
  const daysInMonth = 30
  const cells = $derived([
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ])
</script>

<div class="cal card">
  <div class="head">
    <strong>{month}</strong>
    <span class="navs">
      <button class="nav"><Icon name="chevron-left" size={14} /></button>
      <button class="nav"><Icon name="chevron-right" size={14} /></button>
    </span>
  </div>
  <div class="grid">
    {#each ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'] as d}<span class="dow">{d}</span>{/each}
    {#each cells as day}
      <span class="day" class:sel={day === selected} class:hl={day === highlight}>
        {day ?? ''}
        {#if day && dotDays.includes(day)}<i class="dot"></i>{/if}
      </span>
    {/each}
  </div>
</div>

<style>
  .cal { padding: 1rem; }
  .head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.6rem; font-size: 0.9rem; }
  .navs { display: flex; gap: 2px; }
  .nav { background: none; border: none; color: var(--text-2); padding: 2px; }
  .grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; text-align: center; }
  .dow { font-size: 0.65rem; color: var(--text-3); font-weight: 600; padding: 3px 0; }
  .day { position: relative; font-size: 0.75rem; padding: 5px 0; border-radius: 8px; color: var(--text); }
  .day.hl { background: var(--t-blue); }
  .day.sel { background: var(--primary); color: #fff; font-weight: 700; }
  .dot { position: absolute; bottom: 1px; left: 50%; transform: translateX(-50%); width: 4px; height: 4px; border-radius: 50%; background: var(--c-green); }
</style>
