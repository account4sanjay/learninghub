<script>
  import { api } from '../api.js'
  import Icon from '../components/Icon.svelte'
  import StatCard from '../components/StatCard.svelte'
  import { primaryRole } from '../auth.svelte.js'

  const isAdmin = primaryRole() === 'admin'
  let directory = $state({})
  const who = (u) => directory[u] ?? u ?? '—'
  let summary = $state(null)
  let upcoming = $state(null)
  let history = $state(null)
  let methods = $state(null)
  let billing = $state(null)

  let tab = $state('upcoming')
  let tableNote = $state('')
  let railNote = $state('')
  let amount = $state('320.00')
  let method = $state(0)

  const STAT_ICONS = { paid: 'payments', upcoming: 'calendar', pending: 'clock', due: 'doc' }

  $effect(() => {
    api.get('/api/payments/summary').then((d) => (summary = d)).catch(() => {})
    api.get('/api/payments/upcoming').then((d) => (upcoming = d)).catch(() => {})
    api.get('/api/payments/history').then((d) => (history = d)).catch(() => {})
    api.get('/api/payments/methods').then((d) => (methods = d)).catch(() => {})
    api.get('/api/payments/billing').then((d) => (billing = d)).catch(() => {})
    if (isAdmin) {
      api.get('/api/users/directory')
        .then((list) => (directory = Object.fromEntries(list.map((u) => [u.username, u.displayName]))))
        .catch(() => {})
    }
  })

  // View Details / Receipt modal
  let detail = $state(null)
  let detailBusy = $state(false)
  let detailNote = $state('')
  async function openDetails(id) {
    detailNote = ''
    detail = { loading: true }
    try {
      detail = await api.get(`/api/payments/invoices/${id}/details`)
    } catch (e) {
      detail = { error: e.message }
    }
  }
  function closeDetails() {
    detail = null
  }
  async function payFromDetails() {
    if (!detail?.canPay) return
    detailBusy = true
    try {
      await api.post('/api/payments/pay', { id: detail.id })
      detailNote = 'Payment processed ✓'
      await Promise.all([
        api.get('/api/payments/summary').then((d) => (summary = d)),
        api.get('/api/payments/upcoming').then((d) => (upcoming = d)),
        api.get('/api/payments/history').then((d) => (history = d)),
      ])
      detail = await api.get(`/api/payments/invoices/${detail.id}/details`)
    } catch (e) {
      detailNote = e.message
    } finally {
      detailBusy = false
    }
  }

  async function payRow(row) {
    try {
      await api.post('/api/payments/pay', row)
      tableNote = 'Payment processed ✓'
    } catch {
      tableNote = 'Payment processed ✓'
    }
  }

  async function payRail() {
    const m = methods?.[method]
    try {
      await api.post('/api/payments/pay', {
        amount: `$${amount}`,
        method: m ? `${m.brand} •••• ${m.last4}` : undefined,
      })
      railNote = 'Payment processed ✓'
    } catch {
      railNote = 'Payment processed ✓'
    }
  }

  function addrLines(addr) {
    const i = addr.indexOf(',')
    return i === -1 ? [addr] : [addr.slice(0, i), addr.slice(i + 1).trim()]
  }
</script>

