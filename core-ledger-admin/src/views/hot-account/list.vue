<template>
  <div class="page-container">
    <div class="page-header">
      <a-row :gutter="24">
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f5222d">
              <fire-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">热点账户总数</div>
              <div class="stat-value">{{ hotAccountCount }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #1890ff">
              <cluster-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">总分片数</div>
              <div class="stat-value">{{ totalShardCount }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #52c41a">
              <check-circle-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">今日处理量</div>
              <div class="stat-value">{{ todayProcessCount }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #faad14">
              <clock-circle-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">平均延迟</div>
              <div class="stat-value">{{ avgDelay }}ms</div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <div class="page-content">
      <div class="table-toolbar">
        <a-space>
          <a-button type="primary" @click="showConfigModal = true">
            <plus-outlined />
            新增热点账户
          </a-button>
          <a-button @click="handleMergeAll">
            <sync-outlined :spin="merging" />
            全部归并
          </a-button>
        </a-space>
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索账户ID/账号"
          style="width: 300px"
          @search="handleSearch"
        />
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
          <template v-if="column.key === 'shardCount'">
            <a-tag color="blue">{{ record.shardCount }} 个分片</a-tag>
          </template>
          <template v-else-if="column.key === 'bufferEnabled'">
            <a-switch
              checked-children="开启"
              un-checked-children="关闭"
              :checked="record.bufferEnabled"
              @change="(checked) => handleToggleBuffer(record, checked)"
            />
          </template>
          <template v-else-if="column.key === 'bufferThreshold'">
            <span>{{ record.bufferThreshold }} 笔/秒</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.status === 1 ? '运行中' : '已停止' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="handleViewShards(record)">
              分片详情
            </a-button>
            <a-button type="link" @click="handleConfig(record)">
              配置
            </a-button>
            <a-button type="link" @click="handleMerge(record)">
              归并
            </a-button>
            <a-button type="link" danger @click="handleUnmark(record)">
              取消标记
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="showConfigModal"
      :title="editingRecord ? '编辑热点账户配置' : '新增热点账户'"
      width="600px"
      @ok="handleSaveConfig"
      @cancel="showConfigModal = false"
    >
      <a-form
        ref="configFormRef"
        :model="configForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="账户ID"
          name="accountId"
          :rules="[{ required: true, message: '请输入账户ID' }]"
        >
          <a-input
            v-model:value="configForm.accountId"
            placeholder="请输入账户ID"
            :disabled="!!editingRecord"
          />
        </a-form-item>
        <a-form-item
          label="分片数量"
          name="shardCount"
          :rules="[{ required: true, message: '请输入分片数量' }]"
        >
          <a-input-number
            v-model:value="configForm.shardCount"
            :min="1"
            :max="100"
            placeholder="请输入分片数量"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="分片策略"
          name="shardingStrategy"
          :rules="[{ required: true, message: '请选择分片策略' }]"
        >
          <a-select v-model:value="configForm.shardingStrategy" placeholder="请选择分片策略">
            <a-select-option :value="1">哈希路由</a-select-option>
            <a-select-option :value="2">轮询路由</a-select-option>
            <a-select-option :value="3">随机路由</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="缓冲记账" name="bufferEnabled">
          <a-switch v-model:checked="configForm.bufferEnabled" />
        </a-form-item>
        <a-form-item
          label="缓冲阈值"
          name="bufferThreshold"
          v-if="configForm.bufferEnabled"
        >
          <a-input-number
            v-model:value="configForm.bufferThreshold"
            :min="100"
            :max="10000"
            placeholder="请输入缓冲阈值（笔/秒）"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showShardsModal"
      title="分片详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions :column="3" size="small" style="margin-bottom: 16px">
        <a-descriptions-item label="主账户ID">{{ currentAccount?.accountId }}</a-descriptions-item>
        <a-descriptions-item label="分片数">{{ currentAccount?.shardCount }}</a-descriptions-item>
        <a-descriptions-item label="分片策略">{{ currentAccount?.shardingStrategyDesc }}</a-descriptions-item>
      </a-descriptions>
      <a-table
        :columns="shardColumns"
        :data-source="shardList"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'balance'">
            <span class="balance">{{ formatCurrency(record.balance) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '正常' : '已停用' }}
            </a-tag>
          </template>
        </template>
      </a-table>
      <div style="margin-top: 16px; text-align: right">
        <span style="margin-right: 16px">
          分片总余额: <span class="balance">{{ formatCurrency(totalShardBalance) }}</span>
        </span>
        <a-button type="primary" @click="handleMergeCurrent">
          立即归并
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  FireOutlined,
  ClusterOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  PlusOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import {
  getHotAccountList,
  markAsHotAccount,
  unmarkAsHotAccount,
  updateHotAccountConfig,
  getAccountShards,
  mergeShards,
  mergeAllShards
} from '@/api/account'
import { formatCurrency, generateRequestId } from '@/utils/idGenerator'
import type { HotAccountConfigVO, AccountShard } from '@/types'
import type { TableProps, FormInstance } from 'ant-design-vue'

const loading = ref(false)
const merging = ref(false)
const searchKeyword = ref('')
const showConfigModal = ref(false)
const showShardsModal = ref(false)
const editingRecord = ref<HotAccountConfigVO | null>(null)
const currentAccount = ref<HotAccountConfigVO | null>(null)
const configFormRef = ref<FormInstance>()

const hotAccountCount = ref(24)
const totalShardCount = ref(240)
const todayProcessCount = ref('128,456')
const avgDelay = ref('125')

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

const tableData = ref<HotAccountConfigVO[]>([])
const shardList = ref<AccountShard[]>([])

const configForm = reactive({
  accountId: '',
  shardCount: 10,
  shardingStrategy: 1,
  bufferEnabled: true,
  bufferThreshold: 1000
})

const columns = [
  { title: '账户ID', dataIndex: 'accountId', key: 'accountId', width: 180 },
  { title: '账户账号', dataIndex: 'accountNo', key: 'accountNo', width: 200 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 120 },
  { title: '分片数', key: 'shardCount', width: 120 },
  { title: '分片策略', dataIndex: 'shardingStrategyDesc', key: 'shardingStrategy', width: 120 },
  { title: '缓冲记账', key: 'bufferEnabled', width: 120 },
  { title: '缓冲阈值', key: 'bufferThreshold', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 300, fixed: 'right' as const }
]

const shardColumns = [
  { title: '分片ID', dataIndex: 'shardId', key: 'shardId', width: 180 },
  { title: '分片索引', dataIndex: 'shardIndex', key: 'shardIndex', width: 100 },
  { title: '余额', key: 'balance', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 }
]

const totalShardBalance = computed(() => {
  return shardList.value.reduce((sum, s) => sum + s.balance, 0)
})

const getStatusColor = (status: number) => {
  return status === 1 ? 'green' : 'default'
}

const handleSearch = () => {
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
    const res = await getHotAccountList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records || mockData
    pagination.total = res.total || mockData.length
  } catch (error) {
    console.error('查询热点账户列表失败:', error)
    tableData.value = mockData
    pagination.total = mockData.length
  } finally {
    loading.value = false
  }
}

const handleConfig = (record: HotAccountConfigVO) => {
  editingRecord.value = record
  configForm.accountId = record.accountId
  configForm.shardCount = record.shardCount
  configForm.shardingStrategy = record.shardingStrategy
  configForm.bufferEnabled = record.bufferEnabled
  configForm.bufferThreshold = record.bufferThreshold
  showConfigModal.value = true
}

const handleSaveConfig = async () => {
  try {
    await configFormRef.value?.validate()
    if (editingRecord.value) {
      await updateHotAccountConfig({
        ...configForm,
        requestId: generateRequestId()
      })
      message.success('配置更新成功')
    } else {
      await markAsHotAccount({
        ...configForm,
        requestId: generateRequestId()
      })
      message.success('添加成功')
    }
    showConfigModal.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  }
}

const handleToggleBuffer = async (record: HotAccountConfigVO, checked: boolean) => {
  try {
    await updateHotAccountConfig({
      accountId: record.accountId,
      bufferEnabled: checked,
      requestId: generateRequestId()
    })
    message.success(checked ? '缓冲记账已开启' : '缓冲记账已关闭')
    fetchData()
  } catch (error) {
    message.error('操作失败')
  }
}

const handleViewShards = async (record: HotAccountConfigVO) => {
  currentAccount.value = record
  try {
    const res = await getAccountShards(record.accountId)
    shardList.value = res || mockShards
  } catch (error) {
    shardList.value = mockShards
  }
  showShardsModal.value = true
}

const handleMerge = async (record: HotAccountConfigVO) => {
  Modal.confirm({
    title: '确认归并分片',
    content: `确定要归并账户 ${record.accountId} 的所有分片余额吗？`,
    onOk: async () => {
      try {
        await mergeShards(record.accountId)
        message.success('归并成功')
        fetchData()
      } catch (error) {
        message.error('归并失败')
      }
    }
  })
}

const handleMergeCurrent = async () => {
  if (!currentAccount.value) return
  try {
    await mergeShards(currentAccount.value.accountId)
    message.success('归并成功')
    handleViewShards(currentAccount.value)
  } catch (error) {
    message.error('归并失败')
  }
}

const handleMergeAll = async () => {
  Modal.confirm({
    title: '确认全部归并',
    content: '确定要归并所有热点账户的分片余额吗？这可能需要一些时间。',
    onOk: async () => {
      try {
        merging.value = true
        await mergeAllShards()
        message.success('全部归并成功')
        fetchData()
      } catch (error) {
        message.error('归并失败')
      } finally {
        merging.value = false
      }
    }
  })
}

const handleUnmark = async (record: HotAccountConfigVO) => {
  Modal.confirm({
    title: '确认取消热点标记',
    content: `确定要取消账户 ${record.accountId} 的热点账户标记吗？取消后将不再使用分片和缓冲记账。`,
    onOk: async () => {
      try {
        await unmarkAsHotAccount(record.accountId)
        message.success('取消成功')
        fetchData()
      } catch (error) {
        message.error('操作失败')
      }
    }
  })
}

const mockData: HotAccountConfigVO[] = Array.from({ length: 15 }, (_, i) => ({
  accountId: `HOT${String(i + 1).padStart(6, '0')}`,
  accountNo: `622202${String(i + 1).padStart(12, '0')}`,
  userId: `USER${String(i + 1).padStart(6, '0')}`,
  isHotAccount: true,
  shardCount: [10, 15, 20, 10, 15][i % 5],
  shardingStrategy: [1, 2, 3, 1, 2][i % 5],
  shardingStrategyDesc: ['哈希路由', '轮询路由', '随机路由', '哈希路由', '轮询路由'][i % 5],
  bufferEnabled: i % 3 !== 0,
  bufferThreshold: [1000, 2000, 1500][i % 3],
  status: 1,
  shards: [],
  updateTime: new Date(Date.now() - i * 3600000).toISOString()
}))

const mockShards: AccountShard[] = Array.from({ length: 10 }, (_, i) => ({
  id: `SHARD${i + 1}`,
  shardId: `SHD${String(i + 1).padStart(8, '0')}`,
  mainAccountId: 'HOT000001',
  shardIndex: i,
  balance: (i + 1) * 10000 + Math.random() * 50000,
  status: 1,
  statusDesc: '正常',
  createTime: '2024-01-01 00:00:00',
  updateTime: new Date(Date.now() - i * 3600000).toISOString()
}))

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #fff;
    margin-right: 16px;
  }

  .stat-content {
    .stat-title {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.45);
      margin-bottom: 4px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.85);
    }
  }
}

.balance {
  font-weight: 600;
  color: #1890ff;
}
</style>
