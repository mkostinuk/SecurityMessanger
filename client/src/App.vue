<script setup>
import { useChat } from './composables/useChat.js'
import LoginForm from './components/LoginForm.vue'
import ChatRoom from './components/ChatRoom.vue'

const {
  status,
  loggedIn,
  me,
  messages,
  onlineUsers,
  privateChats,
  login,
  register,
  sendChat,
  sendPrivate,
  deleteMessage
} = useChat()
</script>

<template>
  <LoginForm v-if="!loggedIn" :status="status" @login="login" @register="register" />
  <ChatRoom
    v-else
    :me="me"
    :status="status"
    :messages="messages"
    :users="onlineUsers"
    :private-chats="privateChats"
    @send="sendChat"
    @send-private="sendPrivate"
    @delete="deleteMessage"
  />
</template>
