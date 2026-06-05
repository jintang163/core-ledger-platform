import { useState } from 'react'
import { Modal, Form, Input, Button, Popconfirm, message, Alert } from 'antd'
import { DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { closeAccount } from '@/api/account'
import { generateRequestId } from '@/utils/idGenerator'
import { Account, AccountStatusEnum } from '@/api/types'
import AmountDisplay from '@/components/AmountDisplay/AmountDisplay'

// 销户弹窗组件属性
interface CloseModalProps {
  open: boolean
  account: Account | null
  onCancel: () => void
  onSuccess?: () => void
}

// 表单字段类型
interface CloseFormData {
  remark: string
}

// 销户弹窗组件
function CloseModal({ open, account, onCancel, onSuccess }: CloseModalProps) {
  const [form] = Form.useForm<CloseFormData>()
  const [confirmLoading, setConfirmLoading] = useState(false)
  const queryClient = useQueryClient()

  // 检查账户是否可以销户
  const canClose = account && account.status === AccountStatusEnum.NORMAL && account.balance === 0

  // 销户mutation
  const closeMutation = useMutation({
    mutationFn: closeAccount,
    onSuccess: () => {
      message.success('账户销户成功')
      queryClient.invalidateQueries({ queryKey: ['accountList'] })
      queryClient.invalidateQueries({ queryKey: ['accountDetail', account?.accountId] })
      form.resetFields()
      onSuccess?.()
      onCancel()
    },
    onError: (error) => {
      console.error('销户失败:', error)
    },
    onSettled: () => {
      setConfirmLoading(false)
    },
  })

  // 处理确认销户
  const handleClose = async () => {
    try {
      const values = await form.validateFields()
      setConfirmLoading(true)

      closeMutation.mutate({
        accountId: account!.accountId,
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
          <DeleteOutlined className="text-danger-500" />
          <span>销户</span>
        </div>
      }
      open={open}
      onCancel={handleCancel}
      footer={null}
      destroyOnClose
      width={520}
      className="close-modal"
    >
      {!canClose && (
        <Alert
          message="无法销户"
          description={
            account?.balance !== 0
              ? '账户余额不为零，请先将资金转出后再进行销户操作。'
              : '账户状态异常，只有正常状态的账户才能销户。'
          }
          type="error"
          showIcon
          className="mb-4"
        />
      )}

      <div className="mb-4 p-4 bg-danger-50 rounded-lg border border-danger-200 transition-all duration-300">
        <div className="flex items-start gap-2 mb-3">
          <ExclamationCircleOutlined className="text-danger-500 mt-0.5" />
          <div className="text-sm text-danger-700 font-medium">
            销户操作不可撤销，销户后账户将永久关闭，请谨慎操作！
          </div>
        </div>

        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600">账户号：</span>
            <span className="font-mono font-semibold text-gray-800">{account?.accountNo}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">所属用户：</span>
            <span className="font-medium text-gray-800">{account?.userId}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">账户类型：</span>
            <span className="text-gray-800">{account?.accountTypeDesc}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">当前余额：</span>
            <span>
              <AmountDisplay
                amount={account?.balance}
                currency={account?.currency}
                size="default"
              />
            </span>
          </div>
        </div>
      </div>

      {canClose && (
        <Form
          form={form}
          layout="vertical"
          className="mt-4"
        >
          <Form.Item
            name="remark"
            label="销户原因"
            rules={[
              { required: true, message: '请输入销户原因' },
              { max: 200, message: '销户原因不能超过200个字符' },
            ]}
          >
            <Input.TextArea
              placeholder="请输入销户原因"
              rows={3}
              showCount
              maxLength={200}
            />
          </Form.Item>
        </Form>
      )}

      <div className="flex justify-end gap-3 mt-6">
        <Button
          onClick={handleCancel}
          className="transition-all duration-200 hover:scale-105"
        >
          取消
        </Button>
        <Popconfirm
          title="确认销户"
          description="销户后账户将永久关闭，无法恢复，确定要销户吗？"
          onConfirm={handleClose}
          okText="确认销户"
          cancelText="取消"
          disabled={!canClose}
          okButtonProps={{
            danger: true,
            loading: confirmLoading,
            disabled: !canClose,
          }}
        >
          <Button
            type="primary"
            danger
            loading={confirmLoading}
            disabled={!canClose}
            className="transition-all duration-200 hover:scale-105"
          >
            确认销户
          </Button>
        </Popconfirm>
      </div>
    </Modal>
  )
}

export default CloseModal
