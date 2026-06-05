import { Progress, Tooltip } from 'antd'
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons'
import classNames from 'classnames'
import { formatThousands } from '@/utils/format'
import { getCurrencySymbol } from '@/utils/amount'

// 借贷平衡指示器组件属性
interface BalanceIndicatorProps {
  // 借方总额（单位：分）
  debitTotal: number
  // 贷方总额（单位：分）
  creditTotal: number
  // 货币类型，默认 CNY
  currency?: string
  // 自定义类名
  className?: string
}

// 借贷平衡指示器组件
function BalanceIndicator({
  debitTotal,
  creditTotal,
  currency = 'CNY',
  className,
}: BalanceIndicatorProps) {
  // 计算差额
  const difference = debitTotal - creditTotal
  const isBalanced = difference === 0
  const hasAmount = debitTotal > 0 || creditTotal > 0

  // 获取货币符号
  const symbol = getCurrencySymbol(currency)

  // 格式化金额（分转元）
  const formatAmount = (amount: number): string => {
    return formatThousands((amount / 100).toFixed(2))
  }

  // 计算进度条百分比
  const getProgressPercent = (): number => {
    if (!hasAmount) return 0
    const maxAmount = Math.max(debitTotal, creditTotal)
    if (maxAmount === 0) return 0
    return Math.round((Math.min(debitTotal, creditTotal) / maxAmount) * 100)
  }

  return (
    <div
      className={classNames(
        'p-6 rounded-2xl border transition-all duration-500',
        isBalanced && hasAmount
          ? 'bg-success-50 border-success-200 shadow-success-100'
          : hasAmount
          ? 'bg-danger-50 border-danger-200 shadow-danger-100'
          : 'bg-gray-50 border-gray-200',
        className
      )}
    >
      {/* 标题和状态图标 */}
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-gray-800">借贷平衡校验</h3>
        <Tooltip title={isBalanced ? '借贷平衡' : `借贷不平衡，差额：${symbol}${formatAmount(Math.abs(difference))}`}>
          <div className="flex items-center gap-2">
            {hasAmount ? (
              isBalanced ? (
                <div className="flex items-center gap-2 animate-bounce">
                  <CheckCircleFilled className="text-2xl text-success-500" />
                  <span className="text-success-600 font-semibold">平衡</span>
                </div>
              ) : (
                <div className="flex items-center gap-2 animate-pulse">
                  <CloseCircleFilled className="text-2xl text-danger-500" />
                  <span className="text-danger-600 font-semibold">不平衡</span>
                </div>
              )
            ) : (
              <span className="text-gray-400">请输入金额</span>
            )}
          </div>
        </Tooltip>
      </div>

      {/* 金额显示区域 */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {/* 借方金额 */}
        <div className="text-center p-4 bg-white rounded-xl shadow-sm">
          <div className="text-sm text-gray-500 mb-2">借方总额</div>
          <div
            className={classNames(
              'text-2xl font-bold font-mono tabular-nums transition-all duration-300',
              debitTotal > 0 ? 'text-blue-600' : 'text-gray-400'
            )}
          >
            {symbol}
            {formatAmount(debitTotal)}
          </div>
        </div>

        {/* 差额 */}
        <div className="text-center p-4 bg-white rounded-xl shadow-sm">
          <div className="text-sm text-gray-500 mb-2">差额</div>
          <div
            className={classNames(
              'text-2xl font-bold font-mono tabular-nums transition-all duration-300',
              isBalanced ? 'text-success-600' : 'text-danger-600'
            )}
          >
            {symbol}
            {formatAmount(Math.abs(difference))}
          </div>
        </div>

        {/* 贷方金额 */}
        <div className="text-center p-4 bg-white rounded-xl shadow-sm">
          <div className="text-sm text-gray-500 mb-2">贷方总额</div>
          <div
            className={classNames(
              'text-2xl font-bold font-mono tabular-nums transition-all duration-300',
              creditTotal > 0 ? 'text-red-600' : 'text-gray-400'
            )}
          >
            {symbol}
            {formatAmount(creditTotal)}
          </div>
        </div>
      </div>

      {/* 平衡进度条 */}
      <div className="relative">
        {/* 双进度条可视化 */}
        <div className="h-4 bg-gray-200 rounded-full overflow-hidden mb-3">
          <div className="h-full flex transition-all duration-500">
            {/* 借方进度 */}
            <div
              className="h-full bg-gradient-to-r from-blue-400 to-blue-600 transition-all duration-500"
              style={{
                width: hasAmount ? `${(debitTotal / Math.max(debitTotal, creditTotal, 1)) * 50}%` : '0%',
              }}
            />
            {/* 贷方进度 */}
            <div
              className="h-full bg-gradient-to-r from-red-600 to-red-400 transition-all duration-500"
              style={{
                width: hasAmount ? `${(creditTotal / Math.max(debitTotal, creditTotal, 1)) * 50}%` : '0%',
              }}
            />
          </div>
        </div>

        {/* 整体进度指示 */}
        <Progress
          percent={getProgressPercent()}
          showInfo={false}
          strokeColor={isBalanced ? '#52c41a' : '#ff4d4f'}
          trailColor="#e5e7eb"
          size="small"
        />

        {/* 进度条标签 */}
        <div className="flex justify-between text-xs text-gray-500 mt-2">
          <span className="flex items-center gap-1">
            <span className="w-3 h-3 rounded-full bg-blue-500" />
            借方 {hasAmount ? `${Math.round((debitTotal / Math.max(debitTotal, creditTotal, 1)) * 100)}%` : '0%'}
          </span>
          <span className="flex items-center gap-1">
            贷方 {hasAmount ? `${Math.round((creditTotal / Math.max(debitTotal, creditTotal, 1)) * 100)}%` : '0%'}
            <span className="w-3 h-3 rounded-full bg-red-500" />
          </span>
        </div>
      </div>

      {/* 平衡提示 */}
      {hasAmount && !isBalanced && (
        <div
          className={classNames(
            'mt-4 p-3 rounded-lg text-sm font-medium animate-fade-in',
            difference > 0
              ? 'bg-blue-100 text-blue-700 border border-blue-200'
              : 'bg-red-100 text-red-700 border border-red-200'
          )}
        >
          {difference > 0
            ? `提示：借方比贷方多 ${symbol}${formatAmount(difference)}，请调整贷方金额`
            : `提示：贷方比借方多 ${symbol}${formatAmount(Math.abs(difference))}，请调整借方金额`}
        </div>
      )}

      {isBalanced && hasAmount && (
        <div className="mt-4 p-3 rounded-lg bg-success-100 text-success-700 border border-success-200 text-sm font-medium animate-fade-in">
          ✓ 借贷平衡，金额合计：{symbol}
          {formatAmount(debitTotal)}
        </div>
      )}
    </div>
  )
}

export default BalanceIndicator
