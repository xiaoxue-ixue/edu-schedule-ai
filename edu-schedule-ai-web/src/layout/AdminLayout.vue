<template>
  <el-container class="layout-container">
    <el-aside width="230px" class="layout-aside">
      <div class="logo">
        <el-icon class="logo-icon"><School /></el-icon>
        <span class="logo-text">智能教务管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :default-openeds="openedMenus"
        class="aside-menu"
        router
        unique-opened
      >
        <el-sub-menu v-for="group in menuGroups" :key="group.name" :index="group.name">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.name }}</span>
          </template>
          <el-menu-item
            v-for="item in group.children"
            :key="item.path"
            :index="item.path"
          >
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="其他">
          <template #title>
            <el-icon><User /></el-icon>
            <span>个人设置</span>
          </template>
          <el-menu-item index="/admin/profile">个人信息</el-menu-item>
          <el-menu-item index="/admin/password">修改密码</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="header-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="username">{{ userStore.realName }}</span>
              <el-tag size="small" type="primary" effect="plain">管理员</el-tag>
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
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  School, User, UserFilled, ArrowDown,
  Setting, DataBoard, Tools, Cpu, Calendar, TrendCharts, Monitor
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuGroups = [
  {
    name: '系统管理', icon: Setting,
    children: [
      { path: '/admin/dashboard', title: '教务首页' },
      { path: '/admin/users', title: '用户信息' }
    ]
  },
  {
    name: '教务基础数据', icon: DataBoard,
    children: [
      { path: '/admin/term', title: '学期' },
      { path: '/admin/campus', title: '校区' },
      { path: '/admin/teacher', title: '教师' },
      { path: '/admin/clazz', title: '班级' },
      { path: '/admin/course', title: '课程' },
      { path: '/admin/room', title: '教室' },
      { path: '/admin/time-slot', title: '时间段' }
    ]
  },
  {
    name: '排课约束配置', icon: Tools,
    children: [
      { path: '/admin/teacher-available-time', title: '教师可用时间' },
      { path: '/admin/room-available-time', title: '教室可用时间' },
      { path: '/admin/schedule-rule', title: '排课规则' }
    ]
  },
  {
    name: 'AI能力配置', icon: Cpu,
    children: [
      { path: '/admin/ai-model-config', title: 'AI模型配置' },
      { path: '/admin/prompt-template', title: 'Prompt模板' },
      { path: '/admin/ai-call-log', title: 'AI调用日志' },
      { path: '/admin/function-tool', title: 'Function工具' }
    ]
  },
  {
    name: '智能排课', icon: Calendar,
    children: [
      { path: '/admin/knowledge', title: '教务知识库' },
      { path: '/admin/schedule-task', title: '智能排课任务' },
      // ★ Agent 排课执行菜单路径：agent-execute
      { path: '/admin/agent-execute', title: 'Agent排课执行' },
      { path: '/admin/schedule-detail', title: '排课明细' },
      { path: '/admin/conflict-check', title: '排课冲突检测' },
      { path: '/admin/announcement', title: '公告管理' },
      { path: '/admin/schedule-version', title: '课表版本与发布' }
    ]
  },
  {
    name: '教学质量', icon: TrendCharts,
    children: [
      { path: '/admin/teaching-evaluation', title: '教学评价' },
      { path: '/admin/ai-quality-analysis', title: 'AI教学质量分析' }
    ]
  },
  {
    name: '运行观测', icon: Monitor,
    children: [
      { path: '/admin/run-log', title: '运行日志' }
    ]
  }
]

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '教务管理')

const openedMenus = computed(() => {
  const g = route.meta.group
  return g && g !== '其他' ? [g] : ['其他']
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    }).catch(() => {})
  } else if (cmd === 'profile') {
    router.push('/admin/profile')
  } else if (cmd === 'password') {
    router.push('/admin/password')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background: #fff;
  border-right: 1px solid #e6e8eb;
  overflow-x: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  background: #409eff;
}
.logo-icon {
  font-size: 24px;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
}
.aside-menu {
  border-right: none;
  height: calc(100vh - 60px);
  overflow-y: auto;
}
.layout-header {
  background: #fff;
  border-bottom: 1px solid #e6e8eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
}
.username {
  font-size: 14px;
}
.arrow {
  font-size: 12px;
  color: #909399;
}
.layout-main {
  background: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
}
</style>
