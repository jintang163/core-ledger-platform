<template>
  <div class="dashboard-container">
    <div class="stats-row">
      <a-row :gutter="24">
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #1890ff">
              <user-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">账户总数</div>
              <div class="stat-value">12,847</div>
              <div class="stat-desc">较昨日 +128</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #52c41a">
              <swap-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">今日交易笔数</div>
              <div class="stat-value">28,456</div>
              <div class="stat-desc">较昨日 +1,234</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #faad14">
              <dollar-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">今日交易金额</div>
              <div class="stat-value">¥ 156.8M</div>
              <div class="stat-desc">较昨日 +12.5%</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f5222d">
              <fire-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">热点账户数</div>
              <div class="stat-value">24</div>
              <div class="stat-desc">运行正常</div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <a-row :gutter="24" style="margin-top: 24px">
      <a-col :span="16">
        <a-card title="交易趋势">
          <v-chart class="chart" :option="chartOption" autoresize />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="最近交易">
          <a-list
            :data-source="recentTransactions"
            :render-item="(item) => (
              <a-list-item>
                <a-list-item-meta
                  :title="item.title"
                  :description="item.time"
                />
                <template #extra>
                  <span :class="item.type === 'income' ? 'income' : 'expense'">
                    {{ item.type === 'income' ? '+' : '-' }}{{ item.amount }}
                  </span>
                </template>
              </a-list-item>
            )"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="24" style="margin-top: 24px">
      <a-col :span="12">
        <a-card title="待办事项">
          <a-table
            :columns="todoColumns"
            :data-source="todoList"
            :pagination="false"
            size="small"
          />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="热点账户状态">
          <a-table
            :columns="hotAccountColumns"
            :data-source="hotAccountList"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'normal' ? 'green' : 'orange'">
                  {{ record.status === 'normal' ? '正常' : '高负载' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  UserOutlined,
  SwapOutlined,
  DollarOutlined,
  FireOutlined
} from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const recentTransactions = ref([
  { title: '转账交易 - TXN001', time: '2分钟前', amount: '¥1,200.00', type: 'expense' },
  { title: '存款 - TXN002', time: '5分钟前', amount: '¥5,000.00', type: 'income' },
  { title: '手续费 - TXN003', time: '10分钟前', amount: '¥5.00', type: 'expense' },
  { title: '利息计提 - TXN004', time: '30分钟前', amount: '¥128.50', type: 'income' },
  { title: '调账 - TXN005', time: '1小时前', amount: '¥3,500.00', type: 'expense' }
])

const todoColumns = [
  { title: '事项', dataIndex: 'title', key: 'title' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '状态', dataIndex: 'status', key: 'status' }
]

const todoList = ref([
  { title: '调账申请审批', type: '调账', priority: '高', status: '待处理' },
  { title: '热点账户配置审核', type: '配置', priority: '中', status: '待处理' },
  { title: '异常交易核查', type: '风控', priority: '高', status: '处理中' },
  { title: '日终对账确认', type: '对账', priority: '中', status: '待处理' }
])

const hotAccountColumns = [
  { title: '账户ID', dataIndex: 'accountId', key: 'accountId' },
  { title: '分片数', dataIndex: 'shardCount', key: 'shardCount' },
  { title: '今日交易量', dataIndex: 'transactionCount', key: 'transactionCount' },
  { title: '状态', key: 'status' }
]

const hotAccountList = ref([
  { accountId: 'HOT001', shardCount: 10, transactionCount: '12,456', status: 'normal' },
  { accountId: 'HOT002', shardCount: 20, transactionCount: '28,901', status: 'high' },
  { accountId: 'HOT003', shardCount: 15, transactionCount: '8,234', status: 'normal' }
])

const chartOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: ['交易笔数', '交易金额']
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
  },
  yAxis: [
    {
      type: 'value',
      name: '交易笔数'
    },
    {
      type: 'value',
      name: '交易金额(万)'
    }
  ],
  series: [
    {
      name: '交易笔数',
      type: 'line',
      smooth: true,
      data: [120, 132, 101, 134, 90, 230, 210]
    },
    {
      name: '交易金额',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [220, 182, 191, 234, 290, 330, 310]
    }
  ]
})
</script>

<style scoped lang="less">
.dashboard-container {
  padding: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    margin-right: 20px;
  }
  
  .stat-content {
    flex: 1;
    
    .stat-title {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.45);
      margin-bottom: 8px;
    }
    
    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.85);
      margin-bottom: 4px;
    }
    
    .stat-desc {
      font-size: 12px;
      color: #52c41a;
    }
  }
}

.chart {
  height: 300px;
}

.income {
  color: #52c41a;
  font-weight: 500;
}

.expense {
  color: #f5222d;
  font-weight: 500;
}
</style>
