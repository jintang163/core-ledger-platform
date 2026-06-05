import { post, get } from './request';
import {
  Transaction,
  TransactionCreateDTO,
  TransactionQueryDTO,
  Page,
} from './types';

const BASE_URL = '/api/transaction';

export function createTransaction(data: TransactionCreateDTO): Promise<Transaction> {
  return post<Transaction>(`${BASE_URL}/create`, data);
}

export function getTransaction(transactionId: string): Promise<Transaction> {
  return get<Transaction>(`${BASE_URL}/${transactionId}`);
}

export function getTransactionByBusinessNo(businessNo: string): Promise<Transaction> {
  return get<Transaction>(`${BASE_URL}/business/${businessNo}`);
}

export function queryTransactionList(data: TransactionQueryDTO): Promise<Page<Transaction>> {
  return post<Page<Transaction>>(`${BASE_URL}/query`, data);
}
