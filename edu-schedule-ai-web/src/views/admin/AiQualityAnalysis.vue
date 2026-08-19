<template>
  <div class="page-container">
    <el-card class="mb-3" shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="学期">
          <el-select v-model="query.termId" placeholder="全部学期" clearable filterable style="width: 200px" @change="loadList">
            <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分析维度">
          <el-select v-model="query.dimension" placeholder="全部维度" clearable style="width: 140px" @change="loadList">
            <el-option label="按课程" value="COURSE" />
            <el-option label="按教师" value="TEACHER" />
            <el-option label="按班级" value="CLASS" />
          </el-select>
        </el-form-item>
        <el-form-item label="质量等级">
          <el-select v-model="query.level" placeholder="全部等级" clearable style="width: 140px" @change="loadList">
            <el-option label="优秀" value="优秀" />
            <el-option label="良好" value="良好" />
            <el-option label="合格" value="合格" />
            <el-option label="不合格" value="不合格" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="warning" @click="openGenerate">🤖 AI 生成分析报告</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="dimension" label="维度" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="dimTag(row.dimension)" effect="plain">{{ dimText(row.dimension) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="分析对象" width="150" show-overflow-tooltip />
        <el-table-column prop="evalCount" label="评价数" width="90" align="right" />
        <el-table-column prop="avgScore" label="平均分" width="100" align="right">
          <template #default="{ row }"><b :style="{ color: scoreColor(row.avgScore) }">{{ row.avgScore }}</b></template>
        </el-table-column>
        <el-table-column prop="qualityLevel" label="质量等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.qualityLevel)" effect="dark">{{ row.qualityLevel || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="160" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openReport(row)">查看报告</el-button>
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

    <!-- 生成分析对话框 -->
    <el-dialog v-model="generateVisible" title="🤖 AI 生成教学质量分析报告" width="480px" :close-on-click-modal="false">
      <el-form :model="genForm" label-width="100px">
        <el-form-item label="选择学期" required>
          <el-select v-model="genForm.termId" placeholder="请选择学期" filterable style="width: 100%" @change="onGenTermChange">
            <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分析维度" required>
          <el-radio-group v-model="genForm.dimension">
            <el-radio value="COURSE">按课程</el-radio>
            <el-radio value="TEACHER">按教师</el-radio>
            <el-radio value="CLASS">按班级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分析范围">
          <el-select v-model="genForm.targetId" placeholder="留空=全部生成（推荐）" filterable clearable style="width: 100%">
            <el-option v-for="o in genTargetOptions" :key="o.id" :label="o.name || o.label" :value="o.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="doGenerate">✨ 开始 AI 分析</el-button>
      </template>
    </el-dialog>

    <!-- 报告详情 -->
    <el-dialog v-model="reportVisible" :title="'📊 质量分析报告 - ' + (current?.targetName || '')" width="75%" top="5vh">
      <div v-loading="reportLoading" class="report-wrap">
        <template v-if="current">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="分析维度">{{ dimText(current.dimension) }}</el-descriptions-item>
            <el-descriptions-item label="分析对象">{{ current.targetName }}</el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ current.createTime }}</el-descriptions-item>
            <el-descriptions-item label="样本数">{{ current.evalCount }}</el-descriptions-item>
            <el-descriptions-item label="平均分"><b :style="{ color: scoreColor(current.avgScore), fontSize: '18px' }">{{ current.avgScore }}</b></el-descriptions-item>
            <el-descriptions-item label="质量等级">
              <el-tag :type="levelTag(current.qualityLevel)" effect="dark" size="large">{{ current.qualityLevel }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-divider content-position="left">📝 总结</el-divider>
          <div class="report-box summary">{{ current.summary || '-' }}</div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-divider content-position="left">✅ 亮点与优势</el-divider>
              <div class="report-box good">{{ current.strengths || '-' }}</div>
            </el-col>
            <el-col :span="8">
              <el-divider content-position="left">⚠️ 存在问题</el-divider>
              <div class="report-box bad">{{ current.problems || '-' }}</div>
            </el-col>
            <el-col :span="8">
              <el-divider content-position="left">💡 改进建议</el-divider>
              <div class="report-box suggest">{{ current.suggestions || '-' }}</div>
            </el-col>
          </el-row>
          <el-divider content-position="left">🤖 AI 完整报告</el-divider>
          <pre class="report-box full">{{ current.report || '（无完整报告文本）' }}</pre>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiQualityAnalysisApi } from '@/api/schedule'
import request from '@/utils/request'

const loading = ref(false)
const generating = ref(false)
const reportLoading = ref(false)
const tableData = ref([])
const termOptions = ref([])
const teacherOptions = ref([])
const courseOptions = ref([])
const clazzOptions = ref([])

const query = reactive({ termId: null, dimension: '', level: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const generateVisible = ref(false)
const genForm = reactive({ termId: null, dimension: 'TEACHER', targetId: null })
const genTargetOptions = ref([])

const reportVisible = ref(false)
const current = ref(null)

async function loadOptions() {
  try {
    const [t, ts, cs, cls] = await Promise.all([
      request.get('/term', { params: { page: 1, size: 100 } }),
      request.get('/teacher', { params: { page: 1, size: 500 } }),
      request.get('/course', { params: { page: 1, size: 500 } }),
      request.get('/clazz', { params: { page: 1, size: 500 } })
    ])
    const norm = (d) => Array.isArray(d) ? d : (d?.list || d?.records || [])
    termOptions.value = norm(t.data)
    teacherOptions.value = norm(ts.data).map(x => ({ id: x.id, name: x.name || x.teacherName }))
    courseOptions.value = norm(cs.data).map(x => ({ id: x.id, name: x.name || x.courseName }))
    clazzOptions.value = norm(cls.data).map(x => ({ id: x.id, name: x.name || x.className }))
  } catch (e) { console.warn('下拉加载失败', e) }
}

function onGenTermChange() { genForm.targetId = null; pickTargets() }
function pickTargets() {
  if (genForm.dimension === 'TEACHER') genTargetOptions.value = teacherOptions.value
  else if (genForm.dimension === 'COURSE') genTargetOptions.value = courseOptions.value
  else genTargetOptions.value = clazzOptions.value
}

function dimText(d) { return { COURSE: '按课程', TEACHER: '按教师', CLASS: '按班级' }[d] || d || '-' }
function dimTag(d) { return { COURSE: 'primary', TEACHER: 'success', CLASS: 'warning' }[d] || '' }
function levelTag(l) { return { '优秀': 'success', '良好': 'primary', '合格': 'warning', '不合格': 'danger' }[l] || 'info' }
function scoreColor(s) {
  if (s >= 90) return '#67c23a'; if (s >= 80) return '#409EFF'
  if (s >= 70) return '#e6a23c'; if (s >= 60) return '#909399'
  return '#f56c6c'
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: page.pageNum, pageSize: page.pageSize, termId: query.termId, dimension: query.dimension, qualityLevel: query.level }
    const res = await aiQualityAnalysisApi.list(params)
    if (res?.data) {
      if (Array.isArray(res.data)) { tableData.value = res.data; page.total = res.data.length }
      else { tableData.value = res.data.list || res.data.records || []; page.total = res.data.total ?? 0 }
    }
  } catch (e) { ElMessage.error('加载质量分析失败') }
  finally { loading.value = false }
}
function resetQuery() { query.termId = null; query.dimension = ''; query.level = ''; page.pageNum = 1; loadList() }

function openGenerate() {
  Object.assign(genForm, { termId: query.termId || (termOptions.value[0]?.id), dimension: 'TEACHER', targetId: null })
  pickTargets()
  generateVisible.value = true
}
async function doGenerate() {
  if (!genForm.termId) { ElMessage.warning('请选择学期'); return }
  generating.value = true
  try {
    await aiQualityAnalysisApi.generate({ termId: genForm.termId, dimension: genForm.dimension, targetId: genForm.targetId })
    ElMessage.success('✨ AI 分析生成完成！')
    generateVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.msg || '生成失败，请稍后重试')
  } finally { generating.value = false }
}

async function openReport(row) {
  current.value = row
  reportVisible.value = true
  reportLoading.value = true
  try {
    const res = await aiQualityAnalysisApi.getById(row.id)
    if (res?.data) Object.assign(row, res.data)
    current.value = { ...row }
  } catch (e) { console.warn('详情加载失败，用列表数据展示') }
  finally { reportLoading.value = false }
}

async function removeItem(row) {
  await ElMessageBox.confirm('确认删除该分析报告吗？', '删除确认', { type: 'warning' })
  try {
    await request({ url: `/ai-quality-analysis/${row.id}`, method: 'delete' })
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { ElMessage.error('删除失败') }
}

onMounted(() => { loadOptions(); loadList() })
</script>

<style scoped>
.page-container { padding: 16px; }
.mb-3 { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.report-wrap { max-height: 70vh; overflow-y: auto; padding-right: 6px; }
.report-box {
  background: #f5f7fa; border: 1px solid #ebeef5;
  padding: 14px 16px; border-radius: 6px;
  white-space: pre-wrap; word-break: break-all;
  line-height: 1.7; font-size: 13px; min-height: 60px;
}
.report-box.summary { background: linear-gradient(135deg,#ecf5ff,#f0f9eb); border-color: #a0cfff; color: #303133; font-weight: 500; }
.report-box.good    { background: #f0f9eb; border-color: #c2e7b0; color: #529b2e; }
.report-box.bad     { background: #fef0f0; border-color: #fbc4c4; color: #c45656; }
.report-box.suggest { background: #fdf6ec; border-color: #f5dab1; color: #b88230; }
.report-box.full    { background: #fafafa; border-color: #dcdfe6; max-height: 400px; overflow-y: auto; font-size: 12px; font-family: Consolas, monospace; }
</style>