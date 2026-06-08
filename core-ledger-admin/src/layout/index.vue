<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      :trigger="null"
      width="240"
      theme="dark"
    >
      <div class="logo">
        <bank-outlined style="font-size: 24px; color: #fff" />
        <span v-if="!collapsed" class="logo-text">核心账务运营</span>
      </div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :open-keys="openKeys"
        @update:openKeys="onOpenKeysChange"
      >
        <a-menu-item
          v-for="item in menuItems"
          :key="item.path"
          @click="handleMenuClick(item)"
        >
          <component :is="item.icon" />
          <span>{{ item.title }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <a-button
            type="text"
            @click="collapsed = !collapsed"
            class="collapse-btn"
          >
            <menu-unfold-outlined v-if="collapsed" />
            <menu-fold-outlined v-else />
          </a-button>
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item>首页</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-right">
          <a-dropdown>
            <div class="user-info">
              <a-avatar size="small" style="background-color: #1890ff">
                <user-outlined />
              </a-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '用户' }}</span>
              <down-outlined />
            </div>
            <template #overlay>
              <a-menu @click="handleUserMenuClick">
                <a-menu-item key="profile">
                  <user-outlined />
                  个人中心
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <logout-outlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>

      <a-layout-footer class="footer">
        核心账务运营平台 ©2024 Created by Core Ledger Team
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  UserOutlined,
  SwapOutlined,
  FireOutlined,
  EditOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  DownOutlined,
  LogoutOutlined,
  BankOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/store/user'
import type { MenuProps } from 'ant-design-vue'

const collapsed = ref(false)
const openKeys = ref<string[]>([])
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { path: '/dashboard', title: '运营概览', icon: DashboardOutlined },
  { path: '/accounts', title: '账户查询', icon: UserOutlined },
  { path: '/transactions', title: '流水查询', icon: SwapOutlined },
  { path: '/hot-accounts', title: '热点账户管理', icon: FireOutlined },
  { path: '/adjust-applications', title: '人工调账申请', icon: EditOutlined }
]

const selectedKeys = computed(() => [route.path])
const currentTitle = computed(() => {
  const item = menuItems.find(m => route.path.startsWith(m.path))
  return item?.title || ''
})

const handleMenuClick = (item: { path: string }) => {
  router.push(item.path)
}

const onOpenKeysChange = (keys: string[]) => {
  openKeys.value = keys
}

const handleUserMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  const path = route.path
  const item = menuItems.find(m => path.startsWith(m.path))
  if (item) {
    openKeys.value = [item.path]
  }
})
</script>

<style scoped lang="less">
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  
  .logo-text {
    white-space: nowrap;
  }
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  .collapse-btn {
    font-size: 18px;
  }
  
  .breadcrumb {
    margin: 0;
  }
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 8px 12px;
      border-radius: 4px;
      transition: background-color 0.3s;
      
      &:hover {
        background-color: #f5f5f5;
      }
      
      .username {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.85);
      }
    }
  }
}

.content {
  padding: 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 64px - 70px);
}

.footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  background: #fff;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
