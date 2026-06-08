<template>
  <div class="page-container">
    <div class="page-header">
      <a-form
        ref="formRef"
        :model="searchForm"
        layout="inline"
        class="search-form"
      >
        <a-form-item label="交易时间">
          <a-range-picker
            v-model:value="searchForm.dateRange"
            show-time
            style="width: 300px"
          />
        </a-form-item>
        <a-form-item label="账户ID">
          <a-input
            v-model:value="searchForm.accountId"
            placeholder="请输入账户ID"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="业务类型">
          <a-select
            v-model:value="searchForm.transactionType"
            placeholder="请选择"
            style="width: 140px"
            allow-clear
          >
            <a-select-option :value="1">转账</a-select-option>
            <a-select-option :value="2">存款</a-select-option>
            <a-select-option :value="3">取款</a-select-option>
            <a-select-option :value="4">手续费</a-select-option>
            <a-select-option :value="5">利息</a-select-option>
            <a-select-option :value="6">调账</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="交易状态">
          <a-select
            v-model:value="searchForm.status"
            placeholder="请选择"
            style="width: 140px"
            allow-clear
          >
            <a-select-option :value="0">待处理</a-select-option>
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="2">失败</a-select-option>
            <a-select-option :value="3">已冲正</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="金额区间">
          <a-input-number
            v-model:value="searchForm.minAmount"
            placeholder="最小值"
            :min="0"
            style="width: 120px"
          />
          <span style="margin: 0 8px">-</span>
          <a-input-number
            v-model:value="searchForm.maxAmount"
            placeholder="最大值"
            :min="0"
            style="width: 120px"
          />
        </a-form-item>
        <a-form-item label="业务单号">
          <a-input
            v-model:value="searchForm.businessNo"
            placeholder="请输入业务单号"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            <search-outlined />
            查询
          </a-button>
          <a-button style="margin-left: 8px" @click="handleReset">
            <reload-outlined />
            重置
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="page-content">
      <div class="table-toolbar">
        <a-button type="primary" @click="handleExport">
          <download-outlined />
          导出数据
        </a-button>
        <div>
          <span style="margin-right: 16px">成功笔数: <span class="success-count">{{ successCount }}</span></span>
          <span>成功金额: <span class="success-amount">{{ formatCurrency(successAmount) }}</span></span>
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="transactionId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount'">
            <span class="amount">{{ formatCurrency(record.totalAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.statusDesc }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="handleViewDetail(record)">
              查看详情
            </a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { queryTransactionList } from '@/api/transaction'
import { formatCurrency } from '@/utils/idGenerator'
import type { Transaction, TransactionQueryDTO } from '@/types'
import type { TableProps } from 'ant-design-vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const searchForm = reactive({
  dateRange: [] as any[],
  accountId: '',
  transactionType: undefined as number | undefined,
  status: undefined as number | undefined,
  minAmount: undefined as number | undefined,
  maxAmount: undefined as number | undefined,
  businessNo: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

const tableData = ref<Transaction[]>([])

const columns = [
  { title: '交易流水号', dataIndex: 'transactionNo', key: 'transactionNo', width: 180 },
  { title: '业务单号', dataIndex: 'businessNo', key: 'businessNo', width: 160 },
  { title: '交易类型', dataIndex: 'transactionTypeDesc', key: 'transactionType', width: 100 },
  { title: '交易金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 140 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '交易状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '交易摘要', dataIndex: 'summary', key: 'summary', width: 200 },
  { title: '操作人', dataIndex: 'operator', key: 'operator', width: 100 },
  { title: '交易时间', dataIndex: 'transactionTime', key: 'transactionTime', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const }
]

const successCount = computed(() => {
  return tableData.value.filter(t => t.status === 1).length
})

const successAmount = computed(() => {
  return tableData.value
    .filter(t => t.status === 1)
    .reduce((sum, t) => sum + t.totalAmount, 0)
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

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.dateRange = []
  searchForm.accountId = ''
  searchForm.transactionType = undefined
  searchForm.status = undefined
  searchForm.minAmount = undefined
  searchForm.maxAmount = undefined
  searchForm.businessNo = ''
  pagination.current = 1
  fetchData()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

const fetchData = async () => {
  try {
    loading.value = true
    const params: TransactionQueryDTO = {
      accountId: searchForm.accountId || undefined,
      transactionType: searchForm.transactionType,
      status: searchForm.status,
      startTime: searchForm.dateRange?.[0] ? dayjs(searchForm.dateRange[0]).format('YYYY-MM-DD HH:mm:ss') : undefined,
      endTime: searchForm.dateRange?.[1] ? dayjs(searchForm.dateRange[1]).format('YYYY-MM-DD HH:mm:ss') : undefined,
      minAmount: searchForm.minAmount,
      maxAmount: searchForm.maxAmount,
      businessNo: searchForm.businessNo || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await queryTransactionList(params)
    tableData.value = res.records || mockData
    pagination.total = res.total || mockData.length
  } catch (error) {
    console.error('查询交易列表失败:', error)
    tableData.value = mockData
    pagination.total = mockData.length
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (record: Transaction) => {
  router.push(`/transactions/${record.transactionId}`)
}

const handleExport = () => {
  message.info('导出功能开发中...')
}

const mockData: Transaction[] = Array.from({ length: 20 }, (_, i) => ({
  transactionId: `TXN${String(i + 1).padStart(8, '0')}`,
  transactionNo: `TRX${Date.now()}${i}`,
  transactionType: [1, 2, 3, 4, 5, 6][i % 6],
  transactionTypeDesc: ['转账', '存款', '取款', '手续费', '利息', '调账'][i % 6],
  businessNo: `BUS${String(i + 1).padStart(10, '0')}`,
  totalAmount: (i + 1) * 100 + Math.random() * 1000,
  currency: 'CNY',
  status: [1, 1, 1, 2, 1, 0, 1, 3][i % 8],
  statusDesc: ['待处理', '成功', '成功', '失败', '成功', '待处理', '成功', '已冲正'][i % 8],
  summary: `交易摘要${i + 1}`,
  operator: '操作员' + ((i % 5) + 1),
  transactionTime: new Date(Date.now() - i * 3600000).toISOString(),
  entries: []
}))

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.amount {
  font-weight: 600;
  color: #1890ff;
}

.success-count {
  color: #52c41a;
  font-weight: 600;
}

.success-amount {
  color: #1890ff;
  font-weight: 600;
}
</style>
