import { Account, Transaction, AccountStatusEnum, AccountTypeEnum, TransactionTypeEnum, TransactionStatusEnum, DebitCreditEnum } from '@/api/types'

// 币种描述映射
const currencyDescMap: Record<string, string> = {
  CNY: '人民币',
  USD: '美元',
  EUR: '欧元',
}

// 冻结类型描述映射
const freezeTypeDescMap: Record<number, string> = {
  1: '正常冻结',
  2: '司法冻结',
  3: '风控冻结',
}

// Mock数据库 - 账户数据
export const mockAccounts: Account[] = [
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

// Mock数据库 - 交易数据
export const mockTransactions: Transaction[] = [
  {
    transactionId: 'TXN001',
    transactionNo: 'TXN20250601100101001ABC',
    transactionType: TransactionTypeEnum.TRANSFER,
    transactionTypeDesc: '转账',
    businessNo: 'BIZ20250601100101001XYZ',
    totalAmount: 5000000,
    currency: 'CNY',
    voucherNo: 'VCH2025060001',
    summary: '客户货款转账',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: '张三',
    transactionTime: '2025-06-01 10:01:01',
    createTime: '2025-06-01 10:01:01',
    entries: [
      {
        entryId: 'ETY001',
        transactionId: 'TXN001',
        accountId: 'ACC001',
        accountNo: '622217176400001',
        subjectCode: '1002',
        subjectName: '银行存款',
        direction: DebitCreditEnum.DEBIT,
        directionDesc: '借',
        amount: 5000000,
        currency: 'CNY',
        summary: '收到客户货款',
      },
      {
        entryId: 'ETY002',
        transactionId: 'TXN001',
        accountId: 'ACC002',
        accountNo: '622217176400002',
        subjectCode: '1122',
        subjectName: '应收账款',
        direction: DebitCreditEnum.CREDIT,
        directionDesc: '贷',
        amount: 5000000,
        currency: 'CNY',
        summary: '冲减应收账款',
      },
    ],
  },
  {
    transactionId: 'TXN002',
    transactionNo: 'TXN20250601143025002DEF',
    transactionType: TransactionTypeEnum.DEPOSIT,
    transactionTypeDesc: '存款',
    businessNo: 'BIZ20250601143025002GHI',
    totalAmount: 20000000,
    currency: 'CNY',
    voucherNo: 'VCH2025060002',
    summary: '现金存入银行',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: '李四',
    transactionTime: '2025-06-01 14:30:25',
    createTime: '2025-06-01 14:30:25',
    entries: [
      {
        entryId: 'ETY003',
        transactionId: 'TXN002',
        accountId: 'ACC001',
        accountNo: '622217176400001',
        subjectCode: '1002',
        subjectName: '银行存款',
        direction: DebitCreditEnum.DEBIT,
        directionDesc: '借',
        amount: 20000000,
        currency: 'CNY',
        summary: '现金存入',
      },
      {
        entryId: 'ETY004',
        transactionId: 'TXN002',
        accountId: 'ACC003',
        accountNo: '622217176400003',
        subjectCode: '1001',
        subjectName: '库存现金',
        direction: DebitCreditEnum.CREDIT,
        directionDesc: '贷',
        amount: 20000000,
        currency: 'CNY',
        summary: '现金减少',
      },
    ],
  },
  {
    transactionId: 'TXN003',
    transactionNo: 'TXN20250602091533003JKL',
    transactionType: TransactionTypeEnum.WITHDRAW,
    transactionTypeDesc: '取款',
    businessNo: 'BIZ20250602091533003MNO',
    totalAmount: 3000000,
    currency: 'CNY',
    voucherNo: 'VCH2025060003',
    summary: '提取备用金',
    status: TransactionStatusEnum.PENDING,
    statusDesc: '待处理',
    operator: '王五',
    transactionTime: '2025-06-02 09:15:33',
    createTime: '2025-06-02 09:15:33',
    entries: [
      {
        entryId: 'ETY005',
        transactionId: 'TXN003',
        accountId: 'ACC003',
        accountNo: '622217176400003',
        subjectCode: '1001',
        subjectName: '库存现金',
        direction: DebitCreditEnum.DEBIT,
        directionDesc: '借',
        amount: 3000000,
        currency: 'CNY',
        summary: '提取备用金',
      },
      {
        entryId: 'ETY006',
        transactionId: 'TXN003',
        accountId: 'ACC001',
        accountNo: '622217176400001',
        subjectCode: '1002',
        subjectName: '银行存款',
        direction: DebitCreditEnum.CREDIT,
        directionDesc: '贷',
        amount: 3000000,
        currency: 'CNY',
        summary: '银行存款减少',
      },
    ],
  },
  {
    transactionId: 'TXN004',
    transactionNo: 'TXN20250602112045004PQR',
    transactionType: TransactionTypeEnum.FEE,
    transactionTypeDesc: '手续费',
    businessNo: 'BIZ20250602112045004STU',
    totalAmount: 50000,
    currency: 'CNY',
    voucherNo: 'VCH2025060004',
    summary: '支付银行手续费',
    status: TransactionStatusEnum.FAILED,
    statusDesc: '失败',
    operator: '赵六',
    transactionTime: '2025-06-02 11:20:45',
    createTime: '2025-06-02 11:20:45',
    entries: [
      {
        entryId: 'ETY007',
        transactionId: 'TXN004',
        accountId: 'ACC004',
        accountNo: '622217176400004',
        subjectCode: '6603',
        subjectName: '财务费用',
        direction: DebitCreditEnum.DEBIT,
        directionDesc: '借',
        amount: 50000,
        currency: 'CNY',
        summary: '银行手续费',
      },
      {
        entryId: 'ETY008',
        transactionId: 'TXN004',
        accountId: 'ACC001',
        accountNo: '622217176400001',
        subjectCode: '1002',
        subjectName: '银行存款',
        direction: DebitCreditEnum.CREDIT,
        directionDesc: '贷',
        amount: 50000,
        currency: 'CNY',
        summary: '支付手续费',
      },
    ],
  },
  {
    transactionId: 'TXN005',
    transactionNo: 'TXN20250603154512005VWX',
    transactionType: TransactionTypeEnum.INTEREST,
    transactionTypeDesc: '利息',
    businessNo: 'BIZ20250603154512005YZA',
    totalAmount: 1250000,
    currency: 'CNY',
    voucherNo: 'VCH2025060005',
    summary: '收到存款利息',
    status: TransactionStatusEnum.SUCCESS,
    statusDesc: '成功',
    operator: '张三',
    transactionTime: '2025-06-03 15:45:12',
    createTime: '2025-06-03 15:45:12',
    entries: [
      {
        entryId: 'ETY009',
        transactionId: 'TXN005',
        accountId: 'ACC001',
        accountNo: '622217176400001',
        subjectCode: '1002',
        subjectName: '银行存款',
        direction: DebitCreditEnum.DEBIT,
        directionDesc: '借',
        amount: 1250000,
        currency: 'CNY',
        summary: '收到利息',
      },
      {
        entryId: 'ETY010',
        transactionId: 'TXN005',
        accountId: 'ACC005',
        accountNo: '622217176400005',
        subjectCode: '6603',
        subjectName: '财务费用',
        direction: DebitCreditEnum.CREDIT,
        directionDesc: '贷',
        amount: 1250000,
        currency: 'CNY',
        summary: '利息收入冲减财务费用',
      },
    ],
  },
  {
    transactionId: 'TXN006',
    transactionNo: 'TXN20250603163008006BCD',
    transactionType: TransactionTypeEnum.ADJUST,
    transactionTypeDesc: '调账',
    businessNo: 'BIZ20250603163008006EFG',
    totalAmount: 1000000,
    currency: 'CNY',
    voucherNo: 'VCH2025060006',
    summary: '错账调整',
    status: TransactionStatusEnum.REVERSED,
    statusDesc: '已冲正',
    operator: '李四',
    transactionTime: '2025-06-03 16:30:08',
    createTime: '2025-06-03 16:30:08',
    entries: [
      {
        entryId: 'ETY011',
        transactionId: 'TXN006',
        accountId: 'ACC002',
        accountNo: '622217176400002',
        subjectCode: '1122',
        subjectName: '应收账款',
        direction: DebitCreditEnum.DEBIT,
        directionDesc: '借',
        amount: 1000000,
        currency: 'CNY',
        summary: '调整应收账款',
      },
      {
        entryId: 'ETY012',
        transactionId: 'TXN006',
        accountId: 'ACC001',
        accountNo: '622217176400001',
        subjectCode: '1002',
        subjectName: '银行存款',
        direction: DebitCreditEnum.CREDIT,
        directionDesc: '贷',
        amount: 1000000,
        currency: 'CNY',
        summary: '调整银行存款',
      },
    ],
  },
]

// 账户操作历史记录接口
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

// Mock数据库 - 操作历史数据
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

// 生成账户ID
let accountIdCounter = 11
export function generateAccountId(): string {
  return `ACC${String(accountIdCounter++).padStart(3, '0')}`
}

// 生成交易ID
let transactionIdCounter = 7
export function generateTransactionId(): string {
  return `TXN${String(transactionIdCounter++).padStart(3, '0')}`
}

// 生成分录ID
let entryIdCounter = 13
export function generateEntryId(): string {
  return `ETY${String(entryIdCounter++).padStart(3, '0')}`
}

// 生成交易流水号
export function generateTransactionNo(): string {
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
  const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '')
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  return `TXN${dateStr}${timeStr}${random}`
}

