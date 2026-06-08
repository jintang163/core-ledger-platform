<template>
  <div class="page-container">
    <div class="page-header">
      <a-form
        ref="formRef"
        :model="searchForm"
        layout="inline"
        class="search-form"
      >
        <a-form-item label="用户ID">
          <a-input
            v-model:value="searchForm.userId"
            placeholder="请输入用户ID"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="账户ID">
          <a-input
            v-model:value="searchForm.accountId"
            placeholder="请输入账户ID"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="账户类型">
          <a-select
            v-model:value="searchForm.accountType"
            placeholder="请选择"
            style="width: 140px"
            allow-clear
          >
            <a-select-option :value="1">个人账户</a-select-option>
            <a-select-option :value="2">企业账户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="账户状态">
          <a-select
            v-model:value="searchForm.status"
            placeholder="请选择"
            style="width: 140px"
            allow-clear
          >
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="2">冻结</a-select-option>
            <a-select-option :value="3">已销户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="余额区间">
          <a-input-number
            v-model:value="searchForm.minBalance"
            placeholder="最小值"
            :min="0"
            style="width: 120px"
          />
          <span style="margin: 0 8px">-</span>
          <a-input-number
            v-model:value="searchForm.maxBalance"
            placeholder="最大值"
            :min="0"
            style="width: 120px"
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
        <span>共 {{ pagination.total }} 条记录</span>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="accountId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'balance'">
            <span class="balance">{{ formatCurrency(record.balance) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.statusDesc }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'isHotAccount'">
            <a-tag v-if="record.isHotAccount" color="orange">
              <fire-outlined /> 热点账户
            </a-tag>
            <span v-else class="text-gray">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="handleViewDetail(record)">
              查看详情
            </a-button>
            <a-button type="link" v-if="!record.isHotAccount" @click="handleMarkHot(record)">
              标记热点
            </a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined,
  FireOutlined
} from '@ant-design/icons-vue'
import { getAccountList, markAsHotAccount } from '@/api/account'
import { formatCurrency, generateRequestId } from '@/utils/idGenerator'
import type { Account, AccountQueryDTO } from '@/types'
import type { TableProps } from 'ant-design-vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const searchForm = reactive<AccountQueryDTO>({
  userId: '',
  accountId: '',
  accountType: undefined,
  status: undefined,
  minBalance: undefined,
  maxBalance: undefined,
  pageNum: 1,
  pageSize: 20
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

const tableData = ref<Account[]>([])

const columns = [
  { title: '账户ID', dataIndex: 'accountId', key: 'accountId', width: 180 },
  { title: '账户账号', dataIndex: 'accountNo', key: 'accountNo', width: 200 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 120 },
  { title: '账户类型', dataIndex: 'accountTypeDesc', key: 'accountType', width: 100 },
  { title: '币种', dataIndex: 'currencyDesc', key: 'currency', width: 80 },
  { title: '账户余额', dataIndex: 'balance', key: 'balance', width: 140 },
  { title: '账户状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '热点标记', key: 'isHotAccount', width: 120 },
  { title: '开户时间', dataIndex: 'openTime', key: 'openTime', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    1: 'green',
    2: 'red',
    3: 'default'
  }
  return colors[status] || 'default'
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.userId = ''
  searchForm.accountId = ''
  searchForm.accountType = undefined
  searchForm.status = undefined
  searchForm.minBalance = undefined
  searchForm.maxBalance = undefined
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
    const params: AccountQueryDTO = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await getAccountList(params)
    tableData.value = res.records || mockData
    pagination.total = res.total || mockData.length
  } catch (error) {
    console.error('查询账户列表失败:', error)
    tableData.value = mockData
    pagination.total = mockData.length
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (record: Account) => {
  router.push(`/accounts/${record.accountId}`)
}

const handleMarkHot = (record: Account) => {
  Modal.confirm({
    title: '确认标记为热点账户',
    content: `确定要将账户 ${record.accountId} 标记为热点账户吗？标记后将启用分片和缓冲记账功能。`,
    onOk: async () => {
      try {
        await markAsHotAccount({
          accountId: record.accountId,
          shardCount: 10,
          requestId: generateRequestId()
        })
        message.success('标记成功')
        fetchData()
      } catch (error) {
        message.error('标记失败')
      }
    }
  })
}

const handleExport = () => {
  message.info('导出功能开发中...')
}

const mockData: Account[] = Array.from({ length: 15 }, (_, i) => ({
  accountId: `ACC${String(i + 1).padStart(6, '0')}`,
  accountNo: `622202${String(i + 1).padStart(12, '0')}`,
  userId: `USER${String(i + 1).padStart(6, '0')}`,
  accountType: i % 2 === 0 ? 1 : 2,
  accountTypeDesc: i % 2 === 0 ? '个人账户' : '企业账户',
  currency: 'CNY',
  currencyDesc: '人民币',
  balance: (i + 1) * 10000 + Math.random() * 50000,
  status: [1, 1, 1, 2, 1][i % 5],
  statusDesc: [1, 1, 1, 2, 1][i % 5] === 1 ? '正常' : '冻结',
  isHotAccount: i < 3,
  openTime: new Date(Date.now() - i * 86400000).toISOString()
}))

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.balance {
  font-weight: 600;
  color: #1890ff;
}

.text-gray {
  color: rgba(0, 0, 0, 0.25);
}
</style>
