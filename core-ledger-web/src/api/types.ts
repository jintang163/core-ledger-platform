export enum AccountTypeEnum {
  PERSONAL = 1,
  ENTERPRISE = 2,
}

export const AccountTypeDesc: Record<AccountTypeEnum, string> = {
  [AccountTypeEnum.PERSONAL]: '个人账户',
  [AccountTypeEnum.ENTERPRISE]: '企业账户',
};

export enum AccountStatusEnum {
  NORMAL = 1,
  FROZEN = 2,
  CLOSED = 3,
}

export const AccountStatusDesc: Record<AccountStatusEnum, string> = {
  [AccountStatusEnum.NORMAL]: '正常',
  [AccountStatusEnum.FROZEN]: '冻结',
  [AccountStatusEnum.CLOSED]: '已销户',
};

export enum TransactionTypeEnum {
  TRANSFER = 1,
  DEPOSIT = 2,
  WITHDRAW = 3,
  FEE = 4,
  INTEREST = 5,
  ADJUST = 6,
}

export const TransactionTypeDesc: Record<TransactionTypeEnum, string> = {
  [TransactionTypeEnum.TRANSFER]: '转账',
  [TransactionTypeEnum.DEPOSIT]: '存款',
  [TransactionTypeEnum.WITHDRAW]: '取款',
  [TransactionTypeEnum.FEE]: '手续费',
  [TransactionTypeEnum.INTEREST]: '利息',
  [TransactionTypeEnum.ADJUST]: '调账',
};

export enum TransactionStatusEnum {
  PENDING = 0,
  SUCCESS = 1,
  FAILED = 2,
  REVERSED = 3,
}

export const TransactionStatusDesc: Record<TransactionStatusEnum, string> = {
  [TransactionStatusEnum.PENDING]: '待处理',
  [TransactionStatusEnum.SUCCESS]: '成功',
  [TransactionStatusEnum.FAILED]: '失败',
  [TransactionStatusEnum.REVERSED]: '已冲正',
};

export enum DebitCreditEnum {
  DEBIT = 1,
  CREDIT = 2,
}

export const DebitCreditDesc: Record<DebitCreditEnum, string> = {
  [DebitCreditEnum.DEBIT]: '借',
  [DebitCreditEnum.CREDIT]: '贷',
};

export interface Result<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface Account {
  accountId: string;
  accountNo: string;
  userId: string;
  accountType: AccountTypeEnum;
  accountTypeDesc: string;
  currency: string;
  currencyDesc: string;
  balance: number;
  status: AccountStatusEnum;
  statusDesc: string;
  freezeType?: number;
  freezeTypeDesc?: string;
  freezeRemark?: string;
  freezeTime?: string;
  freezeOperator?: string;
  openTime?: string;
  closeTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface AccountCreateDTO {
  userId: string;
  accountType: AccountTypeEnum;
  currency: string;
  initBalance: number;
  requestId: string;
}

export interface AccountFreezeDTO {
  accountId: string;
  freezeType: number;
  remark?: string;
  operator?: string;
  requestId: string;
}

export interface AccountUnfreezeDTO {
  accountId: string;
  freezeType: number;
  remark?: string;
  operator?: string;
  requestId: string;
}

export interface AccountCloseDTO {
  accountId: string;
  remark?: string;
  operator?: string;
  requestId: string;
}

export interface TransactionEntry {
  entryId: string;
  transactionId: string;
  accountId: string;
  accountNo: string;
  subjectCode: string;
  subjectName: string;
  direction: DebitCreditEnum;
  directionDesc: string;
  amount: number;
  currency: string;
  summary?: string;
  createTime?: string;
}

export interface Transaction {
  transactionId: string;
  transactionNo: string;
  transactionType: TransactionTypeEnum;
  transactionTypeDesc: string;
  businessNo: string;
  totalAmount: number;
  currency: string;
  voucherNo?: string;
  summary?: string;
  status: TransactionStatusEnum;
  statusDesc: string;
  operator?: string;
  transactionTime?: string;
  createTime?: string;
  entries: TransactionEntry[];
}

export interface TransactionEntryDTO {
  accountId: string;
  subjectCode: string;
  subjectName: string;
  direction: DebitCreditEnum;
  amount: number;
  summary?: string;
}

export interface TransactionCreateDTO {
  requestId: string;
  businessNo: string;
  transactionType: TransactionTypeEnum;
  currency: string;
  totalAmount: number;
  summary?: string;
  operator?: string;
  entries: TransactionEntryDTO[];
}

export interface TransactionQueryDTO {
  accountId?: string;
  transactionType?: TransactionTypeEnum;
  status?: TransactionStatusEnum;
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}
