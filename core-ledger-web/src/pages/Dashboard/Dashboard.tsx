import { Card, Button, Tag } from 'antd'
import {
  PlusOutlined,
  MinusOutlined,
  SwapOutlined,
  ArrowRightOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import StatsCard from '../../components/StatsCard/StatsCard'
import BalanceTrendChart from '../../components/Charts/BalanceTrendChart'
import {
  statCardsData,
  balanceTrendData,
  recentTransactions,
  transactionTypeMap,
  transactionStatusMap,
} from './mockData'
import { formatThousands, formatRelativeTimeToNow } from '../../utils/format'
import classNames from 'classnames'

// 仪表盘页面组件
const Dashboard = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-6 md:p-8">
      {/* 页面标题 */}
      <div className="mb-8">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-800 mb-2">
          仪表盘
        </h1>
        <p className="text-gray-500">
          欢迎回来，查看您的账务核心系统概览
        </p>
      </div>

      {/* 统计卡片区域 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {statCardsData.map((item, index) => (
          <StatsCard
            key={index}
            icon={<item.icon />}
            iconGradient={item.iconGradient}
            title={item.title}
            value={item.value}
            unit={item.unit}
            prefix={item.prefix}
            changeRate={item.changeRate}
          />
        ))}
      </div>

      {/* 图表和快捷操作区域 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        {/* 余额趋势图 */}
        <Card
          className="lg:col-span-2 border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
          title={
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-gray-800">余额趋势</h3>
                <p className="text-sm text-gray-500 mt-0.5">近30天余额变化</p>
              </div>
              <div className="flex items-center gap-2">
                <Tag color="blue" className="border-0">
                  <span className="text-sm">实时更新</span>
                </Tag>
              </div>
            </div>
          }
          styles={{
            body: {
              padding: '24px',
            },
          }}
        >
          <BalanceTrendChart data={balanceTrendData} height={320} />
        </Card>

        {/* 快捷操作区 */}
        <Card
          className="border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
          title={
            <div>
              <h3 className="text-lg font-semibold text-gray-800">快捷操作</h3>
              <p className="text-sm text-gray-500 mt-0.5">快速创建业务</p>
            </div>
          }
          styles={{
            body: {
              padding: '24px',
            },
          }}
        >
          <div className="space-y-4">
            {/* 创建账户按钮 */}
            <button
              className="w-full p-6 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 text-white text-left hover:from-blue-600 hover:to-indigo-700 transition-all duration-300 hover:shadow-lg hover:-translate-y-1 group"
            >
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center flex-shrink-0">
                  <PlusOutlined className="text-xl" />
                </div>
                <div className="flex-1">
                  <h4 className="text-lg font-semibold mb-1">创建账户</h4>
                  <p className="text-sm text-white/80 mb-3">
                    快速开立新的账户
                  </p>
                  <span className="inline-flex items-center gap-1 text-sm font-medium opacity-80 group-hover:opacity-100 transition-opacity">
                    立即创建
                    <ArrowRightOutlined className="text-xs group-hover:translate-x-1 transition-transform" />
                  </span>
                </div>
              </div>
            </button>

            {/* 复式记账按钮 */}
            <button
              className="w-full p-6 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 text-white text-left hover:from-emerald-600 hover:to-teal-700 transition-all duration-300 hover:shadow-lg hover:-translate-y-1 group"
            >
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center flex-shrink-0">
                  <SwapOutlined className="text-xl" />
                </div>
                <div className="flex-1">
                  <h4 className="text-lg font-semibold mb-1">复式记账</h4>
                  <p className="text-sm text-white/80 mb-3">
                    记录借贷交易
                  </p>
                  <span className="inline-flex items-center gap-1 text-sm font-medium opacity-80 group-hover:opacity-100 transition-opacity">
                    立即记账
                    <ArrowRightOutlined className="text-xs group-hover:translate-x-1 transition-transform" />
                  </span>
                </div>
              </div>
            </button>
          </div>
        </Card>
      </div>

      {/* 最近交易列表 */}
      <Card
        className="border-0 rounded-2xl shadow-sm hover:shadow-lg transition-all duration-300"
        title={
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-gray-800">最近交易</h3>
              <p className="text-sm text-gray-500 mt-0.5">最新的5条交易记录</p>
            </div>
            <Button type="link" className="text-blue-600 font-medium">
              查看全部 <ArrowRightOutlined />
            </Button>
          </div>
        }
        styles={{
          body: {
            padding: 0,
          },
        }}
      >
        <div className="divide-y divide-gray-100">
          {recentTransactions.map((transaction) => {
            const typeInfo = transactionTypeMap[transaction.type]
            const statusInfo = transactionStatusMap[transaction.status]
            const isPositive = transaction.amount >= 0

            return (
              <div
                key={transaction.id}
                className="px-6 py-4 hover:bg-gray-50 transition-colors duration-200 cursor-pointer group"
              >
                <div className="flex items-center gap-4">
                  {/* 交易类型图标 */}
                  <div
                    className={classNames(
                      'w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0',
                      typeInfo.bgColor
                    )}
                  >
                    {transaction.type === 'income' && (
                      <PlusOutlined className={classNames('text-base', typeInfo.color)} />
                    )}
                    {transaction.type === 'expense' && (
                      <MinusOutlined className={classNames('text-base', typeInfo.color)} />
                    )}
                    {transaction.type === 'transfer' && (
                      <SwapOutlined className={classNames('text-base', typeInfo.color)} />
                    )}
                  </div>

                  {/* 交易信息 */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-medium text-gray-800 truncate">
                        {transaction.description}
                      </span>
                      <span
                        className={classNames(
                          'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium',
                          statusInfo.color,
                          statusInfo.color.replace('text-', 'bg-').replace('600', '50')
                        )}
                      >
                        <span
                          className={classNames(
                            'w-1.5 h-1.5 rounded-full',
                            statusInfo.dotColor
                          )}
                        ></span>
                        {statusInfo.label}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-sm text-gray-500">
                      <span className="truncate">{transaction.account}</span>
                      <span className="flex items-center gap-1 flex-shrink-0">
                        <ClockCircleOutlined className="text-xs" />
                        {formatRelativeTimeToNow(transaction.time)}
                      </span>
                    </div>
                  </div>

                  {/* 金额 */}
                  <div className="text-right flex-shrink-0">
                    <div
                      className={classNames(
                        'text-lg font-bold font-mono',
                        isPositive ? 'text-green-600' : 'text-red-600'
                      )}
                    >
                      {isPositive ? '+' : ''}
                      {formatThousands(transaction.amount)}
                    </div>
                    <div className="text-xs text-gray-400 mt-0.5">
                      {transaction.id}
                    </div>
                  </div>

                  {/* 箭头 */}
                  <ArrowRightOutlined className="text-gray-300 opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all duration-200 flex-shrink-0" />
                </div>
              </div>
            )
          })}
        </div>
      </Card>
    </div>
  )
}

export default Dashboard
