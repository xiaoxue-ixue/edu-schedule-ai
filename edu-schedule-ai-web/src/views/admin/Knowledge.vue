<template>
  <div class="page-container">
    <!-- 顶部操作栏：新增 + 搜索 -->
    <el-card class="mb-3" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索标题/内容"
            clearable
            style="width: 240px"
            @keyup.enter="loadList"
          />
          <el-select v-model="searchCategory" placeholder="全部分类" clearable style="width: 140px; margin-left: 10px">
            <el-option label="制度" value="制度" />
            <el-option label="规则" value="规则" />
            <el-option label="流程" value="流程" />
            <el-option label="口径" value="口径" />
          </el-select>
          <el-button type="primary" style="margin-left: 10px" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="success" @click="openCreate">
            <el-icon><Plus /></el-icon>&nbsp;新增知识
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 列表表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="categoryTag(row.category)" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sliceCount" label="切片数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1 || row.status === '1'" type="success">已切片</el-tag>
            <el-tag v-else type="warning">待切片</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              type="success"
              link
              :disabled="row.status === 1 || row.status === '1'"
              @click="doSlice(row)"
            >
              执行切片
            </el-button>
            <el-button type="danger" link @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="600px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入知识标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="制度" value="制度" />
            <el-option label="规则" value="规则" />
            <el-option label="流程" value="流程" />
            <el-option label="口径" value="口径" />
          </el-select>
        </el-form-item>
        <el-form-item label="原始内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入原始知识内容，系统将自动切片"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待切片" :value="0" />
            <el-option label="已切片" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/baseData'

// ============== 状态 ==============
const loading = ref(false)
const tableData = ref([])
const searchKeyword = ref('')
const searchCategory = ref('')

const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// ============== 弹窗表单 ==============
const formVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const form = reactive({ id: null, title: '', category: '规则', content: '', status: 0 })

const formTitle = computed(() => (isEdit.value ? '编辑知识' : '新增知识'))

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入原始内容', trigger: 'blur' }]
}

// ============== 方法 ==============
const categoryTag = c =>
  ({ 制度: 'primary', 规则: 'success', 流程: 'warning', 口径: 'info' }[c] || '')

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      keyword: searchKeyword.value,
      category: searchCategory.value
    }
    const res = await knowledgeApi.list(params)
    if (res && res.data) {
      if (Array.isArray(res.data)) {
        tableData.value = res.data
        page.total = res.data.length
      } else {
        tableData.value = res.data.list || res.data.records || []
        page.total = res.data.total ?? 0
      }
    }
  } catch (e) {
    ElMessage.error('加载知识库列表失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  searchKeyword.value = ''
  searchCategory.value = ''
  page.pageNum = 1
  loadList()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { id: null, title: '', category: '规则', content: '', status: 0 })
  formVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { ...row })
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (isEdit.value) {
      await knowledgeApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await knowledgeApi.create(form)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } catch (_) { /* 校验未通过 */ }
}

async function removeItem(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '确认删除', { type: 'warning' })
  try {
    await knowledgeApi.remove(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function doSlice(row) {
  await ElMessageBox.confirm(
    `确认对「${row.title}」执行自动切片？\n系统将把内容按句子切分并存储到 sliceContent 字段。`,
    '执行切片',
    { type: 'info' }
  )
  try {
    // 前端模拟切片：按句号/分号/问号/感叹号切分，用分号拼接
    const sentences = (row.content || '')
      .split(/[。？！；;\n]/)
      .map(s => s.trim())
      .filter(s => s.length > 0)
    const sliceContent = sentences.join(';')
    await knowledgeApi.update(row.id, {
      ...row,
      sliceContent,
      sliceCount: sentences.length,
      status: 1
    })
    ElMessage.success(`✅ 切片完成，共 ${sentences.length} 段`)
    loadList()
  } catch (e) {
    ElMessage.error('切片失败：' + (e?.msg || e?.message || '未知错误'))
  }
}

onMounted(loadList)
</script>

<style scoped>
.page-container { padding: 16px; }
.mb-3 { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-left { display: flex; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>