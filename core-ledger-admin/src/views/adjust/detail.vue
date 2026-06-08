<template>
  <div class="page-container">
    <a-page-header
      title="调账申请详情"
      :sub-title="`申请单号: ${applicationId}`"
      @back="$router.back()"
    >
      <template #extra>
        <a-space v-if="applicationInfo.status === 0">
          <a-button type="primary" @click="handleApprove">
            审批
          </a-button>
        </a-space>
        <a-space v-else-if="applicationInfo.status === 1">
          <a-button type="primary" @click="handleExecute">
            执行调账
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <div class="detail-content" v-loading="loading">
      <a-descriptions title="基本信息" :column="3" bordered>
        <a-descriptions-item label="申请单号">{{ applicationInfo.applicationNo }}</a-descriptions-item>
        <a-descriptions-item label="账户ID">{{ applicationInfo.accountId }}</a-descriptions-item>
        <a-descriptions-item label="账户账号">{{ applicationInfo.accountNo }}</a-descriptions-item>
        <a-descriptions-item label="用户ID">{{ applicationInfo.userId }}</a-descriptions-item>
        <a-descriptions-item label="调账类型">
          <a-tag :color="applicationInfo.adjustType === 1 ? 'green' : 'orange'">
            {{ applicationInfo.adjustTypeDesc }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="调账金额">
          <span :class="applicationInfo.adjustType === 1 ? 'increase' : 'decrease'">
            {{ applicationInfo.adjustType === 1 ? '+' : '-' }}{{ formatCurrency(applicationInfo.amount) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="币种">{{ applicationInfo.currency }}</a-descriptions-item>
        <a-descriptions-item label="申请状态">
          <a-tag :color="getStatusColor(applicationInfo.status)">
            {{ applicationInfo.statusDesc }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="关联交易号">
          <a v-if="applicationInfo.transactionId" type="link" @click="goToTransaction">
            {{ applicationInfo.transactionId }}
          </a>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="调账原因" :span="3">{{ applicationInfo.reason }}</a-descriptions-item>
        <a-descriptions-item label="备注信息" :span="3">{{ applicationInfo.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-descriptions title="审批信息" :column="3" bordered v-if="applicationInfo.approver">
        <a-descriptions-item label="审批人">{{ applicationInfo.approver }}</a-descriptions-item>
        <a-descriptions-item label="审批时间">{{ applicationInfo.approveTime }}</a-descriptions-item>
        <a-descriptions-item label="审批结果">
          <a-tag :color="applicationInfo.status === 2 ? 'red' : 'green'">
            {{ applicationInfo.status === 2 ? '已拒绝' : '已同意' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="审批备注" :span="3">{{ applicationInfo.approveRemark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider v-if="applicationInfo.executor" />

      <a-descriptions title="执行信息" :column="3" bordered v-if="applicationInfo.executor">
        <a-descriptions-item label="执行人">{{ applicationInfo.executor }}</a-descriptions-item>
        <a-descriptions-item label="执行时间">{{ applicationInfo.executeTime }}</a-descriptions-item>
        <a-descriptions-item label="执行状态">
          <a-tag color="green">已执行</a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <div class="detail-section">
        <h3 class="section-title">操作日志</h3>
        <a-timeline>
          <a-timeline-item color="blue">
            <p class="log-title">提交调账申请</p>
            <p class="log-time">{{ applicationInfo.applyTime }}</p>
            <p class="log-user">操作人: {{ applicationInfo.applicant }}</p>
          </a-timeline-item>
          <a-timeline-item v-if="applicationInfo.approver" :color="applicationInfo.status === 2 ? 'red' : 'green'">
            <p class="log-title">
              {{ applicationInfo.status === 2 ? '拒绝调账申请' : '通过调账申请' }}
            </p>
            <p class="log-time">{{ applicationInfo.approveTime }}</p>
            <p class="log-user">操作人: {{ applicationInfo.approver }}</p>
            <p v-if="applicationInfo.approveRemark" class="log-remark">
              备注: {{ applicationInfo.approveRemark }}
            </p>
          </a-timeline-item>
          <a-timeline-item v-if="applicationInfo.executor" color="green">
            <p class="log-title">执行调账操作</p>
            <p class="log-time">{{ applicationInfo.executeTime }}</p>
            <p class="log-user">操作人: {{ applicationInfo.executor }}</p>
            <p v-if="applicationInfo.transactionId" class="log-remark">
              交易ID: {{ applicationInfo.transactionId }}
            </p>
          </a-timeline-item>
        </a-timeline>
      </div>
    </div>

    <a-modal
      v-model:open="showApproveModal"
      title="审批调账申请"
      width="500px"
      @ok="handleApproveSubmit"
      @cancel="showApproveModal = false"
    >
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
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getAdjustApplication, approveAdjustApplication, executeAdjustApplication } from '@/api/adjust'
import { formatCurrency, generateRequestId } from '@/utils/idGenerator'
import type { AdjustApplication } from '@/types'
import type { FormInstance } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const applicationId = ref(route.params.id as string)
const loading = ref(false)
const showApproveModal = ref(false)
const approveFormRef = ref<FormInstance>()

const applicationInfo = reactive<AdjustApplication>({
  id: '',
  applicationNo: '',
  accountId: '',
  accountNo: '',
  userId: '',
  adjustType: 1,
  adjustTypeDesc: '',
  amount: 0,
  currency: 'CNY',
  reason: '',
  status: 0,
  statusDesc: '',
  applicant: '',
  applyTime: ''
})

const approveForm = reactive({
  approved: true,
  approveRemark: ''
})

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'red',
    3: 'green'
  }
  return colors[status] || 'default'
}

const goToTransaction = () => {
  if (applicationInfo.transactionId) {
    router.push(`/transactions/${applicationInfo.transactionId}`)
  }
}

const handleApprove = () => {
  approveForm.approved = true
  approveForm.approveRemark = ''
  showApproveModal.value = true
}

const handleApproveSubmit = async () => {
  try {
    await approveFormRef.value?.validate()
    await approveAdjustApplication({
      id: applicationId.value,
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

const handleExecute = () => {
  Modal.confirm({
    title: '确认执行调账',
    content: `确定要执行调账申请 ${applicationInfo.applicationNo} 吗？执行后将实际修改账户余额。`,
    onOk: async () => {
      try {
        await executeAdjustApplication({
          id: applicationId.value,
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

const fetchData = async () => {
  try {
    loading.value = true
    const res = await getAdjustApplication(applicationId.value)
    Object.assign(applicationInfo, res)
  } catch (error) {
    console.error('查询调账申请详情失败:', error)
    Object.assign(applicationInfo, {
      id: applicationId.value,
      applicationNo: 'ADJ202401000001',
      accountId: 'ACC000001',
      accountNo: '6222020000000001',
      userId: 'USER000001',
      adjustType: 1,
      adjustTypeDesc: '增加余额',
      amount: 1000.00,
      currency: 'CNY',
      reason: '客户投诉调账，系统计算错误导致余额少计',
      status: 0,
      statusDesc: '待审批',
      applicant: '运营管理员',
      applyTime: '2024-01-20 10:30:00',
      remark: '客户投诉工单号: COMP20240120001'
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

.increase {
  color: #52c41a;
  font-weight: 600;
  font-size: 18px;
}

.decrease {
  color: #f5222d;
  font-weight: 600;
  font-size: 18px;
}

.detail-section {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 16px;
  }
}

.log-title {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 0 4px 0;
}

.log-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0 0 4px 0;
}

.log-user {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  margin: 0 0 4px 0;
}

.log-remark {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  margin: 0;
}
</style>
