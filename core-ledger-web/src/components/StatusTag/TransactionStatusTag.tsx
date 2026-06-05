import { Tag } from 'antd'
import { Clock, CheckCircle, XCircle, RotateCcw } from 'lucide-react'
import classNames from 'classnames'
import { TransactionStatusEnum, TransactionStatusDesc } from '@/api/types'

// 交易状态标签组件属性
interface TransactionStatusTagProps {
  status: TransactionStatusEnum
  size?: 'small' | 'default' | 'large'
  showIcon?: boolean
  className?: string
}

// 交易状态配置
const statusConfig = {
  [TransactionStatusEnum.PENDING]: {
    color: 'processing',
    bgColor: 'bg-blue-50',
    borderColor: 'border-blue-200',
    textColor: 'text-blue-700',
    icon: Clock,
    dotColor: 'bg-blue-500',
  },
  [TransactionStatusEnum.SUCCESS]: {
    color: 'success',
    bgColor: 'bg-success-50',
    borderColor: 'border-success-200',
    textColor: 'text-success-700',
    icon: CheckCircle,
    dotColor: 'bg-success-500',
  },
  [TransactionStatusEnum.FAILED]: {
    color: 'error',
    bgColor: 'bg-danger-50',
    borderColor: 'border-danger-200',
    textColor: 'text-danger-700',
    icon: XCircle,
    dotColor: 'bg-danger-500',
  },
  [TransactionStatusEnum.REVERSED]: {
    color: 'default',
    bgColor: 'bg-gray-100',
    borderColor: 'border-gray-300',
    textColor: 'text-gray-600',
    icon: RotateCcw,
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

// 交易状态标签组件
function TransactionStatusTag({
  status,
  size = 'default',
  showIcon = true,
  className,
}: TransactionStatusTagProps) {
  const config = statusConfig[status] || statusConfig[TransactionStatusEnum.PENDING]
  const sizeCfg = sizeConfig[size]
  const Icon = config.icon
  const statusDesc = TransactionStatusDesc[status] || '未知'

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
          className={classNames(
            'flex-shrink-0',
            status === TransactionStatusEnum.PENDING && 'animate-spin'
          )}
          style={{
            width: sizeCfg.iconSize,
            height: sizeCfg.iconSize,
            animationDuration: '3s',
          }}
        />
      )}
      <span>{statusDesc}</span>
    </Tag>
  )
}

export default TransactionStatusTag
