<template>
  <div class="page-container">
    <a-page-header
      title="账户详情"
      :sub-title="`账户ID: ${accountId}`"
      @back="$router.back()"
    />

    <div class="detail-content" v-loading="loading">
      <a-descriptions title="基本信息" :column="3" bordered>
        <a-descriptions-item label="账户ID">{{ accountInfo.accountId }}</a-descriptions-item>
        <a-descriptions-item label="账户账号">{{ accountInfo.accountNo }}</a-descriptions-item>
        <a-descriptions-item label="用户ID">{{ accountInfo.userId }}</a-descriptions-item>
        <a-descriptions-item label="账户类型">{{ accountInfo.accountTypeDesc }}</a-descriptions-item>
        <a-descriptions-item label="币种">{{ accountInfo.currencyDesc }}</a-descriptions-item>
        <a-descriptions-item label="账户状态">
          <a-tag :color="getStatusColor(accountInfo.status)">
            {{ accountInfo.statusDesc }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="账户余额" :span="3">
          <span class="balance-large">{{ formatCurrency(accountInfo.balance) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="热点标记">
          <a-tag v-if="accountInfo.isHotAccount" color="orange">
            <fire-outlined /> 热点账户
          </a-tag>
          <span v-else>否</span>
        </a-descriptions-item>
        <a-descriptions-item label="开户时间">{{ accountInfo.openTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ accountInfo.updateTime }}</a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-row :gutter="24">
        <a-col :span="12">
          <a-card title="最近交易">
            <a-table
              :columns="transactionColumns"
              :data-source="recentTransactions"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">
                  <span :class="record.type === 'income' ? 'income' : 'expense'">
                    {{ record.type === 'income' ? '+' : '-' }}{{ formatCurrency(record.amount) }}
                  </span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" @click="viewTransactionDetail(record)">
                    查看
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="操作记录">
            <a-timeline>
              <a-timeline-item color="green">
                <template #dot><check-circle-outlined /></template>
                <p>账户开户</p>
                <p class="time">{{ accountInfo.openTime }}</p>
              </a-timeline-item>
              <a-timeline-item v-if="accountInfo.isHotAccount" color="orange">
                <template #dot><fire-outlined /></template>
                <p>标记为热点账户</p>
                <p class="time">2024-01-15 10:30:00</p>
              </a-timeline-item>
              <a-timeline-item v-if="accountInfo.status === 2" color="red">
                <template #dot><exclamation-circle-outlined /></template>
                <p>账户冻结 - {{ accountInfo.freezeTypeDesc }}</p>
                <p class="time">{{ accountInfo.freezeTime }}</p>
                <p class="remark">操作人: {{ accountInfo.freezeOperator }}</p>
                <p class="remark">备注: {{ accountInfo.freezeRemark }}</p>
              </a-timeline-item>
            </a-timeline>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  FireOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import { getAccount } from '@/api/account'
import { formatCurrency } from '@/utils/idGenerator'
import type { Account } from '@/types'

const route = useRoute()
const router = useRouter()
const accountId = ref(route.params.id as string)
const loading = ref(false)

const accountInfo = reactive<Account>({
  accountId: '',
  accountNo: '',
  userId: '',
  accountType: 1,
  accountTypeDesc: '',
  currency: 'CNY',
  currencyDesc: '人民币',
  balance: 0,
  status: 1,
  statusDesc: ''
})

const transactionColumns = [
  { title: '交易时间', dataIndex: 'time', key: 'time', width: 160 },
  { title: '交易类型', dataIndex: 'typeDesc', key: 'typeDesc' },
  { title: '金额', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 80 }
]

const recentTransactions = ref([
  { id: 'TXN001', time: '2024-01-20 14:30:00', typeDesc: '转账', type: 'expense', amount: 1000, status: '成功' },
  { id: 'TXN002', time: '2024-01-20 10:15:00', typeDesc: '存款', type: 'income', amount: 5000, status: '成功' },
  { id: 'TXN003', time: '2024-01-19 16:45:00', typeDesc: '手续费', type: 'expense', amount: 5, status: '成功' },
  { id: 'TXN004', time: '2024-01-19 09:00:00', typeDesc: '利息', type: 'income', amount: 128.5, status: '成功' },
  { id: 'TXN005', time: '2024-01-18 15:20:00', typeDesc: '转账', type: 'expense', amount: 3500, status: '成功' }
])

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    1: 'green',
    2: 'red',
    3: 'default'
  }
  return colors[status] || 'default'
}

const viewTransactionDetail = (record: any) => {
  router.push(`/transactions/${record.id}`)
}

const fetchData = async () => {
  try {
    loading.value = true
    const res = await getAccount(accountId.value)
    Object.assign(accountInfo, res)
  } catch (error) {
    console.error('查询账户详情失败:', error)
    Object.assign(accountInfo, {
      accountId: accountId.value,
      accountNo: '6222020000000001',
      userId: 'USER000001',
      accountType: 1,
      accountTypeDesc: '个人账户',
      currency: 'CNY',
      currencyDesc: '人民币',
      balance: 125680.50,
      status: 1,
      statusDesc: '正常',
      isHotAccount: true,
      openTime: '2023-01-01 00:00:00',
      updateTime: '2024-01-20 14:30:00'
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.detail-content {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  margin-top: 16px;
}

.balance-large {
  font-size: 24px;
  font-weight: 600;
  color: #1890ff;
}

.income {
  color: #52c41a;
  font-weight: 500;
}

.expense {
  color: #f5222d;
  font-weight: 500;
}

.time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0;
}

.remark {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  margin: 4px 0 0 0;
}
</style>
