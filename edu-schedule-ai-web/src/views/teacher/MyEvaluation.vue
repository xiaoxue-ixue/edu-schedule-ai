<template>
  <div class="page-container">
    <el-card class="summary-card" shadow="never">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-box primary">
            <div class="stat-label">评价总数</div>
            <div class="stat-value">{{ totalCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box success">
            <div class="stat-label">综合平均分</div>
            <div class="stat-value">{{ avgScore }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box warning">
            <div class="stat-label">优秀率 (≥90)</div>
            <div class="stat-value">{{ excellentRate }}%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box info">
            <div class="stat-label">最高分 / 最低分</div>
            <div class="stat-value">{{ maxScore }} / {{ minScore }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="mb-3" shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="学期">
          <el-select v-model="query.termId" placeholder="全部学期" clearable filterable style="width: 200px">
            <el-option v-for="t in termOptions" :key="t.id" :label="t.termName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评价类型">
          <el-select v-model="query.evaluatorType" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="学生评价" value="STUDENT" />
            <el-option label="班级评估" value="CLASS" />
            <el-option label="督导评价" value="SUPERVISOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程/班级">
          <el-input v-model="query.keyword" placeholder="课程名或班级" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="statisticScores">📊 本页统计</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>📝 我收到的评价</span>
          <span class="teacher-name">{{ userStore.realName ? `（教师：${userStore.realName}）` : '' }}</span>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="courseName" label="课程" width="160" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" width="130" />
        <el-table-column prop="evaluatorType" label="评价来源" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="evaluatorTag(row.evaluatorType)" effect="plain">
              {{ evaluatorText(row.evaluatorType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="attitudeScore" label="态度" width="80" align="right" />
        <el-table-column prop="contentScore" label="内容" width="80" align="right" />
        <el-table-column prop="methodScore" label="方法" width="80" align="right" />
        <el-table-column prop="effectScore" label="效果" width="80" align="right" />
        <el-table-column prop="overallScore" label="综合" width="100" align="right">
          <template #default="{ row }">
            <b :style="{ color: scoreColor(row.overallScore) }">{{ row.overallScore }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openView(row)">查看详情</el-button>
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

    <el-dialog v-model="viewVisible" title="评价详情" width="620px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="学期" :span="2">{{ current.termName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ current.courseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ current.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ evaluatorText(current.evaluatorType) }}</el-descriptions-item>
        <el-descriptions-item label="综合得分">
          <b :style="{ color: scoreColor(current.overallScore), fontSize: '18px' }">
            {{ current.overallScore }}
          </b>
        </el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">四维度打分</el-divider>
      <el-table v-if="current" :data="[current]" border show-header>
        <el-table-column prop="attitudeScore" label="教学态度" align="center" />
        <el-table-column prop="contentScore" label="教学内容" align="center" />
        <el-table-column prop="methodScore" label="教学方法" align="center" />
        <el-table-column prop="effectScore" label="教学效果" align="center" />
      </el-table>
      <el-divider content-position="left">反馈与建议</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="label">👍 优点/亮点</div>
          <div class="content-box">{{ current?.feedback || '（未填写）' }}</div>
        </el-col>
        <el-col :span="12">
          <div class="label">💡 改进建议</div>
          <div class="content-box">{{ current?.suggestion || '（未填写）' }}</div>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { teachingEvaluationApi } from '@/api/schedule'
import request from '@/utils/request'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const termOptions = ref([])
const query = reactive({ termId: null, evaluatorType: '', keyword: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const viewVisible = ref(false)
const current = ref(null)

async function loadTerms() {
  try {
    const res = await request.get('/term', { params: { page: 1, size: 100 } })
    const raw = res?.data || []
    termOptions.value = Array.isArray(raw) ? raw : (raw.list || raw.records || [])
  } catch (e) {}
}

function evaluatorText(t) {
  return { STUDENT: '学生评价', CLASS: '班级评估', SUPERVISOR: '督导评价' }[t] || t || '-'
}
function evaluatorTag(t) {
  return { STUDENT: 'primary', CLASS: 'success', SUPERVISOR: 'warning' }[t] || ''
}
function scoreColor(s) {
  if (s >= 90) return '#67c23a'
  if (s >= 80) return '#409EFF'
  if (s >= 70) return '#e6a23c'
  if (s >= 60) return '#909399'
  return '#f56c6c'
}

// ============ 仅看"我"的评价：按当前教师名/ID过滤 ============
function applyMyFilter(rawList) {
  const list = rawList || []
  const me = userStore.user
  if (!me) return list
  const name = (me.realName || me.username || '').trim()
  const uid = me.id ?? me.userId ?? me.teacherId
  if (!name && !uid) return list
  return list.filter(x => {
    const nameMatch = name && (x.teacherName === name || x.teacher === name)
    const idMatch = uid != null && (x.teacherId === uid || x.teacherID === uid)
    return nameMatch || idMatch || !name && !uid
  })
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: page.pageNum, pageSize: page.pageSize, termId: query.termId, evaluatorType: query.evaluatorType, keyword: query.keyword }
    const res = await teachingEvaluationApi.list(params)
    let list = []
    if (res?.data) {
      if (Array.isArray(res.data)) list = res.data
      else list = res.data.list || res.data.records || []
    }
    const myList = applyMyFilter(list)
    tableData.value = myList
    page.total = myList.length
  } catch (e) { ElMessage.error('加载评价失败') }
  finally { loading.value = false }
}

function resetQuery() { query.termId = null; query.evaluatorType = ''; query.keyword = ''; page.pageNum = 1; loadList() }

function openView(row) { current.value = row; viewVisible.value = true }
function statisticScores() {
  const list = tableData.value || []
  if (!list.length) { ElMessage.warning('当前列表为空'); return }
  const scores = list.map(x => Number(x.overallScore) || 0)
  const avg = (scores.reduce((a,b)=>a+b,0) / scores.length).toFixed(2)
  const max = Math.max(...scores).toFixed(2)
  const min = Math.min(...scores).toFixed(2)
  const excellent = scores.filter(s=>s>=90).length
  ElMessage({ type: 'success', duration: 6000, message: `📊 本页 ${list.length} 条：平均分 ${avg} / 最高 ${max} / 最低 ${min} / 优秀(≥90) ${excellent} 条` })
}

const totalCount = computed(() => page.total)
const avgScore = computed(() => {
  const list = tableData.value || []
  if (!list.length) return '-'
  const s = list.map(x => Number(x.overallScore) || 0)
  return (s.reduce((a,b)=>a+b,0) / s.length).toFixed(2)
})
const maxScore = computed(() => {
  const list = tableData.value || []
  if (!list.length) return '-'
  return Math.max(...list.map(x => Number(x.overallScore) || 0)).toFixed(1)
})
const minScore = computed(() => {
  const list = tableData.value || []
  if (!list.length) return '-'
  return Math.min(...list.map(x => Number(x.overallScore) || 0)).toFixed(1)
})
const excellentRate = computed(() => {
  const list = tableData.value || []
  if (!list.length) return 0
  const cnt = list.filter(x => Number(x.overallScore) >= 90).length
  return ((cnt / list.length) * 100).toFixed(1)
})

onMounted(() => { loadTerms(); loadList() })
</script>

<style scoped>
.page-container { padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.summary-card { margin-bottom: 0; }
.stat-box { border-radius: 8px; padding: 18px 20px; color: #fff; }
.stat-box.primary { background: linear-gradient(135deg, #409EFF, #66b1ff); }
.stat-box.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-box.warning { background: linear-gradient(135deg, #e6a23c, #f0c78a); }
.stat-box.info    { background: linear-gradient(135deg, #909399, #a6a9ad); }
.stat-label { font-size: 13px; opacity: .9; }
.stat-value { font-size: 28px; font-weight: 700; margin-top: 6px; }
.mb-3 { margin-bottom: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.teacher-name { font-size: 13px; color: #909399; font-weight: normal; }
.label { font-size: 13px; color: #606266; margin-bottom: 6px; font-weight: 600; }
.content-box { background: #f5f7fa; border: 1px solid #ebeef5; padding: 10px 12px; border-radius: 4px; min-height: 80px; white-space: pre-wrap; font-size: 13px; }
</style>