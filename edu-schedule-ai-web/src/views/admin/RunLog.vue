<template>
  <div class="page-container">
    <el-card class="mb-3" shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="按模块搜索" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="新增" value="CREATE" />
            <el-option label="编辑" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="执行AI排课" value="AGENT_RUN" />
            <el-option label="发布版本" value="VERSION_PUBLISH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operator" placeholder="操作人姓名/ID" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="query.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="type" label="操作类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" effect="plain">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="操作详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="operatorId" label="操作人ID" width="100" align="center" />
        <el-table-column prop="createTime" label="操作时间" width="160" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="操作日志详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模块">{{ current?.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ typeText(current?.type) }}</el-descriptions-item>
        <el-descriptions-item label="操作人ID">{{ current?.operatorId }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作详情">
          <pre style="white-space: pre-wrap; margin: 0; max-height: 300px; overflow-y: auto;">{{ current?.content || '' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { runLogApi } from '@/api/schedule'

const loading = ref(false)
const tableData = ref([])

const query = reactive({ module: '', type: '', operator: '', timeRange: [] })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const detailVisible = ref(false)
const current = ref(null)

function typeText(t) {
  return {
    LOGIN: '登录', LOGOUT: '登出',
    CREATE: '新增', UPDATE: '编辑', DELETE: '删除',
    AGENT_RUN: 'AI排课',
    VERSION_PUBLISH: '发布版本',
    OTHER: '其他'
  }[t] || t || '-'
}
function typeTag(t) {
  return {
    LOGIN: 'success', LOGOUT: 'info',
    CREATE: 'primary', UPDATE: 'warning', DELETE: 'danger',
    AGENT_RUN: '', VERSION_PUBLISH: 'success',
    OTHER: 'info'
  }[t] || ''
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: page.pageNum, pageSize: page.pageSize, module: query.module, type: query.type, operator: query.operator }
    if (query.timeRange?.length === 2) { params.startTime = query.timeRange[0]; params.endTime = query.timeRange[1] }
    const res = await runLogApi.list(params)
    if (res?.data) {
      if (Array.isArray(res.data)) {
        tableData.value = res.data; page.total = res.data.length
      } else {
        tableData.value = res.data.list || res.data.records || []
        page.total = res.data.total ?? 0
      }
    }
  } catch (e) { ElMessage.error('加载运行日志失败') }
  finally { loading.value = false }
}
function resetQuery() {
  query.module = ''; query.type = ''; query.operator = ''; query.timeRange = []
  page.pageNum = 1; loadList()
}
function openDetail(row) { current.value = row; detailVisible.value = true }

onMounted(loadList)
</script>

<style scoped>
.page-container { padding: 16px; }
.mb-3 { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