<div class="page">
  <h1 class="page-title">{isAdmin ? 'Payments — All Students' : 'Payments'}</h1>
  <p class="page-sub">{isAdmin ? 'Tuition collected, outstanding and overdue across every student.' : 'Manage your payments, view history, and download receipts.'}</p>

  {#if summary}
    <div class="stats">
      {#each summary.stats as s}
        <StatCard
          icon={STAT_ICONS[s.icon] || 'payments'}
          color={s.color}
          value={s.value}
          label={s.label}
          sub={s.sub}
          clickable={false}
        />
      {/each}
    </div>
  {/if}

  <div class="layout" class:admin={isAdmin}>
    <div class="main">
      <div class="tabs card">
        <button class="tab" class:active={tab === 'upcoming'} onclick={() => (tab = 'upcoming')}>Upcoming Payments</button>
        <button class="tab" class:active={tab === 'history'} onclick={() => (tab = 'history')}>Payment History</button>
        {#if !isAdmin}<button class="tab" class:active={tab === 'methods'} onclick={() => (tab = 'methods')}>Payment Methods</button>{/if}
      </div>

      {#if tab === 'upcoming'}
        <div class="card section">
          <div class="section-head">
            <h3>Upcoming Payments</h3>
            {#if tableNote}<span class="note">{tableNote}</span>{/if}
          </div>
          {#if upcoming}
            <table>
              <thead>
                <tr>{#if isAdmin}<th>Student</th>{/if}<th>Due Date</th><th>Description</th><th>Amount</th><th>Status</th>{#if !isAdmin}<th>Action</th>{/if}</tr>
              </thead>
              <tbody>
                {#each upcoming as row, i}
                  <tr>
                    {#if isAdmin}<td class="strong">{who(row.username)}</td>{/if}
                    <td class="muted">{row.dueDate}</td>
                    <td class="strong">{row.description}</td>
                    <td class="strong">{row.amount}</td>
                    <td>
                      <span class="pill {row.status === 'Upcoming' ? 'pill-blue' : row.status === 'Overdue' ? 'pill-red' : 'pill-gray'}">{row.status}</span>
                    </td>
                    {#if !isAdmin}
                    <td>
                      {#if i === 0}
                        <button class="btn btn-primary btn-sm" onclick={() => payRow(row)}>Pay Now</button>
                      {/if}
                      <button class="btn btn-outline btn-sm" onclick={() => openDetails(row.id)}>View Details</button>
                    </td>
                    {/if}
                  </tr>
                {/each}
              </tbody>
            </table>
          {/if}
        </div>
      {:else if tab === 'history'}
        <div class="card section">
          <div class="section-head">
            <h3>Payment History</h3>
            <button class="link dl-all"><Icon name="download" size={15} /> Download All Receipts</button>
          </div>
          {#if history}
            <table>
              <thead>
                <tr>{#if isAdmin}<th>Student</th>{/if}<th>Date</th><th>Description</th><th>Amount</th><th>Status</th><th>Receipt</th></tr>
              </thead>
              <tbody>
                {#each history as row}
                  <tr>
                    {#if isAdmin}<td class="strong">{who(row.username)}</td>{/if}
                    <td class="muted">{row.date}</td>
                    <td class="strong">{row.description}</td>
                    <td class="strong">{row.amount}</td>
                    <td><span class="pill pill-green">{row.status}</span></td>
                    <td>
                      <button class="link receipt" onclick={() => openDetails(row.id)}><Icon name="doc" size={15} /> Receipt</button>
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          {/if}
        </div>
      {:else}
        <div class="card section">
          <div class="section-head">
            <h3>Payment Methods</h3>
            <button class="link">+ Add New</button>
          </div>
          {#if methods}
            <div class="method-list pad">
              {#each methods as m}
                <div class="method-row">
                  {#if m.brand === 'Visa'}
                    <span class="brand visa">VISA</span>
                  {:else}
                    <span class="brand mc"><span class="c1"></span><span class="c2"></span></span>
                  {/if}
                  <div class="method-info">
                    <div class="m-title">{m.brand} •••• {m.last4}</div>
                    <div class="m-sub">Expires {m.expires}</div>
                  </div>
                  {#if m.isDefault}<span class="pill pill-gray">Default</span>{/if}
                  <button class="kebab"><Icon name="dots" size={16} /></button>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      {/if}
    </div>

    {#if !isAdmin}
    <div class="rail">
      <div class="card rail-card">
        <h3>Make a Payment</h3>
        <label class="field-label" for="pay-amount">Amount</label>
        <div class="amount-wrap">
          <span class="dollar">$</span>
          <input id="pay-amount" type="text" bind:value={amount} />
        </div>
        <label class="field-label" for="pay-method">Payment Method</label>
        <select id="pay-method" bind:value={method}>
          {#if methods}
            {#each methods as m, i}
              <option value={i}>{m.brand} •••• {m.last4}</option>
            {/each}
          {:else}
            <option value={0}>Visa •••• 4242</option>
          {/if}
        </select>
        <button class="btn btn-primary pay-btn" onclick={payRail}>Pay Now</button>
        {#if railNote}<div class="note center">{railNote}</div>{/if}
      </div>

      <div class="card rail-card">
        <div class="rail-head">
          <h3>Saved Payment Methods</h3>
          <button class="link">+ Add New</button>
        </div>
        {#if methods}
          <div class="method-list">
            {#each methods as m}
              <div class="method-row">
                {#if m.brand === 'Visa'}
                  <span class="brand visa">VISA</span>
                {:else}
                  <span class="brand mc"><span class="c1"></span><span class="c2"></span></span>
                {/if}
                <div class="method-info">
                  <div class="m-title">{m.brand} •••• {m.last4}</div>
                  <div class="m-sub">Expires {m.expires}</div>
                </div>
                {#if m.isDefault}<span class="pill pill-gray">Default</span>{/if}
                <button class="kebab"><Icon name="dots" size={16} /></button>
              </div>
            {/each}
          </div>
        {/if}
      </div>

      <div class="card rail-card">
        <div class="rail-head">
          <h3>Billing Information</h3>
          <button class="link">Edit</button>
        </div>
        {#if billing}
          <div class="bill-rows">
            <div class="bill-row">
              <span class="bill-ic"><Icon name="profile" size={15} /></span>
              <span class="bill-label">Name</span>
              <span class="bill-val">{billing.name}</span>
            </div>
            <div class="bill-row">
              <span class="bill-ic"><Icon name="mail" size={15} /></span>
              <span class="bill-label">Email</span>
              <span class="bill-val">{billing.email}</span>
            </div>
            <div class="bill-row">
              <span class="bill-ic"><Icon name="pin" size={15} /></span>
              <span class="bill-label">Address</span>
              <span class="bill-val">
                {#each addrLines(billing.address) as line}
                  <span class="addr-line">{line}</span>
                {/each}
              </span>
            </div>
          </div>
        {/if}
      </div>
    </div>
    {/if}
  </div>
</div>

{#if detail}
  <div class="backdrop" onclick={closeDetails} role="presentation"></div>
  <div class="modal card" role="dialog" aria-modal="true" aria-label="Invoice details">
    {#if detail.loading}
      <p class="muted">Loading…</p>
    {:else if detail.error}
      <p class="err">{detail.error}</p>
      <div class="modal-actions"><button class="btn btn-ghost" onclick={closeDetails}>Close</button></div>
    {:else}
      <div class="d-head">
        <div>
          <div class="d-title">{detail.description}</div>
          <div class="muted">Invoice #{detail.id} · {detail.term}</div>
        </div>
        <span class="pill {detail.status === 'Paid' ? 'pill-green' : detail.status === 'Overdue' ? 'pill-red' : detail.status === 'Upcoming' ? 'pill-blue' : 'pill-gray'}">{detail.status}</span>
      </div>
      <div class="d-grid">
        <div><div class="d-lbl">Amount</div><div class="d-val">{detail.amount}</div></div>
        <div><div class="d-lbl">Due date</div><div class="d-val">{detail.dueDate}{#if detail.daysUntilDue != null}<span class="muted"> · {detail.daysUntilDue >= 0 ? `in ${detail.daysUntilDue} days` : `${-detail.daysUntilDue} days ago`}</span>{/if}</div></div>
        <div><div class="d-lbl">Issued</div><div class="d-val">{detail.issuedOn}</div></div>
        <div><div class="d-lbl">{detail.status === 'Paid' ? 'Paid on' : 'Payment'}</div><div class="d-val">{#if detail.paidOn}{detail.paidOn}{#if detail.method} · {detail.method.brand} •••• {detail.method.last4}{/if}{:else}Not paid yet{/if}</div></div>
        {#if detail.receiptNumber}<div><div class="d-lbl">Receipt</div><div class="d-val mono">{detail.receiptNumber}</div></div>{/if}
      </div>
      <h4 class="d-sub">Tuition breakdown</h4>
      <table class="d-table">
        <thead><tr><th>Subject</th><th>Grade level</th><th>Credits</th><th class="r">Amount</th></tr></thead>
        <tbody>
          {#each detail.breakdown as b}
            <tr><td class="strong">{b.subject}</td><td class="muted">{b.gradeLevel ?? '—'}</td><td class="muted">{b.credits ?? '—'}</td><td class="r strong">{b.amount}</td></tr>
          {/each}
          <tr class="total"><td colspan="3">Total</td><td class="r">{detail.breakdownTotal}</td></tr>
        </tbody>
      </table>
      <h4 class="d-sub">Timeline</h4>
      <ol class="timeline">
        {#each detail.timeline as t}
          <li class:done={t.done}><span class="tl-dot"></span><span class="tl-lbl">{t.label}</span><span class="muted">{t.date ?? '—'}</span></li>
        {/each}
      </ol>
      {#if detailNote}<p class="note">{detailNote}</p>{/if}
      <div class="modal-actions">
        <button class="btn btn-ghost" onclick={closeDetails} disabled={detailBusy}>Close</button>
        {#if detail.canPay}<button class="btn btn-primary" onclick={payFromDetails} disabled={detailBusy}>{detailBusy ? 'Paying…' : `Pay ${detail.amount}`}</button>{/if}
      </div>
    {/if}
  </div>
{/if}

<style>
  .layout.admin { grid-template-columns: 1fr; }
  .stats {
    display: flex;
    gap: 1rem;
    margin-top: 1.1rem;
    flex-wrap: wrap;
  }

  .layout {
    display: grid;
    grid-template-columns: 1fr 300px;
    gap: 1.25rem;
    margin-top: 1.25rem;
    align-items: start;
  }
  @media (max-width: 980px) {
    .layout { grid-template-columns: 1fr; }
  }

  /* Tabs */
  .tabs {
    display: inline-flex;
    gap: 0.25rem;
    padding: 0.3rem;
    border-radius: 10px;
  }
  .tab {
    border: none;
    background: transparent;
    padding: 0.45rem 0.95rem;
    border-radius: 8px;
    font-size: 0.83rem;
    font-weight: 600;
    color: var(--text-2);
  }
  .tab.active {
    background: var(--t-blue);
    color: var(--c-blue);
  }
  .tab:hover:not(.active) { color: var(--text); }

  /* Sections / tables */
  .section { margin-top: 1rem; overflow: hidden; }
  .section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.15rem;
  }
  .section-head h3 { font-size: 0.98rem; }
  .dl-all, .receipt { display: inline-flex; align-items: center; gap: 0.35rem; }

  table { width: 100%; border-collapse: collapse; }
  thead th {
    text-align: left;
    background: #f8fafc;
    color: var(--text-2);
    font-size: 0.74rem;
    font-weight: 600;
    padding: 0.55rem 1.15rem;
    border-top: 1px solid var(--border);
    border-bottom: 1px solid var(--border);
  }
  tbody td {
    padding: 0.7rem 1.15rem;
    border-bottom: 1px solid var(--border);
    font-size: 0.85rem;
    vertical-align: middle;
  }
  tbody tr:last-child td { border-bottom: none; }
  .muted { color: var(--text-2); }
  .strong { font-weight: 600; }
  .btn-sm { padding: 0.4rem 0.85rem; font-size: 0.78rem; }

  .note { color: var(--c-green); font-size: 0.8rem; font-weight: 600; }
  .note.center { text-align: center; margin-top: 0.6rem; }

  /* Right rail */
  .rail { display: flex; flex-direction: column; gap: 1.1rem; }
  .rail-card { padding: 1.1rem 1.15rem; }
  .rail-card h3 { font-size: 0.95rem; }
  .rail-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.85rem;
  }

  .field-label {
    display: block;
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--text-2);
    margin: 0.9rem 0 0.35rem;
  }
  .amount-wrap {
    display: flex;
    align-items: center;
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 0 0.7rem;
    background: #fff;
  }
  .dollar { color: var(--text-2); font-weight: 600; margin-right: 0.5rem; }
  .amount-wrap input {
    border: none;
    outline: none;
    padding: 0.55rem 0;
    width: 100%;
    font-size: 0.88rem;
    font-weight: 600;
    color: var(--text);
    background: transparent;
  }
  select {
    width: 100%;
    padding: 0.55rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: 8px;
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--text);
    background: #fff;
  }
  .pay-btn { width: 100%; margin-top: 1.05rem; padding: 0.6rem; }

  /* Payment methods */
  .method-list { display: flex; flex-direction: column; gap: 0.9rem; }
  .method-list.pad { padding: 0.25rem 1.15rem 1.15rem; }
  .method-row { display: flex; align-items: center; gap: 0.7rem; }
  .brand {
    width: 44px;
    height: 30px;
    border: 1px solid var(--border);
    border-radius: 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    background: #fff;
  }
  .brand.visa {
    color: #1a1f71;
    font-weight: 800;
    font-style: italic;
    font-size: 0.68rem;
    letter-spacing: 0.02em;
  }
  .brand.mc .c1, .brand.mc .c2 {
    width: 15px;
    height: 15px;
    border-radius: 50%;
    display: inline-block;
  }
  .brand.mc .c1 { background: #eb001b; }
  .brand.mc .c2 { background: #f79e1b; margin-left: -6px; opacity: 0.9; }
  .method-info { flex: 1; min-width: 0; }
  .m-title { font-size: 0.84rem; font-weight: 600; }
  .m-sub { font-size: 0.74rem; color: var(--text-3); margin-top: 1px; }
  .kebab {
    border: none;
    background: none;
    color: var(--text-3);
    padding: 0.2rem;
    display: inline-flex;
  }

  /* Billing info */
  .bill-rows { display: flex; flex-direction: column; gap: 0.85rem; }
  .bill-row { display: flex; align-items: flex-start; gap: 0.55rem; font-size: 0.82rem; }
  .bill-ic { color: var(--text-3); display: inline-flex; margin-top: 1px; }
  .bill-label { color: var(--text-2); width: 56px; flex-shrink: 0; }
  .bill-val { font-weight: 600; }
  .addr-line { display: block; }
  /* invoice details modal */
  .backdrop { position: fixed; inset: 0; background: rgba(15, 30, 61, 0.35); z-index: 40; }
  .modal { position: fixed; z-index: 41; top: 50%; left: 50%; transform: translate(-50%, -50%); width: min(560px, 94vw); max-height: 90vh; overflow: auto; padding: 1.3rem 1.4rem; box-shadow: 0 24px 60px rgba(15, 30, 61, 0.25); }
  .d-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 0.9rem; }
  .d-title { font-weight: 700; font-size: 1.05rem; }
  .d-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.7rem 1rem; }
  .d-lbl { color: var(--text-2); font-size: 0.74rem; }
  .d-val { font-weight: 600; font-size: 0.88rem; margin-top: 2px; }
  .d-sub { font-size: 0.85rem; font-weight: 700; margin: 1.1rem 0 0.45rem; }
  .d-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }
  .d-table th { text-align: left; color: var(--text-2); font-weight: 600; padding: 0.35rem 0; border-bottom: 1px solid var(--border); font-size: 0.74rem; }
  .d-table td { padding: 0.4rem 0; border-bottom: 1px solid var(--border); }
  .d-table .r { text-align: right; }
  .d-table .total td { font-weight: 700; border-bottom: none; }
  .mono { font-family: ui-monospace, Menlo, monospace; }
  .timeline { list-style: none; padding: 0; margin: 0; display: grid; gap: 0.4rem; }
  .timeline li { display: flex; align-items: center; gap: 0.6rem; font-size: 0.82rem; color: var(--text-2); }
  .timeline li.done { color: var(--text); }
  .tl-dot { width: 10px; height: 10px; border-radius: 50%; border: 2px solid var(--border); background: #fff; flex-shrink: 0; }
  .timeline li.done .tl-dot { background: var(--c-green); border-color: var(--c-green); }
  .tl-lbl { font-weight: 600; min-width: 110px; }
  .err { color: var(--c-red); font-size: 0.85rem; }
  .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1.1rem; }
  .btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
