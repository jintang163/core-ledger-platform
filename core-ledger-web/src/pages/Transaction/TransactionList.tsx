import { useState, useMemo } from 'react'
import {
  Card,
  Table,
  Button,
  Input,
  Select,
  DatePicker,
  Form,
  Space,
  Tag,
  Row,
  Col,
  Tooltip,
} from 'antd'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EyeOutlined,
  FilterOutlined,
  DownloadOutlined,
} from '@ant-design/icons'
import classNames from 'classnames'
import type { ColumnsType } from 'antd/es/table'
import { Transaction, TransactionTypeEnum, TransactionStatusEnum } from '@/api/types'
import { mockTransactions, transactionTypeOptions, transactionStatusOptions, accountList } from './mockData'
import TransactionStatusTag from '@/components/StatusTag/TransactionStatusTag'
import AmountDisplay from '@/components/AmountDisplay/AmountDisplay'
import { formatDateTime, formatRelativeTimeToNow } from '@/utils/format'
import TransactionDetail from './TransactionDetail'
import { PAGE_SIZE } from '@/utils/constants'

const { RangePicker } = DatePicker

// 筛选表单数据接口
interface FilterFormData {
  dateRange?: [any, any]
  transactionType?: TransactionTypeEnum
  status?: TransactionStatusEnum
  accountId?: string
  keyword?: string
}

