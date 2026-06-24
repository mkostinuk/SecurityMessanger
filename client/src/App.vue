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
  deleteMessage,
  banUser,
  unbanUser,
  promoteUser,
  requestAllUsers,
  allUsers,
  logout
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
    :all-users="allUsers"
    :private-chats="privateChats"
    @send="sendChat"
    @send-private="sendPrivate"
    @delete="deleteMessage"
    @ban="banUser"
    @unban="unbanUser"
    @promote="promoteUser"
    @refresh-users="requestAllUsers"
    @logout="logout"
  />
</template>
