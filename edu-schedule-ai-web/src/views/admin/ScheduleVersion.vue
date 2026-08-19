<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="query.termId" placeholder="按学期筛选" clearable filterable style="width: 200px" @change="handleSearch">
            <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="版本状态" clearable style="width: 140px" @change="handleSearch">
            <el-option label="📝 草稿" value="DRAFT" />
            <el-option label="✅ 已发布" value="PUBLISHED" />
            <el-option label="❌ 已下线" value="OFFLINE" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>
        <el-button type="success" :icon="Plus" @click="handleCreate">📦 生成课表版本</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="versionName" label="版本名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="termName" label="学期" min-width="140" />
        <el-table-column prop="versionCode" label="版本号" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">v{{ row.versionCode || '1.0' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PUBLISHED'" type="success" effect="dark">✅ 已发布</el-tag>
            <el-tag v-else-if="row.status === 'OFFLINE'" type="info">❌ 已下线</el-tag>
            <el-tag v-else type="warning">📝 草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detailCount" label="课节数" width="90" align="center" />
        <el-table-column prop="createTime" label="生成时间" min-width="160" />
        <el-table-column prop="publishTime" label="发布时间" min-width="160" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 'PUBLISHED'" @click="handlePublish(row)">发布</el-button>
            <el-button link type="warning" v-else @click="handleOffline(row)">下线</el-button>
            <el-button link type="primary" :icon="View" @click="handleView(row)">查看课表</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <!-- 生成版本对话框 -->
    <el-dialog v-model="createVisible" title="📦 生成课表版本" width="520px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="选择学期">
          <el-select v-model="createForm.termId" placeholder="请选择学期" filterable style="width: 100%" @change="onTermChange">
            <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="基于任务">
          <el-select v-model="createForm.taskId" placeholder="请选择已完成的排课任务" filterable style="width: 100%">
            <el-option v-for="t in taskOptions" :key="t.id" :label="t.taskName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本名称">
          <el-input v-model="createForm.versionName" placeholder="例：2026春季排课方案 v1.0" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="createForm.versionCode" placeholder="例：1.0.0" />
        </el-form-item>
        <el-form-item label="版本描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="选填：版本说明、变更记录" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreateVersion">确认生成</el-button>
      </template>
    </el-dialog>

    <!-- 查看课表详情对话框 -->
    <el-dialog v-model="detailVisible" :title="'课表详情 - ' + (currentVersion?.versionName || '')" width="90%" top="5vh">
      <div v-loading="detailLoading" class="detail-wrap">
        <div v-if="!detailList.length" style="padding: 40px; text-align: center; color: #999;">该版本暂无课节数据</div>
        <el-table v-else :data="detailList" stripe border size="small" style="width: 100%">
          <el-table-column prop="weekday" label="星期" width="70" align="center">
            <template #default="{ row }">{{ weekdayText(row.weekday) }}</template>
          </el-table-column>
          <el-table-column prop="sectionStart" label="节次" width="90" align="center">
            <template #default="{ row }">第 {{ row.sectionStart }}-{{ row.sectionEnd }} 节</template>
          </el-table-column>
          <el-table-column prop="weekType" label="周次" width="110" align="center">
            <template #default="{ row }">{{ row.weekType === 'ALL' ? '每周' : (row.weekType === 'ODD' ? '单周' : '双周') }}</template>
          </el-table-column>
          <el-table-column prop="courseName" label="课程" min-width="160" />
          <el-table-column prop="teacherName" label="教师" width="110" />
          <el-table-column prop="className" label="班级" width="130" />
          <el-table-column prop="roomName" label="教室" min-width="130" />
          <el-table-column prop="campusName" label="校区" width="100" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Delete, View } from '@element-plus/icons-vue'
// ✅ 合并成一行导入，去掉重复的 scheduleVersionApi，加上 getScheduleVersionDetail
import { scheduleVersionApi, scheduleTaskApi, publishScheduleVersion, offlineScheduleVersion, getScheduleVersionDetail } from '@/api/schedule'
import request from '@/utils/request'

