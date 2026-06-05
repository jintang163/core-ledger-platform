import { useState, useMemo } from 'react'
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Modal,
  message,
  Descriptions,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  SwapOutlined,
  CopyOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import classNames from 'classnames'
import dayjs from 'dayjs'
import BalanceIndicator from '@/components/BalanceIndicator/BalanceIndicator'
import EntryTable from '@/components/EntryTable/EntryTable'
import {
  TransactionTypeEnum,
  TransactionTypeDesc,
  TransactionEntryDTO,
  Transaction,
  TransactionStatusEnum,
  DebitCreditEnum,
  DebitCreditDesc,
} from '@/api/types'
import {
  transactionTypeOptions,
  currencyList,
  accountList,
} from './mockData'
import { generateBusinessNo, generateTransactionNo } from '@/utils/idGenerator'
import { formatDateTime, copyToClipboard, formatThtils/format'
import { formatAmount, getCurrencySymbol } from '@/utils/amount'
import TransactionStatusTag from '@/components/StatusTag/TransactionStatusTag'

const { Title, Text } = Typography

// 业务表单数据接口
interface BusinessFormData {
  businessNo: string
  transactionType: TransactionTypeEnum
  currency: string
  summary: string
  operator: string
}

// 复式记账页面组件
function TransactionCreate() {
  const [form] = Form.useForm<BusinessFormData>()
  const [loading, setLoading] = useState(false)
  const [previewVisible, setPreviewVisible] = useState(false)
  const [createdTransaction, setCreatedTransaction] = useState<Transaction | null>(null)
  const [debitTotal, setDebitTotal] = useState(0)
  const [creditTotal, setCreditTotal] = useState(0)
  const [entries, setEntries] = useState<TransactionEntryDTO[]>([])
  const [businessNo] = useState(() => generateBusinessNo('BIZ'))

  // 检查借贷是否平衡
  const isBalanced = useMemo(() => {
    return debitTotal === creditTotal && debitTotal > 0 && entries.length >= 2
  }, [debitTotal, creditTotal, entries.length])

  // 计算总金额
  const totalAmount = useMemo(() => {
    return Math.max(debitTotal, creditTotal)
  }, [debitTotal, creditTotal])

  // 获取当前选择的币种
  const selectedCurrency = Form.useWatch('currency', form) || 'CNY'
  const symbol = getCurrencySymbol(selectedCurrency)

  // 处理分录变化
  const handleEntriesChange = (newEntries: TransactionEntryDTO[]) => {
    setEntries(newEntries)
  }

  // 处理借方总额变化
  const handleDebitTotalChange = (total: number) => {
    setDebitTotal(total)
  }

  // 处理贷方总额变化
  const handleCreditTotalChange = (total: number) => {
    setCreditTotal(total)
  }

  // 刷新业务流水号
  const refreshBusinessNo = () => {
    form.setFieldsValue({
      businessNo: generateBusinessNo('BIZ'),
    })
    message.success('业务流水号已刷新')
  }

  // 提交记账
  const handleSubmit = async () => {
    try {
      // 验证表单
      const values = await form.validateFields()

      // 验证借贷平衡
      if (!isBalanced) {
        message.error('借贷不平衡，请检查分录金额')
        return
      }

      // 验证分录数量
      if (entries.length < 2) {
        message.error('至少需要2条分录')
        return
      }

      setLoading(true)

      // 模拟提交延迟
      await new Promise((resolve) => setTimeout(resolve, 1500))

      // 生成凭证号
      const voucherNo = `VCH${dayjs().format('YYYYMM')}${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`

      // 构造交易数据
      const transaction: Transaction = {
        transactionId: generateTransactionNo(),
        transactionNo: generateTransactionNo(),
        transactionType: values.transactionType,
        transactionTypeDesc: TransactionTypeDesc[values.transactionType],
        businessNo: values.businessNo,
        totalAmount: totalAmount,
        currency: values.currency,
        voucherNo,
        summary: values.summary,
        status: TransactionStatusEnum.SUCCESS,
        statusDesc: '成功',
        operator: values.operator,
        transactionTime: formatDateTime(new Date()),
        createTime: formatDateTime(new Date()),
        entries: entries.map((entry, index) => ({
          entryId: `ETY${Date.now()}${String(index).padStart(3, '0')}`,
          transactionId: generateTransactionNo(),
          accountId: entry.accountId,
          accountNo: accountList.find((a) => a.accountId === entry.accountId)?.accountNo || '',
          subjectCode: entry.subjectCode,
          subjectName: entry.subjectName,
          direction: entry.direction,
          directionDesc: DebitCreditDesc[entry.direction],
          amount: entry.amount,
          currency: values.currency,
          summary: entry.summary,
        })),
      }

      setCreatedTransaction(transaction)
      setLoading(false)
      setPreviewVisible(true)

      message.success('记账成功！')
    } catch (error) {
      setLoading(false)
      console.error('提交失败:', error)
      message.error('提交失败，请检查表单数据')
    }
  }

  // 复制凭证号
  const handleCopyVoucherNo = async () => {
    if (createdTransaction?.voucherNo) {
      await copyToClipboard(createdTransaction.voucherNo)
    }
  }

  // 关闭预览并重置表单
  const handleClosePreview = () => {
    setPreviewVisible(false)
    setCreatedTransaction(null)
    form.resetFields()
    form.setFieldsValue({
      businessNo: generateBusinessNo('BIZ'),
      currency: 'CNY',
    })
    setDebitTotal(0)
    setCreditTotal(0)
    setEntries([])
  }

  // 凭证预览表格列
  const previewColumns = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      align: 'center' as const,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '账户ID',
      dataIndex: 'accountId',
      key: 'accountId',
      width: 120,
    },
    {
      title: '科目代码',
      dataIndex: 'subjectCode',
      key: 'subjectCode',
      width: 120,
      render: (code: string) => <span className="font-mono">{code}</span>,
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
          className="font-medium"
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
        <span className="font-mono font-semibold">
          {symbol}
          {formatThousands(formatAmount(amount, 2))}
        </span>
      ),
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      key: 'summary',
      ellipsis: true,
    },
  ]

  return (
    <div className="min-h-screen bg-gray-50 p-6 md:p-8">
      {/* 页面标题 */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 flex items-center justify-center">
            <SwapOutlined className="text-2xl text-white" />
          </div>
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-gray-800">复式记账</h1>
            <p className="text-gray-500 mt-1">记录借贷交易，确保账务平衡</p>
          </div>
        </div>
      </div>

      <div className="space-y-6 max-w-7xl">
        {/* 业务信息卡片 */}
        <Card
          className="border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
          title={
            <div className="flex items-center gap-2">
              <FileTextOutlined className="text-blue-500" />
              <span className="text-lg font-semibold text-gray-800">业务信息</span>
            </div>
          }
        >
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              businessNo,
              currency: 'CNY',
              operator: '当前用户',
            }}
          >
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {/* 业务流水号 */}
              <Form.Item
                label="业务流水号"
                name="businessNo"
                rules={[{ required: true, message: '请输入业务流水号' }]}
              >
                <div className="flex gap-2">
                  <Input
                    placeholder="自动生成，可编辑"
                    className="font-mono"
                    maxLength={50}
                  />
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={refreshBusinessNo}
                    className="flex-shrink-0"
                  />
                </div>
              </Form.Item>

              {/* 交易类型 */}
              <Form.Item
                label="交易类型"
                name="transactionType"
                rules={[{ required: true, message: '请选择交易类型' }]}
              >
                <Select
                  placeholder="请选择交易类型"
                  options={transactionTypeOptions}
                  size="middle"
                  className="w-full"
                />
              </Form.Item>

              {/* 币种 */}
              <Form.Item
                label="币种"
                name="currency"
                rules={[{ required: true, message: '请选择币种' }]}
              >
                <Select
                  placeholder="请选择币种"
                  size="middle"
                  className="w-full"
                >
                  {currencyList.map((curr) => (
                    <Select.Option key={curr.code} value={curr.code}>
                      {curr.symbol} {curr.name} ({curr.code})
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>

              {/* 摘要 */}
              <Form.Item
                label="摘要"
                name="summary"
                className="md:col-span-2"
                rules={[{ required: true, message: '请输入摘要' }]}
              >
                <Input.TextArea
                  placeholder="请输入交易摘要"
                  rows={2}
                  maxLength={200}
                  showCount
                  className="resize-none"
                />
              </Form.Item>

              {/* 操作人 */}
              <Form.Item
                label="操作人"
                name="operator"
                rules={[{ required: true, message: '请输入操作人' }]}
              >
                <Input
                  placeholder="请输入操作人姓名"
                  maxLength={50}
                />
              </Form.Item>
            </div>
          </Form>
        </Card>

        {/* 分录动态表格 */}
        <Card
          className="border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
        >
          <EntryTable
            currency={selectedCurrency}
            onChange={handleEntriesChange}
            onDebitTotalChange={handleDebitTotalChange}
            onCreditTotalChange={handleCreditTotalChange}
          />
        </Card>

        {/* 借贷平衡指示器 */}
        <BalanceIndicator
          debitTotal={debitTotal}
          creditTotal={creditTotal}
          currency={selectedCurrency}
        />

        {/* 提交按钮区域 */}
        <div className="flex justify-center py-6">
          <Button
            type="primary"
            size="large"
            loading={loading}
            disabled={!isBalanced}
            onClick={handleSubmit}
            className={classNames(
              'px-12 h-12 text-lg font-semibold rounded-xl transition-all duration-300',
              isBalanced
                ? 'bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 hover:shadow-lg hover:-translate-y-0.5'
                : 'bg-gray-300 cursor-not-allowed'
            )}
            icon={<CheckCircleOutlined />}
          >
            {loading ? '提交中...' : isBalanced ? '确认记账' : '请确保借贷平衡'}
          </Button>
        </div>
      </div>

      {/* 凭证预览弹窗 */}
      <Modal
        title={
          <div className="flex items-center justify-between">
            <span className="text-xl font-bold text-gray-800">记账凭证预览</span>
            <Button
              type="primary"
              icon={<CopyOutlined />}
              onClick={handleCopyVoucherNo}
              size="middle"
            >
              复制凭证号
            </Button>
          </div>
        }
        open={previewVisible}
        onCancel={handleClosePreview}
        footer={[
          <Button key="close" size="large" onClick={handleClosePreview}>
            关闭
          </Button>,
          <Button
            key="confirm"
            type="primary"
            size="large"
            onClick={handleClosePreview}
            className="bg-gradient-to-r from-emerald-500 to-teal-600"
          >
            完成
          </Button>,
        ]}
        width={900}
        centered
        className="voucher-preview-modal"
      >
        {createdTransaction && (
          <div className="space-y-6 animate-fade-in">
            {/* 大号凭证号 */}
            <div className="text-center py-6 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl">
              <div className="text-sm text-gray-500 mb-2">凭证号</div>
              <Title level={1} className="!m-0 !text-4xl font-mono font-bold text-blue-600">
                {createdTransaction.voucherNo}
              </Title>
              <div className="mt-3">
                <TransactionStatusTag status={createdTransaction.status} size="large" />
              </div>
            </div>

            {/* 交易信息 */}
            <Descriptions
              bordered
              column={2}
              size="middle"
              className="bg-white rounded-xl overflow-hidden"
            >
              <Descriptions.Item label="交易ID">
                <span className="font-mono">{createdTransaction.transactionId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="交易单号">
                <span className="font-mono">{createdTransaction.transactionNo}</span>
              </Descriptions.Item>
              <Descriptions.Item label="业务单号">
                <span className="font-mono">{createdTransaction.businessNo}</span>
              </Descriptions.Item>
              <Descriptions.Item label="交易类型">
                <Tag color="blue">{createdTransaction.transactionTypeDesc}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="币种">
                {getCurrencySymbol(createdTransaction.currency)} {createdTransaction.currency}
              </Descriptions.Item>
              <Descriptions.Item label="交易金额">
                <span className="text-xl font-bold text-blue-600 font-mono">
                  {symbol}
                  {formatThousands(formatAmount(createdTransaction.totalAmount, 2))}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="操作人">
                {createdTransaction.operator}
              </Descriptions.Item>
              <Descriptions.Item label="交易时间">
                {createdTransaction.transactionTime}
              </Descriptions.Item>
              <Descriptions.Item label="摘要" span={2}>
                {createdTransaction.summary}
              </Descriptions.Item>
            </Descriptions>

            {/* 分录表格 */}
            <div>
              <h4 className="text-lg font-semibold text-gray-800 mb-4">分录明细</h4>
              <Table
                columns={previewColumns}
                dataSource={createdTransaction.entries}
                rowKey="entryId"
                pagination={false}
                bordered
                size="middle"
                rowClassName={(record) =>
                  classNames(
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
                    <Table.Summary.Row className="bg-gray-100 font-semibold">
                      <Table.Summary.Cell index={0} colSpan={5} align="right">
                        合计：
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={1} align="right" className="text-blue-600">
                        {symbol}
                        {formatThousands(formatAmount(debitSum, 2))}
                      </Table.Summary.Cell>
                      <Table.Summary.Cell index={2} align="right" className="text-red-600">
                        {symbol}
                        {formatThousands(formatAmount(creditSum, 2))}
                      </Table.Summary.Cell>
                    </Table.Summary.Row>
                  )
                }}
              />
            </div>

            {/* 借贷平衡验证 */}
            <div className="flex items-center justify-center gap-4 py-4">
              <CheckCircleOutlined className="text-2xl text-success-500" />
              <Text type="success" className="text-lg font-semibold">
                借贷平衡，验证通过
              </Text>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default TransactionCreate
