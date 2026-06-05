import { useState, useEffect } from 'react'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { Layout, Breadcrumb, Avatar, Dropdown, Badge, Button, Drawer } from 'antd'
import {
  LayoutDashboard,
  Users,
  BookOpen,
  Receipt,
  Menu,
  X,
  Bell,
  ChevronDown,
  LogOut,
  User,
  Settings,
  Wallet,
} from 'lucide-react'
import classNames from 'classnames'
import useUserStore from '@/store/useUserStore'

const { Header, Sider, Content } = Layout

// 菜单项配置
const menuItems = [
  {
    key: '/dashboard',
    icon: LayoutDashboard,
    label: '仪表盘',
  },
  {
    key: '/accounts',
    icon: Users,
    label: '账户管理',
  },
  {
    key: '/double-entry',
    icon: BookOpen,
    label: '复式记账',
  },
  {
    key: '/transactions',
    icon: Receipt,
    label: '交易明细',
  },
]

// 面包屑映射
const breadcrumbMap: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/accounts': '账户管理',
  '/double-entry': '复式记账',
  '/transactions': '交易明细',
}

// 主布局组件
function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout } = useUserStore()

  // 侧边栏折叠状态（桌面端）
  const [collapsed, setCollapsed] = useState(false)
  // 移动端抽屉状态
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
  // 是否为移动端
  const [isMobile, setIsMobile] = useState(false)

  // 监听窗口大小变化，判断是否为移动端
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768)
    }
    checkMobile()
    window.addEventListener('resize', checkMobile)
    return () => window.removeEventListener('resize', checkMobile)
  }, [])

  // 处理菜单点击
  const handleMenuClick = (key: string) => {
    navigate(key)
    if (isMobile) {
      setMobileDrawerOpen(false)
    }
  }

  // 处理退出登录
  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  // 生成面包屑
  const generateBreadcrumbs = () => {
    const pathname = location.pathname
    const crumbs = [{ title: '首页' }]
    if (breadcrumbMap[pathname]) {
      crumbs.push({ title: breadcrumbMap[pathname] })
    }
    return crumbs
  }

  // 用户下拉菜单
  const userMenuItems = [
    {
      key: 'profile',
      icon: <User className="w-4 h-4" />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'account',
      icon: <Wallet className="w-4 h-4" />,
      label: '我的账户',
      onClick: () => navigate('/accounts'),
    },
    {
      key: 'settings',
      icon: <Settings className="w-4 h-4" />,
      label: '系统设置',
      onClick: () => navigate('/settings'),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogOut className="w-4 h-4" />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  // 渲染侧边栏内容
  const renderSidebarContent = () => (
    <div className="h-full flex flex-col">
      {/* Logo 区域 */}
      <div className="h-16 flex items-center justify-center px-4 border-b border-primary-800/30">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-white/10 rounded-lg flex items-center justify-center">
            <Wallet className="w-6 h-6 text-white" />
          </div>
          {(!collapsed || isMobile) && (
            <div className="flex flex-col">
              <span className="text-white font-bold text-lg leading-tight">
                账务核心
              </span>
              <span className="text-primary-300 text-xs">Core Ledger</span>
            </div>
          )}
        </div>
      </div>

      {/* 菜单区域 */}
      <nav className="flex-1 py-4 overflow-y-auto">
        <ul className="space-y-1 px-3">
          {menuItems.map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.key
            return (
              <li key={item.key}>
                <button
                  onClick={() => handleMenuClick(item.key)}
                  className={classNames(
                    'w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 group',
                    isActive
                      ? 'bg-white/15 text-white shadow-lg shadow-primary-900/30'
                      : 'text-primary-200 hover:bg-white/10 hover:text-white'
                  )}
                >
                  <Icon
                    className={classNames(
                      'w-5 h-5 flex-shrink-0 transition-transform duration-200',
                      isActive ? 'scale-110' : 'group-hover:scale-110'
                    )}
                  />
                  {(!collapsed || isMobile) && (
                    <span className="font-medium transition-opacity duration-200">
                      {item.label}
                    </span>
                  )}
                  {isActive && (!collapsed || isMobile) && (
                    <div className="ml-auto w-1.5 h-1.5 bg-white rounded-full animate-pulse" />
                  )}
                </button>
              </li>
            )
          })}
        </ul>
      </nav>

      {/* 底部信息 */}
      {(!collapsed || isMobile) && (
        <div className="p-4 border-t border-primary-800/30">
          <div className="bg-primary-900/50 rounded-lg p-3">
            <p className="text-primary-300 text-xs mb-1">系统版本</p>
            <p className="text-white text-sm font-medium">v1.0.0</p>
          </div>
        </div>
      )}
    </div>
  )

  return (
    <Layout className="min-h-screen bg-gray-50">
      {/* 桌面端侧边栏 */}
      {!isMobile && (
        <Sider
          width={260}
          collapsedWidth={80}
          collapsed={collapsed}
          trigger={null}
          collapsible
          className="bg-primary-900 border-r border-primary-800/30"
          style={{
            boxShadow: '4px 0 24px rgba(0, 0, 0, 0.1)',
          }}
        >
          {renderSidebarContent()}
        </Sider>
      )}

      {/* 移动端抽屉侧边栏 */}
      <Drawer
        placement="left"
        open={mobileDrawerOpen}
        onClose={() => setMobileDrawerOpen(false)}
        width={260}
        styles={{
          body: {
            padding: 0,
            backgroundColor: '#1e3a8a',
          },
        }}
        closeIcon={<X className="w-5 h-5 text-white" />}
      >
        {renderSidebarContent()}
      </Drawer>

      {/* 主内容区域 */}
      <Layout className="flex flex-col">
        {/* 顶部操作栏 */}
        <Header
          className="bg-white border-b border-gray-200 px-4 md:px-6 flex items-center justify-between sticky top-0 z-40"
          style={{
            height: 64,
            boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)',
          }}
        >
          {/* 左侧：折叠按钮和面包屑 */}
          <div className="flex items-center gap-4">
            {/* 移动端菜单按钮 */}
            {isMobile && (
              <Button
                type="text"
                icon={<Menu className="w-5 h-5" />}
                onClick={() => setMobileDrawerOpen(true)}
                className="md:hidden"
              />
            )}

            {/* 桌面端折叠按钮 */}
            {!isMobile && (
              <Button
                type="text"
                icon={
                  collapsed ? (
                    <Menu className="w-5 h-5" />
                  ) : (
                    <X className="w-5 h-5" />
                  )
                }
                onClick={() => setCollapsed(!collapsed)}
                className="hidden md:flex"
              />
            )}

            {/* 面包屑 */}
            <Breadcrumb
              items={generateBreadcrumbs()}
              className="hidden md:block"
            />
          </div>

          {/* 右侧：通知和用户信息 */}
          <div className="flex items-center gap-2 md:gap-4">
            {/* 通知按钮 */}
            <Badge count={3} size="small">
              <Button
                type="text"
                icon={<Bell className="w-5 h-5 text-gray-600" />}
                className="hover:bg-gray-100"
              />
            </Badge>

            {/* 用户信息 */}
            <Dropdown
              menu={{ items: userMenuItems }}
              placement="bottomRight"
              trigger={['click']}
            >
              <div className="flex items-center gap-2 md:gap-3 cursor-pointer hover:bg-gray-50 px-2 md:px-3 py-1.5 rounded-lg transition-colors duration-200">
                <Avatar
                  size={36}
                  className="bg-primary-600 flex items-center justify-center"
                  icon={<User className="w-5 h-5" />}
                  src={userInfo?.avatar}
                />
                <div className="hidden md:block text-left">
                  <p className="text-sm font-medium text-gray-800">
                    {userInfo?.nickname || '用户'}
                  </p>
                  <p className="text-xs text-gray-500">
                    {userInfo?.role || '管理员'}
                  </p>
                </div>
                <ChevronDown className="w-4 h-4 text-gray-400 hidden md:block" />
              </div>
            </Dropdown>
          </div>
        </Header>

        {/* 内容区域 */}
        <Content className="flex-1 p-4 md:p-6 overflow-auto">
          <div
            className="min-h-full bg-white rounded-xl shadow-sm border border-gray-100 animate-fadeIn"
            style={{
              animation: 'fadeIn 0.3s ease-out',
            }}
          >
            <Outlet />
          </div>
        </Content>
      </Layout>

      {/* 全局动画样式 */}
      <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </Layout>
  )
}

export default MainLayout
