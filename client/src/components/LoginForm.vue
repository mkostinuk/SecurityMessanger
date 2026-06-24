<script setup>
import { ref } from 'vue'

defineProps({ status: String })
const emit = defineEmits(['login', 'register'])

const mode = ref('login')
const username = ref('')
const password = ref('')
const displayName = ref('')
const phone = ref('')
const about = ref('')
const error = ref('')

function login() {
  if (username.value.trim() === '' || password.value === '') {
    error.value = 'enter login and password'
    return
  }
  error.value = ''
  emit('login', username.value.trim(), password.value)
}

function register() {
  const name = username.value.trim()
  if (name === '' || password.value === '') {
    error.value = 'enter login and password'
    return
  }
  if (name.length < 3 || name.length > 20) {
    error.value = 'username must be 3-20 characters'
    return
  }
  if (password.value.length < 6) {
    error.value = 'password must be at least 6 characters'
    return
  }
  error.value = ''
  emit('register', name, password.value, displayName.value.trim(), phone.value.trim(), about.value.trim())
}

function submit() {
  if (mode.value === 'login') login()
  else register()
}

function switchTo(next) {
  mode.value = next
  error.value = ''
}
</script>

<template>
  <div class="login-screen">
    <div class="card login">
      <h1>Security Messenger</h1>
      <p class="subtitle">encrypted real-time chat</p>

      <input v-model="username" placeholder="Login" @keyup.enter="submit" />
      <input v-model="password" type="password" placeholder="Password" @keyup.enter="submit" />

      <template v-if="mode === 'register'">
        <input v-model="displayName" maxlength="50" placeholder="Display name" />
        <input v-model="phone" maxlength="30" placeholder="Phone" />
        <textarea v-model="about" maxlength="300" rows="2" placeholder="About you"></textarea>
      </template>

      <button class="primary wide" @click="submit">
        {{ mode === 'login' ? 'Log In' : 'Create account' }}
      </button>

      <p class="switch">
        <template v-if="mode === 'login'">
          No account? <a @click="switchTo('register')">Sign up</a>
        </template>
        <template v-else>
          Have an account? <a @click="switchTo('login')">Log in</a>
        </template>
      </p>

      <p class="status">{{ error || status }}</p>
    </div>
  </div>
</template>
