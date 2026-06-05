import { useState, useCallback } from 'react'
import { Table, Select, Input, Radio, Button, Popconfirm, message } from 'antd'
import { PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import classNames from 'classnames'
import { DebitCreditEnum, TransactionEntryDTO } from '@/api/types'
import { subjectList, accountList } from '@/pages/Transaction/mockData'
import { yuanToFen, formatAmount, getCurrencySymbol } from '@/utils/amount'
import { generateEntryNo } from '@/utils/idGenerator'

// 分录行数据接口
interface EntryRow {
  key: string
  accountId: string
  subjectCode: string
  subjectName: string
  direction: DebitCreditEnum
  amount: number
  summary: string
}

// 分录动态表格组件属性
interface EntryTableProps {
  // 分录行数据
  value?: TransactionEntryDTO[]
  // 数据变化回调
  onChange?: (value: TransactionEntryDTO[]) => void
  // 借方总额变化回调
  onDebitTotalChange?: (total: number) => void
  // 贷方总额变化回调
  onCreditTotalChange?: (total: number) => void
  // 货币类型，默认 CNY
  currency?: string
  // 是否禁用
  disabled?: boolean
  // 自定义类名
  className?: string
}

// 分录动态表格组件
function EntryTable({
  value,
  onChange,
  onDebitTotalChange,
  onCreditTotalChange,
  currency = 'CNY',
  disabled = false,
  className,
}: EntryTableProps) {
  const [form] = Form.useForm()
  const [searchText, setSearchText] = useState('')
  const [editingKey, setEditingKey] = useState('')

  // 获取货币符号
  const symbol = getCurrencySymbol(currency)

  // 初始化行数据
  const getInitialRows = useCallback((): EntryRow[] => {
    if (value && value.length > 0) {
      return value.map((item, index) => ({
        key: generateEntryNo(),
        accountId: item.accountId,
        subjectCode: item.subjectCode,
        subjectName: item.subjectName,
        direction: item.direction,
        amount: item.amount,
        summary: item.summary || '',
      }))
    }
    return [
      {
        key: generateEntryNo(),
        accountId: '',
        subjectCode: '',
        subjectName: '',
        direction: DebitCreditEnum.DEBIT,
        amount: 0,
        summary: '',
      },
      {
        key: generateEntryNo(),
        accountId: '',
        subjectCode: '',
        subjectName: '',
        direction: DebitCreditEnum.CREDIT,
        amount: 0,
        summary: '',
      },
    ]
  }, [value])

  const [rows, setRows] = useState<EntryRow[]>(getInitialRows)

  // 计算借贷总额
  const calculateTotals = useCallback(
    (currentRows: EntryRow[]) => {
      let debitTotal = 0
      let creditTotal = 0

      currentRows.forEach((row) => {
        const amount = row.amount || 0
        if (row.direction === DebitCreditEnum.DEBIT) {
          debitTotal += amount
        } else {
          creditTotal += amount
        }
      })

      onDebitTotalChange?.(debitTotal)
      onCreditTotalChange?.(creditTotal)
    },
    [onDebitTotalChange, onCreditTotalChange]
  )

  // 更新行数据并触发回调
  const updateRows = useCallback(
    (newRows: EntryRow[]) => {
      setRows(newRows)
      calculateTotals(newRows)

      const dtoData: TransactionEntryDTO[] = newRows
        .filter((row) => row.accountId && row.subjectCode && row.amount > 0)
        .map((row) => ({
          accountId: row.accountId,
          subjectCode: row.subjectCode,
          subjectName: row.subjectName,
          direction: row.direction,
          amount: row.amount,
          summary: row.summary,
        }))

      onChange?.(dtoData)
    },
    [calculateTotals, onChange]
  )

  // 添加分录行
  const addRow = () => {
    if (disabled) return

    const newRow: EntryRow = {
      key: generateEntryNo(),
      accountId: '',
      subjectCode: '',
      subjectName: '',
      direction: DebitCreditEnum.DEBIT,
      amount: 0,
      summary: '',
    }

    const newRows = [...rows, newRow]
    updateRows(newRows)
    message.success('已添加新的分录行')
  }

  // 删除分录行
  const deleteRow = (key: string) => {
    if (disabled) return

    if (rows.length <= 2) {
      message.warning('至少保留2条分录行')
      return
    }

    const newRows = rows.filter((row) => row.key !== key)
    updateRows(newRows)
    message.success('已删除分录行')
  }

  // 更新行字段
  const updateRowField = (key: string, field: keyof EntryRow, value: any) => {
    if (disabled) return

    const newRows = rows.map((row) => {
      if (row.key === key) {
        const updatedRow = { ...row, [field]: value }

        // 如果更新的是科目代码，自动设置科目名称
        if (field === 'subjectCode') {
          const subject = subjectList.find((s) => s.code === value)
          if (subject) {
            updatedRow.subjectName = subject.name
          }
        }

        return updatedRow
      }
      return row
    })

    updateRows(newRows)
  }

  // 金额格式化显示
  const formatAmountDisplay = (amount: number): string => {
    return formatAmount(amount, 2)
  }

  // 金额解析
  const parseAmount = (value: string): number => {
    const cleanValue = value.replace(/[^\d.-]/g, '')
    return yuanToFen(cleanValue)
  }

  // 过滤账户列表
  const filteredAccounts = accountList.filter(
    (acc) =>
      acc.accountId.toLowerCase().includes(searchText.toLowerCase()) ||
      acc.accountNo.includes(searchText) ||
      acc.accountName.toLowerCase().includes(searchText.toLowerCase())
  )

  // 表格列定义
  const columns = [
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
      width: 200,
      render: (_: any, record: EntryRow) => (
        <Select
          showSearch
          placeholder="请选择账户"
          value={record.accountId || undefined}
          onChange={(value) => updateRowField(record.key, 'accountId', value)}
          disabled={disabled}
          optionFilterProp="label"
          style={{ width: '100%' }}
          suffixIcon={<SearchOutlined />}
          filterOption={false}
          onSearch={(value) => setSearchText(value)}
          onClear={() => setSearchText('')}
          allowClear
          size="middle"
          className="transition-all duration-200"
        >
          {filteredAccounts.map((account) => (
            <Select.Option
              key={account.accountId}
              value={account.accountId}
              label={`${account.accountName} - ${account.accountNo}`}
            >
              <div className="flex flex-col">
                <span className="font-medium text-gray-800">{account.accountName}</span>
                <span className="text-xs text-gray-500">
                  {account.accountNo} | 余额：{symbol}
                  {formatAmountDisplay(account.balance)}
                </span>
              </div>
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: '科目代码',
      dataIndex: 'subjectCode',
      key: 'subjectCode',
      width: 180,
      render: (_: any, record: EntryRow) => (
        <Select
          placeholder="请选择科目"
          value={record.subjectCode || undefined}
          onChange={(value) => updateRowField(record.key, 'subjectCode', value)}
          disabled={disabled}
          style={{ width: '100%' }}
          allowClear
          size="middle"
          showSearch
          optionFilterProp="label"
        >
          {subjectList.map((subject) => (
            <Select.Option
              key={subject.code}
              value={subject.code}
              label={`${subject.code} - ${subject.name}`}
            >
              <span className="font-mono">{subject.code}</span>
              <span className="ml-2 text-gray-600">{subject.name}</span>
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: '科目名称',
      dataIndex: 'subjectName',
      key: 'subjectName',
      width: 150,
      render: (_: any, record: EntryRow) => (
        <Input
          value={record.subjectName}
          onChange={(e) => updateRowField(record.key, 'subjectName', e.target.value)}
          disabled={disabled || !!record.subjectCode}
          placeholder="自动填充"
          size="middle"
          className={classNames({
            'bg-gray-50': !!record.subjectCode,
          })}
        />
      ),
    },
    {
      title: '借贷方向',
      dataIndex: 'direction',
      key: 'direction',
      width: 140,
      align: 'center' as const,
      render: (_: any, record: EntryRow) => (
        <Radio.Group
          value={record.direction}
          onChange={(e) => updateRowField(record.key, 'direction', e.target.value)}
          disabled={disabled}
          size="middle"
          buttonStyle="solid"
          className="transition-all duration-200"
        >
          <Radio.Button
            value={DebitCreditEnum.DEBIT}
            className={classNames(
              'transition-all duration-200',
              record.direction === DebitCreditEnum.DEBIT && '!bg-blue-500 !text-white !border-blue-500'
            )}
          >
            借
          </Radio.Button>
          <Radio.Button
            value={DebitCreditEnum.CREDIT}
            className={classNames(
              'transition-all duration-200',
              record.direction === DebitCreditEnum.CREDIT && '!bg-red-500 !text-white !border-red-500'
            )}
          >
            贷
          </Radio.Button>
        </Radio.Group>
      ),
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 160,
      align: 'right' as const,
      render: (_: any, record: EntryRow) => (
        <div className="flex items-center justify-end">
          <span className="text-gray-500 mr-2">{symbol}</span>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={record.amount > 0 ? formatAmountDisplay(record.amount) : ''}
            onChange={(e) => {
              const amount = parseAmount(e.target.value)
              updateRowField(record.key, 'amount', amount)
            }}
            disabled={disabled}
            placeholder="0.00"
            size="middle"
            className="text-right font-mono"
            style={{ width: '120px' }}
          />
        </div>
      ),
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      key: 'summary',
      minWidth: 200,
      render: (_: any, record: EntryRow) => (
        <Input
          value={record.summary}
          onChange={(e) => updateRowField(record.key, 'summary', e.target.value)}
          disabled={disabled}
          placeholder="请输入摘要"
          size="middle"
          maxLength={100}
          showCount
        />
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 80,
      align: 'center' as const,
      fixed: 'right' as const,
      render: (_: any, record: EntryRow) => (
        <Popconfirm
          title="确认删除"
          description="确定要删除这条分录吗？"
          onConfirm={() => deleteRow(record.key)}
          okText="删除"
          cancelText="取消"
          okButtonProps={{ danger: true }}
          disabled={disabled || rows.length <= 2}
        >
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            disabled={disabled || rows.length <= 2}
            className="hover:bg-red-50 transition-colors duration-200"
          />
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className={classNames('space-y-4', className)}>
      {/* 表格标题栏 */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-800">
          分录明细
          <span className="ml-2 text-sm font-normal text-gray-500">
            共 {rows.length} 条记录
          </span>
        </h3>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={addRow}
          disabled={disabled}
          size="middle"
          className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 transition-all duration-300"
        >
          添加分录
        </Button>
      </div>

      {/* 分录表格 */}
      <Table
        columns={columns}
        dataSource={rows}
        pagination={false}
        rowKey="key"
        bordered
        size="middle"
        scroll={{ x: 1200 }}
        rowClassName={(record) =>
          classNames(
            'transition-all duration-300 hover:bg-gray-50',
            record.direction === DebitCreditEnum.DEBIT ? 'bg-blue-50/30' : 'bg-red-50/30'
          )
        }
        className="rounded-xl overflow-hidden shadow-sm"
      />

      {/* 提示信息 */}
      <div className="text-sm text-gray-500 flex items-center gap-4">
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded-full bg-blue-200" />
          借方行
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded-full bg-red-200" />
          贷方行
        </span>
        <span>•</span>
        <span>至少需要2条分录，借贷金额必须相等</span>
      </div>
    </div>
  )
}

export default EntryTable
