<script setup>
import { ref } from 'vue'

defineProps({ status: String })
const emit = defineEmits(['login', 'register'])

const username = ref('')
const password = ref('')
const error = ref('')

function check() {
  if (username.value.trim() === '' || password.value.trim() === '') {
    error.value = 'enter login and password'
    return false
  }
  error.value = ''
  return true
}

function login() {
  if (check()) emit('login', username.value, password.value)
}

function register() {
  if (check()) emit('register', username.value, password.value)
}
</script>

<template>
  <div class="login-screen">
    <div class="card login">
      <h1>Security Messenger</h1>
      <p class="subtitle">encrypted real-time chat</p>

      <input v-model="username" placeholder="Login" />
      <input
        v-model="password"
        type="password"
        placeholder="Password"
        @keyup.enter="login"
      />

      <div class="buttons">
        <button class="primary" @click="login">Log In</button>
        <button class="ghost" @click="register">Sign in</button>
      </div>

      <p class="status">{{ error || status }}</p>
    </div>
  </div>
</template>
