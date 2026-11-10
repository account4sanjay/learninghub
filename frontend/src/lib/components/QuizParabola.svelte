<script>
  // Small SVG parabola renderer used by QuizTake / QuizReview graph options.
  let { graph, width = 170, height = 120 } = $props()

  const W = 150
  const H = 110
  const CX = 75 // world x = 0
  const CY = 55 // world y = 0
  const SX = 9
  const SY = 8

  const path = $derived.by(() => {
    if (!graph) return ''
    const vx = graph.vertexX
    const vy = graph.vertexY
    const a = 0.5 * (graph.opens === 'down' ? -1 : 1)
    const pts = []
    for (let x = vx - 3.4; x <= vx + 3.401; x += 0.2) {
      const y = a * (x - vx) * (x - vx) + vy
      pts.push(`${(CX + x * SX).toFixed(1)} ${(CY - y * SY).toFixed(1)}`)
    }
    return 'M' + pts.join(' L')
  })
</script>

<svg viewBox="0 0 {W} {H}" {width} {height} fill="none" aria-hidden="true">
  <!-- axes -->
  <line x1="6" y1={CY} x2={W - 6} y2={CY} stroke="#c3cddc" stroke-width="1" />
  <line x1={CX} y1="6" x2={CX} y2={H - 6} stroke="#c3cddc" stroke-width="1" />
  <!-- arrows -->
  <path d="M{W - 6} {CY} l-4 -3 m4 3 l-4 3" stroke="#c3cddc" stroke-width="1" />
  <path d="M{CX} 6 l-3 4 m3 -4 l3 4" stroke="#c3cddc" stroke-width="1" />
  <!-- ticks -->
  {#each [-4, -2, 2, 4] as t}
    <line x1={CX + t * SX} y1={CY - 2} x2={CX + t * SX} y2={CY + 2} stroke="#c3cddc" stroke-width="1" />
    <line x1={CX - 2} y1={CY - t * SY} x2={CX + 2} y2={CY - t * SY} stroke="#c3cddc" stroke-width="1" />
  {/each}
  <path d={path} stroke="var(--c-blue)" stroke-width="2.2" stroke-linecap="round" />
</svg>
