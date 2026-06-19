import { ref, reactive } from 'vue'
import { generateKeyPair, exportPublicKey, importPublicKey, encrypt, decrypt } from './crypto.js'

const SERVER_URL = 'ws://localhost:8080'

export function useChat() {
  const status = ref('connecting...')
  const loggedIn = ref(false)
  const me = reactive({ username: '', role: '' })
  const messages = ref([])
  const onlineUsers = ref([])
  const privateChats = reactive({})

  let keyPair = null
  const publicKeys = {}

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

  async function sendPrivate(toUser, text) {
    const key = publicKeys[toUser]
    if (!key) {
      status.value = 'no encryption key for ' + toUser + ' yet'
      return
    }
    const payload = await encrypt(key, text)
    send('PRIVATE_MSG', { to: toUser, payload })
    addPrivate(toUser, me.username, text)
  }

  function addPrivate(chatUser, from, text) {
    if (!privateChats[chatUser]) {
      privateChats[chatUser] = []
    }
    privateChats[chatUser].push({ from, text })
  }

  async function onPacket(packet) {
    switch (packet.type) {
      case 'AUTH_SUCCESS':
        me.username = packet.data.username
        me.role = packet.data.role
        loggedIn.value = true
        status.value = 'online'
        keyPair = await generateKeyPair()
        send('PUBLIC_KEY', { key: await exportPublicKey(keyPair.publicKey) })
        break
      case 'AUTH_FAIL':
        status.value = packet.data.message
        break
      case 'HISTORY':
        messages.value = JSON.parse(packet.data.messages)
        break
      case 'USER_LIST':
        onlineUsers.value = JSON.parse(packet.data.users)
        for (const user of onlineUsers.value) {
          if (user.publicKey && !publicKeys[user.username]) {
            publicKeys[user.username] = await importPublicKey(user.publicKey)
          }
        }
        break
      case 'CHAT_MSG':
        messages.value.push({ username: packet.data.username, text: packet.data.text })
        break
      case 'PRIVATE_MSG': {
        const from = packet.data.from
        const text = await decrypt(keyPair.privateKey, packet.data.payload)
        addPrivate(from, from, text)
        break
      }
      case 'ERROR':
        status.value = 'error: ' + packet.data.message
        break
    }
  }

  return {
    status,
    loggedIn,
    me,
    messages,
    onlineUsers,
    privateChats,
    login,
    register,
    sendChat,
    sendPrivate
  }
}
