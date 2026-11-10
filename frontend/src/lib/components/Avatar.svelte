<script>
  // Shows the stored/default profile picture for `username` (public endpoint),
  // falling back to initials from `name` when no username is given or the image fails.
  let { name = '', size = 34, username = null } = $props()
  let failed = $state(false)
  const initials = $derived(
    (name || '').split(' ').map((p) => p[0]).filter(Boolean).slice(0, 2).join('').toUpperCase() || '?'
  )
  const src = $derived(username ? `/api/users/avatar/${encodeURIComponent(username)}` : null)
  $effect(() => {
    src // reset the failure flag when the user changes
    failed = false
  })
</script>

{#if src && !failed}
  <img class="avatar img" {src} alt={name || username} width={size} height={size} style="width:{size}px;height:{size}px" onerror={() => (failed = true)} />
{:else}
  <div class="avatar" style="width:{size}px;height:{size}px;font-size:{size * 0.38}px">{initials}</div>
{/if}

<style>
  .avatar {
    border-radius: 50%;
    background: linear-gradient(135deg, #64748b, #475569);
    color: #fff;
    display: grid;
    place-items: center;
    font-weight: 600;
    flex-shrink: 0;
  }
  .avatar.img { object-fit: cover; background: #e2e8f0; display: block; }
</style>
