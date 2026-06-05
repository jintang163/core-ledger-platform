
import {
  Drawer,
  Descriptions,
  Table,
  Tag,
  Button,
  Space,
  Typography,
  Divider,
  Card,
  Row,
  Col,
  Tooltip,
} from 'antd'
import {
  CopyOutlined,
  FileTextOutlined,
  SwapOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  UserOutlined,
} from '@ant-design/icons'
import classNames from 'classnames'
import { Transaction, DebitCreditEnum } from '@/api/types'
import TransactionStatusTag from '@/components/StatusTag/TransactionStatusTag'
import AmountDisplay from '@/components/AmountDisplay/AmountDisplay'
import { formatDateTime, copyToClipboard } from '@/utils/format'
import { getCurrencySymbol } from '@/utils/amount'

const { Title, Text } = Typography

// 交易详情抽屉组件属性
interface TransactionDetailProps {
  // 是否显示
  visible: boolean
  // 交易数据
  transaction: Transaction | null
  // 关闭回调
  onClose: () => void
}

// 交易详情抽屉组件
function TransactionDetail({ visible, transaction, onClose }: TransactionDetailProps) {
  const [loading, setLoading] = useState(false)

  // 复制交易ID
  const handleCopyTransactionId = async () => {
    if (transaction?.transactionId) {
      await copyToClipboard(transaction.transactionId)
    }
  }

  // 复制业务单号
  const handleCopyBusinessNo = async () => {
    if (transaction?.businessNo) {
      await copyToClipboard(transaction.businessNo)
    }
  }

  // 复制凭证号
  const handleCopyVoucherNo = async () => {
    if (transaction?.voucherNo) {
      await copyToClipboard(transaction.voucherNo)
    }
  }

  // 复制交易单号
  const handleCopyTransactionNo = async () => {
    if (transaction?.transactionNo) {
      await copyToClipboard(transaction.transactionNo)
    }
  }

  // 分录表格列定义
  const entryColumns = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      align: 'center' as const,
      render: (_: any, __: any, index: number) => (
        <span className="text-gray-500 font-medium">{index + 1}</span>
      ),
    },
    {
      title: '账户ID',
      dataIndex: 'accountId',
      key: 'accountId',
      width: 120,
      render: (text: string) => (
        <Tooltip title={text}>
          <span className="font-mono text-sm">{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '账户账号',
      dataIndex: 'accountNo',
      key: 'accountNo',
      width: 180,
      render: (text: string) => (
        <Tooltip title={text}>
          <span className="font-mono text-sm">{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '科目代码',
      dataIndex: 'subjectCode',
      key: 'subjectCode',
      width: 120,
      render: (code: string) => <span className="font-mono text-sm">{code}</span>,
    },
    {
      title: '科目名称',
      dataIndex: 'subjectName',
      key: 'subjectName',
      width: 150,
    },
    {
      title: '借贷方向',
      dataIndex: 'directionDesc',
      key: 'direction',
      width: 100,
      align: 'center' as const,
      render: (text: string, record: any) => (
        <Tag
          color={record.direction === DebitCreditEnum.DEBIT ? 'blue' : 'red'}
          className="font-medium w-full text-center"
        >
          {text}
        </Tag>
      ),
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 150,
      align: 'right' as const,
      render: (amount: number) => (
        <AmountDisplay
          amount={amount}
          currency={transaction?.currency || 'CNY'}
          size="default"
          useColor={false}
          showSymbol
          thousandSeparator
        />
      ),
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      key: 'summary',
      ellipsis: true,
      render: (text?: string) => text || '-',
    },
  ]

  if (!transaction) return null

  const symbol = getCurrencySymbol(transaction.currency)

  // 计算借贷合计
  const calculateTotals = () => {
    let debitTotal = 0
    let creditTotal = 0
    transaction.entries.forEach((entry) => {
      if (entry.direction === DebitCreditEnum.DEBIT) {
        debitTotal += entry.amount
      } else {
        creditTotal += entry.amount
      }
    })
    return { debitTotal, creditTotal }
  }

  const { debitTotal, creditTotal } = calculateTotals()
  const isBalanced = debitTotal === creditTotal

  return (
    <Drawer
      title={
        <div className="flex items-center justify-between w-full pr-12">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 flex items-center justify-center">
              <FileTextOutlined className="text-lg text-white" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-gray-800 m-0">交易详情</h3>
              <p className="text-xs text-gray-500 m-0 mt-0.5">查看交易的完整信息</p>
            </div>
          </div>
        </div>
      }
      placement="right"
      onClose={onClose}
      open={visible}
      width={900}
      size="large"
      className="transaction-detail-drawer"
      extra={
        <Space>
          <Button onClick={onClose}>关闭</Button>
        </Space>
      }
    >
      <div className="space-y-6 animate-fade-in">
        {/* 凭证号和状态区域 */}
        <Card
          className="border-0 rounded-2xl shadow-sm bg-gradient-to-r from-blue-50 to-indigo-50"
          bordered={false}
        >
          <div className="text-center">
            {transaction.voucherNo && (
              <>
                <div className="text-sm text-gray-500 mb-2 flex items-center justify-center gap-2">
                  <FileTextOutlined />
                  凭证号
                </div>
                <Title level={2} className="!m-0 !text-3xl font-mono font-bold text-blue-600 mb-3">
                  {transaction.voucherNo}
                </Title>
                <Button
                  type="primary"
                  icon={<CopyOutlined />}
                  onClick={handleCopyVoucherNo}
                  size="small"
                  className="mb-4"
                >
                  复制凭证号
                </Button>
                <Divider className="my-4" />
              </>
            )}
            <div className="flex items-center justify-center gap-4">
              <div className="text-center">
                <div className="text-sm text-gray-500 mb-1">状态</div>
                <TransactionStatusTag status={transaction.status} size="large" showIcon />
              </div>
              <div className="w-px h-12 bg-gray-300" />
              <div className="text-center">
                <div className="text-sm text-gray-500 mb-1">交易类型</div>
                <Tag color="blue" className="text-base font-medium px-4 py-1">
                  {transaction.transactionTypeDesc}
                </Tag>
              </div>
              <div className="w-px h-12 bg-gray-300" />
              <div className="text-center">
                <div className="text-sm text-gray-500 mb-1">交易金额</div>
                <div className="text-2xl font-bold text-blue-600 font-mono">
                  <AmountDisplay
                    amount={transaction.totalAmount}
                    currency={transaction.currency}
                    size="xlarge"
                    useColor={false}
                    showSymbol
                    thousandSeparator
                  />
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* 基本信息卡片 */}
        <Card
          className="border-0 rounded-2xl shadow-sm"
          title={
            <div className="flex items-center gap-2">
              <SwapOutlined className="text-blue-500" />
              <span className="text-lg font-semibold text-gray-800">基本信息</span>
            </div>
          }
        >
          <Descriptions bordered column={2} size="middle">
            <Descriptions.Item
              label={
                <span className="flex items-center gap-1">
                  <span>交易ID</span>
                </span>
              }
            >
              <div className="flex items-center gap-2">
                <span className="font-mono">{transaction.transactionId}</span>
                <Tooltip title="复制交易ID">
                  <Button
                    type="text"
                    icon={<CopyOutlined />}
                    size="small"
                    onClick={handleCopyTransactionId}
                    className="text-gray-400 hover:text-blue-500"
                  />
                </Tooltip>
              </div>
            </Descriptions.Item>
            <Descriptions.Item
              label={
                <span className="flex items-center gap-1">
                  <span>交易单号</span>
                </span>
              }
            >
              <div className="flex items-center gap-2">
                <span className="font-mono">{transaction.transactionNo}</span>
                <Tooltip title="复制交易单号">
                  <Button
                    type="text"
                    icon={<CopyOutlined />}
                    size="small"
                    onClick={handleCopyTransactionNo}
                    className="text-gray-400 hover:text-blue-500"
                  />
                </Tooltip>
              </div>
            </Descriptions.Item>
            <Descriptions.Item
              label={
                <span className="flex items-center gap-1">
                  <span>业务单号</span>
                </span>
              }
            >
              <div className="flex items-center gap-2">
                <span className="font-mono">{transaction.businessNo}</span>
                <Tooltip title="复制业务单号">
                  <Button
                    type="text"
                    icon={<CopyOutlined />}
                    size="small"
                    onClick={handleCopyBusinessNo}
                    className="text-gray-400 hover:text-blue-500"
                  />
                </Tooltip>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="币种">
              {symbol} {transaction.currency}
            </Descriptions.Item>
            <Descriptions.Item
              label={
                <span className="flex items-center gap-1">
                  <UserOutlined className="text-gray-400 text-xs" />
                  <span>操作人</span>
                </span>
              }
            >
              {transaction.operator || '-'}
            </Descriptions.Item>
            <Descriptions.Item
              label={
                <span className="flex items-center gap-1">
                  <ClockCircleOutlined className="text-gray-400 text-xs" />
                  <span>交易时间</span>
                </span>
              }
            >
              {formatDateTime(transaction.transactionTime)}
            </Descriptions.Item>
            <Descriptions.Item
              label={
                <span className="flex items-center gap-1">
                  <ClockCircleOutlined className="text-gray-400 text-xs" />
                  <span>创建时间</span>
                </span>
              }
            >
              {formatDateTime(transaction.createTime)}
            </Descriptions.Item>
            <Descriptions.Item label="摘要" span={2}>
              {transaction.summary || '-'}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 分录明细卡片 */}
        <Card
          className="border-0 rounded-2xl shadow-sm"
          title={
            <div className="flex items-center gap-2">
              <FileTextOutlined className="text-blue-500" />
              <span className="text-lg font-semibold text-gray-800">分录明细</span>
              <Tag color="blue" className="ml-2">
                共 {transaction.entries.length} 条
              </Tag>
            </div>
          }
        >
          <Table
            columns={entryColumns}
            dataSource={transaction.entries}
            rowKey="entryId"
            pagination={false}
            bordered
            size="middle"
            scroll={{ x: 1000 }}
            rowClassName={(record) =>
              classNames(
                'transition-all duration-200',
                record.direction === DebitCreditEnum.DEBIT ? 'bg-blue-50/50' : 'bg-red-50/50'
              )
            }
            summary={(pageData) => {
              let debitSum = 0
              let creditSum = 0
              pageData.forEach((item) => {
                if (item.direction === DebitCreditEnum.DEBIT) {
                  debitSum += item.amount
                } else {
                  creditSum += item.amount
                }
              })
              return (
                <>
                  <Table.Summary.Row className="bg-gray-100 font-semibold">
                    <Table.Summary.Cell index={0} colSpan={6} align="right">
                      合计：
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={1} align="right" className="text-blue-600">
                      <AmountDisplay
                        amount={debitSum}
                        currency={transaction.currency}
                        size="default"
                        useColor={false}
                        showSymbol
                        thousandSeparator
                      />
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={2} align="right" className="text-red-600">
                      <AmountDisplay
                        amount={creditSum}
                        currency={transaction.currency}
                        size="default"
                        useColor={false}
                        showSymbol
                        thousandSeparator
                      />
                    </Table.Summary.Cell>
                  </Table.Summary.Row>
                </>
              )
            }}
            className="rounded-xl overflow-hidden"
          />

          {/* 借贷平衡验证 */}
          <div
            className={classNames(
              'mt-4 p-4 rounded-xl flex items-center justify-center gap-4',
              isBalanced ? 'bg-success-50 border border-success-200' : 'bg-danger-50 border border-danger-200'
            )}
          >
            {isBalanced ? (
              <>
                <CheckCircleOutlined className="text-2xl text-success-500" />
                <Text type="success" className="text-base font-semibold">
                  借贷平衡，验证通过
                </Text>
              </>
            ) : (
              <>
                <span className="text-2xl text-danger-500">✕</span>
                <Text type="danger" className="text-base font-semibold">
                  借贷不平衡，差额：{symbol}
                  {Math.abs(debitTotal - creditTotal) / 100}
                </Text>
              </>
            )}
          </div>
        </Card>

        {/* 快捷操作区域 */}
        <Card
          className="border-0 rounded-2xl shadow-sm"
          title={
            <div className="flex items-center gap-2">
              <span className="text-lg font-semibold text-gray-800">快捷操作</span>
            </div>
          }
        >
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8}>
              <Button
                block
                icon={<CopyOutlined />}
                onClick={handleCopyTransactionId}
                size="middle"
                className="h-12"
              >
                复制交易ID
              </Button>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Button
                block
                icon={<CopyOutlined />}
                onClick={handleCopyBusinessNo}
                size="middle"
                className="h-12"
              >
                复制业务单号
              </Button>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Button
                block
                icon={<CopyOutlined />}
                onClick={handleCopyVoucherNo}
                disabled={!transaction.voucherNo}
                size="middle"
                className="h-12"
              >
                复制凭证号
              </Button>
            </Col>
          </Row>
        </Card>
      </div>
    </Drawer>
  )
}

export default TransactionDetail
