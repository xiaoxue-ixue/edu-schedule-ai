<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="search-bar">
          <el-input
            v-model="query.keyword"
            placeholder="搜索模型编码/名称"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>
        <el-button type="success" :icon="Plus" @click="handleAdd">新增配置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="modelCode" label="模型编码" min-width="140" />
        <el-table-column prop="modelName" label="模型名称" min-width="160" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="220" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="warning" :icon="Connection" :loading="testingId === row.id" @click="handleTest(row)">测试</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="form.modelCode" placeholder="如 gpt-4o / glm-4" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如 https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="请输入API Key" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Connection } from '@element-plus/icons-vue'
import { aiModelConfigApi, testAiModelConfig } from '@/api/ai'

const loading = ref(false)
const submitLoading = ref(false)
const testingId = ref(null)
const tableData = ref([])
const total = ref(0)

const query = reactive({ keyword: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  modelCode: '',
  modelName: '',
  baseUrl: '',
  apiKey: '',
  enabled: true,
  remark: ''
})

const rules = {
  modelCode: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入Base URL', trigger: 'blur' }]
}

const dialogTitle = computed(() => (isEdit.value ? '编辑AI模型配置' : '新增AI模型配置'))

async function loadData() {
  loading.value = true
  try {
    const res = await aiModelConfigApi.list({ page: query.page, size: query.size, keyword: query.keyword })
    const data = res.data || {}
    tableData.value = data.list || data.records || []
    total.value = data.total || 0
  } catch (e) {
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
  query.keyword = ''
  query.page = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, { id: null, modelCode: '', modelName: '', baseUrl: '', apiKey: '', enabled: true, remark: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, { ...row, apiKey: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await aiModelConfigApi.update(form.id, { ...form })
        ElMessage.success('修改成功')
      } else {
        const { id, ...payload } = form
        await aiModelConfigApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      // ignore
    } finally {
      submitLoading.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除模型「${row.modelName || row.modelCode}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await aiModelConfigApi.remove(row.id)
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {})
}

async function handleTest(row) {
  testingId.value = row.id
  try {
    const res = await testAiModelConfig(row.id)
    const ok = res.data && (res.data.success !== false)
    if (ok) {
      ElMessage.success(`连通性测试通过：${res.data.message || '连接正常'}`)
    } else {
      ElMessage.warning(`测试结果：${res.data?.message || '连接异常'}`)
    }
  } catch (e) {
    // ignore
  } finally {
    testingId.value = null
  }
}

function resetForm() {
  formRef.value && formRef.value.resetFields()
}

onMounted(() => {
  loadData()
})
</script>
