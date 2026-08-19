<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="search-bar">
          <el-input v-model="query.title" placeholder="搜索公告标题" clearable style="width: 220px" @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="query.type" placeholder="公告类型" clearable style="width: 140px" @change="handleSearch">
            <el-option label="📢 系统公告" value="SYSTEM" />
            <el-option label="📚 教学通知" value="TEACHING" />
            <el-option label="📅 排课通知" value="SCHEDULE" />
            <el-option label="🔧 维护公告" value="MAINTENANCE" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>
        <el-button type="success" :icon="Plus" @click="handleAdd">发布公告</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="plain">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.priority === 'HIGH'" type="danger" effect="dark">🔥 置顶</el-tag>
            <el-tag v-else-if="row.priority === 'MEDIUM'" type="warning">普通</el-tag>
            <el-tag v-else type="info" effect="plain">一般</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">已发布</el-tag>
            <el-tag v-else type="info" effect="plain">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" min-width="160" />
        <el-table-column prop="createBy" label="发布人" width="110" />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 1" @click="handlePublish(row)">发布</el-button>
            <el-button link type="warning" v-else @click="handleOffline(row)">下架</el-button>
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公告类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择公告类型" style="width: 100%">
            <el-option label="📢 系统公告" value="SYSTEM" />
            <el-option label="📚 教学通知" value="TEACHING" />
            <el-option label="📅 排课通知" value="SCHEDULE" />
            <el-option label="🔧 维护公告" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio value="HIGH">🔥 置顶</el-radio>
            <el-radio value="MEDIUM">普通</el-radio>
            <el-radio value="LOW">一般</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="立即发布">
          <el-switch v-model="publishImmediately" active-text="保存后立即发布" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { announcementApi } from '@/api/schedule'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ title: '', type: null, page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const publishImmediately = ref(true)
const formRef = ref(null)
const form = reactive({ id: null, title: '', type: 'SYSTEM', priority: 'MEDIUM', content: '', status: 0 })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

function typeText(t) {
  return { SYSTEM: '📢 系统公告', TEACHING: '📚 教学通知', SCHEDULE: '📅 排课通知', MAINTENANCE: '🔧 维护公告' }[t] || t
}
function typeTagType(t) {
  return { SYSTEM: '', TEACHING: 'success', SCHEDULE: 'warning', MAINTENANCE: 'info' }[t] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await announcementApi.list({ page: query.page, size: query.size, title: query.title, type: query.type })
    const raw = res.data || []
    const list = Array.isArray(raw) ? raw : (raw.list || raw.records || [])
    const start = (query.page - 1) * query.size
    const end = start + query.size
    tableData.value = list.slice(start, end)
    total.value = list.length
  } catch (e) {
    console.error(e)
    tableData.value = []
    total.value = 0
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.title = ''; query.type = null; query.page = 1; loadData() }

function handleAdd() {
  dialogTitle.value = '发布公告'
  Object.assign(form, { id: null, title: '', type: 'SYSTEM', priority: 'MEDIUM', content: '', status: 0 })
  publishImmediately.value = true
  dialogVisible.value = true
}
function handleEdit(row) {
  dialogTitle.value = '编辑公告'
  Object.assign(form, { id: row.id, title: row.title || '', type: row.type || 'SYSTEM', priority: row.priority || 'MEDIUM', content: row.content || '', status: row.status || 0 })
  publishImmediately.value = false
  dialogVisible.value = true
}
function resetForm() { formRef.value?.resetFields() }

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (publishImmediately.value) form.status = 1
      if (form.id) {
        await announcementApi.update(form.id, form)
        ElMessage.success('编辑成功')
      } else {
        await announcementApi.create(form)
        ElMessage.success(publishImmediately.value ? '发布成功' : '保存成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error('保存失败')
    } finally { submitLoading.value = false }
  })
}

function handlePublish(row) {
  ElMessageBox.confirm(`确定发布公告【${row.title}】吗？`, '发布确认', { type: 'success' }).then(async () => {
    try { await announcementApi.update(row.id, { ...row, status: 1 }); ElMessage.success('发布成功'); loadData() }
    catch (e) { ElMessage.error('发布失败') }
  }).catch(() => {})
}
function handleOffline(row) {
  ElMessageBox.confirm(`确定下架公告【${row.title}】吗？`, '下架确认', { type: 'warning' }).then(async () => {
    try { await announcementApi.update(row.id, { ...row, status: 0 }); ElMessage.success('下架成功'); loadData() }
    catch (e) { ElMessage.error('下架失败') }
  }).catch(() => {})
}
function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告【${row.title}】吗？`, '删除确认', { type: 'warning' }).then(async () => {
    try { await announcementApi.delete(row.id); ElMessage.success('删除成功'); loadData() }
    catch (e) { ElMessage.error('删除失败') }
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px; }
.search-bar { display: flex; gap: 10px; flex-wrap: wrap; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>