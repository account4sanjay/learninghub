<script>
  let { percent = 0, size = 130, stroke = 14, label = 'Overall Progress' } = $props()
  const r = $derived((size - stroke) / 2)
  const c = $derived(2 * Math.PI * r)
</script>

<div class="donut" style="width:{size}px;height:{size}px">
  <svg width={size} height={size}>
    <circle cx={size / 2} cy={size / 2} {r} fill="none" stroke="#eef1f6" stroke-width={stroke} />
    <circle
      cx={size / 2} cy={size / 2} {r} fill="none"
      stroke="var(--c-green)" stroke-width={stroke} stroke-linecap="round"
      stroke-dasharray={c} stroke-dashoffset={c * (1 - percent / 100)}
      transform="rotate(-90 {size / 2} {size / 2})" />
  </svg>
  <div class="center">
    <div class="pct">{percent}%</div>
    <div class="lbl">{label}</div>
  </div>
</div>

<style>
  .donut { position: relative; display: grid; place-items: center; }
  svg { position: absolute; inset: 0; }
  .center { text-align: center; z-index: 1; }
  .pct { font-size: 1.4rem; font-weight: 700; }
  .lbl { font-size: 0.62rem; color: var(--text-2); max-width: 70px; }
</style>
