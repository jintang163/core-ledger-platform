import { create } from 'zustand'
import type { Account } from '@/api/types'

// 账户状态接口
interface AccountState {
  // 当前选中的账户
  selectedAccount: Account | null
  // 账户列表
  accountList: Account[]
  // 列表加载状态
  loading: boolean
  // 选中账户
  setSelectedAccount: (account: Account | null) => void
  // 设置账户列表
  setAccountList: (accounts: Account[]) => void
  // 刷新账户列表（供外部API调用后更新）
  refreshAccountList: (accounts: Account[]) => void
  // 根据账户ID获取账户信息
  getAccountById: (accountId: string) => Account | undefined
  // 清空账户状态
  clearAccountState: () => void
  // 设置加载状态
  setLoading: (loading: boolean) => void
}

// 账户状态管理
export const useAccountStore = create<AccountState>((set, get) => ({
  // 初始状态
  selectedAccount: null,
  accountList: [],
  loading: false,

  // 选中账户
  setSelectedAccount: (account: Account | null) => {
    set({ selectedAccount: account })
  },

  // 设置账户列表
  setAccountList: (accounts: Account[]) => {
    set({ accountList: accounts })
  },

  // 刷新账户列表
  refreshAccountList: (accounts: Account[]) => {
    const { selectedAccount } = get()
    // 如果当前有选中的账户，尝试在新列表中找到并保持选中
    if (selectedAccount) {
      const updatedAccount = accounts.find(
        (acc) => acc.accountId === selectedAccount.accountId
      )
      set({
        accountList: accounts,
        selectedAccount: updatedAccount || null,
      })
    } else {
      set({ accountList: accounts })
    }
  },

  // 根据账户ID获取账户信息
  getAccountById: (accountId: string) => {
    const { accountList } = get()
    return accountList.find((acc) => acc.accountId === accountId)
  },

  // 清空账户状态
  clearAccountState: () => {
    set({
      selectedAccount: null,
      accountList: [],
      loading: false,
    })
  },

  // 设置加载状态
  setLoading: (loading: boolean) => {
    set({ loading })
  },
}))

export default useAccountStore
