import { http, HttpResponse } from 'msw'
import { Account, AccountCreateDTO, AccountFreezeDTO, AccountUnfreezeDTO, AccountCloseDTO, Page, AccountStatusEnum } from '@/api/types'
import {
  mockAccounts,
  mockOperationHistory,
  generateAccountId,
  generateAccountNo,
  getCurrentTime,
  getCurrencyDesc,
  getFreezeTypeDesc,
  getAccountTypeDesc,
  getAccountStatusDesc,
  OperationHistory,
} from '../db'

// 账户数据存储
let accounts: Account[] = JSON.parse(JSON.stringify(mockAccounts))
let operationHistory: OperationHistory[] = JSON.parse(JSON.stringify(mockOperationHistory))

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

export const accountHandlers = [
  // POST /api/account/create - 创建账户
  http.post('/api/account/create', async ({ request }) => {
    try {
      const body = (await request.json()) as AccountCreateDTO

      const accountId = generateAccountId()
      const accountNo = generateAccountNo()
      const now = getCurrentTime()

      const newAccount: Account = {
        accountId,
        accountNo,
        userId: body.userId,
        accountType: body.accountType,
        accountTypeDesc: getAccountTypeDesc(body.accountType),
        currency: body.currency,
        currencyDesc: getCurrencyDesc(body.currency),
        balance: body.initBalance,
        status: AccountStatusEnum.NORMAL,
        statusDesc: getAccountStatusDesc(AccountStatusEnum.NORMAL),
        openTime: now,
        createTime: now,
        updateTime: now,
      }

      accounts.unshift(newAccount)
      return successResponse(newAccount)
    } catch (error) {
      return errorResponse('创建账户失败')
    }
  }),

  // GET /api/account/:accountId - 查询账户
  http.get('/api/account/:accountId', ({ params }) => {
    const { accountId } = params
    const account = accounts.find((a) => a.accountId === accountId)

    if (!account) {
      return errorResponse('账户不存在', 404)
    }

    return successResponse(account)
  }),

  // POST /api/account/list - 账户列表
  http.post('/api/account/list', async ({ request }) => {
    try {
      const body = (await request.json()) as {
        userId?: string
        accountType?: number
        status?: number
        currency?: string
        keyword?: string
        pageNum?: number
        pageSize?: number
      }

      let data = [...accounts]

      // 过滤条件
      if (body?.userId) {
        data = data.filter((item) => item.userId === body.userId)
      }
      if (body?.accountType) {
        data = data.filter((item) => item.accountType === body.accountType)
      }
      if (body?.status) {
        data = data.filter((item) => item.status === body.status)
      }
      if (body?.currency) {
        data = data.filter((item) => item.currency === body.currency)
      }
      if (body?.keyword) {
        const keyword = body.keyword.toLowerCase()
        data = data.filter(
          (item) =>
            item.userId.toLowerCase().includes(keyword) ||
            item.accountNo.toLowerCase().includes(keyword)
        )
      }

      // 分页
      const pageNum = body?.pageNum || 1
      const pageSize = body?.pageSize || 10
      const total = data.length
      const start = (pageNum - 1) * pageSize
      const end = start + pageSize
      const records = data.slice(start, end)

      const pageResult: Page<Account> = {
        records,
        total,
        size: pageSize,
        current: pageNum,
        pages: Math.ceil(total / pageSize),
      }

      return successResponse(pageResult)
    } catch (error) {
      return errorResponse('查询账户列表失败')
    }
  }),

  // POST /api/account/freeze - 冻结账户
  http.post('/api/account/freeze', async ({ request }) => {
    try {
      const body = (await request.json()) as AccountFreezeDTO
      const account = accounts.find((a) => a.accountId === body.accountId)

      if (!account) {
        return errorResponse('账户不存在', 404)
      }

      if (account.status === AccountStatusEnum.CLOSED) {
        return errorResponse('已销户账户无法冻结')
      }

      if (account.status === AccountStatusEnum.FROZEN) {
        return errorResponse('账户已冻结')
      }

      const now = getCurrentTime()
      account.status = AccountStatusEnum.FROZEN
      account.statusDesc = getAccountStatusDesc(AccountStatusEnum.FROZEN)
      account.freezeType = body.freezeType
      account.freezeTypeDesc = getFreezeTypeDesc(body.freezeType)
      account.freezeRemark = body.remark
      account.freezeTime = now
      account.freezeOperator = body.operator || '系统管理员'
      account.updateTime = now

      // 添加操作历史
      operationHistory.unshift({
        id: `OP${Date.now()}`,
        operationType: 'freeze',
        operationTypeDesc: '冻结',
        freezeType: body.freezeType,
        freezeTypeDesc: getFreezeTypeDesc(body.freezeType),
        remark: body.remark,
        operator: body.operator || '系统管理员',
        operateTime: now,
      })

      return successResponse(account)
    } catch (error) {
      return errorResponse('冻结账户失败')
    }
  }),

  // POST /api/account/unfreeze - 解冻账户
  http.post('/api/account/unfreeze', async ({ request }) => {
    try {
      const body = (await request.json()) as AccountUnfreezeDTO
      const account = accounts.find((a) => a.accountId === body.accountId)

      if (!account) {
        return errorResponse('账户不存在', 404)
      }

      if (account.status !== AccountStatusEnum.FROZEN) {
        return errorResponse('账户未冻结，无需解冻')
      }

      const now = getCurrentTime()
      account.status = AccountStatusEnum.NORMAL
      account.statusDesc = getAccountStatusDesc(AccountStatusEnum.NORMAL)
      account.freezeType = undefined
      account.freezeTypeDesc = undefined
      account.freezeRemark = undefined
      account.freezeTime = undefined
      account.freezeOperator = undefined
      account.updateTime = now

      // 添加操作历史
      operationHistory.unshift({
        id: `OP${Date.now()}`,
        operationType: 'unfreeze',
        operationTypeDesc: '解冻',
        freezeType: body.freezeType,
        freezeTypeDesc: getFreezeTypeDesc(body.freezeType),
        remark: body.remark,
        operator: body.operator || '系统管理员',
        operateTime: now,
      })

      return successResponse(account)
    } catch (error) {
      return errorResponse('解冻账户失败')
    }
  }),

  // POST /api/account/close - 销户
  http.post('/api/account/close', async ({ request }) => {
    try {
      const body = (await request.json()) as AccountCloseDTO
      const account = accounts.find((a) => a.accountId === body.accountId)

      if (!account) {
        return errorResponse('账户不存在', 404)
      }

      if (account.status === AccountStatusEnum.CLOSED) {
        return errorResponse('账户已销户')
      }

      if (account.balance > 0) {
        return errorResponse('账户还有余额，无法销户')
      }

      const now = getCurrentTime()
      account.status = AccountStatusEnum.CLOSED
      account.statusDesc = getAccountStatusDesc(AccountStatusEnum.CLOSED)
      account.closeTime = now
      account.updateTime = now

      // 添加操作历史
      operationHistory.unshift({
        id: `OP${Date.now()}`,
        operationType: 'close',
        operationTypeDesc: '销户',
        remark: body.remark,
        operator: body.operator || '系统管理员',
        operateTime: now,
      })

      return successResponse(undefined)
    } catch (error) {
      return errorResponse('销户失败')
    }
  }),
]
