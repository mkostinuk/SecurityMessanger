<script setup>
import { ref, reactive } from 'vue'
import LoginForm from './components/LoginForm.vue'
import ChatRoom from './components/ChatRoom.vue'

const SERVER_URL = 'ws://localhost:8080'

const status = ref('connecting...')
const loggedIn = ref(false)
const me = reactive({ username: '', role: '' })
const messages = ref([])

const socket = new WebSocket(SERVER_URL)
socket.onopen = () => { status.value = 'connected' }
socket.onclose = () => { status.value = 'disconnected'; loggedIn.value = false }
socket.onmessage = (event) => onPacket(JSON.parse(event.data))

function send(type, data) {
  socket.send(JSON.stringify({ type, data }))
}

function login(username, password) {
  send('LOGIN', { username, password })
}

function register(username, password) {
  send('REGISTER', { username, password })
}

function sendChat(text) {
  send('CHAT_MSG', { text })
}

function onPacket(packet) {
  switch (packet.type) {
    case 'AUTH_SUCCESS':
      me.username = packet.data.username
      me.role = packet.data.role
      loggedIn.value = true
      status.value = 'online'
      break
    case 'AUTH_FAIL':
      status.value = packet.data.message
      break
    case 'HISTORY':
      messages.value = JSON.parse(packet.data.messages)
      break
    case 'CHAT_MSG':
      messages.value.push({ username: packet.data.username, text: packet.data.text })
      break
    case 'ERROR':
      status.value = 'error: ' + packet.data.message
      break
  }
}
</script>

<template>
  <div class="app">
    <LoginForm v-if="!loggedIn" :status="status" @login="login" @register="register" />
    <ChatRoom v-else :me="me" :status="status" :messages="messages" @send="sendChat" />
  </div>
</template>
