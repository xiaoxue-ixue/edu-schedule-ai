<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索课程/教师/班级" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button type="danger" @click="onClear" :loading="clearing">一键清空</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" size="small">
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="courseName" label="课程" min-width="120" />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="className" label="班级" width="110" />
      <el-table-column prop="roomName" label="教室" width="100" />
      <el-table-column prop="dayOfWeek" label="星期" width="80" align="center" />
      <el-table-column prop="slotName" label="节次" width="90" align="center" />
      <el-table-column label="时间" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.startTime">{{ row.startTime }} - {{ row.endTime }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="conflictReason" label="冲突" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.conflictReason && row.conflictReason !== '无'" type="danger" size="small">有</el-tag>
          <el-tag v-else type="success" size="small">无</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="load"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { clearScheduleDetail } from '@/api/schedule'

const loading = ref(false)
const clearing = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await request({
      url: '/schedule-detail',
      method: 'get',
      params: { page: page.value, size: size.value, keyword: keyword.value }
    })
    if (res.code === 200) {
      const d = res.data
      if (Array.isArray(d)) {
        list.value = d
        total.value = d.length
      } else {
        list.value = d?.rows || d?.list || []
        total.value = d?.total || list.value.length
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  page.value = 1
  load()
}

async function onClear() {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有排课明细吗？此操作不可恢复！',
      '警告',
      { confirmButtonText: '确定清空', cancelButtonText: '取消', type: 'error' }
    )
  } catch { return }
  clearing.value = true
  try {
    await clearScheduleDetail()
    ElMessage.success('已清空')
    load()
  } catch (e) {
    ElMessage.error('清空失败：' + (e.message || e))
  } finally {
    clearing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.filter-bar { margin-bottom: 12px; display: flex; gap: 8px; align-items: center; }
</style>