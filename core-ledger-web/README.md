# 核心账务平台前端

基于 React + TypeScript + Vite 构建的企业级核心账务管理系统前端。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite 5
- **路由**: React Router v6
- **UI 组件**: Ant Design 5
- **状态管理**: Zustand
- **数据请求**: React Query + Axios
- **样式**: TailwindCSS 3
- **图表**: Recharts
- **Mock**: MSW (Mock Service Worker)
- **日期处理**: Day.js

## 项目结构

```
core-ledger-web/
├── public/                     # 静态资源目录
├── src/
│   ├── api/                    # API 接口层
│   │   ├── account.ts          # 账户相关接口
│   │   ├── transaction.ts      # 交易相关接口
│   │   ├── request.ts          # Axios 请求封装
│   │   └── types.ts            # TypeScript 类型定义
│   ├── components/             # 公共组件
│   │   ├── AmountDisplay/      # 金额显示组件
│   │   ├── BalanceIndicator/   # 余额指示器组件
│   │   ├── Charts/             # 图表组件
│   │   ├── EntryTable/         # 分录表格组件
│   │   ├── Layout/             # 布局组件
│   │   │   ├── MainLayout.tsx  # 主布局（侧边栏+顶部栏）
│   │   │   └── PrivateRoute.tsx # 路由守卫
│   │   ├── Modals/             # 弹窗组件
│   │   ├── StatsCard/          # 统计卡片组件
│   │   └── StatusTag/          # 状态标签组件
│   ├── mock/                   # Mock 数据层
│   │   ├── handlers/           # Mock 处理器
│   │   │   ├── accountHandler.ts    # 账户接口Mock
│   │   │   └── transactionHandler.ts # 交易接口Mock
│   │   ├── db.ts               # Mock 数据库
│   │   ├── index.ts            # MSW 初始化
│   │   └── browser.ts          # 浏览器端入口
│   ├── pages/                  # 页面组件
│   │   ├── Account/            # 账户管理页面
│   │   │   ├── AccountList.tsx     # 账户列表
│   │   │   ├── AccountCreate.tsx   # 创建账户
│   │   │   ├── AccountDetail.tsx   # 账户详情
│   │   │   └── mockData.ts         # 页面Mock数据
│   │   ├── Dashboard/          # 仪表盘页面
│   │   │   ├── Dashboard.tsx
│   │   │   └── mockData.ts
│   │   ├── Login/              # 登录页面
│   │   │   └── Login.tsx
│   │   └── Transaction/        # 交易管理页面
│   │       ├── TransactionList.tsx   # 交易列表
│   │       ├── TransactionCreate.tsx # 复式记账
│   │       ├── TransactionDetail.tsx # 交易详情
│   │       └── mockData.ts           # 页面Mock数据
│   ├── store/                  # 状态管理
│   │   ├── useAccountStore.ts  # 账户状态
│   │   └── useUserStore.ts     # 用户状态
│   ├── styles/                 # 全局样式
│   │   └── index.css
│   ├── utils/                  # 工具函数
│   │   ├── amount.ts           # 金额处理
│   │   ├── constants.ts        # 常量定义
│   │   ├── format.ts           # 格式化函数
│   │   └── idGenerator.ts      # ID生成器
│   ├── App.tsx                 # 应用入口组件（路由配置）
│   ├── main.tsx                # 应用入口文件
│   └── vite-env.d.ts           # Vite 类型声明
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
└── postcss.config.js
```

## 路由配置

| 路径 | 页面 | 说明 | 是否需要登录 |
|------|------|------|-------------|
| `/` | - | 重定向到 `/login` | 否 |
| `/login` | 登录页 | 用户登录 | 否 |
| `/404` | 404页面 | 页面未找到 | 否 |
| `/dashboard` | 仪表盘 | 数据概览 | 是 |
| `/accounts` | 账户列表 | 账户管理列表 | 是 |
| `/accounts/create` | 创建账户 | 新建账户 | 是 |
| `/accounts/:id` | 账户详情 | 账户详细信息 | 是 |
| `/transactions/create` | 复式记账 | 创建交易分录 | 是 |
| `/transactions` | 交易列表 | 交易明细列表 | 是 |
| `/transactions/:id` | 交易详情 | 交易详细信息 | 是 |
| `*` | - | 重定向到 `/404` | - |

