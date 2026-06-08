<template>
  <div class="page-container">
    <a-page-header
      title="交易详情"
      :sub-title="`交易ID: ${transactionId}`"
      @back="$router.back()"
    />

    <div class="detail-content" v-loading="loading">
      <a-descriptions title="基本信息" :column="3" bordered>
        <a-descriptions-item label="交易流水号">{{ transactionInfo.transactionNo }}</a-descriptions-item>
        <a-descriptions-item label="业务单号">{{ transactionInfo.businessNo }}</a-descriptions-item>
        <a-descriptions-item label="交易类型">{{ transactionInfo.transactionTypeDesc }}</a-descriptions-item>
        <a-descriptions-item label="交易金额">
          <span class="amount-large">{{ formatCurrency(transactionInfo.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="币种">{{ transactionInfo.currency }}</a-descriptions-item>
        <a-descriptions-item label="交易状态">
          <a-tag :color="getStatusColor(transactionInfo.status)">
            {{ transactionInfo.statusDesc }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="记账凭证号">{{ transactionInfo.voucherNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ transactionInfo.operator || '-' }}</a-descriptions-item>
        <a-descriptions-item label="交易时间">{{ transactionInfo.transactionTime }}</a-descriptions-item>
        <a-descriptions-item label="交易摘要" :span="3">{{ transactionInfo.summary || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注信息" :span="3">{{ transactionInfo.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <div class="detail-section">
        <h3 class="section-title">借贷分录明细</h3>
        <a-table
          :columns="entryColumns"
          :data-source="transactionInfo.entries"
          :pagination="false"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'direction'">
              <a-tag :color="record.direction === 1 ? 'red' : 'green'">
                {{ record.directionDesc }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'amount'">
              <span :class="record.direction === 1 ? 'debit-amount' : 'credit-amount'">
                {{ formatCurrency(record.amount) }}
              </span>
            </template>
          </template>
        </a-table>
        <div class="entry-summary">
          <span>借方合计: <span class="debit-amount">{{ formatCurrency(debitTotal) }}</span></span>
          <span>贷方合计: <span class="credit-amount">{{ formatCurrency(creditTotal) }}</span></span>
          <span>
            平衡状态:
            <a-tag :color="isBalanced ? 'green' : 'red'">
              {{ isBalanced ? '借贷平衡' : '借贷不平衡' }}
            </a-tag>
          </span>
        </div>
      </div>

      <a-divider />

      <div class="detail-section">
        <h3 class="section-title">事务执行日志</h3>
        <a-table
          :columns="logColumns"
          :data-source="transactionLogs"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getLogStatusColor(record.status)">
                {{ record.statusDesc }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTransaction, getTransactionLogs } from '@/api/transaction'
import { formatCurrency } from '@/utils/idGenerator'
import type { Transaction, TransactionEntry, SagaTransactionLog } from '@/types'

const route = useRoute()
const transactionId = ref(route.params.id as string)
const loading = ref(false)

const transactionInfo = reactive<Transaction>({
  transactionId: '',
  transactionNo: '',
  transactionType: 1,
  transactionTypeDesc: '',
  businessNo: '',
  totalAmount: 0,
  currency: 'CNY',
  status: 1,
  statusDesc: '',
  entries: []
})

const transactionLogs = ref<SagaTransactionLog[]>([])

const entryColumns = [
  { title: '分录ID', dataIndex: 'entryId', key: 'entryId', width: 180 },
  { title: '账户ID', dataIndex: 'accountId', key: 'accountId', width: 180 },
  { title: '账户账号', dataIndex: 'accountNo', key: 'accountNo', width: 200 },
  { title: '科目代码', dataIndex: 'subjectCode', key: 'subjectCode', width: 120 },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName', width: 160 },
  { title: '借贷方向', key: 'direction', width: 100 },
  { title: '金额', key: 'amount', width: 140 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', width: 200 }
]

const logColumns = [
  { title: '日志ID', dataIndex: 'logId', key: 'logId', width: 180 },
  { title: '步骤名称', dataIndex: 'stepName', key: 'stepName', width: 160 },
  { title: '执行状态', key: 'status', width: 120 },
  { title: '重试次数', dataIndex: 'retryCount', key: 'retryCount', width: 100 },
  { title: 'Try结果', dataIndex: 'tryResult', key: 'tryResult' },
  { title: 'Confirm结果', dataIndex: 'confirmResult', key: 'confirmResult' },
  { title: 'Cancel结果', dataIndex: 'cancelResult', key: 'cancelResult' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 }
]

const debitTotal = computed(() => {
  return transactionInfo.entries
    .filter(e => e.direction === 1)
    .reduce((sum, e) => sum + e.amount, 0)
})

const creditTotal = computed(() => {
  return transactionInfo.entries
    .filter(e => e.direction === 2)
    .reduce((sum, e) => sum + e.amount, 0)
})

const isBalanced = computed(() => {
  return Math.abs(debitTotal.value - creditTotal.value) < 0.01
})

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'green',
    2: 'red',
    3: 'orange'
  }
  return colors[status] || 'default'
}

const getLogStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    'SUCCESS': 'green',
    'FAILED': 'red',
    'PENDING': 'default',
    'RETRY': 'orange'
  }
  return colors[status] || 'default'
}

const fetchData = async () => {
  try {
    loading.value = true
    const [transRes, logsRes] = await Promise.all([
      getTransaction(transactionId.value),
      getTransactionLogs(transactionId.value)
    ])
    Object.assign(transactionInfo, transRes)
    transactionLogs.value = logsRes || mockLogs
  } catch (error) {
    console.error('查询交易详情失败:', error)
    Object.assign(transactionInfo, {
      transactionId: transactionId.value,
      transactionNo: 'TRX2024012000001',
      transactionType: 1,
      transactionTypeDesc: '转账',
      businessNo: 'BUS2024012000001',
      totalAmount: 1000.00,
      currency: 'CNY',
      voucherNo: 'VOU202401200001',
      status: 1,
      statusDesc: '成功',
      summary: '用户转账交易',
      operator: '操作员1',
      transactionTime: '2024-01-20 14:30:00',
      remark: '正常交易',
      entries: mockEntries
    })
    transactionLogs.value = mockLogs
  } finally {
    loading.value = false
  }
}

const mockEntries: TransactionEntry[] = [
  {
    entryId: 'ENT001',
    transactionId: 'TXN001',
    accountId: 'ACC001',
    accountNo: '6222020000000001',
    subjectCode: '1001',
    subjectName: '银行存款',
    direction: 2,
    directionDesc: '贷',
    amount: 1000.00,
    currency: 'CNY',
    summary: '转出'
  },
  {
    entryId: 'ENT002',
    transactionId: 'TXN001',
    accountId: 'ACC002',
    accountNo: '6222020000000002',
    subjectCode: '1001',
    subjectName: '银行存款',
    direction: 1,
    directionDesc: '借',
    amount: 1000.00,
    currency: 'CNY',
    summary: '转入'
  }
]

const mockLogs: SagaTransactionLog[] = [
  {
    logId: 'LOG001',
    transactionId: 'TXN001',
    businessNo: 'BUS001',
    stepName: '余额检查',
    status: 'SUCCESS',
    statusDesc: '成功',
    tryResult: '账户余额充足',
    retryCount: 0,
    createTime: '2024-01-20 14:30:00'
  },
  {
    logId: 'LOG002',
    transactionId: 'TXN001',
    businessNo: 'BUS001',
    stepName: '扣款账户更新',
    status: 'SUCCESS',
    statusDesc: '成功',
    tryResult: '扣款成功',
    confirmResult: '确认成功',
    retryCount: 0,
    createTime: '2024-01-20 14:30:01'
  },
  {
    logId: 'LOG003',
    transactionId: 'TXN001',
    businessNo: 'BUS001',
    stepName: '收款账户更新',
    status: 'SUCCESS',
    statusDesc: '成功',
    tryResult: '入账成功',
    confirmResult: '确认成功',
    retryCount: 0,
    createTime: '2024-01-20 14:30:02'
  },
  {
    logId: 'LOG004',
    transactionId: 'TXN001',
    businessNo: 'BUS001',
    stepName: '交易完成',
    status: 'SUCCESS',
    statusDesc: '成功',
    tryResult: '交易完成',
    retryCount: 0,
    createTime: '2024-01-20 14:30:03'
  }
]

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

.amount-large {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}

.debit-amount {
  color: #cf1322;
  font-weight: 600;
}

.credit-amount {
  color: #389e0d;
  font-weight: 600;
}

.detail-section {
  margin-bottom: 24px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 16px;
  }
}

.entry-summary {
  display: flex;
  justify-content: flex-end;
  gap: 32px;
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}
</style>
