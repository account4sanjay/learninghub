<script>
  import Icon from './components/Icon.svelte'
  import Avatar from './components/Avatar.svelte'
  import { auth, logout, primaryRole } from './auth.svelte.js'
  import { router, navigate } from './router.svelte.js'

  // stub build: every 501 answered on this route — endpoint, service and owner — until the route changes
  let notImpl = $state([])
  $effect(() => {
    const on = (e) => { if (!notImpl.some((n) => n.endpoint === e.detail.endpoint)) notImpl = [...notImpl, e.detail] }
    window.addEventListener('lh:notimplemented', on)
    return () => window.removeEventListener('lh:notimplemented', on)
  })
  $effect(() => { router.hash; notImpl = [] })
  // stub build: implemented endpoints answered on this route (X-Implemented-By header) — service and owner
  let implemented = $state([])
  $effect(() => {
    const on = (e) => { if (!implemented.some((i) => i.endpoint === e.detail.endpoint)) implemented = [...implemented, e.detail] }
    window.addEventListener('lh:implemented', on)
    return () => window.removeEventListener('lh:implemented', on)
  })
  $effect(() => { router.hash; implemented = [] })
  // one line per service owner: "payment-service (Victoria Achom)" → its endpoints
  const byOwner = (items, label) => {
    const groups = new Map()
    for (const it of items) {
      const key = label(it)
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key).push(it.endpoint ?? '')
    }
    return [...groups].map(([owner, endpoints]) => ({ owner, endpoints }))
  }
  const implementedGroups = $derived(byOwner(implemented, (i) => i.by))
  const notImplGroups = $derived(byOwner(notImpl, (n) => `${n.service ?? 'service'}${n.owner ? ` (${n.owner})` : ''}`))

  let { children } = $props()

  // menu per role — student: Dashboard, Schedule, My Classes, Learning, Assessments, Payments, Profile
  //                 teacher: Dashboard, Schedule, My Classes, Students, Learning, Assessments, Profile
  //                 admin:   Dashboard, Schedule, Learning, Assessments, Payments, Profile, Users, Subjects
  const NAV = [
    { label: 'Dashboard', icon: 'home', hash: '#/dashboard' },
    { label: 'Schedule', icon: 'schedule', hash: '#/schedule' },
    { label: 'My Classes', icon: 'classes', hash: '#/my-classes', roles: ['student', 'teacher'] },
    { label: 'Students', icon: 'grad', hash: '#/students', roles: ['teacher'] },
    { label: 'Learning', icon: 'learning', hash: '#/learning' },
    { label: 'Assessments', icon: 'assessments', hash: '#/assessments' },
    { label: 'Payments', icon: 'payments', hash: '#/payments', roles: ['student', 'admin'] },
    { label: 'Profile', icon: 'profile', hash: '#/profile' },
    { label: 'Users', icon: 'grad', hash: '#/admin/users', roles: ['admin'] },
    { label: 'Subjects', icon: 'classes', hash: '#/admin/subjects', roles: ['admin'] },
    { label: 'Enrollment', icon: 'grad', hash: '#/admin/enrollment', roles: ['admin'] },
  ]
  const nav = $derived(NAV.filter((i) => !i.roles || i.roles.includes(primaryRole())))
  const active = $derived('#/' + (router.hash.replace(/^#\/?/, '').split('?')[0].split('/').slice(0, router.hash.startsWith('#/admin') ? 2 : 1).join('/') || 'dashboard'))
  const role = $derived(primaryRole())

  let menuOpen = $state(false)
  let menuEl = $state(null)

  function onWindowClick(e) {
    if (menuOpen && menuEl && !menuEl.contains(e.target)) menuOpen = false
  }
  function onWindowKey(e) {
    if (e.key === 'Escape') menuOpen = false
  }
  function go(hash) {
    menuOpen = false
    navigate(hash)
  }
  function signOut() {
    menuOpen = false
    logout()
  }
</script>

<svelte:window onclick={onWindowClick} onkeydown={onWindowKey} />

<div class="shell">
  <aside>
    <div class="brand" onclick={() => navigate('#/dashboard')} role="button" tabindex="0" onkeydown={() => {}}>
      <span class="logo">📖</span> Learning<span class="hub">Hub</span>
    </div>
    <nav>
      {#each nav as item}
        <button class="nav-item" class:active={active === item.hash} onclick={() => navigate(item.hash)}>
          <Icon name={item.icon} size={17} />
          {item.label}
        </button>
      {/each}
    </nav>
  </aside>

  <div class="main">
    <header>
      <div class="search">
        <Icon name="search" size={15} />
        <input placeholder="Search classes, subjects, or resources..." />
      </div>
      <div class="right">
        <button class="bell"><Icon name="bell" size={18} /><i class="badge">2</i></button>
        <div class="user-menu" bind:this={menuEl}>
          <button
            class="user"
            class:open={menuOpen}
            onclick={() => (menuOpen = !menuOpen)}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
          >
            <Avatar name={auth.user?.name} username={auth.user?.username} />
            <div class="user-text">
              <div class="uname">{auth.user?.name}</div>
              <div class="urole">{role}</div>
            </div>
            <span class="caret" aria-hidden="true">▾</span>
          </button>
          {#if menuOpen}
            <div class="dropdown" role="menu">
              <div class="dd-head">
                <div class="uname">{auth.user?.name}</div>
                <div class="urole">@{auth.user?.username}</div>
              </div>
              <button class="dd-item" role="menuitem" onclick={() => go('#/profile')}>
                <Icon name="profile" size={15} /> My profile
              </button>
              <button class="dd-item danger" role="menuitem" onclick={signOut}>
                <Icon name="logout" size={15} /> Sign out
              </button>
            </div>
          {/if}
        </div>
      </div>
    </header>
    <div class="content">
      {#if implemented.length}
        <div class="impl" role="status">
          <strong>Implemented</strong>
          <ul>{#each implementedGroups as g (g.owner)}<li><b>{g.owner}</b>: {g.endpoints.join(' · ')}</li>{/each}</ul>
        </div>
      {/if}
      {#if notImpl.length}
        <div class="not-impl" role="status">
          <strong>Not Implemented</strong>
          <ul>{#each notImplGroups as g (g.owner)}<li><b>{g.owner}</b>: {g.endpoints.join(' · ')}</li>{/each}</ul>
          <button class="dismiss" onclick={() => (notImpl = [])} aria-label="dismiss">×</button>
        </div>
      {/if}
      {@render children?.()}
    </div>
  </div>
</div>

<style>
  .shell { display: flex; min-height: 100vh; }
  .impl ul, .not-impl ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.15rem; font-family: ui-monospace, Menlo, monospace; font-size: 0.8rem; }
  .impl li b, .not-impl li b { font-weight: 700; font-family: inherit; }
  .impl { display: flex; align-items: flex-start; gap: 0.6rem; margin: 0.9rem 1.25rem 0; padding: 0.6rem 0.9rem; border: 1px solid #86efac; background: #f0fdf4; color: #14532d; border-radius: 10px; font-size: 0.86rem; flex-wrap: wrap; }
  .impl strong { font-weight: 700; }
  .impl span { font-family: ui-monospace, Menlo, monospace; font-size: 0.8rem; }
  .not-impl { display: flex; align-items: flex-start; gap: 0.6rem; margin: 0.9rem 1.25rem 0; padding: 0.6rem 0.9rem; border: 1px solid #fcd34d; background: #fffbeb; color: #78350f; border-radius: 10px; font-size: 0.86rem; flex-wrap: wrap; }
  .not-impl strong { font-weight: 700; }
  .not-impl span { font-family: ui-monospace, Menlo, monospace; font-size: 0.8rem; }
  .not-impl .dismiss { margin-left: auto; background: none; border: none; color: #92400e; font-size: 1rem; cursor: pointer; }
  aside {
    width: 200px; flex-shrink: 0; background: var(--navy); color: #cbd5e1;
    display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh;
  }
  .brand { padding: 1.1rem 1rem; font-weight: 700; color: #fff; font-size: 1.02rem; cursor: pointer; }
  .brand .hub { color: #7ea8ff; }
  .logo { margin-right: 2px; }
  nav { flex: 1; padding: 0.4rem 0.6rem; display: flex; flex-direction: column; gap: 2px; }
  .nav-item {
    display: flex; align-items: center; gap: 0.65rem; width: 100%;
    background: none; border: none; color: #b6c2d6; text-align: left;
    padding: 0.6rem 0.75rem; border-radius: 9px; font-size: 0.86rem; font-weight: 500;
  }
  .nav-item:hover { background: rgba(255, 255, 255, 0.07); color: #fff; }
  .nav-item.active { background: var(--primary); color: #fff; }
  .main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  header {
    background: #fff; border-bottom: 1px solid var(--border);
    display: flex; align-items: center; justify-content: space-between;
    padding: 0.55rem 1.5rem; gap: 1rem; position: sticky; top: 0; z-index: 20;
  }
  .search {
    display: flex; align-items: center; gap: 0.5rem; color: var(--text-3);
    background: var(--bg); border: 1px solid var(--border); border-radius: 9px;
    padding: 0.45rem 0.8rem; width: min(430px, 45vw);
  }
  .search input { border: none; outline: none; background: none; width: 100%; font-size: 0.83rem; color: var(--text); }
  .right { display: flex; align-items: center; gap: 1.1rem; }
  .bell { position: relative; background: none; border: none; color: var(--text-2); }
  .badge {
    position: absolute; top: -4px; right: -5px; background: var(--c-red); color: #fff;
    font-size: 0.58rem; font-style: normal; font-weight: 700;
    border-radius: 999px; padding: 1px 4px; border: 2px solid #fff;
  }
  .user-menu { position: relative; }
  .user {
    display: flex; align-items: center; gap: 0.6rem; cursor: pointer;
    background: none; border: none; padding: 0.25rem 0.4rem; border-radius: 10px;
    color: var(--text); text-align: left;
  }
  .user:hover, .user.open { background: var(--bg); }
  .user-text { line-height: 1.2; }
  .uname { font-weight: 600; font-size: 0.84rem; }
  .urole { color: var(--text-3); font-size: 0.72rem; text-transform: capitalize; }
  .caret { color: var(--text-3); font-size: 0.75rem; margin-left: 0.1rem; }
  .dropdown {
    position: absolute; right: 0; top: calc(100% + 8px); min-width: 210px;
    background: var(--card); border: 1px solid var(--border); border-radius: var(--radius);
    box-shadow: var(--shadow), 0 12px 32px rgba(15, 30, 61, 0.12); padding: 0.4rem; z-index: 30;
  }
  .dd-head { padding: 0.5rem 0.7rem 0.6rem; border-bottom: 1px solid var(--border); margin-bottom: 0.3rem; }
  .dd-head .urole { text-transform: none; }
  .dd-item {
    display: flex; align-items: center; gap: 0.6rem; width: 100%;
    background: none; border: none; text-align: left; color: var(--text);
    padding: 0.55rem 0.7rem; border-radius: 8px; font-size: 0.84rem; font-weight: 500; cursor: pointer;
  }
  .dd-item:hover { background: var(--bg); }
  .dd-item.danger { color: var(--c-red); }
  .dd-item.danger:hover { background: var(--t-red); }
  .content { flex: 1; }
</style>
