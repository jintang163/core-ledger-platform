import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { enableMocking } from './mock'
import App from './App'
import './styles/index.css'

// dayjs中文本地化
dayjs.locale('zh-cn')

// React Query客户端配置
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 失败重试次数
      retry: 1,
      // 窗口聚焦时重新获取
      refetchOnWindowFocus: false,
      // 数据缓存时间（5分钟）
      staleTime: 5 * 60 * 1000,
    },
  },
})

// Ant Design主题配置
const antdTheme = {
  algorithm: theme.defaultAlgorithm,
  token: {
    // 主题色
    colorPrimary: '#1E3A8A',
    // 成功色
    colorSuccess: '#059669',
    // 错误色
    colorError: '#DC2626',
    // 警告色
    colorWarning: '#D97706',
    // 圆角
    borderRadius: 6,
    // 字体
    fontFamily: 'Inter, system-ui, sans-serif',
    // 字号
    fontSize: 14,
  },
  components: {
    Button: {
      colorPrimary: '#1E3A8A',
      algorithm: true,
    },
    Table: {
      colorPrimary: '#1E3A8A',
    },
    Modal: {
      colorPrimary: '#1E3A8A',
    },
  },
}

// 渲染应用
function renderApp() {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      {/* React Query Provider */}
      <QueryClientProvider client={queryClient}>
        {/* Ant Design ConfigProvider */}
        <ConfigProvider locale={zhCN} theme={antdTheme}>
          {/* React Router */}
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ConfigProvider>
      </QueryClientProvider>
    </React.StrictMode>,
  )
}

// 应用入口：开发环境先启动Mock服务再渲染
async function bootstrap() {
  if (import.meta.env.DEV) {
    await enableMocking()
  }
  renderApp()
}

bootstrap()
