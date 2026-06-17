<script setup>
import { ref, watch, nextTick, computed } from 'vue'

const props = defineProps({
  me: Object,
  status: String,
  messages: Array,
  users: Array,
})
const emit = defineEmits(['send'])

const draft = ref('')
const listRef = ref(null)

const initial = computed(() => (props.me.username || '?').charAt(0).toUpperCase())

function submit() {
  const text = draft.value.trim()
  if (text === '') return
  emit('send', text)
  draft.value = ''
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  },
)
</script>

<template>
  <div class="card chat">
    <aside class="sidebar">
      <div class="brand">Security Messenger</div>

      <nav class="channels">
        <div class="channel active"># general</div>
      </nav>

      <div class="online">
        <div class="online-title">Online — {{ users.length }}</div>
        <div v-for="u in users" :key="u.username" class="online-user">
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
        <strong># general</strong>
        <span class="status">{{ status }}</span>
      </header>

      <div class="messages" ref="listRef">
        <div v-for="(m, i) in messages" :key="i" class="bubble" :class="{ own: m.username === me.username }">
          <span class="author">{{ m.username }}</span>
          <span class="text">{{ m.text }}</span>
        </div>
        <p v-if="messages.length === 0" class="empty">No messages yet — say hi</p>
      </div>

      <div class="composer">
        <input v-model="draft" placeholder="Type a message..." @keyup.enter="submit" />
        <button class="primary" @click="submit">Send</button>
      </div>
    </section>
  </div>
</template>
