import { http, HttpResponse } from 'msw'
import { Transaction, TransactionCreateDTO, TransactionQueryDTO, Page, TransactionStatusEnum, TransactionEntry } from '@/api/types'
import {
  mockTransactions,
  generateTransactionId,
  generateTransactionNo,
  generateBusinessNo,
  generateEntryId,
  getCurrentTime,
  getTransactionTypeDesc,
  getTransactionStatusDesc,
  getDebitCreditDesc,
} from '../db'

// 交易数据存储
let transactions: Transaction[] = JSON.parse(JSON.stringify(mockTransactions))

// 统一成功响应
function successResponse<T>(data: T): HttpResponse {
  return HttpResponse.json({
    code: 200,
    message: 'success',
    data,
    timestamp: Date.now(),
  })
}

// 统一错误响应
function errorResponse(message: string, code: number = 500): HttpResponse {
  return HttpResponse.json({
    code,
    message,
    data: null,
    timestamp: Date.now(),
  })
}

export const transactionHandlers = [
  // POST /api/transaction/create - 复式记账
  http.post('/api/transaction/create', async ({ request }) => {
    try {
      const body = (await request.json()) as TransactionCreateDTO

      const transactionId = generateTransactionId()
      const transactionNo = generateTransactionNo()
      const businessNo = body.businessNo || generateBusinessNo()
      const now = getCurrentTime()

      // 生成分录数据
      const entries: TransactionEntry[] = body.entries.map((entry, index) => ({
        entryId: generateEntryId(),
        transactionId,
        accountId: entry.accountId,
        accountNo: `6222171764${String(10000 + index).padStart(5, '0')}`,
        subjectCode: entry.subjectCode,
        subjectName: entry.subjectName,
        direction: entry.direction,
        directionDesc: getDebitCreditDesc(entry.direction),
        amount: entry.amount,
        currency: body.currency,
        summary: entry.summary,
        createTime: now,
      }))

      const newTransaction: Transaction = {
        transactionId,
        transactionNo,
        transactionType: body.transactionType,
        transactionTypeDesc: getTransactionTypeDesc(body.transactionType),
        businessNo,
        totalAmount: body.totalAmount,
        currency: body.currency,
        voucherNo: `VCH${Date.now().toString().slice(-6)}`,
        summary: body.summary,
        status: TransactionStatusEnum.SUCCESS,
        statusDesc: getTransactionStatusDesc(TransactionStatusEnum.SUCCESS),
        operator: body.operator || '系统管理员',
        transactionTime: now,
        createTime: now,
        entries,
      }

      transactions.unshift(newTransaction)
      return successResponse(newTransaction)
    } catch (error) {
      return errorResponse('创建交易失败')
    }
  }),

  // GET /api/transaction/:transactionId - 查询交易
  http.get('/api/transaction/:transactionId', ({ params }) => {
    const { transactionId } = params
    const transaction = transactions.find((t) => t.transactionId === transactionId)

    if (!transaction) {
      return errorResponse('交易不存在', 404)
    }

    return successResponse(transaction)
  }),

  // GET /api/transaction/business/:businessNo - 按业务号查询
  http.get('/api/transaction/business/:businessNo', ({ params }) => {
    const { businessNo } = params
    const transaction = transactions.find((t) => t.businessNo === businessNo)

    if (!transaction) {
      return errorResponse('交易不存在', 404)
    }

    return successResponse(transaction)
  }),

  // POST /api/transaction/query - 交易列表
  http.post('/api/transaction/query', async ({ request }) => {
    try {
      const body = (await request.json()) as TransactionQueryDTO

      let data = [...transactions]

      // 过滤条件
      if (body?.accountId) {
        data = data.filter((item) =>
          item.entries.some((entry) => entry.accountId === body.accountId)
        )
      }
      if (body?.transactionType) {
        data = data.filter((item) => item.transactionType === body.transactionType)
      }
      if (body?.status) {
        data = data.filter((item) => item.status === body.status)
      }
      if (body?.startTime) {
        data = data.filter((item) => (item.transactionTime || '') >= body.startTime!)
      }
      if (body?.endTime) {
        data = data.filter((item) => (item.transactionTime || '') <= body.endTime!)
      }

      // 分页
      const pageNum = body?.pageNum || 1
      const pageSize = body?.pageSize || 10
      const total = data.length
      const start = (pageNum - 1) * pageSize
      const end = start + pageSize
      const records = data.slice(start, end)

      const pageResult: Page<Transaction> = {
        records,
        total,
        size: pageSize,
        current: pageNum,
        pages: Math.ceil(total / pageSize),
      }

      return successResponse(pageResult)
    } catch (error) {
      return errorResponse('查询交易列表失败')
    }
  }),
]
