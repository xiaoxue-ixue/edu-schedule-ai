<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="search-bar">
          <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" clearable style="width: 240px"
            @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px" @change="handleSearch">
            <el-option label="管理员" value="admin" />
            <el-option label="教师" value="teacher" />
            <el-option label="学生" value="student" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>
        <div>
          <el-button type="success" :icon="Upload" @click="openBatchDialog">批量导入学生</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="plain">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联班级/教师" min-width="160">
          <template #default="{ row }">
            <span v-if="row.role === 'student' && row.className">班级：{{ row.className }}</span>
            <span v-else-if="row.role === 'teacher' && row.teacherName">教师：{{ row.teacherName }}</span>
            <span v-else-if="row.role === 'admin'">—</span>
            <el-tag v-else type="warning" size="small">未关联</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="默认123456" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%" @change="onRoleChange">
            <el-option label="管理员" value="admin" />
            <el-option label="教师" value="teacher" />
            <el-option label="学生" value="student" />
          </el-select>
        </el-form-item>
        <!-- ★ 教师角色：关联教师 -->
        <el-form-item v-if="form.role === 'teacher'" label="关联教师">
          <el-select v-model="form.teacherId" placeholder="选择教师" filterable clearable style="width: 100%">
            <el-option v-for="t in teacherOptions" :key="t.id" :label="`${t.name}（工号:${t.teacherNo || t.id}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <!-- ★ 学生角色：关联班级 -->
        <el-form-item v-if="form.role === 'student'" label="所属班级">
          <el-select v-model="form.classId" placeholder="选择班级" filterable clearable style="width: 100%">
            <el-option v-for="c in classOptions" :key="c.id" :label="`${c.className}（${c.studentCount || 0}人）`" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入学生对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量导入学生" width="480px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>按班级批量生成学生账号，用户名格式为"前缀+3位序号"（如 stu001），默认密码 123456</template>
      </el-alert>
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="选择班级">
          <el-select v-model="batchForm.classId" placeholder="请选择班级" filterable style="width: 100%">
            <el-option v-for="c in classOptions" :key="c.id" :label="`${c.className}（${c.studentCount || 0}人）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名前缀">
          <el-input v-model="batchForm.prefix" placeholder="如 stu、2024cs等" />
        </el-form-item>
        <el-form-item label="创建数量">
          <el-input-number v-model="batchForm.count" :min="1" :max="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchSubmit">开始创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Upload } from '@element-plus/icons-vue'
import { listUsers, createUser, updateUser, deleteUser, batchCreateStudents } from '@/api/user'
import { clazzApi, teacherApi } from '@/api/baseData'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const teacherOptions = ref([])
const classOptions = ref([])

const query = reactive({ keyword: '', role: '', page: 1, size: 10 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: null, username: '', realName: '', password: '', role: 'student', teacherId: null, classId: null })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}
const dialogTitle = ref('新增用户')

// 批量导入
const batchDialogVisible = ref(false)
const batchLoading = ref(false)
const batchForm = reactive({ classId: null, prefix: 'stu', count: 30 })

function roleText(role) { return { admin: '管理员', teacher: '教师', student: '学生' }[role] || role }
function roleTagType(role) { return { admin: 'primary', teacher: 'success', student: 'warning' }[role] || 'info' }

async function loadData() {
  loading.value = true
  try {
    const res = await listUsers({ page: query.page, size: query.size, keyword: query.keyword, role: query.role })
    const data = res.data || {}
    tableData.value = data.list || data.records || []
    total.value = data.total || 0
  } catch (e) { tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function loadOptions() {
  try {
    const [cRes, tRes] = await Promise.all([
      request.get('/clazz', { params: { page: 1, size: 200 } }),
      request.get('/teacher', { params: { page: 1, size: 200 } })
    ])
    const cRaw = cRes?.data || []
    classOptions.value = Array.isArray(cRaw) ? cRaw : (cRaw.list || cRaw.records || [])
    const tRaw = tRes?.data || []
    teacherOptions.value = Array.isArray(tRaw) ? tRaw : (tRaw.list || tRaw.records || [])
  } catch (e) {}
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.keyword = ''; query.role = ''; query.page = 1; loadData() }

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(form, { id: null, username: '', realName: '', password: '', role: 'student', teacherId: null, classId: null })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(form, { id: row.id, username: row.username, realName: row.realName, password: '', role: row.role, teacherId: row.teacherId, classId: row.classId })
  dialogVisible.value = true
}

function onRoleChange() {
  if (form.role !== 'teacher') form.teacherId = null
  if (form.role !== 'student') form.classId = null
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateUser(form.id, { username: form.username, realName: form.realName, role: form.role, teacherId: form.teacherId, classId: form.classId })
        ElMessage.success('修改成功')
      } else {
        await createUser({ username: form.username, realName: form.realName, password: form.password || '123456', role: form.role, teacherId: form.teacherId, classId: form.classId })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {} finally { submitLoading.value = false }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
    .then(async () => { await deleteUser(row.id); ElMessage.success('删除成功'); loadData() }).catch(() => {})
}

function openBatchDialog() {
  Object.assign(batchForm, { classId: null, prefix: 'stu', count: 30 })
  batchDialogVisible.value = true
}

async function handleBatchSubmit() {
  if (!batchForm.classId) { ElMessage.warning('请选择班级'); return }
  batchLoading.value = true
  try {
    const res = await batchCreateStudents({ classId: batchForm.classId, prefix: batchForm.prefix, count: batchForm.count })
    const created = res.data || 0
    ElMessage.success(`成功创建 ${created} 个学生账号（默认密码 123456）`)
    batchDialogVisible.value = false
    loadData()
  } catch (e) {} finally { batchLoading.value = false }
}

function resetForm() { formRef.value && formRef.value.resetFields() }

onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.search-bar { display: flex; gap: 8px; flex-wrap: wrap; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>