import { useState, useMemo } from 'react'
import {
  Table,
  Button,
  Form,
  Select,
  Input,
  Space,
  Card,
  Pagination,
  Skeleton,
  Alert,
  Tooltip,
} from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  EyeOutlined,
  LockOutlined,
  UnlockOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import type { TableProps } from 'antd'
import { getAccountList } from '@/api/account'
import { Account, AccountStatusEnum, AccountTypeEnum } from '@/api/types'
import { CURRENCY_MAP, PAGE_SIZE } from '@/utils/constants'
import AmountDisplay from '@/components/AmountDisplay/AmountDisplay'
import AccountStatusTag from '@/components/StatusTag/AccountStatusTag'
import { getMockAccountList } from './mockData'
import FreezeModal from '@/components/Modals/FreezeModal'
import UnfreezeModal from '@/components/Modals/UnfreezeModal'
import CloseModal from '@/components/Modals/CloseModal'
import AccountCreate from './AccountCreate'
import AccountDetail from './AccountDetail'

// 筛选表单字段类型
interface FilterFormData {
  accountType?: number
  status?: number
  currency?: string
  keyword?: string
}

// 账户列表页面
function AccountList() {
  const [form] = Form.useForm<FilterFormData>()
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE)
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null)
  const [freezeModalOpen, setFreezeModalOpen] = useState(false)
  const [unfreezeModalOpen, setUnfreezeModalOpen] = useState(false)
  const [closeModalOpen, setCloseModalOpen] = useState(false)
  const [createDrawerOpen, setCreateDrawerOpen] = useState(false)
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
  const [useMockData, setUseMockData] = useState(true)

  // 查询账户列表
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['accountList', pageNum, pageSize, form.getFieldsValue()],
    queryFn: async () => {
      const filters = form.getFieldsValue()
      const params = {
        ...filters,
        pageNum,
        pageSize,
      }
      if (useMockData) {
        await new Promise((resolve) => setTimeout(resolve, 500))
        return getMockAccountList(params)
      }
      return getAccountList(params)
    },
  })

  // 表格列配置
  const columns: TableProps<Account>['columns'] = useMemo(
    () => [
      {
        title: '账户ID',
        dataIndex: 'accountId',
        key: 'accountId',
        width: 120,
        ellipsis: { showTitle: true },
        render: (text: string) => (
          <Tooltip title={text}>
            <span className="font-mono text-gray-700">{text}</span>
          </Tooltip>
        ),
      },
      {
        title: '账户号',
        dataIndex: 'accountNo',
        key: 'accountNo',
        width: 180,
        ellipsis: { showTitle: true },
        render: (text: string) => (
          <Tooltip title={text}>
            <span className="font-mono font-medium text-gray-800">{text}</span>
          </Tooltip>
        ),
      },
      {
        title: '用户ID',
        dataIndex: 'userId',
        key: 'userId',
        width: 120,
        ellipsis: { showTitle: true },
        render: (text: string) => (
          <Tooltip title={text}>
            <span className="font-mono text-gray-700">{text}</span>
          </Tooltip>
        ),
      },
      {
        title: '账户类型',
        dataIndex: 'accountTypeDesc',
        key: 'accountType',
        width: 100,
        render: (text: string, record) => (
          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700'>
            {text}
          </span>
        ),
      },
      {
        title: '币种',
        dataIndex: 'currencyDesc',
        key: 'currency',
        width: 100,
        render: (text: string) => (
          <span className="text-gray-700">{text}</span>
        ),
      },
      {
        title: '余额',
        dataIndex: 'balance',
        key: 'balance',
        width: 150,
        render: (value: number, record) => (
          <AmountDisplay
            amount={value}
            currency={record.currency}
            size="default"
          />
        ),
        sorter: (a, b) => a.balance - b.balance,
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        render: (status: AccountStatusEnum) => (
          <AccountStatusTag status={status} />
        ),
      },
      {
        title: '开户时间',
        dataIndex: 'openTime',
        key: 'openTime',
        width: 180,
        render: (text: string) => (
          <span className="text-gray-600 text-sm">{text}</span>
        ),
      },
      {
        title: '操作',
        key: 'action',
        width: 220,
        fixed: 'right',
        render: (_: any, record: Account) => (
          <Space size="small">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
              className="transition-all duration-200 hover:scale-105"
            >
              查看
            </Button>
            {record.status === AccountStatusEnum.NORMAL && (
              <Button
                type="link"
                size="small"
                icon={<LockOutlined />}
                onClick={() => handleFreeze(record)}
                className="text-warning-600 hover:text-warning-700 transition-all duration-200 hover:scale-105"
              >
                冻结
              </Button>
            )}
            {record.status === AccountStatusEnum.FROZEN && (
              <Button
                type="link"
                size="small"
                icon={<UnlockOutlined />}
                onClick={() => handleUnfreeze(record)}
                className="text-success-600 hover:text-success-700 transition-all duration-200 hover:scale-105"
              >
                解冻
              </Button>
            )}
            {record.status !== AccountStatusEnum.CLOSED && (
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleClose(record)}
                className="transition-all duration-200 hover:scale-105"
              >
                销户
              </Button>
            )}
          </Space>
        ),
      },
    ],
    []
  )

  // 处理查询
  const handleSearch = () => {
    setPageNum(1)
    refetch()
  }

  // 处理重置
  const handleReset = () => {
    form.resetFields()
    setPageNum(1)
    refetch()
  }

  // 处理查看详情
  const handleViewDetail = (record: Account) => {
    setSelectedAccount(record)
    setDetailDrawerOpen(true)
  }

  // 处理冻结
  const handleFreeze = (record: Account) => {
    setSelectedAccount(record)
    setFreezeModalOpen(true)
  }

  // 处理解冻
  const handleUnfreeze = (record: Account) => {
    setSelectedAccount(record)
    setUnfreezeModalOpen(true)
  }

  // 处理销户
  const handleClose = (record: Account) => {
    setSelectedAccount(record)
    setCloseModalOpen(true)
  }

  // 处理分页变化
  const handlePageChange = (page: number, size: number) => {
    setPageNum(page)
    setPageSize(size)
  }

  return (
    <div className="p-6 bg-gray-50 min-h-screen">
      <Card
        title={
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-xl font-semibold text-gray-800">账户管理</span>
              <Button
                type="text"
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => setUseMockData(!useMockData)}
                className="ml-2 text-xs"
              >
                {useMockData ? '使用Mock数据' : '使用真实API'}
              </Button>
            </div>
          </div>
        }
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setCreateDrawerOpen(true)}
            className="transition-all duration-200 hover:scale-105"
          >
            新建账户
          </Button>
        }
        className="shadow-sm"
      >
        {/* 筛选表单 */}
        <Form
          form={form}
          layout="inline"
          className="mb-6"
          onFinish={handleSearch}
        >
          <Form.Item name="accountType" label="账户类型">
            <Select
              placeholder="全部类型"
              allowClear
              style={{ width: 150 }}
              options={[
                { value: AccountTypeEnum.PERSONAL, label: '个人账户' },
                { value: AccountTypeEnum.ENTERPRISE, label: '企业账户' },
              ]}
            />
          </Form.Item>

          <Form.Item name="status" label="状态">
            <Select
              placeholder="全部状态"
              allowClear
              style={{ width: 150 }}
              options={[
                { value: AccountStatusEnum.NORMAL, label: '正常' },
                { value: AccountStatusEnum.FROZEN, label: '冻结' },
                { value: AccountStatusEnum.CLOSED, label: '已销户' },
              ]}
            />
          </Form.Item>

          <Form.Item name="currency" label="币种">
            <Select
              placeholder="全部币种"
              allowClear
              style={{ width: 150 }}
              options={Object.entries(CURRENCY_MAP).map(([value, label]) => ({
                value,
                label,
              }))}
            />
          </Form.Item>

          <Form.Item name="keyword" label="">
            <Input
              placeholder="搜索用户ID/账户号"
              allowClear
              style={{ width: 250 }}
              prefix={<SearchOutlined className="text-gray-400" />}
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        {/* 错误提示 */}
        {isError && (
          <Alert
            message="加载失败"
            description={error instanceof Error ? error.message : '请稍后重试'}
            type="error"
            showIcon
            className="mb-4"
          />
        )}

        {/* 数据表格 */}
        <Skeleton active loading={isLoading}>
          <Table
            rowKey="accountId"
            columns={columns}
            dataSource={data?.records}
            pagination={false}
            scroll={{ x: 1200 }}
            rowClassName={() => 'transition-all duration-200 hover:bg-blue-50 hover:shadow-sm' }
            className="account-table"
          />
        </Skeleton>

        {/* 分页组件 */}
        <div className="flex justify-end mt-4">
          <Pagination
            current={pageNum}
            pageSize={pageSize}
            total={data?.total || 0}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条记录'}
            onChange={handlePageChange}
            className="transition-all duration-200"
          />
        </div>
      </Card>

      {/* 冻结弹窗 */}
      <FreezeModal
        open={freezeModalOpen}
        account={selectedAccount}
        onCancel={() => setFreezeModalOpen(false)}
        onSuccess={() => refetch()}
      />

      {/* 解冻弹窗 */}
      <UnfreezeModal
        open={unfreezeModalOpen}
        account={selectedAccount}
        onCancel={() => setUnfreezeModalOpen(false)}
        onSuccess={() => refetch()}
      />

      {/* 销户弹窗 */}
      <CloseModal
        open={closeModalOpen}
        account={selectedAccount}
        onCancel={() => setCloseModalOpen(false)}
        onSuccess={() => refetch()}
      />

      {/* 创建账户抽屉 */}
      <AccountCreate
        open={createDrawerOpen}
        onCancel={() => setCreateDrawerOpen(false)}
        onSuccess={() => {
          refetch()
        }}
      />

      {/* 账户详情抽屉 */}
      <AccountDetail
        open={detailDrawerOpen}
        account={selectedAccount}
        onCancel={() => setDetailDrawerOpen(false)}
        onFreeze={handleFreeze}
        onUnfreeze={handleUnfreeze}
        onClose={handleClose}
      />
    </div>
  )
}

export default AccountList