const loading = ref(false)
const creating = ref(false)
const detailLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ termId: null, status: null, page: 1, size: 10 })

const createVisible = ref(false)
const createForm = reactive({ taskId: null, termId: null, versionName: '', versionCode: '1.0.0', description: '' })

const detailVisible = ref(false)
const currentVersion = ref(null)
const detailList = ref([])

const termOptions = ref([])
const taskOptions = ref([])

function weekdayText(d) { return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][d] || d }

async function loadDropdowns() {
  try {
    const [t, ts] = await Promise.all([
      request.get('/term', { params: { page: 1, size: 100 } }),
      scheduleTaskApi.list({ page: 1, size: 300 })
    ])
    termOptions.value = (t.data?.list || t.data?.records || t.data || [])
    const raw = ts.data || []
    taskOptions.value = Array.isArray(raw) ? raw : (raw.list || raw.records || [])
  } catch (e) { console.warn('下拉框加载失败', e) }
}

function onTermChange(tid) {
  createForm.taskId = null
  createForm.versionName = termOptions.value.find(x => x.id === tid)?.termName ? (termOptions.value.find(x => x.id === tid).termName + ' 排课方案') : ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await scheduleVersionApi.list({ page: query.page, size: query.size, termId: query.termId, status: query.status })
    const raw = res.data || []
    const list = Array.isArray(raw) ? raw : (raw.list || raw.records || [])
    const start = (query.page - 1) * query.size
    const end = start + query.size
    tableData.value = list.slice(start, end)
    total.value = list.length
  } catch (e) { console.error(e); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.termId = null; query.status = null; query.page = 1; loadData() }

function handleCreate() {
  Object.assign(createForm, { taskId: null, termId: null, versionName: '', versionCode: '1.0.0', description: '' })
  createVisible.value = true
}

async function doCreateVersion() {
  if (!createForm.taskId) { ElMessage.warning('请选择排课任务'); return }
  if (!createForm.versionName) { ElMessage.warning('请输入版本名称'); return }
  creating.value = true
  try {
    const task = taskOptions.value.find(x => x.id === createForm.taskId)
    const payload = {
      taskId: createForm.taskId,
      termId: task?.termId || createForm.termId,
      versionName: createForm.versionName,
      versionCode: createForm.versionCode,
      description: createForm.description,
      status: 'DRAFT'
    }
    await scheduleVersionApi.create(payload)
    ElMessage.success('版本生成成功')
    createVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('生成失败') }
  finally { creating.value = false }
}

async function handlePublish(row) {
  ElMessageBox.confirm(`确定发布版本【${row.versionName}】吗？\n发布后教师和学生端将看到此课表。`, '发布确认', { type: 'success' }).then(async () => {
    try {
      await publishScheduleVersion(row.id)
      ElMessage.success('发布成功！')
      loadData()
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '发布失败')
    }
  }).catch(() => {})
}

async function handleOffline(row) {
  ElMessageBox.confirm(`确定下线版本【${row.versionName}】吗？`, '下线确认', { type: 'warning' }).then(async () => {
    try {
      await offlineScheduleVersion(row.id)
      ElMessage.success('下线成功！')
      loadData()
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '下线失败')
    }
  }).catch(() => {})
}

async function handleView(row) {
  currentVersion.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    // ✅ 改成调用独立的 getScheduleVersionDetail 函数
    const res = await getScheduleVersionDetail(row.id)
    const raw = res.data?.list || res.data?.records || res.data || []
    detailList.value = Array.isArray(raw) ? raw : []
  } catch (e) { console.error(e); detailList.value = [] }
  finally { detailLoading.value = false }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除版本【${row.versionName}】吗？`, '删除确认', { type: 'warning' }).then(async () => {
    try { await scheduleVersionApi.delete(row.id); ElMessage.success('删除成功'); loadData() }
    catch (e) { ElMessage.error('删除失败') }
  }).catch(() => {})
}

onMounted(() => { loadDropdowns(); loadData() })
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px; }
.search-bar { display: flex; gap: 10px; flex-wrap: wrap; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
.detail-wrap { max-height: 60vh; overflow-y: auto; }
</style>