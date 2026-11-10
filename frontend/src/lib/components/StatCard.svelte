<script>
  import Icon from './Icon.svelte'
  let { icon = 'classes', color = 'blue', value, label, sub = '', progress = null, clickable = true, href = null } = $props()
</script>

{#if href}
  <a class="stat card link-card" {href}>
  <div class="icon-box {color}"><Icon name={icon} size={20} /></div>
  <div class="body">
    <div class="value">{value}</div>
    <div class="label">{label}</div>
    {#if sub}<div class="sub">{sub}</div>{/if}
    {#if progress != null}
      <div class="track"><div class="fill" style="width:{progress}%"></div></div>
    {/if}
  </div>
  {#if clickable}<div class="chev"><Icon name="chevron-right" size={16} /></div>{/if}
  </a>
{:else}
  <div class="stat card">
  <div class="icon-box {color}"><Icon name={icon} size={20} /></div>
  <div class="body">
    <div class="value">{value}</div>
    <div class="label">{label}</div>
    {#if sub}<div class="sub">{sub}</div>{/if}
    {#if progress != null}
      <div class="track"><div class="fill" style="width:{progress}%"></div></div>
    {/if}
  </div>
  {#if clickable}<div class="chev"><Icon name="chevron-right" size={16} /></div>{/if}
  </div>
{/if}

<style>
  .stat { display: flex; align-items: center; gap: 0.9rem; padding: 1rem 1.1rem; flex: 1; min-width: 200px; }
  .icon-box { width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center; flex-shrink: 0; }
  .icon-box.blue { background: var(--t-blue); color: var(--c-blue); }
  .icon-box.green { background: var(--t-green); color: var(--c-green); }
  .icon-box.purple { background: var(--t-purple); color: var(--c-purple); }
  .icon-box.amber, .icon-box.orange { background: var(--t-amber); color: var(--c-amber); }
  .icon-box.red { background: var(--t-red); color: var(--c-red); }
  .icon-box.teal { background: var(--t-teal); color: var(--c-teal); }
  .body { flex: 1; min-width: 0; }
  .value { font-size: 1.35rem; font-weight: 700; line-height: 1.15; }
  .label { color: var(--text-2); font-size: 0.82rem; margin-top: 1px; }
  .sub { color: var(--text-3); font-size: 0.72rem; margin-top: 2px; }
  .track { height: 5px; background: #eef1f6; border-radius: 3px; margin-top: 6px; }
  .fill { height: 100%; border-radius: 3px; background: var(--c-green); }
  .chev { color: var(--text-3); }
  .link-card { color: inherit; text-decoration: none; cursor: pointer; transition: box-shadow 0.15s, transform 0.15s, border-color 0.15s; }
  .link-card:hover { text-decoration: none; border-color: var(--primary); box-shadow: 0 6px 18px rgba(15, 30, 61, 0.1); transform: translateY(-1px); }
  .link-card:hover .chev { color: var(--primary); }
</style>
