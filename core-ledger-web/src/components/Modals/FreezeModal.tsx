import { useState } from 'react'
import { Modal, Form, Select, Input, Button, Popconfirm, message } from 'antd'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { freezeAccount } from '@/api/account'
import { generateRequestId } from '@/utils/idGenerator'
import { FREEZE_TYPE_MAP } from '@/utils/constants'
import { Account } from '@/api/types'

// 冻结弹窗组件属性
interface FreezeModalProps {
  open: boolean
  account: Account | null
  onCancel: () => void
  onSuccess?: () => void
}

// 表单字段类型
interface FreezeFormData {
  freezeType: number
  remark: string
}

// 冻结弹窗组件
function FreezeModal({ open, account, onCancel, onSuccess }: FreezeModalProps) {
  const [form] = Form.useForm<FreezeFormData>()
  const [confirmLoading, setConfirmLoading] = useState(false)
  const queryClient = useQueryClient()

  // 冻结账户mutation
  const freezeMutation = useMutation({
    mutationFn: freezeAccount,
    onSuccess: () => {
      message.success('账户冻结成功')
      queryClient.invalidateQueries({ queryKey: ['accountList'] })
      queryClient.invalidateQueries({ queryKey: ['accountDetail', account?.accountId] })
      form.resetFields()
      onSuccess?.()
      onCancel()
    },
    onError: (error) => {
      console.error('冻结失败:', error)
    },
    onSettled: () => {
      setConfirmLoading(false)
    },
  })

  // 处理确认冻结
  const handleFreeze = async () => {
    try {
      const values = await form.validateFields()
      setConfirmLoading(true)

      freezeMutation.mutate({
        accountId: account!.accountId,
        freezeType: values.freezeType,
        remark: values.remark,
        operator: '当前操作员',
        requestId: generateRequestId(),
      })
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  // 处理取消
  const handleCancel = () => {
    form.resetFields()
    onCancel()
  }

  return (
    <Modal
      title={
        <div className="flex items-center gap-2">
          <ExclamationCircleOutlined className="text-warning-500" />
          <span>冻结账户</span>
        </div>
      }
      open={open}
      onCancel={handleCancel}
      footer={null}
      destroyOnClose
      width={480}
      className="freeze-modal"
    >
      <div className="mb-4 p-4 bg-warning-50 rounded-lg border border-warning-200 transition-all duration-300">
        <div className="text-sm text-gray-600 mb-2">
          即将冻结账户：
        </div>
        <div className="font-mono font-semibold text-gray-800">
          {account?.accountNo}
        </div>
        <div className="text-sm text-gray-500 mt-1">
          所属用户：{account?.userId}
        </div>
      </div>

      <Form
        form={form}
        layout="vertical"
        className="mt-4"
        initialValues={{ freezeType: 1 }}
      >
        <Form.Item
          name="freezeType"
          label="冻结类型"
          rules={[{ required: true, message: '请选择冻结类型' }]}
        >
          <Select
            placeholder="请选择冻结类型"
            options={Object.entries(FREEZE_TYPE_MAP).map(([value, label]) => ({
              value: Number(value),
              label,
            }))}
            className="w-full"
          />
        </Form.Item>

        <Form.Item
          name="remark"
          label="备注"
          rules={[
            { max: 200, message: '备注不能超过200个字符' },
          ]}
        >
          <Input.TextArea
            placeholder="请输入冻结原因（选填）"
            rows={3}
            showCount
            maxLength={200}
          />
        </Form.Item>
      </Form>

      <div className="flex justify-end gap-3 mt-6">
        <Button
          onClick={handleCancel}
          className="transition-all duration-200 hover:scale-105"
        >
          取消
        </Button>
        <Popconfirm
          title="确认冻结"
          description="冻结后账户将无法进行任何交易操作，确定要冻结该账户吗？"
          onConfirm={handleFreeze}
          okText="确认冻结"
          cancelText="取消"
          okButtonProps={{
            danger: true,
            loading: confirmLoading,
          }}
        >
          <Button
            type="primary"
            danger
            loading={confirmLoading}
            className="transition-all duration-200 hover:scale-105"
          >
            确认冻结
          </Button>
        </Popconfirm>
      </div>
    </Modal>
  )
}

export default FreezeModal
