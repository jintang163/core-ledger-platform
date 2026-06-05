import { useState, useEffect } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

// 登录页面组件
const Login = () => {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [loginSuccess, setLoginSuccess] = useState(false)
  const [captchaCode, setCaptchaCode] = useState('')

  // 生成随机验证码
  const generateCaptcha = () => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    let code = ''
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    setCaptchaCode(code)
  }

  // 初始化验证码
  useEffect(() => {
    generateCaptcha()
  }, [])

  // 处理登录提交
  const handleLogin = async (values: any) => {
    // 校验验证码
    if (values.captcha.toUpperCase() !== captchaCode) {
      message.error('验证码错误')
      generateCaptcha()
      return
    }

    setLoading(true)

    // 模拟登录请求
    await new Promise(resolve => setTimeout(resolve, 1500))

    setLoading(false)
    setLoginSuccess(true)
    message.success('登录成功')

    // 延迟跳转，显示成功动画
    setTimeout(() => {
      navigate('/dashboard')
    }, 1000)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-900 via-blue-800 to-indigo-900 relative overflow-hidden">
      {/* 背景装饰 */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-blue-500 rounded-full opacity-20 blur-3xl"></div>
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-indigo-500 rounded-full opacity-20 blur-3xl"></div>
        <div className="absolute top-1/2 left-1/4 w-64 h-64 bg-cyan-500 rounded-full opacity-10 blur-3xl"></div>
      </div>

      {/* 登录卡片 */}
      <Card
        className={`w-full max-w-md mx-4 relative z-10 transition-all duration-500 ${
          loginSuccess ? 'scale-105 shadow-2xl' : 'shadow-xl'
        }`}
        styles={{
          body: {
            padding: '40px',
          },
        }}
      >
        {/* Logo和标题 */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl mb-4 shadow-lg">
            <UserOutlined className="text-3xl text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">账务核心管理系统</h1>
          <p className="text-gray-500">请登录您的账户</p>
        </div>

        {/* 登录表单 */}
        <Form
          form={form}
          name="login"
          initialValues={{ remember: true }}
          onFinish={handleLogin}
          size="large"
          className="space-y-5"
        >
          {/* 用户名 */}
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined className="text-gray-400" />}
              placeholder="用户名"
              className="h-12 rounded-lg"
              disabled={loading || loginSuccess}
            />
          </Form.Item>

          {/* 密码 */}
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined className="text-gray-400" />}
              placeholder="密码"
              className="h-12 rounded-lg"
              disabled={loading || loginSuccess}
            />
          </Form.Item>

          {/* 验证码 */}
          <Form.Item
            name="captcha"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <div className="flex gap-3">
              <Input
                prefix={<SafetyOutlined className="text-gray-400" />}
                placeholder="验证码"
                className="h-12 rounded-lg flex-1"
                disabled={loading || loginSuccess}
                maxLength={4}
              />
              <div
                onClick={generateCaptcha}
                className="h-12 px-4 bg-gradient-to-r from-gray-100 to-gray-200 rounded-lg flex items-center justify-center cursor-pointer select-none hover:from-gray-200 hover:to-gray-300 transition-all min-w-28"
                style={{
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: '20px',
                  fontWeight: 600,
                  letterSpacing: '3px',
                  color: '#1E3A8A',
                }}
              >
                {captchaCode}
              </div>
            </div>
          </Form.Item>

          {/* 登录按钮 */}
          <Form.Item className="mb-0">
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              className={`h-12 text-base font-medium rounded-lg transition-all duration-300 ${
                loginSuccess
                  ? 'bg-green-500 !border-green-500'
                  : 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 !border-transparent'
              }`}
              icon={loginSuccess ? <CheckCircleOutlined /> : undefined}
            >
              {loginSuccess ? '登录成功' : loading ? '登录中...' : '登 录'}
            </Button>
          </Form.Item>
        </Form>

        {/* 底部链接 */}
        <div className="mt-6 flex justify-between text-sm">
          <a href="#" className="text-blue-600 hover:text-blue-700 transition-colors">
            忘记密码？
          </a>
          <a href="#" className="text-blue-600 hover:text-blue-700 transition-colors">
            注册账号
          </a>
        </div>
      </Card>

      {/* 版权信息 */}
      <div className="absolute bottom-6 left-0 right-0 text-center text-white/60 text-sm z-10">
        <p>© 2024 账务核心管理系统 v1.0.0</p>
        <p className="mt-1 text-xs text-white/40">Powered by Core Ledger Platform</p>
      </div>
    </div>
  )
}

export default Login
