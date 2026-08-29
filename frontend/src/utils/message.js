import { reactive } from 'vue'

export const messages = reactive([])

let seq = 0

export function showMessage(text, type = 'error') {
  const id = ++seq
  messages.push({ id, text, type })
  setTimeout(() => {
    const idx = messages.findIndex((m) => m.id === id)
    if (idx !== -1) messages.splice(idx, 1)
  }, 3500)
}

export function showError(text) {
  showMessage(text, 'error')
}

export function showSuccess(text) {
  showMessage(text, 'success')
}
