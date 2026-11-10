<script>
  import { api } from '../api.js'
  import { auth } from '../auth.svelte.js'
  import { router, query } from '../router.svelte.js'
  import Icon from '../components/Icon.svelte'
  import Avatar from '../components/Avatar.svelte'

  const ROLES = ['student', 'teacher', 'admin']
  const ROLE_PILL = { admin: 'pill-red', teacher: 'pill-purple', student: 'pill-blue' }
  const TITLES = ['', 'Mr', 'Ms', 'Dr']
  const GENDERS = ['', 'male', 'female']
  let gradeLevels = $state([]) // from GET /api/enrollment/grade-levels only — no built-in list (a 501 shows in the Not Implemented banner)
  api.get('/api/enrollment/grade-levels').then((g) => (gradeLevels = Array.isArray(g) ? g : [])).catch(() => (gradeLevels = []))
  const gradeName = (v) => gradeLevels.find((g) => g.id === v || g.name === v)?.name ?? v
  // bumped after an upload so <img> avatars refresh
  let avatarVersion = $state(0)
  let pictureFile = $state(null)

  let users = $state(null)
  let error = $state('')
  let notice = $state('')
  let search = $state('')
  let roleFilter = $state('all')
  // #/admin/users?role=student|teacher|admin preselects the role filter (and follows later hash changes)
  $effect(() => {
    void router.hash
    const r = query().get('role')
    if (r && ROLES.includes(r)) roleFilter = r
    else if (r === 'all' || r === '') roleFilter = 'all'
  })
  let busy = $state(false)

  // form state: null = closed, { mode: 'create' | 'edit', ... }
  let form = $state(null)

  async function load() {
    error = ''
    try {
      users = await api.get('/api/users/admin/users')
    } catch (e) {
      error = e.message
      users = []
    }
  }
  load()

  const filtered = $derived.by(() => {
    if (!users) return []
    const q = search.trim().toLowerCase()
    return users.filter(
      (u) =>
        (roleFilter === 'all' || u.role === roleFilter) &&
        (!q || `${u.username} ${u.firstName ?? ''} ${u.lastName ?? ''} ${u.email ?? ''}`.toLowerCase().includes(q)),
    )
  })

  function openCreate() {
    form = { mode: 'create', username: '', title: '', gender: '', firstName: '', lastName: '', email: '', password: '', role: 'student', gradeLevel: gradeLevels[0]?.id ?? 'freshman', enabled: true }
    pictureFile = null
  }
  function openEdit(u) {
    form = { mode: 'edit', id: u.id, username: u.username, title: u.title ?? '', gender: u.gender ?? '', firstName: u.firstName ?? '', lastName: u.lastName ?? '', email: u.email ?? '', password: '', role: u.role ?? 'student', gradeLevel: gradeLevels.find((g) => g.id === u.gradeLevel || g.name === u.gradeLevel)?.id ?? u.gradeLevel ?? gradeLevels[0]?.id ?? 'freshman', enabled: u.enabled }
    pictureFile = null
  }

  async function uploadPicture(id) {
    if (!pictureFile) return
    const fd = new FormData()
    fd.append('file', pictureFile)
    await api.upload(`/api/users/admin/users/${id}/avatar`, fd)
    avatarVersion++
  }
  function onPick(e) {
    pictureFile = e.target.files?.[0] ?? null
  }

  async function run(label, fn) {
    busy = true
    error = ''
    notice = ''
    try {
      await fn()
      notice = label
      await load()
    } catch (e) {
      error = e.message
    } finally {
      busy = false
    }
  }

  async function save(e) {
    e.preventDefault()
    const f = form
    if (f.mode === 'create') {
      if (!f.username || !f.firstName || !f.lastName || !f.password) {
        error = 'Username, first name, last name and password are required.'
        return
      }
      await run(`User ${f.username} created`, async () => {
        const created = await api.post('/api/users/admin/users', { username: f.username, title: f.title || null, gender: f.gender || null, firstName: f.firstName, lastName: f.lastName, email: f.email, password: f.password, role: f.role, gradeLevel: f.role === 'student' ? f.gradeLevel : null, enabled: f.enabled })
        await uploadPicture(created.id)
        form = null
      })
    } else {
      await run(`User ${f.username} updated`, async () => {
        const body = { title: f.title || null, gender: f.gender || null, firstName: f.firstName, lastName: f.lastName, email: f.email, role: f.role, gradeLevel: f.role === 'student' ? f.gradeLevel : null, enabled: f.enabled }
        if (f.password) body.password = f.password
        await api.put(`/api/users/admin/users/${f.id}`, body)
        await uploadPicture(f.id)
        form = null
      })
    }
  }

  function toggle(u) {
    const action = u.enabled ? 'suspend' : 'activate'
    run(`User ${u.username} ${u.enabled ? 'suspended' : 'activated'}`, () => api.post(`/api/users/admin/users/${u.id}/${action}`))
  }

  function remove(u) {
    if (!confirm(`Delete user ${u.username}? This removes the Keycloak account and the profile.`)) return
    run(`User ${u.username} deleted`, () => api.del(`/api/users/admin/users/${u.id}`))
  }

  const isMe = (u) => u.username === auth.user?.username
  const fullName = (u) => [u.title, u.firstName, u.lastName].filter(Boolean).join(' ') || u.username
  const fmtDate = (ts) => (ts ? new Date(ts).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—')
</script>

<div class="page">
  <div class="head">
    <div>
      <h1 class="page-title">Users</h1>
      <p class="page-sub">Create, update, suspend or delete accounts. Identities and roles are stored in Keycloak, so changes apply at the next sign-in.</p>
    </div>
    <button class="btn btn-primary new-btn" onclick={openCreate}><Icon name="plus" size={14} /> New user</button>
  </div>

  {#if notice}<div class="banner ok">{notice}</div>{/if}
  {#if error}<div class="banner err">{error}</div>{/if}

  <div class="card toolbar">
    <div class="search">
      <Icon name="search" size={15} />
      <input placeholder="Search by username, name or email…" bind:value={search} />
    </div>
    <div class="filters">
      <button class="seg" class:on={roleFilter === 'all'} onclick={() => (roleFilter = 'all')}>All</button>
      {#each ROLES as r}
        <button class="seg" class:on={roleFilter === r} onclick={() => (roleFilter = r)}>{r}</button>
      {/each}
    </div>
    <span class="count">{filtered.length} of {users?.length ?? 0}</span>
  </div>

  <div class="card list">
    {#if users === null}
      <p class="loading">Loading users…</p>
    {:else if filtered.length === 0}
      <p class="loading">No users match.</p>
    {:else}
      <table class="tbl">
        <thead>
          <tr><th>User</th><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Created</th><th class="right">Actions</th></tr>
        </thead>
        <tbody>
          {#each filtered as u (u.id)}
            <tr class:suspended={!u.enabled}>
              <td>
                <div class="who">
                  {#key avatarVersion}<Avatar name={fullName(u)} username={u.username} size={30} />{/key}
                  <div>
                    <div class="name">{fullName(u)} {#if isMe(u)}<span class="you">you</span>{/if}</div>
                  </div>
                </div>
              </td>
              <td class="mono">{u.username}</td>
              <td class="muted">{u.email ?? '—'}</td>
              <td>{#if u.role}<span class="pill {ROLE_PILL[u.role]}">{u.role}</span>{:else}<span class="pill pill-gray">no role</span>{/if}{#if u.role === 'student' && u.gradeLevel}<span class="grade">{gradeName(u.gradeLevel)}</span>{/if}</td>
              <td>
                {#if u.enabled}<span class="pill pill-green">Active</span>{:else}<span class="pill pill-amber">Suspended</span>{/if}
              </td>
              <td class="muted nowrap">{fmtDate(u.createdTimestamp)}</td>
              <td class="right actions">
                <button class="btn btn-ghost sm" onclick={() => openEdit(u)} disabled={busy}><Icon name="edit" size={13} /> Edit</button>
                <button class="btn btn-outline sm" onclick={() => toggle(u)} disabled={busy || isMe(u)} title={isMe(u) ? 'You cannot suspend yourself' : ''}>
                  {u.enabled ? 'Suspend' : 'Activate'}
                </button>
                <button class="btn btn-danger-outline sm" onclick={() => remove(u)} disabled={busy || isMe(u)}>Delete</button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    {/if}
  </div>
</div>

{#if form}
  <div class="backdrop" onclick={() => (form = null)} role="presentation"></div>
  <form class="modal card" onsubmit={save}>
    <h2>{form.mode === 'create' ? 'New user' : `Edit ${form.username}`}</h2>
    <div class="fields">
      {#if form.mode === 'create'}
        <label class="wide">Username <input bind:value={form.username} placeholder="e.g. teacher2" autocomplete="off" required /></label>
      {/if}
      <label>Title
        <select bind:value={form.title}>
          {#each TITLES as t}<option value={t}>{t || '—'}</option>{/each}
        </select>
      </label>
      <label>Gender
        <select bind:value={form.gender}>
          {#each GENDERS as g}<option value={g}>{g || '—'}</option>{/each}
        </select>
      </label>
      <label>First name <input bind:value={form.firstName} required /></label>
      <label>Last name <input bind:value={form.lastName} required /></label>
      <label class="wide">Email <input type="email" bind:value={form.email} /></label>
      <label class="wide">
        {form.mode === 'create' ? 'Password' : 'New password (leave blank to keep)'}
        <input type="password" bind:value={form.password} autocomplete="new-password" required={form.mode === 'create'} />
      </label>
      <label>Role
        <select bind:value={form.role}>
          {#each ROLES as r}<option value={r}>{r}</option>{/each}
        </select>
      </label>
      {#if form.role === 'student'}
        <label>Grade level
          <select bind:value={form.gradeLevel}>
            {#each gradeLevels as g (g.id)}<option value={g.id}>{g.name}{g.gradeNumber ? ` (grade ${g.gradeNumber})` : ''}</option>{/each}
          </select>
        </label>
      {/if}
      <label class="wide">Profile picture {form.mode === 'edit' ? '(leave empty to keep)' : '(optional, default avatar by gender)'}
        <input type="file" accept="image/*" onchange={onPick} />
      </label>
      <label class="check"><input type="checkbox" bind:checked={form.enabled} /> Account enabled (can sign in)</label>
    </div>
    <div class="modal-actions">
      <button type="button" class="btn btn-ghost" onclick={() => (form = null)} disabled={busy}>Cancel</button>
      <button type="submit" class="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : form.mode === 'create' ? 'Create user' : 'Save changes'}</button>
    </div>
  </form>
{/if}

<style>
  .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
  .new-btn { display: inline-flex; align-items: center; gap: 0.4rem; margin-left: auto; }
  .banner { padding: 0.6rem 0.9rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 0.8rem; }
  .banner.ok { background: var(--t-green); color: #166534; }
  .banner.err { background: var(--t-red); color: #991b1b; }

  .toolbar { display: flex; align-items: center; gap: 0.8rem; padding: 0.6rem 0.9rem; flex-wrap: wrap; margin-bottom: 1rem; }
  .search { display: flex; align-items: center; gap: 0.5rem; color: var(--text-3); background: var(--bg); border: 1px solid var(--border); border-radius: 9px; padding: 0.4rem 0.7rem; flex: 1 1 260px; }
  .search input { border: none; outline: none; background: none; width: 100%; font-size: 0.85rem; color: var(--text); }
  .filters { display: flex; background: #eef1f6; border-radius: 999px; padding: 3px; }
  .seg { border: none; background: none; padding: 0.28rem 0.8rem; border-radius: 999px; font-size: 0.78rem; font-weight: 600; color: var(--text-2); text-transform: capitalize; }
  .seg.on { background: var(--primary); color: #fff; }
  .count { color: var(--text-3); font-size: 0.78rem; margin-left: auto; }

  .list { padding: 0; overflow-x: auto; }
  .loading { padding: 1.25rem; color: var(--text-2); }
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
  .tbl th { text-align: left; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-3); padding: 0.75rem 1rem; border-bottom: 1px solid var(--border); }
  .tbl td { padding: 0.65rem 1rem; border-bottom: 1px solid var(--border); vertical-align: middle; }
  .tbl tr:last-child td { border-bottom: none; }
  .tbl tr.suspended td { opacity: 0.6; }
  .right { text-align: right; }
  .nowrap { white-space: nowrap; }
  .muted { color: var(--text-2); }
  .who { display: flex; align-items: center; gap: 0.6rem; }
  .name { font-weight: 600; }
  .mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.8rem; }
  .grade { display: inline-block; margin-left: 0.4rem; font-size: 0.72rem; color: var(--text-2); }
  .you { font-size: 0.65rem; background: var(--t-blue); color: var(--primary); border-radius: 999px; padding: 0 0.4rem; margin-left: 0.3rem; font-weight: 700; }
  .actions { white-space: nowrap; display: flex; gap: 0.35rem; justify-content: flex-end; }
  .btn.sm { padding: 0.32rem 0.6rem; font-size: 0.76rem; display: inline-flex; align-items: center; gap: 0.3rem; }
  .btn:disabled { opacity: 0.5; cursor: not-allowed; }

  .backdrop { position: fixed; inset: 0; background: rgba(15, 30, 61, 0.35); z-index: 40; }
  .modal { position: fixed; z-index: 41; top: 50%; left: 50%; transform: translate(-50%, -50%); width: min(520px, 94vw); padding: 1.3rem 1.4rem; box-shadow: 0 24px 60px rgba(15, 30, 61, 0.25); }
  .modal h2 { font-size: 1.1rem; margin-bottom: 0.9rem; }
  .fields { display: grid; grid-template-columns: 1fr 1fr; gap: 0.7rem 0.9rem; }
  .fields label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.78rem; font-weight: 600; color: var(--text-2); }
  .fields label.wide, .fields .check { grid-column: 1 / -1; }
  .fields input, .fields select { border: 1px solid var(--border); border-radius: 9px; padding: 0.5rem 0.7rem; font: inherit; font-size: 0.86rem; color: var(--text); background: #fff; }
  .fields input:focus, .fields select:focus { outline: none; border-color: var(--primary); }
  .check { flex-direction: row !important; align-items: center; gap: 0.5rem !important; }
  .check input { width: auto; }
  .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1.1rem; }
  @media (max-width: 600px) { .fields { grid-template-columns: 1fr; } }
</style>
