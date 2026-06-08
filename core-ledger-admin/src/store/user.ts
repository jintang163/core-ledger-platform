import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface UserInfo {
  id: string
  username: string
  nickname: string
  role: string
  avatar?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  const setToken = (t: string) => {
    token.value = t
  }

  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
  }

  const login = (username: string, password: string) => {
    return new Promise<UserInfo>((resolve) => {
      setTimeout(() => {
        const mockUser: UserInfo = {
          id: '1',
          username,
          nickname: '运营管理员',
          role: 'admin',
          avatar: ''
        }
        setToken('mock-token-' + Date.now())
        setUserInfo(mockUser)
        resolve(mockUser)
      }, 500)
    })
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    login,
    logout
  }
}, {
  persist: {
    key: 'core-ledger-admin-user',
    paths: ['token', 'userInfo']
  }
})
