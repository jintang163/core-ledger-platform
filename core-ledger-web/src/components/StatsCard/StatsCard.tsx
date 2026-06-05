import { ReactNode } from 'react'
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import { formatThousands } from '../../utils/format'
import classNames from 'classnames'

// 统计卡片组件属性接口
interface StatsCardProps {
  // 图标
  icon: ReactNode
  // 图标渐变颜色（起始色, 结束色）
  iconGradient: [string, string]
  // 标题
  title: string
  // 数值
  value: number | string
  // 单位（可选）
  unit?: string
  // 同比变化率（百分比，如 0.125 表示 12.5%）
  changeRate?: number
  // 前缀符号（如 ¥）
  prefix?: string
  // 自定义类名
  className?: string
}

// 统计卡片组件
const StatsCard = ({
  icon,
  iconGradient,
  title,
  value,
  unit,
  changeRate,
  prefix,
  className,
}: StatsCardProps) => {
  // 判断变化方向
  const isPositive = changeRate !== undefined && changeRate >= 0

  // 格式化数值
  const formattedValue = typeof value === 'number' ? formatThousands(value) : value

  return (
    <div
      className={classNames(
        'bg-gradient-to-br from-white to-gray-50 rounded-2xl p-6 border border-gray-100',
        'shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 ease-out',
        'cursor-default',
        className
      )}
    >
      <div className="flex items-center gap-4">
        {/* 左侧图标 */}
        <div
          className="w-14 h-14 rounded-xl flex items-center justify-center text-white text-2xl shadow-lg"
          style={{
            background: `linear-gradient(135deg, ${iconGradient[0]}, ${iconGradient[1]})`,
          }}
        >
          {icon}
        </div>

        {/* 右侧内容 */}
        <div className="flex-1 min-w-0">
          {/* 标题 */}
          <p className="text-sm text-gray-500 mb-1 truncate">{title}</p>

          {/* 数值 */}
          <div className="flex items-baseline gap-1">
            {prefix && <span className="text-lg text-gray-600 font-medium">{prefix}</span>}
            <span className="text-2xl font-bold text-gray-800 font-mono">{formattedValue}</span>
            {unit && <span className="text-sm text-gray-500 ml-1">{unit}</span>}
          </div>

          {/* 变化率 */}
          {changeRate !== undefined && (
            <div className="flex items-center gap-1 mt-2">
              <span
                className={classNames(
                  'inline-flex items-center gap-0.5 px-2 py-0.5 rounded-full text-xs font-medium',
                  isPositive
                    ? 'bg-green-50 text-green-600'
                    : 'bg-red-50 text-red-600'
                )}
              >
                {isPositive ? (
                  <ArrowUpOutlined className="text-xs" />
                ) : (
                  <ArrowDownOutlined className="text-xs" />
                )}
                {Math.abs(changeRate * 100).toFixed(1)}%
              </span>
              <span className="text-xs text-gray-400 ml-1">同比</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default StatsCard
