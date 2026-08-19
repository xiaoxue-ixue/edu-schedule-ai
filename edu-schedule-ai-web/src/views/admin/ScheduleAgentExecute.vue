<template>
  <div class="page-container">
    <!-- 任务选择区 -->
    <el-card shadow="never" class="mb16">
      <div class="select-row">
        <span class="label">选择排课任务：</span>
        <el-select
          v-model="selectedTaskId"
          placeholder="请选择排课任务"
          filterable
          style="width: 360px"
          @change="onTaskChange"
        >
          <el-option
            v-for="t in taskOptions"
            :key="t.id"
            :label="t.name + '（' + (t.termName || '—') + '）'"
            :value="t.id"
          />
        </el-select>
        <el-button
          type="primary"
          :icon="Cpu"
          :disabled="!selectedTaskId"
          :loading="executing"
          @click="handleExecute"
        >{{ executing ? '执行中...' : '执行 Agent 排课' }}</el-button>
        <el-button :icon="RefreshLeft" :disabled="!selectedTaskId" @click="refreshTaskInfo">刷新任务状态</el-button>
      </div>
      <div v-if="currentTask" class="task-info">
        <div><span class="info-label">任务名称：</span>{{ currentTask.name }}</div>
        <div><span class="info-label">学期：</span>{{ currentTask.termName || '—' }}</div>
        <div><span class="info-label">策略：</span>{{ currentTask.strategy || 'AGENT' }}</div>
        <div><span class="info-label">状态：</span>
          <el-tag :type="statusType(currentTask.status)" effect="plain">{{ statusText(currentTask.status) }}</el-tag>
        </div>
        <div><span class="info-label">进度：</span>
          <el-progress :percentage="currentTask.progress || 0" style="width: 220px" />
        </div>
      </div>
    </el-card>

    <!-- 执行日志 + 排课结果 -->
    <el-row :gutter="16">
      <!-- 左：执行日志 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><List /></el-icon>
              <span>Agent 执行日志</span>
              <div v-if="executing" class="status-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>排课中...</span>
              </div>
              <div v-else-if="logLines.length" class="status-done">
                <el-icon color="#67c23a"><CircleCheck /></el-icon>
                <span>已完成</span>
              </div>
            </div>
          </template>
          <div class="log-box" ref="logBoxRef">
            <div v-for="(line, i) in logLines" :key="i" :class="['log-line', line.level]">
              <span class="log-time">{{ line.time }}</span>
              <span class="log-level">{{ line.level }}</span>
              <span class="log-msg">{{ line.msg }}</span>
            </div>
            <div v-if="!logLines.length" class="log-empty">
              还没有日志，请选择任务并点击【执行 Agent 排课】。
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右：排课明细 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon><Calendar /></el-icon>
              <span>本次排课明细（共 {{ detailList.length }} 条）</span>
            </div>
          </template>
          <div class="detail-table-wrapper">
            <el-table
              v-loading="detailLoading"
              :data="detailList"
              stripe
              border
              size="small"
              style="width: 100%"
              max-height="520"
            >
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="courseName" label="课程" min-width="120" show-overflow-tooltip />
              <el-table-column prop="teacherName" label="教师" width="90" show-overflow-tooltip />
              <el-table-column prop="className" label="班级" width="90" show-overflow-tooltip />
              <el-table-column prop="roomName" label="教室" width="90" show-overflow-tooltip />
              <el-table-column label="时间" width="110" align="center">
                <template #default="{ row }">
                  {{ weekText(row.dayOfWeek) }} {{ row.slotName || '—' }}
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="detail-footer">
            <el-button type="primary" :disabled="!selectedTaskId || detailLoading" :icon="Refresh" @click="loadDetailList">
              刷新明细
            </el-button>
            <el-button type="success" :disabled="!detailList.length" :icon="Search" @click="goToDetailPage">
              前往 排课明细 页面查看全部
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Cpu, RefreshLeft, List, Loading, CircleCheck, Calendar, Refresh, Search
} from '@element-plus/icons-vue'
import { scheduleTaskApi, scheduleDetailApi, executeScheduleAgent } from '@/api/schedule'

const router = useRouter()

// ========== 任务选择 ==========
const taskOptions = ref([])
const selectedTaskId = ref(null)
const currentTask = ref(null)

// ========== 日志 ==========
const logLines = reactive([])
const executing = ref(false)
const logBoxRef = ref(null)

// ========== 排课明细 ==========
const detailList = ref([])
const detailLoading = ref(false)

const weekMap = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
function weekText(d) {
  return weekMap[d] || d || '—'
}

