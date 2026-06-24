import { ref, reactive } from 'vue'
import { generateKeyPair, exportPublicKey, importPublicKey, encrypt, decrypt } from './crypto.js'

const SERVER_URL = 'ws://localhost:8080'

export function useChat() {
  const status = ref('connecting...')
  const loggedIn = ref(false)
  const me = reactive({ username: '', role: '', displayName: '', phone: '', about: '' })
  const messages = ref([])
  const onlineUsers = ref([])
  const allUsers = ref([])
  const privateChats = reactive({})

  let keyPair = null
  const publicKeys = {}

  let socket = null
  let reconnectTimer = null

  function connect() {
    socket = new WebSocket(SERVER_URL)
    socket.onopen = () => { status.value = 'connected' }
    socket.onmessage = (event) => onPacket(JSON.parse(event.data))
    socket.onclose = () => {
      status.value = 'disconnected, reconnecting...'
      loggedIn.value = false
      reconnectTimer = setTimeout(connect, 2000)
    }
  }
  connect()

  function send(type, data) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      status.value = 'no connection to server'
      return
    }
    socket.send(JSON.stringify({ type, data }))
  }

  function logout() {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    socket.onclose = null
    socket.close()
    loggedIn.value = false
    me.username = ''
    me.role = ''
    me.displayName = ''
    me.phone = ''
    me.about = ''
    messages.value = []
    onlineUsers.value = []
    allUsers.value = []
    for (const name of Object.keys(privateChats)) delete privateChats[name]
    for (const name of Object.keys(publicKeys)) delete publicKeys[name]
    keyPair = null
    status.value = 'connecting...'
    connect()
  }

  function login(username, password) {
    send('LOGIN', { username, password })
  }

  function register(username, password, displayName, phone, about) {
    send('REGISTER', { username, password, displayName, phone, about })
  }

  function sendChat(text) {
    send('CHAT_MSG', { text })
  }

  function deleteMessage(id) {
    send('ADMIN_ACTION', { action: 'DELETE_MSG', messageId: String(id) })
  }

  function banUser(username) {
    send('ADMIN_ACTION', { action: 'BAN_USER', target: username })
  }

  function unbanUser(username) {
    send('ADMIN_ACTION', { action: 'UNBAN', target: username })
  }

  function promoteUser(username) {
    send('ADMIN_ACTION', { action: 'PROMOTE', target: username })
  }

  function requestAllUsers() {
    send('ADMIN_ACTION', { action: 'LIST_USERS' })
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
        me.displayName = packet.data.displayName || ''
        me.phone = packet.data.phone || ''
        me.about = packet.data.about || ''
        loggedIn.value = true
        status.value = 'online'
        keyPair = await generateKeyPair()
        send('PUBLIC_KEY', { key: await exportPublicKey(keyPair.publicKey) })
        if (me.role === 'ADMIN') requestAllUsers()
        break
      case 'ALL_USERS':
        allUsers.value = JSON.parse(packet.data.users)
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
        {
          const mine = onlineUsers.value.find((u) => u.username === me.username)
          if (mine) me.role = mine.role
        }
        break
      case 'CHAT_MSG':
        messages.value.push({
          id: Number(packet.data.id),
          username: packet.data.username,
          text: packet.data.text
        })
        break
      case 'MSG_DELETED':
        messages.value = messages.value.filter((m) => m.id !== Number(packet.data.id))
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
    sendPrivate,
    deleteMessage,
    banUser,
    unbanUser,
    promoteUser,
    requestAllUsers,
    allUsers,
    logout
  }
}
