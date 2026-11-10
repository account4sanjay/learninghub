<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'
  import Avatar from '../components/Avatar.svelte'

  let profile = $state(null)
  let editing = $state(false)
  let draft = $state(null)
  let saving = $state(false)
  let saveError = $state('')
  let saved = $state('')
  let pictureFile = $state(null)
  let avatarVersion = $state(0)
  const TITLES = ['', 'Mr', 'Ms', 'Dr']
  const GENDERS = ['', 'male', 'female']

  api.get('/api/users/profile').then((d) => (profile = d))

  function startEdit() {
    draft = {
      title: profile.title ?? '',
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      gender: profile.gender ?? '',
      email: profile.email ?? '',
      phone: profile.phone ?? '',
      location: profile.location ?? '',
      address: profile.personal.address ?? '',
      bio: profile.bio ?? '',
      guardian: { ...(profile.guardian ?? {}) },
      preferences: { timezone: profile.preferences?.timezone ?? '', notifications: profile.preferences?.notifications ?? 'Email & Push' },
    }
    pictureFile = null
    saveError = ''
    saved = ''
    editing = true
  }
  async function saveEdit(e) {
    e.preventDefault()
    saving = true
    saveError = ''
    try {
      const body = { ...draft, title: draft.title || null, gender: draft.gender || null }
      profile = await api.put('/api/users/profile', body)
      if (pictureFile) {
        const fd = new FormData()
        fd.append('file', pictureFile)
        await api.upload('/api/users/profile/avatar', fd, 'PUT')
        avatarVersion++
      }
      editing = false
      saved = 'Profile updated'
    } catch (err) {
      saveError = err.message
    } finally {
      saving = false
    }
  }

  const STAT_ICON = { classes: 'classes', lessons: 'check', assessments: 'trophy', hours: 'clock' }

  // students see the full academic profile; teachers and admins only personal/contact data
  const isStudent = $derived(profile?.role === 'Student')
  const showGuardian = $derived(isStudent && !!profile?.guardian)
  const showAcademic = $derived(isStudent && !!profile?.academic)
  const showStats = $derived(isStudent && (profile?.stats?.length ?? 0) > 0)
  const personalRows = $derived(
    profile
      ? [
          { icon: 'profile', label: 'Full Name', value: profile.personal.fullName },
          ...(isStudent
            ? [
                { icon: 'calendar', label: 'Date of Birth', value: profile.personal.dateOfBirth },
                { icon: 'grad', label: 'Grade', value: profile.personal.grade },
                { icon: 'home', label: 'School', value: profile.personal.school },
              ]
            : []),
          { icon: 'mail', label: 'Email', value: profile.personal.email },
          { icon: 'phone', label: 'Phone', value: profile.personal.phone },
          { icon: 'pin', label: 'Address', value: profile.personal.address },
        ]
      : []
  )
  const guardianRows = $derived(
    profile?.guardian
      ? [
          { icon: 'profile', label: profile.guardian.relation, value: profile.guardian.name },
          { icon: 'mail', label: 'Email', value: profile.guardian.email },
          { icon: 'phone', label: 'Phone', value: profile.guardian.phone },
        ]
      : []
  )
</script>

