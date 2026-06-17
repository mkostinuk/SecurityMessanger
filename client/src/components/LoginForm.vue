<script setup>
import { ref } from 'vue'

defineProps({ status: String })
const emit = defineEmits(['login', 'register'])

const username = ref('')
const password = ref('')
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
      @keyup.enter="emit('login', username, password)"
    />

    <div class="buttons">
      <button class="primary" @click="emit('login', username, password)">Log In</button>
      <button class="ghost" @click="emit('register', username, password)">Sign in</button>
    </div>

    <p class="status">{{ status }}</p>
    </div>
  </div>
</template>
