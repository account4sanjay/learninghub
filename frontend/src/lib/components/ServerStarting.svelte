<script>
  import { server } from '../server.svelte.js'
</script>

<div class="overlay">
  <div class="box card">
    <div class="spinner"></div>
    <h2>Server is starting…</h2>
    <p>
      The backend services are booting and registering with service discovery.
      This usually takes about a minute after the stack starts.
    </p>
    <p class="retry">Retrying automatically — attempt {server.attempts}. The page will refresh when the server is ready.</p>
  </div>
</div>

<style>
  .overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(15, 30, 61, 0.55);
    backdrop-filter: blur(3px);
    display: grid;
    place-items: center;
  }
  .box {
    max-width: 420px;
    padding: 2.2rem 2.5rem;
    text-align: center;
  }
  .spinner {
    width: 42px;
    height: 42px;
    margin: 0 auto 1.1rem;
    border-radius: 50%;
    border: 4px solid var(--t-blue);
    border-top-color: var(--primary);
    animation: spin 0.9s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }
  h2 { font-size: 1.15rem; margin-bottom: 0.6rem; }
  p { color: var(--text-2); font-size: 0.86rem; line-height: 1.5; }
  .retry { margin-top: 0.8rem; color: var(--text-3); font-size: 0.78rem; }
</style>
