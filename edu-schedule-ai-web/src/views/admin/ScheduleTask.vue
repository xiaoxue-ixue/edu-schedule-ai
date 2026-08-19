<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="search-bar">
          <el-input
            v-model="query.taskName"
            placeholder="搜索任务名称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>
        <el-button type="success" :icon="Plus" @click="handleAdd">新增排课任务</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="termName" label="学期" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160" align="center">
          <template #default="{ row }">
            <el-progress :percentage="calcProgress(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="340" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Cpu" :loading="executingId === row.id" @click="handleExecute(row)">执行Agent排课</el-button>
            <el-button link type="success" :icon="Refresh" @click="handleRefreshStatus(row)">刷新状态</el-button>
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="不填则自动用学期+时间生成（可留空）" />
        </el-form-item>

        <el-form-item label="学期" prop="termId">
          <el-select v-model="form.termId" placeholder="请选择学期" filterable style="width: 100%">
            <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="要排的课程" prop="courseIds">
          <el-select
            v-model="form.courseIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="可多选课程（不选默认排全部）"
            style="width: 100%"
          >
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="排课策略" prop="ruleStrategy">
          <el-select v-model="form.ruleStrategy" placeholder="请选择排课策略" style="width: 100%">
            <el-option label="🤖 智能 Agent 排课（推荐）" value="AGENT" />
            <el-option label="贪心算法" value="GREEDY" />
            <el-option label="遗传算法" value="GA" />
          </el-select>
        </el-form-item>

        <el-form-item label="任务目标" prop="objective">
          <el-input v-model="form.objective" placeholder="例：为本学期课程生成无冲突的排课方案（可留空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行状态对话框 -->
    <el-dialog v-model="logVisible" title="Agent 排课执行状态" width="620px" top="10vh">
      <div class="exec-status">
        <el-icon v-if="isRunning" class="is-loading" color="#409eff"><Loading /></el-icon>
        <el-icon v-else-if="isSuccess" color="#67c23a"><CircleCheck /></el-icon>
        <el-icon v-else-if="isFailed" color="#f56c6c"><CircleClose /></el-icon>
        <span>{{ execStatusText }}</span>
      </div>
      <div class="progress-box" v-if="currentTask">
        <el-progress :percentage="calcProgress(currentTask)" :status="isFailed ? 'exception' : (isSuccess ? 'success' : '')" />
        <p style="margin-top:8px;color:#606266;font-size:13px">
          已排 {{ currentTask?.successCourses || 0 }} / {{ currentTask?.totalCourses || 0 }} 门课
        </p>
      </div>
      <template #footer>
        <el-button @click="logVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="isRunning" @click="loadData">刷新列表</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Cpu, Loading, CircleCheck, CircleClose, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { scheduleTaskApi, executeScheduleAgent } from '@/api/schedule'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ taskName: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogTitle = ref('新增排课任务')
const formRef = ref(null)
const form = reactive({
  id: null,
  taskName: '',
  termId: null,
  courseIds: [],
  ruleStrategy: 'AGENT',
  objective: ''
})
const rules = {
  termId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  ruleStrategy: [{ required: true, message: '请选择排课策略', trigger: 'change' }]
}

const termOptions = ref([])
const courseOptions = ref([])

// ✅ Bug1修复：状态值与后端对齐（SUCCESS / FAIL）
function statusType(s) {
  const map = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAIL: 'danger' }
  return map[s] || 'info'
}
function statusText(s) {
  const map = { PENDING: '待执行', RUNNING: '执行中', SUCCESS: '已完成', FAIL: '失败' }
  return map[s] || s || '待执行'
}

// ✅ Bug2修复：用 successCourses / totalCourses 计算进度
function calcProgress(row) {
  if (!row) return 0
  const total = row.totalCourses || 0
  const success = row.successCourses || 0
  if (total === 0) return 0
  return Math.round((success / total) * 100)
}

// ✅ Bug4修复：轮询状态
const executingId = ref(null)
const logVisible = ref(false)
const currentTask = ref(null)
const pollTimer = ref(null)

const isRunning = computed(() => currentTask.value?.status === 'RUNNING')
const isSuccess = computed(() => currentTask.value?.status === 'SUCCESS')
const isFailed = computed(() => currentTask.value?.status === 'FAIL')
const execStatusText = computed(() => {
  if (isRunning.value) return 'Agent 正在后台排课中，请耐心等待...'
  if (isSuccess.value) return '✅ 排课已完成！可以去【排课明细】查看结果'
  if (isFailed.value) return '❌ 排课失败，请查看后端日志排查原因'
  return '等待中...'
})

