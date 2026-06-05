import { setupWorker } from 'msw/browser'
import { accountHandlers } from './handlers/accountHandler'
import { transactionHandlers } from './handlers/transactionHandler'

// 合并所有Mock处理器
export const handlers = [...accountHandlers, ...transactionHandlers]

// 创建MSW Worker实例
export const worker = setupWorker(...handlers)

// Mock服务器启动配置
export async function enableMocking() {
  if (import.meta.env.DEV) {
    await worker.start({
      // 打印MSW注册日志
      onUnhandledRequest: 'bypass',
      // 服务工作者的范围
      serviceWorker: {
        url: '/mockServiceWorker.js',
      },
    })
    console.log('[MSW] Mock服务已启动')
  }
}
