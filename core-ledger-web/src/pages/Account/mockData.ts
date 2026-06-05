import { Account, AccountStatusEnum, AccountTypeEnum, Transaction, TransactionTypeEnum, TransactionStatusEnum } from '@/api/types'

// 操作历史记录接口
export interface OperationHistory {
  id: string
  operationType: 'freeze' | 'unfreeze' | 'close'
  operationTypeDesc: string
  freezeType?: number
  freezeTypeDesc?: string
  remark?: string
  operator: string
  operateTime: string
}

// 账户列表Mock数据
export const mockAccountList: Account[] = [
  {
    accountId: 'ACC001',
    accountNo: '622217176400001',
    userId: 'USER001',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'CNY',
    currencyDesc: '人民币',
    balance: 12568000,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-01-15 09:30:00',
    createTime: '2024-01-15 09:30:00',
    updateTime: '2024-06-01 14:20:00',
  },
  {
    accountId: 'ACC002',
    accountNo: '622217176400002',
    userId: 'USER002',
    accountType: AccountTypeEnum.ENTERPRISE,
    accountTypeDesc: '企业账户',
    currency: 'CNY',
    currencyDesc: '人民币',
    balance: 56892300,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-02-20 10:15:00',
    createTime: '2024-02-20 10:15:00',
    updateTime: '2024-06-02 11:30:00',
  },
  {
    accountId: 'ACC003',
    accountNo: '622217176400003',
    userId: 'USER003',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'USD',
    currencyDesc: '美元',
    balance: 856000,
    status: AccountStatusEnum.FROZEN,
    statusDesc: '冻结',
    freezeType: 2,
    freezeTypeDesc: '司法冻结',
    freezeRemark: '涉及经济纠纷',
    freezeTime: '2024-05-10 16:45:00',
    freezeOperator: '管理员',
    openTime: '2024-03-05 14:20:00',
    createTime: '2024-03-05 14:20:00',
    updateTime: '2024-05-10 16:45:00',
  },
  {
    accountId: 'ACC004',
    accountNo: '622217176400004',
    userId: 'USER004',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'CNY',
    currencyDesc: '人民币',
    balance: 0,
    status: AccountStatusEnum.CLOSED,
    statusDesc: '已销户',
    closeTime: '2024-05-25 09:00:00',
    openTime: '2024-01-20 11:30:00',
    createTime: '2024-01-20 11:30:00',
    updateTime: '2024-05-25 09:00:00',
  },
  {
    accountId: 'ACC005',
    accountNo: '622217176400005',
    userId: 'USER005',
    accountType: AccountTypeEnum.ENTERPRISE,
    accountTypeDesc: '企业账户',
    currency: 'EUR',
    currencyDesc: '欧元',
    balance: 3250000,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-04-10 08:45:00',
    createTime: '2024-04-10 08:45:00',
    updateTime: '2024-06-03 16:20:00',
  },
  {
    accountId: 'ACC006',
    accountNo: '622217176400006',
    userId: 'USER001',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'USD',
    currencyDesc: '美元',
    balance: 2568000,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-02-28 15:00:00',
    createTime: '2024-02-28 15:00:00',
    updateTime: '2024-06-01 10:15:00',
  },
  {
    accountId: 'ACC007',
    accountNo: '622217176400007',
    userId: 'USER006',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'CNY',
    currencyDesc: '人民币',
    balance: 4520000,
    status: AccountStatusEnum.FROZEN,
    statusDesc: '冻结',
    freezeType: 1,
    freezeTypeDesc: '正常冻结',
    freezeRemark: '风控监测异常交易',
    freezeTime: '2024-05-28 14:30:00',
    freezeOperator: '风控系统',
    openTime: '2024-03-18 09:10:00',
    createTime: '2024-03-18 09:10:00',
    updateTime: '2024-05-28 14:30:00',
  },
  {
    accountId: 'ACC008',
    accountNo: '622217176400008',
    userId: 'USER007',
    accountType: AccountTypeEnum.ENTERPRISE,
    accountTypeDesc: '企业账户',
    currency: 'CNY',
    currencyDesc: '人民币',
    balance: 125800000,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-01-08 11:20:00',
    createTime: '2024-01-08 11:20:00',
    updateTime: '2024-06-04 09:45:00',
  },
  {
    accountId: 'ACC009',
    accountNo: '622217176400009',
    userId: 'USER008',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'CNY',
    currencyDesc: '人民币',
    balance: 685000,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-05-01 13:45:00',
    createTime: '2024-05-01 13:45:00',
    updateTime: '2024-06-03 15:10:00',
  },
  {
    accountId: 'ACC010',
    accountNo: '622217176400010',
    userId: 'USER009',
    accountType: AccountTypeEnum.PERSONAL,
    accountTypeDesc: '个人账户',
    currency: 'EUR',
    currencyDesc: '欧元',
    balance: 1568000,
    status: AccountStatusEnum.NORMAL,
    statusDesc: '正常',
    openTime: '2024-04-22 10:30:00',
    createTime: '2024-04-22 10:30:00',
    updateTime: '2024-06-02 14:00:00',
  },
]

