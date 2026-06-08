export enum AccountTypeEnum {
  PERSONAL = 1,
  ENTERPRISE = 2
}

export const AccountTypeDesc: Record<AccountTypeEnum, string> = {
  [AccountTypeEnum.PERSONAL]: '个人账户',
  [AccountTypeEnum.ENTERPRISE]: '企业账户'
}

export enum AccountStatusEnum {
  NORMAL = 1,
  FROZEN = 2,
  CLOSED = 3
}

export const AccountStatusDesc: Record<AccountStatusEnum, string> = {
  [AccountStatusEnum.NORMAL]: '正常',
  [AccountStatusEnum.FROZEN]: '冻结',
  [AccountStatusEnum.CLOSED]: '已销户'
}

export enum TransactionTypeEnum {
  TRANSFER = 1,
  DEPOSIT = 2,
  WITHDRAW = 3,
  FEE = 4,
  INTEREST = 5,
  ADJUST = 6
}

export const TransactionTypeDesc: Record<TransactionTypeEnum, string> = {
  [TransactionTypeEnum.TRANSFER]: '转账',
  [TransactionTypeEnum.DEPOSIT]: '存款',
  [TransactionTypeEnum.WITHDRAW]: '取款',
  [TransactionTypeEnum.FEE]: '手续费',
  [TransactionTypeEnum.INTEREST]: '利息',
  [TransactionTypeEnum.ADJUST]: '调账'
}

export enum TransactionStatusEnum {
  PENDING = 0,
  SUCCESS = 1,
  FAILED = 2,
  REVERSED = 3
}

export const TransactionStatusDesc: Record<TransactionStatusEnum, string> = {
  [TransactionStatusEnum.PENDING]: '待处理',
  [TransactionStatusEnum.SUCCESS]: '成功',
  [TransactionStatusEnum.FAILED]: '失败',
  [TransactionStatusEnum.REVERSED]: '已冲正'
}

export enum DebitCreditEnum {
  DEBIT = 1,
  CREDIT = 2
}

export const DebitCreditDesc: Record<DebitCreditEnum, string> = {
  [DebitCreditEnum.DEBIT]: '借',
  [DebitCreditEnum.CREDIT]: '贷'
}

export enum AdjustStatusEnum {
  PENDING = 0,
  APPROVED = 1,
  REJECTED = 2,
  EXECUTED = 3
}

export const AdjustStatusDesc: Record<AdjustStatusEnum, string> = {
  [AdjustStatusEnum.PENDING]: '待审批',
  [AdjustStatusEnum.APPROVED]: '已审批',
  [AdjustStatusEnum.REJECTED]: '已拒绝',
  [AdjustStatusEnum.EXECUTED]: '已执行'
}

export enum AdjustTypeEnum {
  INCREASE = 1,
  DECREASE = 2
}

export const AdjustTypeDesc: Record<AdjustTypeEnum, string> = {
  [AdjustTypeEnum.INCREASE]: '增加余额',
  [AdjustTypeEnum.DECREASE]: '扣减余额'
}

export enum ShardingStrategyEnum {
  HASH = 1,
  ROUND_ROBIN = 2,
  RANDOM = 3
}

export const ShardingStrategyDesc: Record<ShardingStrategyEnum, string> = {
  [ShardingStrategyEnum.HASH]: '哈希路由',
  [ShardingStrategyEnum.ROUND_ROBIN]: '轮询路由',
  [ShardingStrategyEnum.RANDOM]: '随机路由'
}

export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface Account {
  accountId: string
  accountNo: string
  userId: string
  accountType: AccountTypeEnum
  accountTypeDesc: string
  currency: string
  currencyDesc: string
  balance: number
  status: AccountStatusEnum
  statusDesc: string
  freezeType?: number
  freezeTypeDesc?: string
  freezeRemark?: string
  freezeTime?: string
  freezeOperator?: string
  openTime?: string
  closeTime?: string
  createTime?: string
  updateTime?: string
  isHotAccount?: boolean
  shardCount?: number
}

export interface AccountQueryDTO {
  userId?: string
  accountId?: string
  accountNo?: string
  accountType?: AccountTypeEnum
  status?: AccountStatusEnum
  minBalance?: number
  maxBalance?: number
  pageNum?: number
  pageSize?: number
}

export interface TransactionEntry {
  entryId: string
  transactionId: string
  accountId: string
  accountNo: string
  subjectCode: string
  subjectName: string
  direction: DebitCreditEnum
  directionDesc: string
  amount: number
  currency: string
  summary?: string
  createTime?: string
}

export interface Transaction {
  transactionId: string
  transactionNo: string
  transactionType: TransactionTypeEnum
  transactionTypeDesc: string
  businessNo: string
  totalAmount: number
  currency: string
  voucherNo?: string
  summary?: string
  status: TransactionStatusEnum
  statusDesc: string
  operator?: string
  transactionTime?: string
  createTime?: string
  entries: TransactionEntry[]
  remark?: string
}

export interface TransactionQueryDTO {
  accountId?: string
  transactionType?: TransactionTypeEnum
  status?: TransactionStatusEnum
  startTime?: string
  endTime?: string
  minAmount?: number
  maxAmount?: number
  businessNo?: string
  pageNum?: number
  pageSize?: number
}

export interface AccountShard {
  id: string
  shardId: string
  mainAccountId: string
  shardIndex: number
  balance: number
  status: number
  statusDesc: string
  createTime?: string
  updateTime?: string
}

export interface HotAccountConfigVO {
  accountId: string
  accountNo: string
  userId: string
  isHotAccount: boolean
  shardCount: number
  shardingStrategy: ShardingStrategyEnum
  shardingStrategyDesc: string
  bufferEnabled: boolean
  bufferThreshold: number
  shards: AccountShard[]
  createTime?: string
  updateTime?: string
}

export interface HotAccountConfigDTO {
  accountId: string
  shardCount?: number
  shardingStrategy?: ShardingStrategyEnum
  bufferEnabled?: boolean
  bufferThreshold?: number
}

export interface AdjustApplication {
  id: string
  applicationNo: string
  accountId: string
  accountNo: string
  userId: string
  adjustType: AdjustTypeEnum
  adjustTypeDesc: string
  amount: number
  currency: string
  reason: string
  status: AdjustStatusEnum
  statusDesc: string
  applicant: string
  applyTime: string
  approver?: string
  approveTime?: string
  approveRemark?: string
  executor?: string
  executeTime?: string
  transactionId?: string
  remark?: string
}

export interface AdjustApplicationCreateDTO {
  accountId: string
  adjustType: AdjustTypeEnum
  amount: number
  reason: string
  remark?: string
  requestId: string
}

export interface AdjustApplicationApproveDTO {
  id: string
  approved: boolean
  approveRemark?: string
  requestId: string
}

export interface AdjustApplicationExecuteDTO {
  id: string
  requestId: string
}

export interface AdjustApplicationQueryDTO {
  accountId?: string
  status?: AdjustStatusEnum
  adjustType?: AdjustTypeEnum
  startTime?: string
  endTime?: string
  applicant?: string
  pageNum?: number
  pageSize?: number
}

export interface SagaTransactionLog {
  logId: string
  transactionId: string
  businessNo: string
  stepName: string
  status: string
  statusDesc: string
  tryResult?: string
  confirmResult?: string
  cancelResult?: string
  retryCount: number
  nextRetryTime?: string
  createTime?: string
  updateTime?: string
}