// 交易明细列表页面组件
function TransactionList() {
  const [form] = Form.useForm<FilterFormData>()
  const [data, setData] = useState<Transaction[]>(mockTransactions)
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGE_SIZE,
    total: mockTransactions.length,
  })
  const [detailVisible, setDetailVisible] = useState(false)
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null)
  const [filterVisible, setFilterVisible] = useState(true)

  // 筛选数据
  const filteredData = useMemo(() => {
    let result = [...mockTransactions]

    const values = form.getFieldsValue()

    // 日期范围筛选
    if (values.dateRange && values.dateRange.length === 2) {
      const [start, end] = values.dateRange
      const startTime = start.startOf('day').valueOf()
      const endTime = end.endOf('day').valueOf()
      result = result.filter((item) => {
        const itemTime = new Date(item.transactionTime || '').getTime()
        return itemTime >= startTime && itemTime <= endTime
      })
    }

    // 交易类型筛选
    if (values.transactionType) {
      result = result.filter((item) => item.transactionType === values.transactionType)
    }

    // 状态筛选
    if (values.status !== undefined) {
      result = result.filter((item) => item.status === values.status)
    }

    // 账户ID筛选
    if (values.accountId) {
      result = result.filter((item) =>
        item.entries.some((entry) => entry.accountId === values.accountId)
      )
    }

    // 关键词搜索
    if (values.keyword) {
      const keyword = values.keyword.toLowerCase()
      result = result.filter(
        (item) =>
          item.transactionId.toLowerCase().includes(keyword) ||
          item.businessNo.toLowerCase().includes(keyword) ||
          item.transactionNo.toLowerCase().includes(keyword) ||
          (item.voucherNo && item.voucherNo.toLowerCase().includes(keyword)) ||
          (item.summary && item.summary.toLowerCase().includes(keyword))
      )
    }

    return result
  }, [form])

  // 表格列定义
  const columns: ColumnsType<Transaction> = [
    {
      title: '交易ID',
      dataIndex: 'transactionId',
      key: 'transactionId',
      width: 150,
      fixed: 'left' as const,
      render: (text: string) => (
        <Tooltip title={text}>
          <span className="font-mono text-sm text-gray-700">{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '业务单号',
      dataIndex: 'businessNo',
      key: 'businessNo',
      width: 180,
      render: (text: string) => (
        <Tooltip title={text}>
          <span className="font-mono text-sm text-gray-700">{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '凭证号',
      dataIndex: 'voucherNo',
      key: 'voucherNo',
      width: 150,
      render: (text?: string) => (
        text ? (
          <Tag color="purple" className="font-mono text-sm">
            {text}
          </Tag>
        ) : (
          <span className="text-gray-400">-</span>
        )
      ),
    },
    {
      title: '交易类型',
      dataIndex: 'transactionTypeDesc',
      key: 'transactionType',
      width: 100,
      render: (text: string, record) => {
        const typeColors: Record<number, string> = {
          [TransactionTypeEnum.TRANSFER]: 'blue',
          [TransactionTypeEnum.DEPOSIT]: 'green',
          [TransactionTypeEnum.WITHDRAW]: 'orange',
          [TransactionTypeEnum.FEE]: 'red',
          [TransactionTypeEnum.INTEREST]: 'gold',
          [TransactionTypeEnum.ADJUST]: 'purple',
        }
        return (
          <Tag color={typeColors[record.transactionType] || 'default'} className="font-medium">
            {text}
          </Tag>
        )
      },
    },
    {
      title: '金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 160,
      align: 'right' as const,
      render: (amount: number, record) => (
        <AmountDisplay
          amount={amount}
          currency={record.currency}
          size="default"
          useColor={false}
          showSymbol
          thousandSeparator
        />
      ),
      sorter: (a, b) => a.totalAmount - b.totalAmount,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      align: 'center' as const,
      render: (status: TransactionStatusEnum) => (
        <TransactionStatusTag status={status} showIcon size="middle" />
      ),
      filters: [
        { text: '待处理', value: TransactionStatusEnum.PENDING },
        { text: '成功', value: TransactionStatusEnum.SUCCESS },
        { text: '失败', value: TransactionStatusEnum.FAILED },
        { text: '已冲正', value: TransactionStatusEnum.REVERSED },
      ],
      onFilter: (value, record) => record.status === value,
    },
    {
      title: '交易时间',
      dataIndex: 'transactionTime',
      key: 'transactionTime',
      width: 180,
      render: (time?: string) => (
        <div>
          <div className="text-sm text-gray-700">{formatDateTime(time)}</div>
          <div className="text-xs text-gray-400">{formatRelativeTimeToNow(time)}</div>
        </div>
      ),
      sorter: (a, b) => {
        const timeA = new Date(a.transactionTime || '').getTime()
        const timeB = new Date(b.transactionTime || '').getTime()
        return timeA - timeB
      },
      defaultSortOrder: 'descend',
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (_, record) => (
        <Tooltip title="查看详情">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
            className="text-blue-500 hover:text-blue-600 transition-colors"
          >
            详情
          </Button>
        </Tooltip>
      ),
    },
  ]

  // 处理搜索
  const handleSearch = async () => {
    setLoading(true)
    try {
      await form.validateFields()
      // 模拟搜索延迟
      await new Promise((resolve) => setTimeout(resolve, 500))

      setPagination((prev) => ({
        ...prev,
        current: 1,
        total: filteredData.length,
      }))
      setData(filteredData)
    } catch (error) {
      console.error('搜索失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 处理重置
  const handleReset = () => {
    form.resetFields()
    setData(mockTransactions)
    setPagination({
      current: 1,
      pageSize: PAGE_SIZE,
      total: mockTransactions.length,
    })
  }

  // 处理分页变化
  const handlePageChange = (page: number, pageSize: number) => {
    setPagination((prev) => ({
      ...prev,
      current: page,
      pageSize,
    }))
  }

  // 查看详情
  const handleViewDetail = (record: Transaction) => {
    setSelectedTransaction(record)
    setDetailVisible(true)
  }

  // 关闭详情
  const handleCloseDetail = () => {
    setDetailVisible(false)
    setSelectedTransaction(null)
  }

  // 导出数据
  const handleExport = () => {
    console.log('导出数据:', data)
    // 模拟导出成功
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 md:p-8">
      {/* 页面标题 */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-800 mb-2">交易明细</h1>
          <p className="text-gray-500">查看和管理所有交易记录</p>
        </div>
        <div className="flex gap-3">
          <Button
            icon={<DownloadOutlined />}
            onClick={handleExport}
            size="middle"
          >
            导出
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            size="middle"
            className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700"
          >
            新建交易
          </Button>
        </div>
      </div>

      <div className="space-y-6 max-w-full">
        {/* 高级筛选卡片 */}
        <Card
          className="border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
          title={
            <div
              className="flex items-center justify-between cursor-pointer"
              onClick={() => setFilterVisible(!filterVisible)}
            >
              <div className="flex items-center gap-2">
                <FilterOutlined className="text-blue-500" />
                <span className="text-lg font-semibold text-gray-800">高级筛选</span>
              </div>
              <span className="text-sm text-gray-500">
                {filterVisible ? '收起' : '展开'}
              </span>
            </div>
          }
        >
          {filterVisible && (
            <Form
              form={form}
              layout="vertical"
              className="mt-4 animate-fade-in"
            >
              <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} md={8} lg={6}>
                  <Form.Item label="日期范围" name="dateRange">
                    <RangePicker
                      style={{ width: '100%' }}
                      size="middle"
                      placeholder={['开始日期', '结束日期']}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12} md={8} lg={6}>
                  <Form.Item label="交易类型" name="transactionType">
                    <Select
                      placeholder="请选择交易类型"
                      allowClear
                      options={transactionTypeOptions}
                      size="middle"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12} md={8} lg={6}>
                  <Form.Item label="状态" name="status">
                    <Select
                      placeholder="请选择状态"
                      allowClear
                      options={transactionStatusOptions}
                      size="middle"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12} md={8} lg={6}>
                  <Form.Item label="账户ID" name="accountId">
                    <Select
                      showSearch
                      placeholder="请选择账户"
                      allowClear
                      optionFilterProp="label"
                      size="middle"
                    >
                      {accountList.map((account) => (
                        <Select.Option
                          key={account.accountId}
                          value={account.accountId}
                          label={`${account.accountName} - ${account.accountNo}`}
                        >
                          <div className="flex flex-col">
                            <span className="font-medium">{account.accountName}</span>
                            <span className="text-xs text-gray-500 font-mono">{account.accountNo}</span>
                          </div>
                        </Select.Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} md={16} lg={18}>
                  <Form.Item label="关键词搜索" name="keyword">
                    <Input
                      placeholder="输入交易ID、业务单号、凭证号或摘要搜索"
                      prefix={<SearchOutlined className="text-gray-400" />}
                      allowClear
                      size="middle"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8} lg={6}>
                  <Form.Item label="&nbsp;">
                    <Space className="w-full" style={{ justifyContent: 'flex-end' }}>
                      <Button
                        icon={<ReloadOutlined />}
                        onClick={handleReset}
                        size="middle"
                      >
                        重置
                      </Button>
                      <Button
                        type="primary"
                        icon={<SearchOutlined />}
                        onClick={handleSearch}
                        loading={loading}
                        size="middle"
                        className="bg-gradient-to-r from-blue-500 to-indigo-600"
                      >
                        搜索
                      </Button>
                    </Space>
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          )}
        </Card>

        {/* 数据表格卡片 */}
        <Card
          className="border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
          title={
            <div className="flex items-center justify-between">
              <div>
                <span className="text-lg font-semibold text-gray-800">交易列表</span>
                <span className="ml-3 text-sm text-gray-500">
                  共 <span className="text-blue-600 font-semibold">{pagination.total}</span> 条记录
                </span>
              </div>
            </div>
          }
        >
          <Table
            columns={columns}
            dataSource={data}
            rowKey="transactionId"
            loading={loading}
            pagination={{
              ...pagination,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
              pageSizeOptions: ['10', '20', '50', '100'],
              onChange: handlePageChange,
            }}
            scroll={{ x: 1200 }}
            bordered
            size="middle"
            rowClassName={(record) =>
              classNames(
                'transition-all duration-200 hover:bg-blue-50/50 cursor-pointer',
                record.status === TransactionStatusEnum.FAILED && 'bg-red-50/30',
                record.status === TransactionStatusEnum.REVERSED && 'bg-gray-50/50'
              )
            }
            onRow={(record) => ({
              onClick: () => handleViewDetail(record),
            })}
            className="rounded-xl overflow-hidden"
          />
        </Card>
      </div>

      {/* 交易详情抽屉 */}
      <TransactionDetail
        visible={detailVisible}
        transaction={selectedTransaction}
        onClose={handleCloseDetail}
      />
    </div>
  )
}

export default TransactionList
