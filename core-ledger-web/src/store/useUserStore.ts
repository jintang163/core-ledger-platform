import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// 用户信息接口
export interface UserInfo {
  userId: string
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  role?: string
}

// 用户状态接口
interface UserState {
  userInfo: UserInfo | null
  token: string | null
  isLoggedIn: boolean
  login: (userInfo: UserInfo, token: string) => void
  logout: () => void
  updateUserInfo: (userInfo: Partial<UserInfo>) => void
  clearUser: () => void
}

// localStorage 存储键名
const STORAGE_KEY = 'core-ledger-user'

// 用户状态管理
export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      // 初始状态
      userInfo: null,
      token: null,
      isLoggedIn: false,

      // 登录方法
      login: (userInfo: UserInfo, token: string) => {
        set({
          userInfo,
          token,
          isLoggedIn: true,
        })
      },

      // 退出登录方法
      logout: () => {
        set({
          userInfo: null,
          token: null,
          isLoggedIn: false,
        })
        // 清除其他可能的存储
        localStorage.removeItem(STORAGE_KEY)
      },

      // 更新用户信息
      updateUserInfo: (userInfo: Partial<UserInfo>) => {
        set((state) => ({
          userInfo: state.userInfo ? { ...state.userInfo, ...userInfo } : null,
        }))
      },

      // 清除用户数据（内部方法）
      clearUser: () => {
        set({
          userInfo: null,
          token: null,
          isLoggedIn: false,
        })
      },
    }),
    {
      // 持久化配置
      name: STORAGE_KEY,
      // 选择需要持久化的字段
      partialize: (state) => ({
        userInfo: state.userInfo,
        token: state.token,
        isLoggedIn: state.isLoggedIn,
      }),
    }
  )
)

export default useUserStore
