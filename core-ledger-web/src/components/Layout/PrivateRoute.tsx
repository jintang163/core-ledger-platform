import { useEffect } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import useUserStore from '@/store/useUserStore'

// 路由守卫组件属性
interface PrivateRouteProps {
  children: React.ReactNode
}

// 路由守卫组件
function PrivateRoute({ children }: PrivateRouteProps) {
  const { isLoggedIn, token } = useUserStore()
  const location = useLocation()

  // 检查登录状态
  useEffect(() => {
    // 如果没有登录状态但有token，设置为已登录
    if (!isLoggedIn && token) {
      useUserStore.getState().isLoggedIn = true
    }
  }, [isLoggedIn, token])

  // 未登录状态，重定向到登录页
  if (!isLoggedIn && !token) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location }}
      />
    )
  }

  // 已登录，显示子组件
  return <>{children}</>
}

export default PrivateRoute
