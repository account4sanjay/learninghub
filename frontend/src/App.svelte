<script>
  import { auth, primaryRole } from './lib/auth.svelte.js'
  import { server } from './lib/server.svelte.js'
  import { router, segments } from './lib/router.svelte.js'
  import Shell from './lib/Shell.svelte'
  import ServerStarting from './lib/components/ServerStarting.svelte'
  import Login from './lib/pages/Login.svelte'
  import Dashboard from './lib/pages/Dashboard.svelte'
  import Schedule from './lib/pages/Schedule.svelte'
  import ScheduleBrowse from './lib/pages/ScheduleBrowse.svelte'
  import MyClasses from './lib/pages/MyClasses.svelte'
  import Learning from './lib/pages/Learning.svelte'
  import Assessments from './lib/pages/Assessments.svelte'
  import AssessmentDetails from './lib/pages/AssessmentDetails.svelte'
  import QuizTake from './lib/pages/QuizTake.svelte'
  import QuizReview from './lib/pages/QuizReview.svelte'
  import Payments from './lib/pages/Payments.svelte'
  import Profile from './lib/pages/Profile.svelte'
  import AdminUsers from './lib/pages/AdminUsers.svelte'
  import AdminSubjects from './lib/pages/AdminSubjects.svelte'
  import AdminEnrollment from './lib/pages/AdminEnrollment.svelte'
  import TeacherStudents from './lib/pages/TeacherStudents.svelte'
  import AssessmentEditor from './lib/pages/AssessmentEditor.svelte'
  import TeacherSubmissions from './lib/pages/TeacherSubmissions.svelte'

  const view = $derived.by(() => {
    const seg = segments()
    const [root, id, sub] = seg
    const role = primaryRole()
    if (!root || root === 'dashboard') return { c: Dashboard }
    if (root === 'schedule') {
      if (id === 'browse') return { c: role === 'student' ? ScheduleBrowse : Schedule }
      return { c: Schedule }
    }
    if (root === 'my-classes') return { c: role === 'admin' ? Dashboard : MyClasses }
    if (root === 'learning') return { c: Learning }
    if (root === 'students') return { c: role === 'teacher' ? TeacherStudents : role === 'admin' ? AdminEnrollment : Dashboard }
    if (root === 'assessments') {
      if (!id) return { c: Assessments }
      if (id === 'submissions') return { c: role === 'student' ? Assessments : TeacherSubmissions }
      if (id === 'new') return { c: role === 'teacher' ? AssessmentEditor : Assessments }
      if (sub === 'edit') return { c: role === 'teacher' ? AssessmentEditor : Assessments, props: { id } }
      if (sub === 'take') return { c: role === 'student' ? QuizTake : Assessments, props: { id } }
      if (sub === 'review') return { c: role === 'student' ? QuizReview : Assessments, props: { id } }
      return { c: AssessmentDetails, props: { id } }
    }
    if (root === 'payments') return { c: role === 'teacher' ? Dashboard : Payments }
    if (root === 'profile') return { c: Profile }
    if (root === 'admin' && auth.user?.roles?.includes('admin')) return { c: id === 'subjects' ? AdminSubjects : id === 'enrollment' ? AdminEnrollment : AdminUsers }
    return { c: Dashboard }
  })
  const Page = $derived(view.c)
</script>

{#if server.starting}
  <ServerStarting />
{/if}

{#if !auth.user}
  <Login />
{:else}
  <Shell>
    <Page {...view.props ?? {}} />
  </Shell>
{/if}
