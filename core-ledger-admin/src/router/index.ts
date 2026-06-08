import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运营概览', icon: 'dashboard' }
      },
      {
        path: 'accounts',
        name: 'AccountList',
        component: () => import('@/views/account/list.vue'),
        meta: { title: '账户查询', icon: 'user' }
      },
      {
        path: 'accounts/:id',
        name: 'AccountDetail',
        component: () => import('@/views/account/detail.vue'),
        meta: { title: '账户详情', icon: 'user', hidden: true }
      },
      {
        path: 'transactions',
        name: 'TransactionList',
        component: () => import('@/views/transaction/list.vue'),
        meta: { title: '流水查询', icon: 'swap' }
      },
      {
        path: 'transactions/:id',
        name: 'TransactionDetail',
        component: () => import('@/views/transaction/detail.vue'),
        meta: { title: '交易详情', icon: 'swap', hidden: true }
      },
      {
        path: 'hot-accounts',
        name: 'HotAccountList',
        component: () => import('@/views/hot-account/list.vue'),
        meta: { title: '热点账户管理', icon: 'fire' }
      },
      {
        path: 'adjust-applications',
        name: 'AdjustApplicationList',
        component: () => import('@/views/adjust/list.vue'),
        meta: { title: '人工调账申请', icon: 'edit' }
      },
      {
        path: 'adjust-applications/:id',
        name: 'AdjustApplicationDetail',
        component: () => import('@/views/adjust/detail.vue'),
        meta: { title: '调账申请详情', icon: 'edit', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  
  if (requiresAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    if (to.meta.title) {
      document.title = `${to.meta.title} - 核心账务运营平台`
    }
    next()
  }
})

export default router
