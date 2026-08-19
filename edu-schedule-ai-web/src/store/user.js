import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('edu_token') || '',
    user: JSON.parse(localStorage.getItem('edu_user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.user?.role || '',
    realName: (state) => state.user?.realName || state.user?.username || ''
  },
  actions: {
    setToken(token) {
      this.token = token
      if (token) {
        localStorage.setItem('edu_token', token)
      } else {
        localStorage.removeItem('edu_token')
      }
    },
    setUser(user) {
      this.user = user
      if (user) {
        localStorage.setItem('edu_user', JSON.stringify(user))
      } else {
        localStorage.removeItem('edu_user')
      }
    },
    logout() {
      this.setToken('')
      this.setUser(null)
    }
  }
})
