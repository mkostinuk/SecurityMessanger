const ALGO = { name: 'RSA-OAEP', hash: 'SHA-256' }

export async function generateKeyPair() {
  return crypto.subtle.generateKey(
    {
      name: 'RSA-OAEP',
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: 'SHA-256'
    },
    true,
    ['encrypt', 'decrypt']
  )
}

export async function exportPublicKey(publicKey) {
  const spki = await crypto.subtle.exportKey('spki', publicKey)
  return bufferToBase64(spki)
}

export async function importPublicKey(base64) {
  const buffer = base64ToBuffer(base64)
  return crypto.subtle.importKey('spki', buffer, ALGO, true, ['encrypt'])
}

export async function encrypt(publicKey, text) {
  const data = new TextEncoder().encode(text)
  const encrypted = await crypto.subtle.encrypt(ALGO, publicKey, data)
  return bufferToBase64(encrypted)
}

export async function decrypt(privateKey, base64) {
  const buffer = base64ToBuffer(base64)
  const decrypted = await crypto.subtle.decrypt(ALGO, privateKey, buffer)
  return new TextDecoder().decode(decrypted)
}

function bufferToBase64(buffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
}

function base64ToBuffer(base64) {
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0)).buffer
}