// 操作历史Mock数据
export const mockOperationHistory: OperationHistory[] = [
  {
    id: 'OP001',
    operationType: 'freeze',
    operationTypeDesc: '冻结',
    freezeType: 2,
    freezeTypeDesc: '司法冻结',
    remark: '涉及经济纠纷案件',
    operator: '管理员',
    operateTime: '2024-05-10 16:45:00',
  },
  {
    id: 'OP002',
    operationType: 'unfreeze',
    operationTypeDesc: '解冻',
    freezeType: 1,
    freezeTypeDesc: '正常冻结',
    remark: '风险解除',
    operator: '风控系统',
    operateTime: '2024-05-15 10:20:00',
  },
  {
    id: 'OP003',
    operationType: 'freeze',
    operationTypeDesc: '冻结',
    freezeType: 1,
    freezeTypeDesc: '正常冻结',
    remark: '监测异常大额交易',
    operator: '风控系统',
    operateTime: '2024-05-28 14:30:00',
  },
  {
    id: 'OP004',
    operationType: 'close',
    operationTypeDesc: '销户',
    remark: '用户主动申请销户',
    operator: '系统管理员',
    operateTime: '2024-05-25 09:00:00',
  },
]

// 交易记录Mock数据
export const mockTransactions: Transaction[] = [
  {
    transactionId: 'TXN001',
    transactionNo: 'TXN202406040001',
    transactionType: TransactionTypeEnum.TRANSFER,
    transactionTypeDesc: '转账',
    businessNo: 'BIZ202406040001',
    totalAmount: 500000,
    currency: 'CNY',
    summary: '货款结算',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: 'USER001',
    transactionTime: '2024-06-04 15:32:18',
    createTime: '2024-06-04 15:32:18',
    entries: [],
  },
  {
    transactionId: 'TXN002',
    transactionNo: 'TXN202406030001',
    transactionType: TransactionTypeEnum.DEPOSIT,
    transactionTypeDesc: '存款',
    businessNo: 'BIZ202406030001',
    totalAmount: 1000000,
    currency: 'CNY',
    summary: '现金存入',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: 'USER002',
    transactionTime: '2024-06-03 10:15:42',
    createTime: '2024-06-03 10:15:42',
    entries: [],
  },
  {
    transactionId: 'TXN003',
    transactionNo: 'TXN202406020001',
    transactionType: TransactionTypeEnum.WITHDRAW,
    transactionTypeDesc: '取款',
    businessNo: 'BIZ202406020001',
    totalAmount: 200000,
    currency: 'CNY',
    summary: '备用金支取',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: 'USER003',
    transactionTime: '2024-06-02 14:28:05',
    createTime: '2024-06-02 14:28:05',
    entries: [],
  },
  {
    transactionId: 'TXN004',
    transactionNo: 'TXN202406010001',
    transactionType: TransactionTypeEnum.INTEREST,
    transactionTypeDesc: '利息',
    businessNo: 'BIZ202406010001',
    totalAmount: 12568,
    currency: 'CNY',
    summary: '季度结息',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: '系统',
    transactionTime: '2024-06-01 09:00:00',
    createTime: '2024-06-01 09:00:00',
    entries: [],
  },
]

// 获取分页账户列表
export function getMockAccountList(params?: {
  userId?: string
  accountType?: number
  status?: number
  currency?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}) {
  let data = [...mockAccountList]

  if (params?.userId) {
    data = data.filter((item) => item.userId === params.userId)
  }
  if (params?.accountType) {
    data = data.filter((item) => item.accountType === params.accountType)
  }
  if (params?.status) {
    data = data.filter((item) => item.status === params.status)
  }
  if (params?.currency) {
    data = data.filter((item) => item.currency === params.currency)
  }
  if (params?.keyword) {
    const keyword = params.keyword.toLowerCase()
    data = data.filter(
      (item) =>
        item.userId.toLowerCase().includes(keyword) ||
        item.accountNo.toLowerCase().includes(keyword)
    )
  }

  const pageNum = params?.pageNum || 1
  const pageSize = params?.pageSize || 10
  const total = data.length
  const start = (pageNum - 1) * pageSize
  const end = start + pageSize
  const records = data.slice(start, end)

  return {
    records,
    total,
    size: pageSize,
    current: pageNum,
    pages: Math.ceil(total / pageSize),
  }
}

// 获取账户详情
export function getMockAccountDetail(accountId: string): Account | undefined {
  return mockAccountList.find((item) => item.accountId === accountId)
}

// 获取账户操作历史
export function getMockOperationHistory(accountId: string): OperationHistory[] {
  return mockOperationHistory
}

// 获取账户交易记录
export function getMockTransactions(accountId: string): Transaction[] {
  return mockTransactions
}
