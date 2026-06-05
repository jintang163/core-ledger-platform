import { post, get } from './request';
import {
  Account,
  AccountCreateDTO,
  AccountFreezeDTO,
  AccountUnfreezeDTO,
  AccountCloseDTO,
  Page,
} from './types';

const BASE_URL = '/api/account';

export function createAccount(data: AccountCreateDTO): Promise<Account> {
  return post<Account>(`${BASE_URL}/create`, data);
}

export function getAccount(accountId: string): Promise<Account> {
  return get<Account>(`${BASE_URL}/${accountId}`);
}

export function getAccountByNo(accountNo: string): Promise<Account> {
  return get<Account>(`${BASE_URL}/no/${accountNo}`);
}

export function getAccountList(params?: {
  userId?: string;
  accountType?: number;
  status?: number;
  pageNum?: number;
  pageSize?: number;
}): Promise<Page<Account>> {
  return post<Page<Account>>(`${BASE_URL}/list`, params);
}

export function freezeAccount(data: AccountFreezeDTO): Promise<Account> {
  return post<Account>(`${BASE_URL}/freeze`, data);
}

export function unfreezeAccount(data: AccountUnfreezeDTO): Promise<Account> {
  return post<Account>(`${BASE_URL}/unfreeze`, data);
}

export function closeAccount(data: AccountCloseDTO): Promise<void> {
  return post<void>(`${BASE_URL}/close`, data);
}
