import { useMemo } from 'react'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import { formatThousands } from '../../utils/format'

// 余额趋势数据接口
interface BalanceData {
  // 日期/时间
  date: string
  // 余额
  balance: number
}

// 余额趋势图表组件属性
interface BalanceTrendChartProps {
  // 数据
  data: BalanceData[]
  // 高度
  height?: number
  // 主色调
  primaryColor?: string
  // 次要颜色
  secondaryColor?: string
}

// 自定义Tooltip内容
const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white rounded-lg shadow-lg border border-gray-100 p-3">
        <p className="text-sm text-gray-500 mb-1">{label}</p>
        <p className="text-base font-semibold text-blue-600">
          <span className="text-gray-600 font-normal">余额：</span>
          ¥{formatThousands(payload[0].value)}
        </p>
      </div>
    )
  }
  return null
}

// 余额趋势图表组件
const BalanceTrendChart = ({
  data,
  height = 300,
  primaryColor = '#3B82F6',
  secondaryColor = '#8B5CF6',
}: BalanceTrendChartProps) => {
  // 计算渐变ID，避免冲突
  const gradientId = useMemo(() => `balanceGradient_${Math.random().toString(36).substr(2, 9)}`, [])

  // 计算Y轴刻度格式化
  const formatYAxis = (value: number) => {
    if (value >= 10000) {
      return `${(value / 10000).toFixed(0)}万`
    }
    if (value >= 1000) {
      return `${(value / 1000).toFixed(0)}k`
    }
    return value.toString()
  }

  return (
    <div className="w-full">
      <ResponsiveContainer width="100%" height={height}>
        <AreaChart
          data={data}
          margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
        >
          {/* 定义渐变 */}
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="5%"
                stopColor={primaryColor}
                stopOpacity={0.3}
              />
              <stop
                offset="95%"
                stopColor={primaryColor}
                stopOpacity={0}
              />
            </linearGradient>
          </defs>

          {/* 网格线 */}
          <CartesianGrid
            strokeDasharray="3 3"
            stroke="#E5E7EB"
            vertical={false}
          />

          {/* X轴 */}
          <XAxis
            dataKey="date"
            axisLine={false}
            tickLine={false}
            tick={{
              fill: '#6B7280',
              fontSize: 12,
            }}
            dy={10}
          />

          {/* Y轴 */}
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={{
              fill: '#6B7280',
              fontSize: 12,
            }}
            tickFormatter={formatYAxis}
            width={50}
          />

          {/* 自定义Tooltip */}
          <Tooltip
            content={<CustomTooltip />}
            cursor={{
              stroke: primaryColor,
              strokeWidth: 1,
              strokeDasharray: '5 5',
            }}
          />

          {/* 区域填充 */}
          <Area
            type="monotone"
            dataKey="balance"
            stroke={`url(#${gradientId})`}
            strokeWidth={3}
            fill={`url(#${gradientId})`}
            activeDot={{
              r: 6,
              fill: primaryColor,
              stroke: '#fff',
              strokeWidth: 2,
            }}
            dot={{
              r: 3,
              fill: primaryColor,
              strokeWidth: 0,
            }}
          />

          {/* 顶部曲线（覆盖在渐变上方的实线） */}
          <Area
            type="monotone"
            dataKey="balance"
            stroke={primaryColor}
            strokeWidth={3}
            fill="none"
            dot={false}
            activeDot={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}

export default BalanceTrendChart
