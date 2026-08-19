<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">我的课表</span>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="gridData"
        border
        stripe
        style="width: 100%"
        :cell-style="cellStyle"
      >
        <el-table-column prop="slotName" label="节次" width="110" fixed align="center" />
        <el-table-column
          v-for="day in days"
          :key="day.value"
          :label="day.label"
          min-width="160"
          align="center"
        >
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
              <div class="course-info class-name">
                <el-icon><Reading /></el-icon>{{ row.cells[day.value].className }}
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
import { ref, onMounted } from 'vue'
import { Refresh, User, Location, Reading } from '@element-plus/icons-vue'
import { getMySchedule } from '@/api/teacher'

const loading = ref(false)
const gridData = ref([])

const days = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

function cellStyle() {
  return { verticalAlign: 'middle' }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMySchedule()
    const list = res.data || []
    gridData.value = buildGrid(list)
  } catch (e) {
    gridData.value = []
  } finally {
    loading.value = false
  }
}

function buildGrid(list) {
  // 按 slotNo 分组，构建 行(节次) × 列(星期) 网格
  const slotMap = new Map()
  list.forEach((item) => {
    const slotNo = item.slotNo || item.slotId || item.id
    const timeStr = item.startTime && item.endTime ? ` ${item.startTime}-${item.endTime}` : ''
    const slotName = (item.slotName || item.sectionStart || `第${slotNo}节`) + timeStr
    if (!slotMap.has(slotNo)) {
      slotMap.set(slotNo, { slotName, slotNo, cells: {} })
    }
    const day = item.dayOfWeek || item.day || item.weekday
    if (day) {
      slotMap.get(slotNo).cells[day] = item
    }
  })
  return Array.from(slotMap.values()).sort((a, b) => (a.slotNo > b.slotNo ? 1 : -1))
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.schedule-cell {
  padding: 8px 4px;
  background: #ecf5ff;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}
.course-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  font-size: 14px;
}
.course-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
}
.class-name {
  color: #909399;
}
.empty-cell {
  color: #c0c4cc;
}
</style>
