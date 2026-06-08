<template>
  <div class="page-container">
    <div class="page-header">
      <a-form
        ref="formRef"
        :model="searchForm"
        layout="inline"
        class="search-form"
      >
        <a-form-item label="账户ID">
          <a-input
            v-model:value="searchForm.accountId"
            placeholder="请输入账户ID"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="申请状态">
          <a-select
            v-model:value="searchForm.status"
            placeholder="请选择"
            style="width: 140px"
            allow-clear
          >
            <a-select-option :value="0">待审批</a-select-option>
            <a-select-option :value="1">已审批</a-select-option>
            <a-select-option :value="2">已拒绝</a-select-option>
            <a-select-option :value="3">已执行</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="调账类型">
          <a-select
            v-model:value="searchForm.adjustType"
            placeholder="请选择"
            style="width: 140px"
            allow-clear
          >
            <a-select-option :value="1">增加余额</a-select-option>
            <a-select-option :value="2">扣减余额</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="申请时间">
          <a-range-picker
            v-model:value="searchForm.dateRange"
            show-time
            style="width: 300px"
          />
        </a-form-item>
        <a-form-item label="申请人">
          <a-input
            v-model:value="searchForm.applicant"
            placeholder="请输入申请人"
            style="width: 140px"
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
        <a-button type="primary" @click="showCreateModal = true">
          <plus-outlined />
          新增调账申请
        </a-button>
        <span>共 {{ pagination.total }} 条记录</span>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'adjustType'">
            <a-tag :color="record.adjustType === 1 ? 'green' : 'orange'">
              {{ record.adjustTypeDesc }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            <span :class="record.adjustType === 1 ? 'increase' : 'decrease'">
              {{ record.adjustType === 1 ? '+' : '-' }}{{ formatCurrency(record.amount) }}
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.statusDesc }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="handleViewDetail(record)">
              详情
            </a-button>
            <a-button
              v-if="record.status === 0"
              type="link"
              @click="handleApprove(record)"
            >
              审批
            </a-button>
            <a-button
              v-if="record.status === 1"
              type="link"
              @click="handleExecute(record)"
            >
              执行
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="showCreateModal"
      title="新增调账申请"
      width="600px"
      @ok="handleCreate"
      @cancel="showCreateModal = false"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="账户ID"
          name="accountId"
          :rules="[{ required: true, message: '请输入账户ID' }]"
        >
          <a-input
            v-model:value="createForm.accountId"
            placeholder="请输入账户ID"
          />
        </a-form-item>
        <a-form-item
          label="调账类型"
          name="adjustType"
          :rules="[{ required: true, message: '请选择调账类型' }]"
        >
          <a-select v-model:value="createForm.adjustType" placeholder="请选择调账类型">
            <a-select-option :value="1">增加余额</a-select-option>
            <a-select-option :value="2">扣减余额</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="调账金额"
          name="amount"
          :rules="[{ required: true, message: '请输入调账金额' }]"
        >
          <a-input-number
            v-model:value="createForm.amount"
            :min="0.01"
            :step="0.01"
            :precision="2"
            placeholder="请输入调账金额"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="调账原因"
          name="reason"
          :rules="[{ required: true, message: '请输入调账原因' }]"
        >
          <a-textarea
            v-model:value="createForm.reason"
            :rows="3"
            placeholder="请输入调账原因"
          />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="createForm.remark"
            :rows="2"
            placeholder="请输入备注（选填）"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showApproveModal"
      title="审批调账申请"
      width="500px"
      @ok="handleApproveSubmit"
      @cancel="showApproveModal = false"
    >
      <a-descriptions :column="1" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="申请单号">{{ currentRecord?.applicationNo }}</a-descriptions-item>
        <a-descriptions-item label="账户ID">{{ currentRecord?.accountId }}</a-descriptions-item>
        <a-descriptions-item label="调账类型">
          <a-tag :color="currentRecord?.adjustType === 1 ? 'green' : 'orange'">
            {{ currentRecord?.adjustTypeDesc }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="调账金额">
          <span :class="currentRecord?.adjustType === 1 ? 'increase' : 'decrease'">
            {{ currentRecord?.adjustType === 1 ? '+' : '-' }}{{ formatCurrency(currentRecord?.amount || 0) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="调账原因">{{ currentRecord?.reason }}</a-descriptions-item>
      </a-descriptions>
      <a-form
        ref="approveFormRef"
        :model="approveForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="审批结果"
          name="approved"
          :rules="[{ required: true, message: '请选择审批结果' }]"
        >
          <a-radio-group v-model:value="approveForm.approved">
            <a-radio :value="true">同意</a-radio>
            <a-radio :value="false">拒绝</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          v-if="!approveForm.approved"
          label="审批备注"
          name="approveRemark"
          :rules="[{ required: true, message: '请输入拒绝原因' }]"
        >
          <a-textarea
            v-model:value="approveForm.approveRemark"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </a-form-item>
        <a-form-item v-else label="审批备注" name="approveRemark">
          <a-textarea
            v-model:value="approveForm.approveRemark"
            :rows="2"
            placeholder="请输入备注（选填）"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  queryAdjustApplicationList,
  createAdjustApplication,
  approveAdjustApplication,
  executeAdjustApplication
} from '@/api/adjust'
import { formatCurrency, generateRequestId } from '@/utils/idGenerator'
import type { AdjustApplication, AdjustApplicationQueryDTO } from '@/types'
import type { TableProps, FormInstance } from 'ant-design-vue'

const router = useRouter()
const formRef = ref()
const createFormRef = ref<FormInstance>()
const approveFormRef = ref<FormInstance>()
const loading = ref(false)
const showCreateModal = ref(false)
const showApproveModal = ref(false)
const currentRecord = ref<AdjustApplication | null>(null)

const searchForm = reactive<AdjustApplicationQueryDTO & { dateRange: any[] }>({
  accountId: '',
  status: undefined,
  adjustType: undefined,
  dateRange: [],
  applicant: '',
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

const tableData = ref<AdjustApplication[]>([])

const createForm = reactive({
  accountId: '',
  adjustType: 1,
  amount: undefined as number | undefined,
  reason: '',
  remark: ''
})

const approveForm = reactive({
  approved: true,
  approveRemark: ''
})

const columns = [
  { title: '申请单号', dataIndex: 'applicationNo', key: 'applicationNo', width: 180 },
  { title: '账户ID', dataIndex: 'accountId', key: 'accountId', width: 140 },
  { title: '调账类型', key: 'adjustType', width: 120 },
  { title: '调账金额', key: 'amount', width: 140 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '调账原因', dataIndex: 'reason', key: 'reason', width: 200, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant', width: 100 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'red',
    3: 'green'
  }
  return colors[status] || 'default'
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.accountId = ''
  searchForm.status = undefined
  searchForm.adjustType = undefined
  searchForm.dateRange = []
  searchForm.applicant = ''
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
    const params: AdjustApplicationQueryDTO = {
      accountId: searchForm.accountId || undefined,
      status: searchForm.status,
      adjustType: searchForm.adjustType,
      startTime: searchForm.dateRange?.[0] ? dayjs(searchForm.dateRange[0]).toDate() as any : undefined,
      endTime: searchForm.dateRange?.[1] ? dayjs(searchForm.dateRange[1]).toDate() as any : undefined,
      applicant: searchForm.applicant || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await queryAdjustApplicationList(params)
    tableData.value = res.records || mockData
    pagination.total = res.total || mockData.length
  } catch (error) {
    console.error('查询调账申请列表失败:', error)
    tableData.value = mockData
    pagination.total = mockData.length
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (record: AdjustApplication) => {
  router.push(`/adjust-applications/${record.id}`)
}

const handleCreate = async () => {
  try {
    await createFormRef.value?.validate()
    await createAdjustApplication({
      ...createForm,
      requestId: generateRequestId()
    })
    message.success('申请提交成功')
    showCreateModal.value = false
    fetchData()
  } catch (error) {
    message.error('提交失败')
  }
}

const handleApprove = (record: AdjustApplication) => {
  currentRecord.value = record
  approveForm.approved = true
  approveForm.approveRemark = ''
  showApproveModal.value = true
}

const handleApproveSubmit = async () => {
  if (!currentRecord.value) return
  try {
    await approveFormRef.value?.validate()
    await approveAdjustApplication({
      id: currentRecord.value.id,
      approved: approveForm.approved,
      approveRemark: approveForm.approveRemark,
      requestId: generateRequestId()
    })
    message.success(approveForm.approved ? '审批通过' : '已拒绝')
    showApproveModal.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  }
}

const handleExecute = (record: AdjustApplication) => {
  Modal.confirm({
    title: '确认执行调账',
    content: `确定要执行调账申请 ${record.applicationNo} 吗？执行后将实际修改账户余额。`,
    onOk: async () => {
      try {
        await executeAdjustApplication({
          id: record.id,
          requestId: generateRequestId()
        })
        message.success('执行成功')
        fetchData()
      } catch (error) {
        message.error('执行失败')
      }
    }
  })
}

const mockData: AdjustApplication[] = Array.from({ length: 15 }, (_, i) => ({
  id: `ADJ${String(i + 1).padStart(8, '0')}`,
  applicationNo: `ADJ202401${String(i + 1).padStart(6, '0')}`,
  accountId: `ACC${String(i + 1).padStart(6, '0')}`,
  accountNo: `622202${String(i + 1).padStart(12, '0')}`,
  userId: `USER${String(i + 1).padStart(6, '0')}`,
  adjustType: i % 2 === 0 ? 1 : 2,
  adjustTypeDesc: i % 2 === 0 ? '增加余额' : '扣减余额',
  amount: (i + 1) * 100 + Math.random() * 1000,
  currency: 'CNY',
  reason: `调账原因${i + 1}`,
  status: [0, 1, 3, 2, 1][i % 5],
  statusDesc: ['待审批', '已审批', '已执行', '已拒绝', '已审批'][i % 5],
  applicant: '申请人' + ((i % 5) + 1),
  applyTime: new Date(Date.now() - i * 3600000).toISOString(),
  approver: i > 0 ? '审批员' + ((i % 3) + 1) : undefined,
  approveTime: i > 0 ? new Date(Date.now() - i * 3600000 + 1800000).toISOString() : undefined,
  remark: i % 3 === 0 ? '备注信息' : undefined
}))

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="less">
.increase {
  color: #52c41a;
  font-weight: 600;
}

.decrease {
  color: #f5222d;
  font-weight: 600;
}
</style>
