import { Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from 'antd'
import MainLayout from './components/Layout/MainLayout'
import PrivateRoute from './components/Layout/PrivateRoute'
import Login from './pages/Login/Login'
import Dashboard from './pages/Dashboard/Dashboard'
import AccountList from './pages/Account/AccountList'
import AccountCreate from './pages/Account/AccountCreate'
import AccountDetail from './pages/Account/AccountDetail'
import TransactionCreate from './pages/Transaction/TransactionCreate'
import TransactionList from './pages/Transaction/TransactionList'
import TransactionDetail from './pages/Transaction/TransactionDetail'

const { Content } = Layout

// 404页面组件
function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <h1 className="text-6xl font-bold text-blue-800 mb-4">404</h1>
      <p className="text-xl text-gray-600 mb-8">页面未找到</p>
      <button
        onClick={() => (window.location.href = '/dashboard')}
        className="px-6 py-2 bg-blue-800 text-white rounded hover:bg-blue-700 transition-colors"
      >
        返回首页
      </button>
    </div>
  )
}

// 应用路由配置
function App() {
  return (
    <Layout className="min-h-screen bg-gray-50">
      <Content className="flex-1">
        <Routes>
          {/* 首页重定向到登录 */}
          <Route path="/" element={<Navigate to="/login" replace />}

          {/* 登录页 - 无需登录 */}
          <Route path="/login" element={<Login />} />

          {/* 404页面 - 无需登录 */}
          <Route path="/404" element={<NotFound />} />

          {/* 需要登录的路由 - 使用 MainLayout 包裹 */}
          <Route
            element={
              <PrivateRoute>
                <MainLayout />
              </PrivateRoute>
            }
          >
            {/* 仪表盘 */}
            <Route path="/dashboard" element={<Dashboard />} />

            {/* 账户管理 */}
            <Route path="/accounts" element={<AccountList />} />
            <Route path="/accounts/create" element={<AccountCreate />} />
            <Route path="/accounts/:id" element={<AccountDetail />} />

            {/* 复式记账 */}
            <Route path="/transactions/create" element={<TransactionCreate />} />

            {/* 交易明细 */}
            <Route path="/transactions" element={<TransactionList />} />
            <Route path="/transactions/:id" element={<TransactionDetail />} />
          </Route>

          {/* 重定向到404 */}
          <Route path="*" element={<Navigate to="/404" replace />}
        </Routes>
      </Content>
    </Layout>
  )
}

export default App