<div class="page">
  {#if profile}
    <!-- header card -->
    <div class="hero card">
      {#key avatarVersion}<Avatar name={profile.name} username={profile.username} size={76} />{/key}
      <div class="hero-body">
        <div class="hero-name-row">
          <h1 class="hero-name">{profile.name}</h1>
          {#if isStudent && profile.grade}<span class="pill pill-blue">{profile.grade}</span>{/if}
          <span class="pill pill-gray">{profile.role}</span>
        </div>
        <div class="hero-contact">
          <span class="chip"><Icon name="mail" size={14} /> {profile.email}</span>
          <span class="chip"><Icon name="phone" size={14} /> {profile.phone}</span>
          <span class="chip"><Icon name="pin" size={14} /> {profile.location}</span>
        </div>
        <p class="hero-bio">{profile.bio}</p>
      </div>
      {#if !editing}<button class="btn btn-primary edit-btn" onclick={startEdit}><Icon name="edit" size={14} /> Edit Profile</button>{/if}
    </div>

    {#if editing}
      <form class="edit card" onsubmit={saveEdit}>
        <h3>Edit profile</h3>
        <div class="fields">
          <label>Title
            <select bind:value={draft.title}>
              {#each TITLES as t}<option value={t}>{t || '—'}</option>{/each}
            </select>
          </label>
          <label>Gender
            <select bind:value={draft.gender}>
              {#each GENDERS as g}<option value={g}>{g || '—'}</option>{/each}
            </select>
          </label>
          <label>First name <input bind:value={draft.firstName} required /></label>
          <label>Last name <input bind:value={draft.lastName} required /></label>
          <label class="wide">Profile picture (leave empty to keep the current one)
            <input type="file" accept="image/*" onchange={(e) => (pictureFile = e.target.files?.[0] ?? null)} />
          </label>
          <label>Email <input type="email" bind:value={draft.email} /></label>
          <label>Phone <input bind:value={draft.phone} /></label>
          <label>Location <input bind:value={draft.location} /></label>
          <label class="wide">Address <input bind:value={draft.address} /></label>
          <label class="wide">Bio <textarea rows="2" bind:value={draft.bio}></textarea></label>
          {#if isStudent}
            <label>Guardian name <input bind:value={draft.guardian.name} /></label>
            <label>Guardian relation <input bind:value={draft.guardian.relation} /></label>
            <label>Guardian email <input type="email" bind:value={draft.guardian.email} /></label>
            <label>Guardian phone <input bind:value={draft.guardian.phone} /></label>
          {/if}
          <label>Timezone <input bind:value={draft.preferences.timezone} /></label>
          <label>Notifications
            <select bind:value={draft.preferences.notifications}>
              {#each ['Email & Push', 'Email only', 'Push only', 'None'] as o}<option>{o}</option>{/each}
            </select>
          </label>
        </div>
        {#if saveError}<p class="err">{saveError}</p>{/if}
        <div class="edit-actions">
          <button type="button" class="btn btn-ghost" onclick={() => (editing = false)} disabled={saving}>Cancel</button>
          <button type="submit" class="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
        </div>
      </form>
    {:else}

    {#if saved}<div class="banner ok">{saved}</div>{/if}


    {#if showStats}
      <div class="stats-row">
        {#each profile.stats as s}
          <StatCard icon={STAT_ICON[s.icon] || s.icon} color={s.color} value={s.value} label={s.label} sub={s.sub || ''} progress={s.progress ?? null} />
        {/each}
      </div>
    {/if}


      <div class="grid">
        <div class="col">
          <!-- Personal Information -->
          <div class="info-card card">
            <div class="card-head">
              <h3>Personal Information</h3>
            </div>
            {#each personalRows as r}
              <div class="row">
                <span class="row-icon"><Icon name={r.icon} size={15} /></span>
                <span class="row-label">{r.label}</span>
                <span class="row-value">{r.value}</span>
              </div>
            {/each}
          </div>

        </div>

        <div class="col">
          <!-- Academic Information (students only) -->
          {#if showAcademic}
          <div class="info-card card">
            <div class="card-head">
              <h3>Academic Information</h3>
            </div>
            <div class="row">
              <span class="row-icon"><Icon name="grad" size={15} /></span>
              <span class="row-label">Current Grade</span>
              <span class="row-value"><span class="pill pill-blue">{profile.academic.currentGrade}</span></span>
            </div>
            <div class="row">
              <span class="row-icon"><Icon name="calendar" size={15} /></span>
              <span class="row-label">Academic Year</span>
              <span class="row-value">{profile.academic.academicYear}</span>
            </div>
            <div class="row">
              <span class="row-icon"><Icon name="home" size={15} /></span>
              <span class="row-label">School</span>
              <span class="row-value">{profile.academic.school}</span>
            </div>
            <div class="row top">
              <span class="row-icon"><Icon name="classes" size={15} /></span>
              <span class="row-label">Classes</span>
              <span class="row-value pills">
                {#each profile.academic.classes as c}
                  <span class="pill pill-{c.color}">{c.name}</span>
                {/each}
              </span>
            </div>
          </div>

          {/if}

          <!-- Parent / Guardian (students only) -->
          {#if showGuardian}
          <div class="info-card card">
            <div class="card-head">
              <h3>Parent / Guardian Information</h3>
            </div>
            {#each guardianRows as r}
              <div class="row">
                <span class="row-icon"><Icon name={r.icon} size={15} /></span>
                <span class="row-label">{r.label}</span>
                <span class="row-value">{r.value}</span>
              </div>
            {/each}
          </div>
          {/if}

          <!-- Preferences -->
          <div class="info-card card">
            <div class="card-head">
              <h3>Preferences</h3>
            </div>
            <div class="prefs">
              <div class="pref">
                <span class="pref-icon"><Icon name="clock" size={15} /></span>
                <div>
                  <div class="pref-label">Timezone</div>
                  <div class="pref-value">{profile.preferences.timezone}</div>
                </div>
              </div>
              <div class="pref">
                <span class="pref-icon"><Icon name="bell" size={15} /></span>
                <div>
                  <div class="pref-label">Notifications</div>
                  <div class="pref-value">{profile.preferences.notifications}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    {/if}
  {:else}
    <p class="page-sub">Loading&hellip;</p>
  {/if}
</div>

<style>
  .hero { display: flex; align-items: flex-start; gap: 1.1rem; padding: 1.25rem 1.4rem; }
  .hero-body { flex: 1; min-width: 0; }
  .hero-name-row { display: flex; align-items: center; gap: 0.55rem; flex-wrap: wrap; }
  .hero-name { font-size: 1.35rem; font-weight: 700; }
  .hero-contact { display: flex; gap: 1.1rem; flex-wrap: wrap; margin-top: 0.5rem; }
  .chip { display: inline-flex; align-items: center; gap: 0.35rem; color: var(--text-2); font-size: 0.8rem; }
  .hero-bio { color: var(--text-2); font-size: 0.82rem; margin-top: 0.55rem; }
  .edit-btn { display: inline-flex; align-items: center; gap: 0.4rem; flex-shrink: 0; }

  .stats-row { display: flex; gap: 1rem; margin-top: 1rem; flex-wrap: wrap; }
  .banner { margin-top: 0.8rem; padding: 0.55rem 0.9rem; border-radius: 10px; font-size: 0.84rem; }
  .banner.ok { background: var(--t-green); color: #166534; }
  .edit { margin-top: 1rem; padding: 1.1rem 1.3rem; }
  .edit h3 { font-size: 0.95rem; font-weight: 700; margin-bottom: 0.8rem; }
  .fields { display: grid; grid-template-columns: 1fr 1fr; gap: 0.65rem 0.9rem; }
  .fields label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.76rem; font-weight: 600; color: var(--text-2); }
  .fields .wide { grid-column: 1 / -1; }
  .fields input, .fields select, .fields textarea { border: 1px solid var(--border); border-radius: 9px; padding: 0.48rem 0.7rem; font: inherit; font-size: 0.85rem; color: var(--text); background: #fff; }
  .fields input:focus, .fields select:focus, .fields textarea:focus { outline: none; border-color: var(--primary); }
  .err { color: var(--c-red); font-size: 0.8rem; margin-top: 0.6rem; }
  .edit-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem; }
  @media (max-width: 700px) { .fields { grid-template-columns: 1fr; } }

  .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem; align-items: start; }
  .col { display: flex; flex-direction: column; gap: 1rem; }

  .info-card { padding: 1.1rem 1.2rem; }
  .card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem; }
  .card-head h3 { font-size: 0.95rem; font-weight: 700; }

  .row { display: flex; align-items: center; gap: 0.6rem; padding: 0.55rem 0; border-bottom: 1px solid var(--border); }
  .row:last-child { border-bottom: none; }
  .row.top { align-items: flex-start; }
  .row-icon { color: var(--text-3); width: 20px; display: grid; place-items: center; flex-shrink: 0; }
  .row.top .row-icon { margin-top: 2px; }
  .row-label { color: var(--text-2); font-size: 0.8rem; width: 118px; flex-shrink: 0; }
  .row-value { flex: 1; font-weight: 600; font-size: 0.82rem; color: var(--text); }
  .row-value.pills { display: flex; flex-wrap: wrap; gap: 0.35rem; }


  .prefs { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; }
  .pref { display: flex; align-items: flex-start; gap: 0.5rem; padding: 0.35rem 0; }
  .pref + .pref { border-left: 1px solid var(--border); padding-left: 0.9rem; }
  .pref-icon { color: var(--text-2); display: grid; place-items: center; margin-top: 1px; }
  .pref-label { color: var(--text-2); font-size: 0.75rem; }
  .pref-value { font-weight: 600; font-size: 0.8rem; margin-top: 2px; }

  @media (max-width: 950px) {
    .grid { grid-template-columns: 1fr; }
    .prefs { grid-template-columns: 1fr; }
    .pref + .pref { border-left: none; padding-left: 0; }
  }
</style>
