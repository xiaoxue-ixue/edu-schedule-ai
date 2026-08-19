<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">我的课表</span>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-alert v-if="myClassId" type="success" :closable="false" show-icon style="margin-bottom: 12px">
        <template #title>当前班级ID：{{ myClassId }}{{ myClassName ? '（' + myClassName + '）' : '' }}，已自动加载本班级课表</template>
      </el-alert>
      <el-alert v-else type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        <template #title>未绑定班级，请联系管理员分配班级。下方可手动按班级ID查询</template>
      </el-alert>

      <div class="search-bar" v-if="!myClassId">
        <el-input v-model="filters.classId" placeholder="班级ID" clearable style="width: 160px" />
        <el-button type="primary" :icon="Search" :loading="loading" @click="loadData">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="gridData" border stripe style="width: 100%">
        <el-table-column prop="slotName" label="节次" width="110" fixed align="center" />
        <el-table-column v-for="day in days" :key="day.value" :label="day.label" min-width="160" align="center">
          <template #default="{ row }">
            <div v-if="row.cells[day.value]" class="schedule-cell">
              <div class="course-name">{{ row.cells[day.value].courseName }}</div>
              <div v-if="row.cells[day.value].startTime" class="time-info" style="font-size:11px;color:#909399;margin-top:2px;">
                {{ row.cells[day.value].startTime }} - {{ row.cells[day.value].endTime }}
              </div>
              <div class="course-info">
                <el-icon><User /></el-icon>{{ row.cells[day.value].teacherName }}
              </div>
              <div class="course-info">
                <el-icon><Location /></el-icon>{{ row.cells[day.value].roomName }}
              </div>
            </div>
            <span v-else class="empty-cell">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !gridData.length" description="暂无课表数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, User, Location } from '@element-plus/icons-vue'
import { getStudentSchedule, getMyInfo } from '@/api/student'

const loading = ref(false)
const gridData = ref([])
const myClassId = ref(null)
const myClassName = ref('')

const filters = reactive({ classId: '' })

const days = [
  { label: '周一', value: 1 }, { label: '周二', value: 2 }, { label: '周三', value: 3 },
  { label: '周四', value: 4 }, { label: '周五', value: 5 }, { label: '周六', value: 6 }, { label: '周日', value: 7 }
]

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (myClassId.value) params.classId = myClassId.value
    else if (filters.classId) params.classId = filters.classId
    const res = await getStudentSchedule(params)
    const list = res.data || []
    gridData.value = buildGrid(list)
  } catch (e) {
    gridData.value = []
  } finally {
    loading.value = false
  }
}

function buildGrid(list) {
  const slotMap = new Map()
  list.forEach((item) => {
    const slotNo = item.slotNo || item.slotId || item.id
    const timeStr = item.startTime && item.endTime ? ` ${item.startTime}-${item.endTime}` : ''
    const slotName = (item.slotName || item.sectionStart || `第${slotNo}节`) + timeStr
    if (!slotMap.has(slotNo)) slotMap.set(slotNo, { slotName, slotNo, cells: {} })
    const day = item.dayOfWeek || item.day || item.weekday
    if (day) slotMap.get(slotNo).cells[day] = item
  })
  return Array.from(slotMap.values()).sort((a, b) => (a.slotNo > b.slotNo ? 1 : -1))
}

onMounted(async () => {
  try {
    const res = await getMyInfo()
    const info = res.data || {}
    myClassId.value = info.classId
  } catch (e) {
    // 未登录或非学生角色
  }
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 16px; font-weight: 600; }
.search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.schedule-cell { padding: 8px 4px; background: #fdf6ec; border-radius: 6px; border-left: 3px solid #e6a23c; }
.course-name { font-weight: 600; color: #303133; margin-bottom: 4px; font-size: 14px; }
.course-info { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #606266; margin-top: 2px; }
.empty-cell { color: #c0c4cc; }
</style>