## Mock 接口列表

### 账户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/account/create` | 创建账户 |
| GET | `/api/account/:accountId` | 查询账户详情 |
| POST | `/api/account/list` | 账户列表（分页） |
| POST | `/api/account/freeze` | 冻结账户 |
| POST | `/api/account/unfreeze` | 解冻账户 |
| POST | `/api/account/close` | 销户 |

### 交易接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/transaction/create` | 复式记账（创建交易） |
| GET | `/api/transaction/:transactionId` | 查询交易详情 |
| GET | `/api/transaction/business/:businessNo` | 按业务号查询交易 |
| POST | `/api/transaction/query` | 交易列表（分页查询） |

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装依赖

```bash
cd core-ledger-web
npm install
```

### 初始化 MSW Service Worker

首次使用 Mock 服务需要初始化 Service Worker：

```bash
npx msw init public/ --save
```

这会在 `public` 目录下生成 `mockServiceWorker.js` 文件。

### 启动开发服务器

```bash
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`，Mock 服务会自动启用。

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

### 预览生产构建

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

## Mock 服务说明

本项目使用 [MSW (Mock Service Worker)](https://mswjs.io/) 进行接口模拟。

### 工作原理

MSW 通过 Service Worker 拦截浏览器发出的 HTTP 请求，并返回预设的 Mock 数据。这种方式的优势：

1. **真实网络请求**：请求会真实地发送到网络层面，只是被 Service Worker 拦截
2. **无侵入性**：不需要修改业务代码，生产环境不会包含 Mock 代码
3. **浏览器开发者工具可见**：可以在 Network 面板中看到完整的请求/响应
4. **数据持久化**：Mock 数据在运行时保存在内存中，支持 CRUD 操作

### 启用/禁用 Mock

Mock 服务仅在开发环境自动启用，通过 `import.meta.env.DEV` 判断。

如需临时禁用 Mock 服务，可以：

1. 修改 `src/main.tsx` 中的 `bootstrap` 函数，注释掉 `enableMocking()` 调用
2. 或者在浏览器控制台执行 `worker.stop()`

### Mock 数据结构

Mock 数据集中管理在 `src/mock/db.ts` 中，包含：

- 10 条预置账户数据（ACC001 - ACC010）
- 6 条预置交易数据（TXN001 - TXN006）
- 4 条操作历史数据（OP001 - OP004）
- 各种 ID 生成器和描述转换函数

所有 Mock 数据与各页面的 `mockData.ts` 保持一致。

## 开发指南

### 新增页面

1. 在 `src/pages/` 下创建页面组件
2. 在 `src/App.tsx` 中添加路由配置
3. 如需登录权限，将路由放在 `MainLayout` 包裹的路由组内

### 新增 Mock 接口

1. 在 `src/mock/handlers/` 下创建对应的 handler 文件
2. 在 `src/mock/index.ts` 中合并 handler
3. 如果需要新的数据类型，在 `src/mock/db.ts` 中添加

### API 调用规范

1. 接口定义在 `src/api/` 目录下
2. 使用 `@tanstack/react-query` 进行数据管理
3. 类型定义集中在 `src/api/types.ts`

## 登录说明

开发环境可使用以下信息登录：

- 用户名：`admin`
- 密码：`123456`

登录状态通过 Zustand 管理，存储在 localStorage 中。

## 常见问题

### Q: Mock 服务不生效？

A: 请检查：
1. 确认已执行 `npx msw init public/ --save`
2. 浏览器控制台是否有 `[MSW] Mock服务已启动` 日志
3. 浏览器开发者工具 Application 面板中 Service Worker 是否激活

### Q: 如何对接真实后端？

A: 
1. 移除或注释 `src/main.tsx` 中的 `enableMocking()` 调用
2. 在 `vite.config.ts` 中配置代理，或者修改 `src/api/request.ts` 中的 `baseURL`

### Q: 页面路由跳转不生效？

A: 检查 `src/App.tsx` 中的路由配置，确保路径正确。嵌套路由需要使用 `<Outlet />` 渲染子路由。
