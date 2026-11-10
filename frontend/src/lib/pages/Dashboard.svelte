<script>
  import { primaryRole } from '../auth.svelte.js'
  import DashboardStudent from './DashboardStudent.svelte'
  import DashboardTeacher from './DashboardTeacher.svelte'
  import DashboardAdmin from './DashboardAdmin.svelte'

  const role = $derived(primaryRole())
</script>

{#if role === 'admin'}
  <DashboardAdmin />
{:else if role === 'teacher'}
  <DashboardTeacher />
{:else}
  <DashboardStudent />
{/if}