// 生成业务号
export function generateBusinessNo(): string {
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
  const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '')
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  return `BIZ${dateStr}${timeStr}${random}`
}

// 生成账户账号
export function generateAccountNo(): string {
  const prefix = '6222171764'
  const suffix = String(Math.floor(Math.random() * 100000)).padStart(5, '0')
  return `${prefix}${suffix}`
}

// 获取当前时间字符串
export function getCurrentTime(): string {
  return new Date().toISOString().replace('T', ' ').substring(0, 19)
}

// 获取币种描述
export function getCurrencyDesc(currency: string): string {
  return currencyDescMap[currency] || currency
}

// 获取冻结类型描述
export function getFreezeTypeDesc(freezeType: number): string {
  return freezeTypeDescMap[freezeType] || '未知'
}

// 获取账户类型描述
export function getAccountTypeDesc(accountType: AccountTypeEnum): string {
  const map: Record<AccountTypeEnum, string> = {
    [AccountTypeEnum.PERSONAL]: '个人账户',
    [AccountTypeEnum.ENTERPRISE]: '企业账户',
  }
  return map[accountType] || '未知'
}

// 获取账户状态描述
export function getAccountStatusDesc(status: AccountStatusEnum): string {
  const map: Record<AccountStatusEnum, string> = {
    [AccountStatusEnum.NORMAL]: '正常',
    [AccountStatusEnum.FROZEN]: '冻结',
    [AccountStatusEnum.CLOSED]: '已销户',
  }
  return map[status] || '未知'
}

