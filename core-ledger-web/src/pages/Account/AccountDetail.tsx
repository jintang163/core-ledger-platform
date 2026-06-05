import { useState } from 'react'
import {
  Drawer,
  Tabs,
  Card,
  Button,
  Space,
  List,
  Tag,
  Descriptions,
  Empty,
  Skeleton,
  Divider,
  Row,
  Col,
  Tooltip,
} from 'antd'
import {
  CloseOutlined,
  LockOutlined,
  UnlockOutlined,
  DeleteOutlined,
  WalletOutlined,
  BankOutlined,
  ClockCircleOutlined,
  UserOutlined,
  FileTextOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  AlertCircleOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import type { TabsProps } from 'antd'
import { getAccount } from '@/api/account'
import { Account, AccountStatusEnum, Transaction } from '@/api/types'
import AmountDisplay from '@/components/AmountDisplay/AmountDisplay'
import AccountStatusTag from '@/components/StatusTag/AccountStatusTag'
import TransactionStatusTag from '@/components/StatusTag/TransactionStatusTag'
import { getMockAccountDetail, getMockOperationHistory, getMockTransactions, OperationHistory } from './mockData'

// 账户详情抽屉组件属性
interface AccountDetailProps {
  open: boolean
  account: Account | null
  onCancel: () => void
  onFreeze: (account: Account) => void
  onUnfreeze: (account: Account) => void
  onClose: (account: Account) => void
}

// 账户详情抽屉组件
function AccountDetail({
  open,
  account,
  onCancel,
  onFreeze,
  onUnfreeze,
  onClose,
}: AccountDetailProps) {
  const [useMockData, setUseMockData] = useState(true)

  // 查询账户详情
  const {
    data: accountDetail,
    isLoading: accountLoading,
  } = useQuery({
    queryKey: ['accountDetail', account?.accountId],
    queryFn: async () => {
      if (!account?.accountId) return null
      if (useMockData) {
        await new Promise((resolve) => setTimeout(resolve, 300))
        return getMockAccountDetail(account.accountId) || account
      }
      return getAccount(account.accountId)
    },
    enabled: open && !!account?.accountId,
  })

  // 查询操作历史
  const {
    data: operationHistory,
    isLoading: historyLoading,
  } = useQuery({
    queryKey: ['operationHistory', account?.accountId],
    queryFn: async () => {
      if (!account?.accountId) return []
      if (useMockData) {
        await new Promise((resolve) => setTimeout(resolve, 200))
        return getMockOperationHistory(account.accountId)
      }
      return []
    },
    enabled: open && !!account?.accountId,
  })

  // 查询交易记录
  const {
    data: transactions,
    isLoading: transactionLoading,
  } = useQuery({
    queryKey: ['accountTransactions', account?.accountId],
    queryFn: async () => {
      if (!account?.accountId) return []
      if (useMockData) {
        await new Promise((resolve) => setTimeout(resolve, 200))
        return getMockTransactions(account.accountId)
      }
      return []
    },
    enabled: open && !!account?.accountId,
  })

  // 计算可用余额和冻结余额
  const currentBalance = accountDetail?.balance || 0
  const frozenBalance = accountDetail?.status === AccountStatusEnum.FROZEN ? currentBalance : 0
  const availableBalance = currentBalance - frozenBalance

  // 获取操作类型样式
  const getOperationTypeStyle = (type: string) => {
    switch (type) {
      case 'freeze':
        return {
          color: 'warning',
          icon: LockOutlined,
          text: '冻结',
          bgClass: 'bg-warning-100',
          textClass: 'text-warning-600',
        }
      case 'unfreeze':
        return {
          color: 'success',
          icon: UnlockOutlined,
          text: '解冻',
          bgClass: 'bg-success-100',
          textClass: 'text-success-600',
        }
      case 'close':
        return {
          color: 'default',
          icon: DeleteOutlined,
          text: '销户',
          bgClass: 'bg-gray-100',
          textClass: 'text-gray-600',
        }
      default:
        return {
          color: 'default',
          icon: FileTextOutlined,
          text: type,
          bgClass: 'bg-gray-100',
          textClass: 'text-gray-600',
        }
    }
  }

  // Tab项配置
  const tabItems: TabsProps['items'] = [
    {
      key: 'basic',
      label: (
        <span className="flex items-center gap-1">
          <BankOutlined />
          基本信息
        </span>
      ),
      children: (
        <Skeleton active loading={accountLoading}>
          {accountDetail ? (
            <div className="space-y-6">
              {/* 账户状态卡片 */}
              <Card className="shadow-sm transition-all duration-300 hover:shadow-md">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <div className="text-sm text-gray-500 mb-1">账户状态</div>
                    <AccountStatusTag status={accountDetail.status} size="large" />
                  </div>
                  <div className="flex items-center gap-2">
                    {accountDetail.status === AccountStatusEnum.NORMAL && (
                      <Tooltip title="冻结账户">
                        <Button
                          type="text"
                          icon={<LockOutlined />}
                          onClick={() => onFreeze(accountDetail)}
                          className="text-warning-600 hover:text-warning-700 transition-all duration-200 hover:scale-105"
                        >
                          冻结
                        </Button>
                      </Tooltip>
                    )}
                    {accountDetail.status === AccountStatusEnum.FROZEN && (
                      <Tooltip title="解冻账户">
                        <Button
                          type="text"
                          icon={<UnlockOutlined />}
                          onClick={() => onUnfreeze(accountDetail)}
                          className="text-success-600 hover:text-success-700 transition-all duration-200 hover:scale-105"
                        >
                          解冻
                        </Button>
                      </Tooltip>
                    )}
                    {accountDetail.status !== AccountStatusEnum.CLOSED && (
                      <Tooltip title="销户">
                        <Button
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => onClose(accountDetail)}
                          className="transition-all duration-200 hover:scale-105"
                        >
                          销户
                        </Button>
                      </Tooltip>
                    )}
                  </div>
                </div>

                {/* 冻结信息 */}
                {accountDetail.status === AccountStatusEnum.FROZEN && (
                  <div className="mt-4 p-3 bg-warning-50 rounded-lg border border-warning-200">
                    <div className="flex items-center gap-2 mb-2">
                      <AlertCircleOutlined className="text-warning-500" />
                      <span className="font-medium text-warning-700">冻结信息</span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="text-gray-500">冻结类型：</span>
                        <span className="text-gray-800">{accountDetail.freezeTypeDesc || '-'}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">冻结时间：</span>
                        <span className="text-gray-800">{accountDetail.freezeTime || '-'}</span>
                      </div>
                      <div className="col-span-2">
                        <span className="text-gray-500">冻结备注：</span>
                        <span className="text-gray-800">{accountDetail.freezeRemark || '-'}</span>
                      </div>
                      <div className="col-span-2">
                        <span className="text-gray-500">操作人：</span>
                        <span className="text-gray-800">{accountDetail.freezeOperator || '-'}</span>
                      </div>
                    </div>
                  </div>
                )}
              </Card>

              {/* 余额信息卡片 */}
              <Card
                title={
                  <div className="flex items-center gap-2">
                    <WalletOutlined className="text-primary-500" />
                    <span className="font-semibold">余额信息</span>
                  </div>
                }
                className="shadow-sm transition-all duration-300 hover:shadow-md"
              >
                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={8}>
                    <div className="p-4 bg-gradient-to-br from-blue-50 to-blue-100 rounded-lg text-center transition-all duration-300 hover:scale-105">
                      <div className="text-sm text-gray-500 mb-2">当前余额</div>
                      <AmountDisplay
                        amount={currentBalance}
                        currency={accountDetail.currency}
                        size="xlarge"
                      />
                    </div>
                  </Col>
                  <Col xs={24} sm={8}>
                    <div className="p-4 bg-gradient-to-br from-green-50 to-green-100 rounded-lg text-center transition-all duration-300 hover:scale-105">
                      <div className="text-sm text-gray-500 mb-2">可用余额</div>
                      <AmountDisplay
                        amount={availableBalance}
                        currency={accountDetail.currency}
                        size="xlarge"
                      />
                    </div>
                  </Col>
                  <Col xs={24} sm={8}>
                    <div className="p-4 bg-gradient-to-br from-yellow-50 to-yellow-100 rounded-lg text-center transition-all duration-300 hover:scale-105">
                      <div className="text-sm text-gray-500 mb-2">冻结余额</div>
                      <AmountDisplay
                        amount={frozenBalance}
                        currency={accountDetail.currency}
                        size="xlarge"
                      />
                    </div>
                  </Col>
                </Row>
              </Card>

              {/* 基本信息 */}
              <Card
                title={
                  <div className="flex items-center gap-2">
                    <UserOutlined className="text-primary-500" />
                    <span className="font-semibold">账户信息</span>
                  </div>
                }
                className="shadow-sm transition-all duration-300 hover:shadow-md"
              >
                <Descriptions column={2} bordered size="small">
                  <Descriptions.Item label="账户ID">
                    <span className="font-mono">{accountDetail.accountId}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="账户号">
                    <span className="font-mono font-medium">{accountDetail.accountNo}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="用户ID">
                    <span className="font-mono">{accountDetail.userId}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="账户类型">
                    <Tag color="blue">{accountDetail.accountTypeDesc}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="币种">
                    {accountDetail.currencyDesc} ({accountDetail.currency})
                  </Descriptions.Item>
                  <Descriptions.Item label="账户状态">
                    <AccountStatusTag status={accountDetail.status} />
                  </Descriptions.Item>
                  <Descriptions.Item label="开户时间">
                    <span className="text-gray-600">{accountDetail.openTime || '-'}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="销户时间">
                    <span className="text-gray-600">{accountDetail.closeTime || '-'}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="创建时间" span={2}>
                    <span className="text-gray-600">{accountDetail.createTime || '-'}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="更新时间" span={2}>
                    <span className="text-gray-600">{accountDetail.updateTime || '-'}</span>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </div>
          ) : (
            <Empty description="未找到账户信息" />
          )}
        </Skeleton>
      ),
    },
    {
      key: 'history',
      label: (
        <span className="flex items-center gap-1">
          <ClockCircleOutlined />
          操作历史
        </span>
      ),
      children: (
        <Skeleton active loading={historyLoading}>
          {operationHistory && operationHistory.length > 0 ? (
            <List
              dataSource={operationHistory}
              renderItem={(item: OperationHistory) => {
                const style = getOperationTypeStyle(item.operationType)
                const Icon = style.icon
                return (
                  <List.Item
                    key={item.id}
                    className="transition-all duration-200 hover:bg-gray-50 rounded-lg px-2"
                  >
                    <List.Item.Meta
                      avatar={
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center ${style.bgClass} ${style.textClass}`}>
                          <Icon />
                        </div>
                      }
                      title={
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <Tag color={style.color}>{style.text}</Tag>
                            {item.freezeTypeDesc && (
                              <span className="text-sm text-gray-500">
                                ({item.freezeTypeDesc})
                              </span>
                            )}
                          </div>
                          <span className="text-xs text-gray-400">{item.operateTime}</span>
                        </div>
                      }
                      description={
                        <div className="text-sm">
                          {item.remark && (
                            <div className="text-gray-600 mb-1">
                              备注：{item.remark}
                            </div>
                          )}
                          <div className="text-gray-500">
                            操作人：{item.operator}
                          </div>
                        </div>
                      }
                    />
                  </List.Item>
                )
              }}
            />
          ) : (
            <Empty description="暂无操作历史" />
          )}
        </Skeleton>
      ),
    },
    {
      key: 'transactions',
      label: (
        <span className="flex items-center gap-1">
          <FileTextOutlined />
          交易记录
        </span>
      ),
      children: (
        <Skeleton active loading={transactionLoading}>
          {transactions && transactions.length > 0 ? (
            <List
              dataSource={transactions}
              renderItem={(item: Transaction) => (
                <List.Item
                  key={item.transactionId}
                  className="transition-all duration-200 hover:bg-gray-50 rounded-lg px-2"
                >
                  <List.Item.Meta
                    avatar={
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                        item.totalAmount >= 0
                          ? 'bg-green-100 text-green-600'
                          : 'bg-red-100 text-red-600'
                      }`}>
                        {item.totalAmount >= 0 ? (
                          <ArrowDownOutlined />
                        ) : (
                          <ArrowUpOutlined />
                        )}
                      </div>
                    }
                    title={
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{item.transactionTypeDesc}</span>
                          <TransactionStatusTag status={item.status} size="small" />
                        </div>
                        <AmountDisplay
                          amount={item.totalAmount}
                          currency={item.currency}
                          size="default"
                        />
                      </div>
                    }
                    description={
                      <div className="text-sm">
                        <div className="text-gray-600 mb-1">
                          {item.summary || '无摘要'}
                        </div>
                        <div className="flex items-center justify-between text-gray-500">
                          <span className="font-mono text-xs">{item.transactionNo}</span>
                          <span className="text-xs">{item.transactionTime}</span>
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          ) : (
            <Empty description="暂无交易记录" />
          )}
        </Skeleton>
      ),
    },
  ]

  return (
    <Drawer
      title={
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BankOutlined className="text-primary-500" />
            <span className="font-semibold">账户详情</span>
            {account && (
              <span className="text-sm text-gray-500 font-mono">
                {account.accountNo}
              </span>
            )}
          </div>
          <Button
            type="text"
            size="small"
            onClick={() => setUseMockData(!useMockData)}
            className="text-xs"
          >
            {useMockData ? 'Mock数据' : '真实API'}
          </Button>
        </div>
      }
      open={open}
      onClose={onCancel}
      width={720}
      destroyOnClose
      className="account-detail-drawer"
      extra={
        <Button
          icon={<CloseOutlined />}
          onClick={onCancel}
          className="transition-all duration-200 hover:scale-105"
        >
          关闭
        </Button>
      }
    >
      <Divider className="my-4" />
      <Tabs
        defaultActiveKey="basic"
        items={tabItems}
        className="account-detail-tabs"
      />
    </Drawer>
  )
}

export default AccountDetail
