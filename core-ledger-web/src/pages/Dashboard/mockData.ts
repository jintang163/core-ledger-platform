import {
  WalletOutlined,
  TransactionOutlined,
  RiseOutlined,
  MoneyCollectOutlined,
} from '@ant-design/icons'

// 统计卡片数据接口
interface StatCardData {
  // 图标
  icon: any
  // 图标渐变颜色
  iconGradient: [string, string]
  // 标题
  title: string
  // 数值
  value: number
  // 单位
  unit?: string
  // 前缀
  prefix?: string
  // 同比变化率
  changeRate: number
}

// 余额趋势数据接口
interface BalanceTrendData {
  date: string
  balance: number
}

// 交易数据接口
interface TransactionData {
  id: string
  // 交易类型
  type: 'income' | 'expense' | 'transfer'
  // 交易描述
  description: string
  // 金额
  amount: number
  // 交易账户
  account: string
  // 交易时间
  time: string
  // 交易状态
  status: 'success' | 'pending' | 'failed'
}

// 统计卡片数据
export const statCardsData: StatCardData[] = [
  {
    icon: WalletOutlined,
    iconGradient: ['#3B82F6', '#1D4ED8'],
    title: '账户总数',
    value: 1286,
    unit: '个',
    changeRate: 0.125,
  },
  {
    icon: TransactionOutlined,
    iconGradient: ['#10B981', '#059669'],
    title: '今日交易数',
    value: 342,
    unit: '笔',
    changeRate: 0.083,
  },
  {
    icon: RiseOutlined,
    iconGradient: ['#F59E0B', '#D97706'],
    title: '今日交易额',
    value: 5689230,
    unit: '元',
    prefix: '¥',
    changeRate: -0.032,
  },
  {
    icon: MoneyCollectOutlined,
    iconGradient: ['#8B5CF6', '#7C3AED'],
    title: '总余额',
    value: 125680450,
    unit: '元',
    prefix: '¥',
    changeRate: 0.156,
  },
]

// 余额趋势数据（近30天）
export const balanceTrendData: BalanceTrendData[] = [
  { date: '05/06', balance: 98500000 },
  { date: '05/07', balance: 99200000 },
  { date: '05/08', balance: 101500000 },
  { date: '05/09', balance: 100800000 },
  { date: '05/10', balance: 102300000 },
  { date: '05/11', balance: 103500000 },
  { date: '05/12', balance: 104200000 },
  { date: '05/13', balance: 103800000 },
  { date: '05/14', balance: 105100000 },
  { date: '05/15', balance: 106800000 },
  { date: '05/16', balance: 107500000 },
  { date: '05/17', balance: 106200000 },
  { date: '05/18', balance: 108900000 },
  { date: '05/19', balance: 110200000 },
  { date: '05/20', balance: 111500000 },
  { date: '05/21', balance: 110800000 },
  { date: '05/22', balance: 112300000 },
  { date: '05/23', balance: 113500000 },
  { date: '05/24', balance: 114200000 },
  { date: '05/25', balance: 115800000 },
  { date: '05/26', balance: 116500000 },
  { date: '05/27', balance: 117800000 },
  { date: '05/28', balance: 118500000 },
  { date: '05/29', balance: 119200000 },
  { date: '05/30', balance: 120500000 },
  { date: '05/31', balance: 121800000 },
  { date: '06/01', balance: 123200000 },
  { date: '06/02', balance: 124100000 },
  { date: '06/03', balance: 124800000 },
  { date: '06/04', balance: 125680450 },
]

// 最近交易数据
export const recentTransactions: TransactionData[] = [
  {
    id: 'TXN202406040001',
    type: 'income',
    description: '客户A货款结算',
    amount: 125000,
    account: '主营业务收入账户',
    time: '2024-06-04 15:32:18',
    status: 'success',
  },
  {
    id: 'TXN202406040002',
    type: 'expense',
    description: '办公设备采购',
    amount: -28500,
    account: '管理费用账户',
    time: '2024-06-04 14:15:42',
    status: 'success',
  },
  {
    id: 'TXN202406040003',
    type: 'transfer',
    description: '账户间资金划转',
    amount: 500000,
    account: '基本存款账户 → 一般存款账户',
    time: '2024-06-04 11:28:05',
    status: 'success',
  },
  {
    id: 'TXN202406040004',
    type: 'income',
    description: '投资收益分红',
    amount: 85000,
    account: '投资收益账户',
    time: '2024-06-04 10:05:33',
    status: 'success',
  },
  {
    id: 'TXN202406040005',
    type: 'expense',
    description: '员工工资发放',
    amount: -456800,
    account: '应付职工薪酬账户',
    time: '2024-06-04 09:30:00',
    status: 'pending',
  },
]

// 交易类型映射
export const transactionTypeMap = {
  income: {
    label: '收入',
    color: 'text-green-600',
    bgColor: 'bg-green-50',
  },
  expense: {
    label: '支出',
    color: 'text-red-600',
    bgColor: 'bg-red-50',
  },
  transfer: {
    label: '转账',
    color: 'text-blue-600',
    bgColor: 'bg-blue-50',
  },
}

// 交易状态映射
export const transactionStatusMap = {
  success: {
    label: '成功',
    color: 'text-green-600',
    dotColor: 'bg-green-500',
  },
  pending: {
    label: '处理中',
    color: 'text-yellow-600',
    dotColor: 'bg-yellow-500',
  },
  failed: {
    label: '失败',
    color: 'text-red-600',
    dotColor: 'bg-red-500',
  },
}
