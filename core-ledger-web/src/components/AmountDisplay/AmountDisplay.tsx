import classNames from 'classnames'
import { formatAmount, getCurrencySymbol } from '@/utils/amount'

// 金额显示组件属性
interface AmountDisplayProps {
  // 金额（单位：分）
  amount: number | string | null | undefined
  // 货币类型，默认 CNY
  currency?: string
  // 小数位数，默认 2
  decimals?: number
  // 是否显示货币符号
  showSymbol?: boolean
  // 是否使用颜色区分正负
  useColor?: boolean
  // 尺寸
  size?: 'small' | 'default' | 'large' | 'xlarge'
  // 自定义类名
  className?: string
  // 是否显示千分位
  thousandSeparator?: boolean
}

// 尺寸配置
const sizeConfig = {
  small: 'text-sm',
  default: 'text-base',
  large: 'text-lg',
  xlarge: 'text-2xl',
}

// 金额显示组件
function AmountDisplay({
  amount,
  currency = 'CNY',
  decimals = 2,
  showSymbol = true,
  useColor = true,
  size = 'default',
  className,
  thousandSeparator = true,
}: AmountDisplayProps) {
  // 转换为元
  const yuanAmount = formatAmount(amount, decimals)
  const numValue = parseFloat(yuanAmount)

  // 获取货币符号
  const symbol = showSymbol ? getCurrencySymbol(currency) : ''

  // 格式化千分位
  const formatNumber = (num: string): string => {
    if (!thousandSeparator) return num
    const parts = num.split('.')
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    return parts.join('.')
  }

  // 判断正负
  const isNegative = numValue < 0
  const isPositive = numValue > 0
  const isZero = numValue === 0

  // 显示的金额（去掉负号，统一处理）
  const displayAmount = formatNumber(Math.abs(numValue).toFixed(decimals))

  return (
    <span
      className={classNames(
        'font-mono font-semibold tracking-tight transition-all duration-200',
        sizeConfig[size],
        {
          'text-success-600': useColor && isPositive,
          'text-danger-600': useColor && isNegative,
          'text-gray-600': useColor && isZero,
          'text-gray-800': !useColor,
        },
        className
      )}
      style={{
        fontVariantNumeric: 'tabular-nums',
        fontFeatureSettings: '"tnum"',
      }}
    >
      {/* 负号 */}
      {isNegative && <span className="mr-0.5">-</span>}

      {/* 货币符号 */}
      {symbol && <span className="mr-1 font-normal opacity-80">{symbol}</span>}

      {/* 金额数值 */}
      <span className="tabular-nums">{displayAmount}</span>
    </span>
  )
}

export default AmountDisplay
