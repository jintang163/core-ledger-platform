import { get, post } from '@/utils/request'
import type {
  AdjustApplication,
  AdjustApplicationCreateDTO,
  AdjustApplicationApproveDTO,
  AdjustApplicationExecuteDTO,
  AdjustApplicationQueryDTO,
  Page
} from '@/types'

export function createAdjustApplication(data: AdjustApplicationCreateDTO): Promise<AdjustApplication> {
  return post<AdjustApplication>('/adjust/create', data)
}

export function approveAdjustApplication(data: AdjustApplicationApproveDTO): Promise<AdjustApplication> {
  return post<AdjustApplication>('/adjust/approve', data)
}

export function executeAdjustApplication(data: AdjustApplicationExecuteDTO): Promise<AdjustApplication> {
  return post<AdjustApplication>('/adjust/execute', data)
}

export function getAdjustApplication(id: string): Promise<AdjustApplication> {
  return get<AdjustApplication>(`/adjust/${id}`)
}

export function queryAdjustApplicationList(params: AdjustApplicationQueryDTO): Promise<Page<AdjustApplication>> {
  return post<Page<AdjustApplication>>('/adjust/query', params)
}
