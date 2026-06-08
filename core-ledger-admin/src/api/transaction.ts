import { get, post } from '@/utils/request'
import type {
  Transaction,
  TransactionQueryDTO,
  Page,
  SagaTransactionLog
} from '@/types'

export function getTransaction(transactionId: string): Promise<Transaction> {
  return get<Transaction>(`/transaction/${transactionId}`)
}

export function queryTransactionList(params: TransactionQueryDTO): Promise<Page<Transaction>> {
  return post<Page<Transaction>>('/transaction/query', params)
}

export function getTransactionLogs(transactionId: string): Promise<SagaTransactionLog[]> {
  return get<SagaTransactionLog[]>(`/transaction/${transactionId}/logs`)
}
