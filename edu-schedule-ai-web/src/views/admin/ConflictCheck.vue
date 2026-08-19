<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="form">
          <el-form-item label="学期">
            <el-select v-model="form.termId" placeholder="请选择学期" filterable style="width: 260px">
              <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="排课任务">
            <el-select v-model="form.taskId" placeholder="全部任务" clearable filterable style="width: 300px">
              <el-option v-for="t in taskOptions" :key="t.id" :label="t.taskName || ('排课任务#' + t.id)" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" :loading="checking" @click="handleCheck">🔍 执行冲突检测</el-button>
            <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="stat-row" v-if="stats">
        <el-card class="stat-card" shadow="hover" :body-style="{ padding: '16px 20px' }">
          <div class="stat-label">检测总条数</div>
          <div class="stat-value">{{ stats.total }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover" :body-style="{ padding: '16px 20px' }">
          <div class="stat-label">存在冲突的课程</div>
          <div class="stat-value danger">{{ stats.conflictCount }}</div>
        </el-card>
        <el-card class="stat-card" shadow="hover" :body-style="{ padding: '16px 20px' }">
          <div class="stat-label">通过率</div>
          <div class="stat-value success">{{ passRate }}%</div>
        </el-card>
      </div>

      <div v-if="checked">
        <el-alert
          v-if="stats.conflictCount === 0"
          type="success"
          show-icon
          :closable="false"
          title="✅ 太棒了！本学期排课没有任何冲突"
          description="所有课程均已通过教师时间、班级时间、教室占用、约束规则四层检测。"
          class="mb-16"
        />
        <el-alert
          v-else
          type="warning"
          show-icon
          :closable="false"
          :title="`⚠️ 检测到 ${stats.conflictCount} 条冲突课程，共 ${totalConflicts} 处问题`"
          description="请在下方表格查看详情并逐条处理。"
          class="mb-16"
        />

        <el-tabs v-model="activeTab" class="conflict-tabs">
          <el-tab-pane label="全部" name="all">
            <ConflictTable :list="conflicts" />
          </el-tab-pane>
          <el-tab-pane label="时间冲突（教师）" name="TEACHER_TIME">
            <ConflictTable :list="filterBy('TEACHER_TIME')" />
          </el-tab-pane>
          <el-tab-pane label="时间冲突（班级）" name="CLASS_TIME">
            <ConflictTable :list="filterBy('CLASS_TIME')" />
          </el-tab-pane>
          <el-tab-pane label="时间冲突（教室）" name="ROOM_TIME">
            <ConflictTable :list="filterBy('ROOM_TIME')" />
          </el-tab-pane>
          <el-tab-pane label="约束违反" name="CONSTRAINT">
            <ConflictTable :list="filterByConstraint()" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineComponent, h } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { termApi, scheduleTaskApi, conflictCheckApi, checkConflict } from '@/api/schedule'

const termOptions = ref([])
const taskOptions = ref([])
const form = reactive({ termId: null, taskId: null })
const checking = ref(false)
const checked = ref(false)
const stats = ref(null)
const conflicts = ref([])
const activeTab = ref('all')

const ConflictTable = defineComponent({
  name: 'ConflictTable',
  props: { list: { type: Array, default: () => [] } },
  setup(props) {
    const weekMap = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
    return () => props.list.length === 0
      ? h('el-empty', { description: '无数据' })
      : h('el-table', { data: props.list, stripe: true, border: true, style: 'width: 100%' }, () => [
          h('el-table-column', { type: 'index', label: '#', width: 60, align: 'center' }),
          h('el-table-column', { prop: 'courseName', label: '课程', minWidth: 120 }),
          h('el-table-column', { prop: 'teacherName', label: '教师', minWidth: 100 }),
          h('el-table-column', { prop: 'className', label: '班级', minWidth: 120 }),
          h('el-table-column', { prop: 'roomName', label: '教室', minWidth: 120 }),
          h('el-table-column', { label: '时间', minWidth: 160, formatter: (r) =>
            `${weekMap[r.row.dayOfWeek] || r.row.dayOfWeek || '—'} 第${r.row.slotId}节` }),
          h('el-table-column', { label: '冲突类型', width: 160 }, {
            default: ({ row }) => h('el-tag', {
              type: ['TEACHER_TIME','CLASS_TIME','ROOM_TIME'].includes(row.conflictType) ? 'danger' : 'warning',
              size: 'small'
            }, () => typeText(row.conflictType))
          }),
          h('el-table-column', { prop: 'reason', label: '冲突详情', minWidth: 280 })
        ])
  }
})

