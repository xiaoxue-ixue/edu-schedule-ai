<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="search-bar">
          <el-input
            v-model="query.keyword"
            placeholder="关键词搜索"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>
        <el-button type="success" :icon="Plus" @click="handleAdd">新增{{ titleSuffix }}</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :min-width="col.width || 120"
          :align="col.align || 'left'"
        >
          <template v-if="col.dict" #default="{ row }">
            <el-tag v-if="col.dict[row[col.prop]]" :type="(col.dict[row[col.prop]].type) || 'info'" effect="plain">
              {{ col.dict[row[col.prop]].text }}
            </el-tag>
            <span v-else>{{ row[col.prop] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item
          v-for="col in formColumns"
          :key="col.prop"
          :label="col.label"
          :prop="col.prop"
        >
          <el-input-number
            v-if="col.type === 'number'"
            v-model="form[col.prop]"
            :min="col.min"
            controls-position="right"
            style="width: 100%"
          />
          <el-select
            v-else-if="col.type === 'select'"
            v-model="form[col.prop]"
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option
              v-for="opt in col.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-switch v-else-if="col.type === 'switch'" v-model="form[col.prop]" />
          <el-input
            v-else
            v-model="form[col.prop]"
            :type="col.inputType || 'text'"
            :placeholder="`请输入${col.label}`"
          />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  // CRUD api 对象 { list, create, update, remove }
  api: { type: Object, required: true },
  // 表格列 { prop, label, width, align, dict }
  columns: { type: Array, default: () => [] },
  // 表单列（用于新增/编辑）{ prop, label, type, options, required, min, inputType }
  formColumns: { type: Array, default: () => [] },
  // 页面标题后缀，如 "学期"
  titleSuffix: { type: String, default: '' },
  // 主键字段
  idField: { type: String, default: 'id' }
})

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({ keyword: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({})

const dialogTitle = computed(() => (isEdit.value ? '编辑' : '新增') + props.titleSuffix)

const rules = computed(() => {
  const r = {}
  props.formColumns.forEach((col) => {
    if (col.required) {
      r[col.prop] = [{ required: true, message: `请输入${col.label}`, trigger: col.type === 'select' ? 'change' : 'blur' }]
    }
  })
  return r
})

function initForm() {
  props.formColumns.forEach((col) => {
    form[col.prop] = col.type === 'number' ? (col.default ?? 0) : (col.type === 'switch' ? (col.default ?? false) : (col.default ?? ''))
  })
}

async function loadData() {
  loading.value = true
  try {
    const res = await props.api.list({ page: query.page, size: query.size, keyword: query.keyword })
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
  initForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  initForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = { ...form }
      if (isEdit.value) {
        const id = payload[props.idField]
        await props.api.update(id, payload)
        ElMessage.success('修改成功')
      } else {
        delete payload[props.idField]
        await props.api.create(payload)
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
  const id = row[props.idField]
  ElMessageBox.confirm(`确定删除该${props.titleSuffix}吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await props.api.remove(id)
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {})
}

function resetForm() {
  formRef.value && formRef.value.resetFields()
}

onMounted(() => {
  initForm()
  loadData()
})
</script>
