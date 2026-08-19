<template>
  <div class="page-container">
    <div class="welcome">
      <h2 class="page-title">教务运行首页</h2>
      <p class="welcome-sub">欢迎回来，{{ userStore.realName }}，这里是教务运行数据概览。</p>
    </div>

    <el-row :gutter="16">
      <el-col :xs="12" :sm="12" :md="6" v-for="(card, i) in statCards" :key="i">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-row">
            <div class="stat-icon" :style="{ background: card.bg }">
              <el-icon :size="26" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <el-statistic :title="card.title" :value="card.value" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">快捷入口</span>
          </template>
          <div class="quick-grid">
            <div
              v-for="q in quickEntries"
              :key="q.path"
              class="quick-item"
              @click="router.push(q.path)"
            >
              <el-icon :size="22" :color="q.color"><component :is="q.icon" /></el-icon>
              <span>{{ q.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">最近动态</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(t, i) in timeline"
              :key="i"
              :timestamp="t.time"
              :type="t.type"
            >
              {{ t.content }}
            </el-timeline-item>
          </el-timeline>
          <div v-if="timeline.length === 0" style="text-align:center;color:#909399;padding:20px">
            暂无动态
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import {
  Calendar, Cpu, WarningFilled, TrendCharts, DataBoard, Tools, Bell, Document
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const statCards = ref([
  { title: '排课任务数', value: 0, icon: Calendar, bg: '#409eff' },
  { title: 'AI调用数', value: 0, icon: Cpu, bg: '#67c23a' },
  { title: '待处理冲突', value: 0, icon: WarningFilled, bg: '#f56c6c' },
  { title: '质量分析数', value: 0, icon: TrendCharts, bg: '#e6a23c' }
])

const quickEntries = [
  { title: '排课任务', path: '/admin/schedule-task', icon: Calendar, color: '#409eff' },
  { title: '基础数据', path: '/admin/term', icon: DataBoard, color: '#67c23a' },
  { title: 'AI配置', path: '/admin/ai-model-config', icon: Cpu, color: '#9c27b0' },
  { title: '约束配置', path: '/admin/teacher-available-time', icon: Tools, color: '#e6a23c' },
  { title: '公告管理', path: '/admin/announcement', icon: Bell, color: '#f56c6c' },
  { title: '运行日志', path: '/admin/run-log', icon: Document, color: '#909399' }
]

const timeline = ref([])

onMounted(async () => {
  try {
    const res = await request({ url: '/dashboard/stats', method: 'get' })
    if (res.code === 200 && res.data) {
      const d = res.data
      statCards.value[0].value = Number(d.taskTotal || 0)
      statCards.value[1].value = Number(d.aiCallTotal || 0)
      statCards.value[2].value = Number(d.conflictPending || 0)
      statCards.value[3].value = Number(d.qualityAnalysisTotal || 0)
    }
  } catch (e) {
    console.warn('加载首页统计失败', e)
  }
  try {
    const res = await request({ url: '/dashboard/timeline', method: 'get' })
    if (res.code === 200 && Array.isArray(res.data)) {
      timeline.value = res.data
    }
  } catch (e) {
    console.warn('加载最近动态失败', e)
  }
})
</script>

<style scoped>
.welcome-sub {
  color: #909399;
  font-size: 13px;
  margin-bottom: 16px;
}
.stat-card {
  border-radius: 8px;
  margin-bottom: 8px;
}
.stat-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px 8px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: #606266;
}
.quick-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}
</style>