function typeText(t) {
  return {
    TEACHER_TIME: '教师撞课',
    CLASS_TIME: '班级撞课',
    ROOM_TIME: '教室撞课',
    TEACHER_UNAVAILABLE: '教师不可用',
    ROOM_UNAVAILABLE: '教室不可用',
    ROOM_CAPACITY: '容量不足',
    OTHER: '其他'
  }[t] || t
}

const passRate = computed(() => {
  if (!stats.value) return 0
  const { total, conflictCount } = stats.value
  if (total === 0) return 100
  return Math.round(((total - conflictCount) / total) * 100)
})
const totalConflicts = computed(() => conflicts.value.length)

function filterBy(type) {
  return conflicts.value.filter(c => c.conflictType === type)
}
function filterByConstraint() {
  return conflicts.value.filter(c => ['TEACHER_UNAVAILABLE','ROOM_UNAVAILABLE','ROOM_CAPACITY','OTHER'].includes(c.conflictType))
}

async function loadTerms() {
  try {
    const res = await termApi.list({ status: 1 })
    const d = res.data || []
    termOptions.value = Array.isArray(d) ? d : (d.list || [])
    if (termOptions.value.length > 0) {
      form.termId = termOptions.value[0].id
    }
  } catch (e) { console.error(e) }
}
async function loadTasks() {
  try {
    const res = await scheduleTaskApi.list({ size: 100 })
    const d = res.data || []
    taskOptions.value = Array.isArray(d) ? d : (d.list || d.records || [])
  } catch (e) { console.error(e) }
}

async function handleCheck() {
  if (!form.termId) {
    ElMessage.warning('请先选择学期')
    return
  }
  checking.value = true
  checked.value = false
  try {
    const body = { termId: form.termId }
    if (form.taskId) body.taskId = form.taskId
    const res = await checkConflict(body)
    const r = res.data || {}
    stats.value = { total: r.total || 0, conflictCount: r.conflictCount || 0 }
    const detailRes = await request({
      url: '/schedule-detail', method: 'get',
      params: { termId: form.termId, taskId: form.taskId || undefined }
    })
    const details = (detailRes.data || [])
    const detailMap = new Map(details.map(d => [d.id, d]))
    const confRes = await conflictCheckApi.list({ termId: form.termId, taskId: form.taskId || undefined })
    const confList = Array.isArray(confRes.data) ? confRes.data : (confRes.data?.list || [])
    conflicts.value = confList.map(c => {
      const d = detailMap.get(c.detailId) || {}
      return {
        detailId: c.detailId,
        conflictType: c.conflictType,
        reason: c.description,
        status: c.status,
        courseName: d.courseName || ('课程#' + d.courseId),
        teacherName: d.teacherName || ('教师#' + d.teacherId),
        className: d.className || ('班级#' + d.classId),
        roomName: d.roomName || ('教室#' + d.roomId),
        dayOfWeek: d.dayOfWeek,
        slotId: d.slotId
      }
    })
    checked.value = true
    if (stats.value.conflictCount === 0) {
      ElMessage.success('✅ 检测完成！没有任何冲突')
    } else {
      ElMessage.warning(`⚠️ 检测完成：共 ${stats.value.conflictCount} 条冲突课程`)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('检测失败：' + (e.message || '未知错误'))
  } finally {
    checking.value = false
  }
}

function handleReset() {
  form.termId = termOptions.value[0]?.id || null
  form.taskId = null
  checked.value = false
  stats.value = null
  conflicts.value = []
}

onMounted(async () => {
  await Promise.all([loadTerms(), loadTasks()])
  if (form.termId) {
    await handleCheck()
  }
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.stat-row { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-card { flex: 1; border-radius: 10px; min-height: 84px; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 26px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-value.danger { color: #f56c6c; }
.stat-value.success { color: #67c23a; }
.mb-16 { margin-bottom: 16px; }
.conflict-tabs { margin-top: 4px; }
</style>