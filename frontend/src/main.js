import { mount } from 'svelte'
import './app.css'
import App from './App.svelte'
import { restoreSession } from './lib/auth.svelte.js'

restoreSession().finally(() => {
  mount(App, { target: document.getElementById('app') })
})
