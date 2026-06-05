import { Tag } from 'antd'
import { CheckCircle, PauseCircle, XCircle } from 'lucide-react'
import classNames from 'classnames'
import { AccountStatusEnum, AccountStatusDesc } from '@/api/types'

// 账户状态标签组件属性
interface AccountStatusTagProps {
  status: AccountStatusEnum
  size?: 'small' | 'default' | 'large'
  showIcon?: boolean
  className?: string
}

// 账户状态配置
const statusConfig = {
  [AccountStatusEnum.NORMAL]: {
    color: 'success',
    bgColor: 'bg-success-50',
    borderColor: 'border-success-200',
    textColor: 'text-success-700',
    icon: CheckCircle,
    dotColor: 'bg-success-500',
  },
  [AccountStatusEnum.FROZEN]: {
    color: 'warning',
    bgColor: 'bg-warning-50',
    borderColor: 'border-warning-200',
    textColor: 'text-warning-700',
    icon: PauseCircle,
    dotColor: 'bg-warning-500',
  },
  [AccountStatusEnum.CLOSED]: {
    color: 'default',
    bgColor: 'bg-gray-100',
    borderColor: 'border-gray-300',
    textColor: 'text-gray-600',
    icon: XCircle,
    dotColor: 'bg-gray-500',
  },
}

// 尺寸配置
const sizeConfig = {
  small: {
    padding: 'px-2 py-0.5 text-xs',
    iconSize: 12,
  },
  default: {
    padding: 'px-3 py-1 text-sm',
    iconSize: 14,
  },
  large: {
    padding: 'px-4 py-1.5 text-base',
    iconSize: 16,
  },
}

// 账户状态标签组件
function AccountStatusTag({
  status,
  size = 'default',
  showIcon = true,
  className,
}: AccountStatusTagProps) {
  const config = statusConfig[status] || statusConfig[AccountStatusEnum.NORMAL]
  const sizeCfg = sizeConfig[size]
  const Icon = config.icon
  const statusDesc = AccountStatusDesc[status] || '未知'

  return (
    <Tag
      className={classNames(
        'inline-flex items-center gap-1.5 border rounded-full font-medium transition-all duration-200 hover:scale-105',
        config.bgColor,
        config.borderColor,
        config.textColor,
        sizeCfg.padding,
        className
      )}
      style={{
        margin: 0,
        lineHeight: 1.5,
      }}
    >
      {showIcon && (
        <Icon
          className="flex-shrink-0"
          style={{ width: sizeCfg.iconSize, height: sizeCfg.iconSize }}
        />
      )}
      <span>{statusDesc}</span>
    </Tag>
  )
}

export default AccountStatusTag
