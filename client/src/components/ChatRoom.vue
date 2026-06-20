<script setup>
import { ref, watch, nextTick, computed } from 'vue'

const props = defineProps({
  me: Object,
  status: String,
  messages: Array,
  users: Array,
  privateChats: Object
})
const emit = defineEmits(['send', 'send-private', 'delete'])

const draft = ref('')
const listRef = ref(null)
const activeChat = ref('general')

const initial = computed(() => (props.me.username || '?').charAt(0).toUpperCase())

const otherUsers = computed(() =>
  props.users.filter((u) => u.username !== props.me.username)
)

const openDialogs = computed(() => Object.keys(props.privateChats))

const isPrivate = computed(() => activeChat.value !== 'general')

const canModerate = computed(
  () => props.me.role === 'MODERATOR' || props.me.role === 'ADMIN'
)

const currentMessages = computed(() => {
  if (!isPrivate.value) {
    return props.messages.map((m) => ({ id: m.id, author: m.username, text: m.text }))
  }
  const conv = props.privateChats[activeChat.value] || []
  return conv.map((m) => ({ id: null, author: m.from, text: m.text }))
})

function openChat(username) {
  if (username === props.me.username) return
  activeChat.value = username
}

function submit() {
  const text = draft.value.trim()
  if (text === '') return
  if (isPrivate.value) {
    emit('send-private', activeChat.value, text)
  } else {
    emit('send', text)
  }
  draft.value = ''
}

watch(
  [() => currentMessages.value.length, activeChat],
  async () => {
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  }
)
</script>

<template>
  <div class="card chat">
    <aside class="sidebar">
      <div class="brand">Security Messenger</div>

      <nav class="channels">
        <div
          class="channel"
          :class="{ active: activeChat === 'general' }"
          @click="activeChat = 'general'"
        >
          # general
        </div>
        <div
          v-for="name in openDialogs"
          :key="name"
          class="channel"
          :class="{ active: activeChat === name }"
          @click="activeChat = name"
        >
          🔒 {{ name }}
        </div>
      </nav>

      <div class="online">
        <div class="online-title">Online — {{ users.length }}</div>
        <div
          v-for="u in otherUsers"
          :key="u.username"
          class="online-user"
          @click="openChat(u.username)"
        >
          <span class="dot"></span>
          <span class="online-name">{{ u.username }}</span>
          <span v-if="u.role !== 'USER'" class="role">{{ u.role }}</span>
        </div>
      </div>

      <div class="profile">
        <div class="avatar">{{ initial }}</div>
        <div class="profile-info">
          <strong>{{ me.username }}</strong>
          <span class="role">{{ me.role }}</span>
        </div>
      </div>
    </aside>

    <section class="main">
      <header class="chat-header">
        <strong v-if="!isPrivate"># general</strong>
        <strong v-else>🔒 {{ activeChat }}</strong>
        <span class="status">{{ status }}</span>
      </header>

      <div class="messages" ref="listRef">
        <div
          v-for="(m, i) in currentMessages"
          :key="i"
          class="bubble"
          :class="{ own: m.author === me.username }"
        >
          <span class="author">{{ m.author }}</span>
          <span class="text">{{ m.text }}</span>
          <button
            v-if="canModerate && m.id"
            class="del"
            title="delete message"
            @click="emit('delete', m.id)"
          >
            ✕
          </button>
        </div>
        <p v-if="currentMessages.length === 0" class="empty">
          {{ isPrivate ? 'Private chat — messages are encrypted' : 'No messages yet — say hi' }}
        </p>
      </div>

      <div class="composer">
        <input v-model="draft" placeholder="Type a message..." @keyup.enter="submit" />
        <button class="primary" @click="submit">Send</button>
      </div>
    </section>
  </div>
</template>