function statusType(s) {
  const map = { PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'info' }
  return map[s] || 'info'
}
function statusText(s) {
  const map = { PENDING: '待执行', RUNNING: '执行中', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消' }
  return map[s] || s || '待执行'
}

function addLog(level, msg) {
  const now = new Date()
  const time = now.toTimeString().slice(0, 8) + '.' + String(now.getMilliseconds()).padStart(3, '0')
  logLines.push({ time, level, msg })
  nextTick(() => {
    if (logBoxRef.value) {
      logBoxRef.value.scrollTop = logBoxRef.value.scrollHeight
    }
  })
}

async function loadTaskOptions() {
  try {
    const res = await scheduleTaskApi.list({ page: 1, size: 100 })
    const data = res.data || {}
    taskOptions.value = data.list || data.records || []
  } catch (e) {
    ElMessage.error('加载排课任务失败：' + (e.message || e))
  }
}

function onTaskChange(id) {
  currentTask.value = taskOptions.value.find((t) => t.id === id) || null
  loadDetailList()
}

function refreshTaskInfo() {
  loadTaskOptions().then(() => {
    if (selectedTaskId.value) {
      currentTask.value = taskOptions.value.find((t) => t.id === selectedTaskId.value) || null
    }
    loadDetailList()
  })
}

async function loadDetailList() {
  if (!selectedTaskId.value) {
    detailList.value = []
    return
  }
  detailLoading.value = true
  try {
    const res = await scheduleDetailApi.list({ page: 1, size: 500, taskId: selectedTaskId.value })
    const data = res.data
    if (Array.isArray(data)) {
      detailList.value = data
    } else {
      detailList.value = data?.list || data?.records || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    detailLoading.value = false
  }
}

function goToDetailPage() {
  router.push('/admin/schedule-detail')
}

async function handleExecute() {
  if (!selectedTaskId.value) {
    ElMessage.warning('请先选择排课任务')
    return
  }
  ElMessageBox.confirm(
    '确定要启动 Agent 自动排课吗？执行过程中会实时输出日志。',
    '确认启动',
    { confirmButtonText: '启动', cancelButtonText: '取消', type: 'info' }
  ).then(async () => {
    logLines.length = 0
    executing.value = true
    addLog('INFO', '▶ 开始调用 Agent 排课接口（taskId=' + selectedTaskId.value + '）...')

    try {
      const res = await executeScheduleAgent(selectedTaskId.value)
      const body = res || {}
      const logs = body.data?.logs || body.logs || []
      const message = body.data?.message || body.message

      if (Array.isArray(logs) && logs.length) {
        logs.forEach((l) => {
          const lv = typeof l === 'object' ? (l.level || l.type || 'INFO') : 'INFO'
          const msg = typeof l === 'object' ? (l.message || l.msg || JSON.stringify(l)) : String(l)
          addLog(lv, msg)
        })
      } else if (message) {
        addLog('INFO', message)
      } else {
        addLog('SUCCESS', '✅ Agent 已返回。可点击【刷新明细】查看生成的排课记录。')
      }
      ElMessage.success('Agent 排课已完成')
    } catch (e) {
      const msg = e?.response?.data?.message || e?.message || String(e)
      addLog('ERROR', '❌ 执行失败：' + msg)
      ElMessage.error('执行失败：' + msg)
    } finally {
      executing.value = false
      refreshTaskInfo()
    }
  }).catch(() => {})
}

onMounted(() => {
  loadTaskOptions()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.select-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.select-row .label { font-size: 14px; color: #303133; font-weight: 500; }
.task-info { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px 20px; margin-top: 12px; padding: 10px 14px; background: #f5f7fa; border-radius: 6px; }
.info-label { color: #606266; font-size: 13px; }

.card-header { display: flex; align-items: center; gap: 6px; }
.card-header .status-loading,
.card-header .status-done { margin-left: auto; display: flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; }
.card-header .status-done { color: #67c23a; }

.log-box {
  height: 520px;
  overflow-y: auto;
  background: #1e1e1e;
  color: #dcdcdc;
  padding: 10px 12px;
  border-radius: 4px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.7;
}
.log-line { display: flex; gap: 8px; }
.log-time { color: #858585; flex-shrink: 0; }
.log-level { flex-shrink: 0; min-width: 44px; font-weight: 600; }
.log-line.INFO  .log-level { color: #4fc3f7; }
.log-line.DEBUG .log-level { color: #9e9e9e; }
.log-line.WARN  .log-level { color: #ffb74d; }
.log-line.ERROR .log-level { color: #ef5350; }
.log-line.SUCCESS .log-level { color: #66bb6a; }
.log-empty { color: #6e6e6e; text-align: center; padding: 40px 0; }

.detail-table-wrapper { margin-bottom: 12px; }
.detail-footer { display: flex; justify-content: flex-end; gap: 10px; padding-top: 8px; border-top: 1px solid #ebeef5; }
</style>