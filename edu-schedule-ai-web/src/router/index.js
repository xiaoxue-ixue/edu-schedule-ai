import { createRouter, createWebHistory } from 'vue-router'
import { h } from 'vue'
import { useUserStore } from '@/store/user'
import PlaceholderPage from '@/components/PlaceholderPage.vue'

function placeholder(title, apiPath = '') {
  return {
    name: `Placeholder_${title}`,
    render() {
      return h(PlaceholderPage, { title, apiPath })
    }
  }
}

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { public: true }
  },
  // ============ 管理员 ============
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { roles: ['admin'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '教务首页', group: '系统管理' }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/admin/UserList.vue'),
        meta: { title: '用户信息', group: '系统管理' }
      },
      // 教务基础数据
      { path: 'term', name: 'TermList', component: () => import('@/views/admin/TermList.vue'), meta: { title: '学期', group: '教务基础数据' } },
      { path: 'campus', name: 'CampusList', component: () => import('@/views/admin/CampusList.vue'), meta: { title: '校区', group: '教务基础数据' } },
      { path: 'teacher', name: 'TeacherList', component: () => import('@/views/admin/TeacherList.vue'), meta: { title: '教师', group: '教务基础数据' } },
      { path: 'clazz', name: 'ClazzList', component: () => import('@/views/admin/ClazzList.vue'), meta: { title: '班级', group: '教务基础数据' } },
      { path: 'course', name: 'CourseList', component: () => import('@/views/admin/CourseList.vue'), meta: { title: '课程', group: '教务基础数据' } },
      { path: 'room', name: 'RoomList', component: () => import('@/views/admin/RoomList.vue'), meta: { title: '教室', group: '教务基础数据' } },
      { path: 'time-slot', name: 'TimeSlotList', component: () => import('@/views/admin/TimeSlotList.vue'), meta: { title: '时间段', group: '教务基础数据' } },
      // 排课约束配置
      { path: 'teacher-available-time', name: 'TeacherAvailableTime', component: () => import('@/views/admin/TeacherAvailableTime.vue'), meta: { title: '教师可用时间', group: '排课约束配置' } },
      { path: 'room-available-time', name: 'RoomAvailableTime', component: () => import('@/views/admin/RoomAvailableTime.vue'), meta: { title: '教室可用时间', group: '排课约束配置' } },
      { path: 'schedule-rule', name: 'ScheduleRule', component: () => import('@/views/admin/ScheduleRule.vue'), meta: { title: '排课规则', group: '排课约束配置' } },
      // AI 能力配置
      { path: 'ai-model-config', name: 'AiModelConfig', component: () => import('@/views/admin/AiModelConfig.vue'), meta: { title: 'AI模型配置', group: 'AI能力配置' } },
      { path: 'prompt-template', name: 'PromptTemplate', component: () => import('@/views/admin/PromptTemplate.vue'), meta: { title: 'Prompt模板', group: 'AI能力配置' } },
      { path: 'ai-call-log', name: 'AiCallLog', component: () => import('@/views/admin/AiCallLog.vue'), meta: { title: 'AI调用日志', group: 'AI能力配置' } },
      { path: 'function-tool', name: 'FunctionTool', component: () => import('@/views/admin/FunctionTool.vue'), meta: { title: 'Function工具', group: 'AI能力配置' } },
      // 智能排课
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/admin/Knowledge.vue'), meta: { title: '教务知识库', group: '智能排课' } },
      { path: 'schedule-task', name: 'ScheduleTask', component: () => import('@/views/admin/ScheduleTask.vue'), meta: { title: '智能排课任务', group: '智能排课' } },
      // ★ Agent 排课执行：真实组件，路径 agent-execute
      { path: 'agent-execute', name: 'ScheduleAgentExecute', component: () => import('@/views/admin/ScheduleAgentExecute.vue'), meta: { title: 'Agent排课执行', group: '智能排课' } },
      { path: 'schedule-detail', name: 'ScheduleDetail', component: () => import('@/views/admin/ScheduleDetail.vue'), meta: { title: '排课明细', group: '智能排课' } },
      { path: 'conflict-check', name: 'ConflictCheck', component: () => import('@/views/admin/ConflictCheck.vue'), meta: { title: '排课冲突检测', group: '智能排课' } },
      { path: 'announcement', name: 'Announcement', component: () => import('@/views/admin/Announcement.vue'), meta: { title: '公告管理', group: '智能排课' } },
      { path: 'schedule-version', name: 'ScheduleVersion', component: () => import('@/views/admin/ScheduleVersion.vue'), meta: { title: '课表版本与发布', group: '智能排课' } },
      // 教学质量
      { path: 'teaching-evaluation', name: 'TeachingEvaluation', component: () => import('@/views/admin/TeachingEvaluation.vue'), meta: { title: '教学评价', group: '教学质量' } },
      { path: 'ai-quality-analysis', name: 'AiQualityAnalysis', component: () => import('@/views/admin/AiQualityAnalysis.vue'), meta: { title: 'AI教学质量分析', group: '教学质量' } },
      // 运行观测
      { path: 'run-log', name: 'RunLog', component: () => import('@/views/admin/RunLog.vue'), meta: { title: '运行日志', group: '运行观测' } },
      // 通用
      { path: 'profile', name: 'AdminProfile', component: () => import('@/views/Profile.vue'), meta: { title: '个人信息', group: '其他' } },
      { path: 'password', name: 'AdminPassword', component: () => import('@/views/ChangePassword.vue'), meta: { title: '修改密码', group: '其他' } }
    ]
  },
  // ============ 教师 ============
  {
    path: '/teacher',
    component: () => import('@/layout/TeacherLayout.vue'),
    redirect: '/teacher/my-schedule',
    meta: { roles: ['teacher'] },
    children: [
      { path: 'my-schedule', name: 'TeacherMySchedule', component: () => import('@/views/teacher/MySchedule.vue'), meta: { title: '我的课表' } },
      { path: 'my-evaluation', name: 'TeacherMyEvaluation', component: () => import('@/views/teacher/MyEvaluation.vue'), meta: { title: '我收到的评价' } },
      { path: 'my-available-time', name: 'TeacherMyAvailableTime', component: () => import('@/views/teacher/MyAvailableTime.vue'), meta: { title: '我的可用时间' } },
      { path: 'announcement', name: 'TeacherAnnouncement', component: () => import('@/views/teacher/Announcement.vue'), meta: { title: '教务公告' } },
      { path: 'profile', name: 'TeacherProfile', component: () => import('@/views/Profile.vue'), meta: { title: '个人信息' } },
      { path: 'password', name: 'TeacherPassword', component: () => import('@/views/ChangePassword.vue'), meta: { title: '修改密码' } }
    ]
  },
  // ============ 学生 ============
  {
    path: '/student',
    component: () => import('@/layout/StudentLayout.vue'),
    redirect: '/student/schedule',
    meta: { roles: ['student'] },
    children: [
      { path: 'schedule', name: 'StudentScheduleHome', component: () => import('@/views/student/ScheduleHome.vue'), meta: { title: '课表首页' } },
      { path: 'announcement', name: 'StudentAnnouncement', component: () => import('@/views/student/Announcement.vue'), meta: { title: '教务公告' } },
      { path: 'evaluation', name: 'StudentEvaluation', component: () => import('@/views/student/Evaluation.vue'), meta: { title: '教学评教' } },
      { path: 'profile', name: 'StudentProfile', component: () => import('@/views/Profile.vue'), meta: { title: '个人信息' } },
      { path: 'password', name: 'StudentPassword', component: () => import('@/views/ChangePassword.vue'), meta: { title: '修改密码' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export function roleHome(role) {
  if (role === 'admin') return '/admin/dashboard'
  if (role === 'teacher') return '/teacher/my-schedule'
  if (role === 'student') return '/student/schedule'
  return '/login'
}

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public || to.path === '/login') {
    if (to.path === '/login' && userStore.isLoggedIn) {
      return next(roleHome(userStore.role))
    }
    return next()
  }
  if (!userStore.isLoggedIn) {
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
  const roles = to.meta.roles
  if (roles && roles.length && !roles.includes(userStore.role)) {
    return next(roleHome(userStore.role))
  }
  next()
})

export default router