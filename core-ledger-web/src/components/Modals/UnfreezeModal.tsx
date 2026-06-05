import { useState } from 'react'
import { Modal, Form, Input, Button, Popconfirm, message } from 'antd'
import { UnlockOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { unfreezeAccount } from '@/api/account'
import { generateRequestId } from '@/utils/idGenerator'
import { Account } from '@/api/types'

// 解冻弹窗组件属性
interface UnfreezeModalProps {
  open: boolean
  account: Account | null
  onCancel: () => void
  onSuccess?: () => void
}

// 表单字段类型
interface UnfreezeFormData {
  freezeType: number
  remark: string
}

// 解冻弹窗组件
function UnfreezeModal({ open, account, onCancel, onSuccess }: UnfreezeModalProps) {
  const [form] = Form.useForm<UnfreezeFormData>()
  const [confirmLoading, setConfirmLoading] = useState(false)
  const queryClient = useQueryClient()

  // 解冻账户mutation
  const unfreezeMutation = useMutation({
    mutationFn: unfreezeAccount,
    onSuccess: () => {
      message.success('账户解冻成功')
      queryClient.invalidateQueries({ queryKey: ['accountList'] })
      queryClient.invalidateQueries({ queryKey: ['accountDetail', account?.accountId] })
      form.resetFields()
      onSuccess?.()
      onCancel()
    },
    onError: (error) => {
      console.error('解冻失败:', error)
    },
    onSettled: () => {
      setConfirmLoading(false)
    },
  })

  // 处理确认解冻
  const handleUnfreeze = async () => {
    try {
      const values = await form.validateFields()
      setConfirmLoading(true)

      unfreezeMutation.mutate({
        accountId: account!.accountId,
        freezeType: values.freezeType || account!.freezeType || 1,
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
          <UnlockOutlined className="text-success-500" />
          <span>解冻账户</span>
        </div>
      }
      open={open}
      onCancel={handleCancel}
      footer={null}
      destroyOnClose
      width={480}
      className="unfreeze-modal"
    >
      <div className="mb-4 p-4 bg-success-50 rounded-lg border border-success-200 transition-all duration-300">
        <div className="text-sm text-gray-600 mb-2">
          即将解冻账户：
        </div>
        <div className="font-mono font-semibold text-gray-800">
          {account?.accountNo}
        </div>
        <div className="text-sm text-gray-500 mt-1">
          所属用户：{account?.userId}
        </div>
        {account?.freezeTypeDesc && (
          <div className="text-sm text-warning-600 mt-1">
            当前冻结类型：{account.freezeTypeDesc}
          </div>
        )}
        {account?.freezeRemark && (
          <div className="text-sm text-gray-500 mt-1">
            冻结备注：{account.freezeRemark}
          </div>
        )}
      </div>

      <Form
        form={form}
        layout="vertical"
        className="mt-4"
        initialValues={{
          freezeType: account?.freezeType || 1,
        }}
      >
        <Form.Item name="freezeType" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          name="remark"
          label="备注"
          rules={[
            { max: 200, message: '备注不能超过200个字符' },
          ]}
        >
          <Input.TextArea
            placeholder="请输入解冻原因（选填）"
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
          title="确认解冻"
          description="解冻后账户将恢复正常交易功能，确定要解冻该账户吗？"
          onConfirm={handleUnfreeze}
          okText="确认解冻"
          cancelText="取消"
          okButtonProps={{
            type: 'primary',
            loading: confirmLoading,
          }}
        >
          <Button
            type="primary"
            loading={confirmLoading}
            className="transition-all duration-200 hover:scale-105 bg-success-500 hover:bg-success-600"
          >
            确认解冻
          </Button>
        </Popconfirm>
      </div>
    </Modal>
  )
}

export default UnfreezeModal
