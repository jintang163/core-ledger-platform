import { get, post } from '@/utils/request'
import type {
  Account,
  AccountQueryDTO,
  Page,
  HotAccountConfigVO,
  HotAccountConfigDTO,
  AccountShard
} from '@/types'

export function getAccount(accountId: string): Promise<Account> {
  return get<Account>(`/account/${accountId}`)
}

export function getAccountList(params: AccountQueryDTO): Promise<Page<Account>> {
  return post<Page<Account>>('/account/list', params)
}

export function markAsHotAccount(data: HotAccountConfigDTO): Promise<HotAccountConfigVO> {
  return post<HotAccountConfigVO>('/hot-account/mark', data)
}

export function unmarkAsHotAccount(accountId: string): Promise<void> {
  return post<void>(`/hot-account/unmark/${accountId}`)
}

export function getHotAccountConfig(accountId: string): Promise<HotAccountConfigVO> {
  return get<HotAccountConfigVO>(`/hot-account/config/${accountId}`)
}

export function updateHotAccountConfig(data: HotAccountConfigDTO): Promise<HotAccountConfigVO> {
  return post<HotAccountConfigVO>('/hot-account/config', data)
}

export function getHotAccountList(params?: {
  pageNum?: number
  pageSize?: number
}): Promise<Page<HotAccountConfigVO>> {
  return post<Page<HotAccountConfigVO>>('/hot-account/list', params)
}

export function getAccountShards(mainAccountId: string): Promise<AccountShard[]> {
  return get<AccountShard[]>(`/hot-account/shards/${mainAccountId}`)
}

export function mergeShards(mainAccountId: string): Promise<number> {
  return post<number>(`/hot-account/merge/${mainAccountId}`)
}

export function mergeAllShards(): Promise<void> {
  return post<void>('/hot-account/merge-all')
}
