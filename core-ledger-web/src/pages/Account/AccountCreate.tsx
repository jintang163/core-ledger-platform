import { useState } from 'react'
import {
  Drawer,
  Form,
  Input,
  Select,
  InputNumber,
  Button,
  Space,
  message,
  Alert,
} from 'antd'
import { PlusOutlined, SaveOutlined, CloseOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { createAccount } from '@/api/account'
import { generateRequestId } from '@/utils/idGenerator'
import { CURRENCY_MAP, MAX_BALANCE, MIN_BALANCE } from '@/utils/constants'
import { AccountTypeEnum } from '@/api/types'
import AmountDisplay from '@/components/AmountDisplay/AmountDisplay'

// 创建账户抽屉组件属性
interface AccountCreateProps {
  open: boolean
  onCancel: () => void
  onSuccess?: () => void
}

// 表单字段类型
interface AccountCreateFormData {
  userId: string
  accountType: AccountTypeEnum
  currency: string
  initBalance: number
}

// 创建账户抽屉组件
function AccountCreate({ open, onCancel, onSuccess }: AccountCreateProps) {
  const [form] = Form.useForm<AccountCreateFormData>()
  const [submitting, setSubmitting] = useState(false)
  const [showPreview, setShowPreview] = useState(false)

  // 创建账户mutation
  const createMutation = useMutation({
    mutationFn: createAccount,
    onSuccess: (data) => {
      message.success('账户创建成功')
      form.resetFields()
      setShowPreview(false)
      onSuccess?.()
      onCancel()
      console.log('创建的账户信息:', data)
    },
    onError: (error) => {
      console.error('创建账户失败:', error)
    },
    onSettled: () => {
      setSubmitting(false)
    },
  })

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setSubmitting(true)

      // 转换初始余额为分（乘以100）
      const initBalanceInCents = Math.round(values.initBalance * 100)

      createMutation.mutate({
        userId: values.userId,
        accountType: values.accountType,
        currency: values.currency,
        initBalance: initBalanceInCents,
        requestId: generateRequestId(),
      })
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  // 处理关闭
  const handleClose = () => {
    form.resetFields()
    setShowPreview(false)
    onCancel()
  }

  // 获取表单预览值
  const formValues = form.getFieldsValue()

  return (
    <Drawer
      title={
        <div className="flex items-center gap-2">
          <PlusOutlined className="text-primary-500" />
          <span>新建账户</span>
        </div>
      }
      open={open}
      onClose={handleClose}
      width={520}
      destroyOnClose
      className="account-create-drawer"
      extra={
        <Space>
          <Button
            icon={<CloseOutlined />}
            onClick={handleClose}
            className="transition-all duration-200 hover:scale-105"
          >
            取消
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSubmit}
            loading={submitting}
            className="transition-all duration-200 hover:scale-105"
          >
            确认创建
          </Button>
        </Space>
      }
    >
      <Alert
        message="账户信息将用于资金交易，请确保填写的信息准确无误。"
        type="info"
        showIcon
        className="mb-6"
      />

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          accountType: AccountTypeEnum.PERSONAL,
          currency: 'CNY',
          initBalance: 0,
        }}
        onValuesChange={() => setShowPreview(true)}
      >
        <Form.Item
          name="userId"
          label="用户ID"
          rules={[
            { required: true, message: '请输入用户ID' },
            { min: 3, max: 50, message: '用户ID长度应为3-50个字符' },
            { pattern: /^[a-zA-Z0-9_]+$/, message: '用户ID只能包含字母、数字和下划线' },
          ]}
        >
          <Input
            placeholder="请输入用户ID"
            allowClear
            className="transition-all duration-200"
          />
        </Form.Item>

        <Form.Item
          name="accountType"
          label="账户类型"
          rules={[{ required: true, message: '请选择账户类型' }]}
        >
          <Select
            placeholder="请选择账户类型"
            options={[
              { value: AccountTypeEnum.PERSONAL, label: '个人账户' },
              { value: AccountTypeEnum.ENTERPRISE, label: '企业账户' },
            ]}
            className="w-full transition-all duration-200"
          />
        </Form.Item>

        <Form.Item
          name="currency"
          label="币种"
          rules={[{ required: true, message: '请选择币种' }]}
        >
          <Select
            placeholder="请选择币种"
            options={Object.entries(CURRENCY_MAP).map(([value, label]) => ({
              value,
              label: `${label} (${value})`,
            }))}
            className="w-full transition-all duration-200"
          />
        </Form.Item>

        <Form.Item
          name="initBalance"
          label="初始余额"
          rules={[
            { required: true, message: '请输入初始余额' },
            {
              type: 'number',
              min: MIN_BALANCE,
              max: MAX_BALANCE,
              message: `初始余额应在 ${MIN_BALANCE} - ${MAX_BALANCE} 之间`,
            },
          ]}
        >
          <InputNumber
            placeholder="请输入初始余额"
            min={MIN_BALANCE}
            max={MAX_BALANCE}
            step={0.01}
            precision={2}
            style={{ width: '100%' }}
            className="transition-all duration-200"
            addonBefore={formValues.currency}
          />
        </Form.Item>
      </Form>

      {/* 预览区域 */}
      {showPreview && formValues.userId && (
        <div className="mt-6 p-4 bg-gray-50 rounded-lg border border-gray-200 transition-all duration-500 animate-fadeIn">
          <div className="text-sm font-medium text-gray-700 mb-3">创建预览</div>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">用户ID：</span>
              <span className="font-mono text-gray-800">{formValues.userId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">账户类型：</span>
              <span className="text-gray-800">
                {formValues.accountType === AccountTypeEnum.PERSONAL ? '个人账户' : '企业账户'}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">币种：</span>
              <span className="text-gray-800">
                {CURRENCY_MAP[formValues.currency] || formValues.currency}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">初始余额：</span>
              <span>
                <AmountDisplay
                  amount={formValues.initBalance ? Math.round(formValues.initBalance * 100) : 0}
                  currency={formValues.currency}
                  size="default"
                />
              </span>
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        .animate-fadeIn {
          animation: fadeIn 0.3s ease-out;
        }
      `}</style>
    </Drawer>
  )
}

export default AccountCreate
