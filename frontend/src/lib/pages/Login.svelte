<script>
  import Icon from '../components/Icon.svelte'
  import { auth, login } from '../auth.svelte.js'

  let username = $state('')
  let password = $state('')
  let showPassword = $state(false)

  const FEATURES = [
    { icon: 'learning', color: 'blue', title: 'Engaging Learning', sub: 'Access courses, classes and resources.' },
    { icon: 'grad', color: 'green', title: 'Empower Teachers', sub: 'Deliver and track learning effectively.' },
    { icon: 'analytics', color: 'purple', title: 'Insights for Growth', sub: 'Track progress and performance.' },
    { icon: 'classes', color: 'orange', title: 'Simple Administration', sub: 'Manage users, classes, schedules and more.' },
  ]

  async function submit(e) {
    e.preventDefault()
    if (!username || !password) return
    try {
      await login(username, password)
    } catch {
      /* error shown from auth store */
    }
  }

  function demo(user) {
    username = user
    password = { admin1: 'admin123', teacher1: 'teacher123', student1: 'student123' }[user]
  }
</script>

<main class="split">
  <section class="brand-side">
    <div class="brand-inner">
      <div class="logo-row"><span class="logo">📖</span><span class="brand-name">Learning<span class="hub">Hub</span></span></div>
      <p class="motto">Learn. Teach. Manage. Grow.</p>
      <p class="pitch">A modern learning platform for students, teachers and organizations.</p>
      <div class="features">
        {#each FEATURES as f}
          <div class="feature">
            <div class="f-icon {f.color}"><Icon name={f.icon} size={17} /></div>
            <div>
              <div class="f-title">{f.title}</div>
              <div class="f-sub">{f.sub}</div>
            </div>
          </div>
        {/each}
      </div>
    </div>
  </section>

  <section class="form-side">
    <form class="form-box" onsubmit={submit}>
      <h1>Sign in to LearningHub</h1>
      <p class="sub">Access your account to continue</p>

      <label for="username">Username</label>
      <div class="input">
        <Icon name="profile" size={15} />
        <input id="username" placeholder="Enter your username" bind:value={username} autocomplete="username" />
      </div>

      <label for="password">Password</label>
      <div class="input">
        <Icon name="bookmark" size={15} />
        <input
          id="password"
          type={showPassword ? 'text' : 'password'}
          placeholder="Enter your password"
          bind:value={password}
          autocomplete="current-password"
        />
        <button type="button" class="eye" onclick={() => (showPassword = !showPassword)} aria-label="Toggle password">👁</button>
      </div>

      {#if auth.error}<p class="error">{auth.error}</p>{/if}

      <button class="btn btn-primary signin" type="submit" disabled={auth.loading}>
        {auth.loading ? 'Signing in…' : 'Sign In'}
      </button>

      <div class="demo card">
        <span class="info">ⓘ</span>
        <div>
          <div class="demo-title">Demo accounts:</div>
          <div class="demo-links">
            <button type="button" class="link" onclick={() => demo('admin1')}>admin1</button> /
            <button type="button" class="link" onclick={() => demo('teacher1')}>teacher1</button> /
            <button type="button" class="link" onclick={() => demo('student1')}>student1</button>
          </div>
        </div>
      </div>
    </form>
  </section>
</main>

<style>
  .split { display: grid; grid-template-columns: 1fr 1.05fr; min-height: 100vh; }
  .brand-side {
    background:
      linear-gradient(180deg, rgba(13, 27, 54, 0.88) 0%, rgba(15, 33, 66, 0.82) 55%, rgba(20, 42, 82, 0.9) 100%),
      radial-gradient(1000px 500px at 20% 0%, #1c3868 0%, #0e1c38 60%, #0a1428 100%);
    color: #fff; display: flex; align-items: center; padding: 3rem;
  }
  .brand-inner { max-width: 380px; }
  .logo-row { display: flex; align-items: center; gap: 0.5rem; }
  .logo { font-size: 2rem; }
  .brand-name { font-size: 2rem; font-weight: 800; }
  .hub { color: #7ea8ff; }
  .motto { font-size: 1.15rem; font-weight: 600; margin-top: 1.3rem; }
  .pitch { color: #b9c7de; margin-top: 0.5rem; margin-bottom: 2rem; }
  .features { display: flex; flex-direction: column; gap: 0.9rem; }
  .feature {
    display: flex; gap: 0.8rem; align-items: center;
    background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(255, 255, 255, 0.09);
    border-radius: 12px; padding: 0.7rem 0.9rem;
  }
  .f-icon { width: 34px; height: 34px; border-radius: 9px; display: grid; place-items: center; flex-shrink: 0; }
  .f-icon.blue { background: #2f5fd0; }
  .f-icon.green { background: #199c6d; }
  .f-icon.purple { background: #7c5cd6; }
  .f-icon.orange { background: #d98a24; }
  .f-title { font-weight: 600; font-size: 0.9rem; }
  .f-sub { color: #a9b8d0; font-size: 0.76rem; }

  .form-side { background: #f7f9fc; display: grid; place-items: center; padding: 2rem; }
  .form-box { width: min(400px, 92vw); }
  h1 { font-size: 1.5rem; }
  .sub { color: var(--text-2); margin: 0.3rem 0 1.6rem; }
  label { display: block; font-size: 0.8rem; font-weight: 600; margin: 0.9rem 0 0.35rem; }
  .input {
    display: flex; align-items: center; gap: 0.5rem;
    background: #fff; border: 1px solid var(--border); border-radius: 10px;
    padding: 0.6rem 0.8rem; color: var(--text-3);
  }
  .input:focus-within { border-color: var(--primary); }
  .input input { border: none; outline: none; flex: 1; font-size: 0.88rem; color: var(--text); background: none; }
  .eye { background: none; border: none; color: var(--text-3); }
  .error { color: var(--c-red); font-size: 0.8rem; margin-top: 0.6rem; }
  .signin { width: 100%; padding: 0.7rem; font-size: 0.95rem; margin-top: 1.2rem; border-radius: 10px; }
  .signin:disabled { opacity: 0.6; }
  .demo { display: flex; gap: 0.6rem; padding: 0.8rem 1rem; margin-top: 1.3rem; background: #eff4ff; border-color: #d8e4fb; }
  .info { color: var(--primary); font-weight: 700; }
  .demo-title { font-size: 0.78rem; font-weight: 600; }
  .demo-links { font-size: 0.8rem; margin-top: 2px; }
  @media (max-width: 900px) { .split { grid-template-columns: 1fr; } .brand-side { display: none; } }
</style>
