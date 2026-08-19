<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <el-icon class="logo-icon"><Notebook /></el-icon>
        <span class="logo-text">学生课表</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="aside-menu"
        router
      >
        <el-menu-item index="/student/schedule">
          <el-icon><Calendar /></el-icon>
          <span>课表首页</span>
        </el-menu-item>
        <el-menu-item index="/student/announcement">
          <el-icon><Bell /></el-icon>
          <span>教务公告</span>
        </el-menu-item>
        <el-menu-item index="/student/evaluation">
          <el-icon><EditPen /></el-icon>
          <span>教学评教</span>
        </el-menu-item>
        <el-menu-item index="/student/profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>
        <el-menu-item index="/student/password">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <span class="header-title">{{ currentTitle }}</span>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><UserFilled /></el-icon>
            <span class="username">{{ userStore.realName }}</span>
            <el-tag size="small" type="warning" effect="plain">学生</el-tag>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Notebook, Calendar, Bell, EditPen, User, Lock, UserFilled, ArrowDown
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '学生课表')

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      }).catch(() => {})
  } else if (cmd === 'profile') {
    router.push('/student/profile')
  } else if (cmd === 'password') {
    router.push('/student/password')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.layout-aside { background: #fff; border-right: 1px solid #e6e8eb; }
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  gap: 8px; color: #fff; background: #e6a23c;
}
.logo-icon { font-size: 24px; }
.logo-text { font-size: 16px; font-weight: 600; }
.aside-menu { border-right: none; height: calc(100vh - 60px); }
.layout-header {
  background: #fff; border-bottom: 1px solid #e6e8eb;
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px;
}
.header-title { font-size: 16px; font-weight: 600; }
.user-info { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #303133; }
.username { font-size: 14px; }
.arrow { font-size: 12px; color: #909399; }
.layout-main { background: #f5f7fa; padding: 16px; overflow-y: auto; }
</style>