async function loadDropdowns() {
  try {
    const [t, cs] = await Promise.all([
      request.get('/term', { params: { page: 1, size: 100 } }),
      request.get('/course', { params: { page: 1, size: 300 } })
    ])
    termOptions.value = (t.data?.list || t.data?.records || t.data || [])
    courseOptions.value = (cs.data?.list || cs.data?.records || cs.data || [])
  } catch (e) {
    console.warn('加载下拉框失败：', e)
  }
}

// ✅ Bug5修复：去掉双重分页，直接用后端返回的分页数据
async function loadData() {
  loading.value = true
  try {
    const res = await scheduleTaskApi.list({ page: query.page, size: query.size, taskName: query.taskName })
    const d = res.data || {}
    tableData.value = d.rows || d.list || d.records || []
    total.value = d.total || 0
  } catch (e) {
    console.error(e)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}
function handleReset() {
  query.taskName = ''
  query.page = 1
  loadData()
}

function handleAdd() {
  dialogTitle.value = '新增排课任务'
  Object.assign(form, { id: null, taskName: '', termId: null, courseIds: [], ruleStrategy: 'AGENT', objective: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑排课任务'
  // ✅ Bug3修复：从 courseScope 反解析 courseIds
  let courseIds = []
  if (row.courseScope && row.courseScope.startsWith('课程ID:')) {
    courseIds = row.courseScope.substring(4).split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
  }
  Object.assign(form, {
    id: row.id,
    taskName: row.taskName || '',
    termId: row.termId || null,
    courseIds,
    ruleStrategy: row.ruleStrategy || 'AGENT',
    objective: row.objective || ''
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
}

// ✅ Bug3修复：courseIds 转成 courseScope 提交
async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = {
        id: form.id,
        taskName: form.taskName || undefined,
        termId: form.termId,
        ruleStrategy: form.ruleStrategy,
        objective: form.objective || undefined,
        // ✅ 关键：把数组转成后端认识的 courseScope 格式
        courseScope: form.courseIds && form.courseIds.length > 0
          ? '课程ID:' + form.courseIds.join(',')
          : undefined
      }
      if (form.id) {
        await scheduleTaskApi.update(form.id, payload)
        ElMessage.success('编辑成功')
      } else {
        await scheduleTaskApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error('保存失败：' + (e?.message || '请稍后重试'))
    } finally {
      submitLoading.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除任务【${row.taskName || '排课任务#' + row.id}】吗？`, '删除确认', { type: 'warning' }).then(async () => {
    try {
      await scheduleTaskApi.delete(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// ✅ Bug4修复：异步提交 + 轮询状态
async function handleExecute(row) {
  executingId.value = row.id
  currentTask.value = row
  logVisible.value = true

  try {
    const res = await executeScheduleAgent(row.id)
    ElMessage.info(res.data?.message || '任务已提交后台运行')

    // 启动轮询
    startPolling(row.id)
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || String(e)
    ElMessage.error('提交失败：' + msg)
    executingId.value = null
  }
}

function startPolling(taskId) {
  stopPolling()
  pollTimer.value = setInterval(async () => {
    await handleRefreshStatus({ id: taskId })
    if (currentTask.value && (currentTask.value.status === 'SUCCESS' || currentTask.value.status === 'FAIL')) {
      stopPolling()
      executingId.value = null
      loadData()
    }
  }, 5000) // 每 5 秒轮询一次
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

async function handleRefreshStatus(row) {
  try {
    const res = await request({ url: `/schedule-task/${row.id}/status`, method: 'get' })
    if (res.code === 200 && res.data) {
      currentTask.value = res.data
      // 同步更新列表中的行
      const idx = tableData.value.findIndex(t => t.id === row.id)
      if (idx >= 0) {
        tableData.value[idx] = { ...tableData.value[idx], ...res.data }
      }
    }
  } catch (e) {
    console.error('刷新状态失败', e)
  }
}

onMounted(() => {
  loadDropdowns()
  loadData()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px; }
.search-bar { display: flex; gap: 10px; flex-wrap: wrap; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
.exec-status { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: #f5f7fa; border-radius: 6px; margin-bottom: 16px; font-size: 14px; }
.progress-box { padding: 0 16px; }
</style>