// 获取交易类型描述
export function getTransactionTypeDesc(type: TransactionTypeEnum): string {
  const map: Record<TransactionTypeEnum, string> = {
    [TransactionTypeEnum.TRANSFER]: '转账',
    [TransactionTypeEnum.DEPOSIT]: '存款',
    [TransactionTypeEnum.WITHDRAW]: '取款',
    [TransactionTypeEnum.FEE]: '手续费',
    [TransactionTypeEnum.INTEREST]: '利息',
    [TransactionTypeEnum.ADJUST]: '调账',
  }
  return map[type] || '未知'
}

// 获取交易状态描述
export function getTransactionStatusDesc(status: TransactionStatusEnum): string {
  const map: Record<TransactionStatusEnum, string> = {
    [TransactionStatusEnum.PENDING]: '待处理',
    [TransactionStatusEnum.SUCCESS]: '成功',
    [TransactionStatusEnum.FAILED]: '失败',
    [TransactionStatusEnum.REVERSED]: '已冲正',
  }
  return map[status] || '未知'
}

// 获取借贷方向描述
export function getDebitCreditDesc(direction: DebitCreditEnum): string {
  const map: Record<DebitCreditEnum, string> = {
    [DebitCreditEnum.DEBIT]: '借',
    [DebitCreditEnum.CREDIT]: '贷',
  }
  return map[direction] || '未知'
}
