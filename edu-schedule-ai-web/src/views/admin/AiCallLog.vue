<template>
  <div class="page-container">
    <el-card class="mb-3" shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="任务ID">
          <el-input-number v-model="query.taskId" :min="0" placeholder="任务ID" style="width: 140px" />
        </el-form-item>
        <el-form-item label="模型编码">
          <el-input v-model="query.modelCode" placeholder="如 default" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="调用时间">
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
          <el-button type="warning" @click="statisticSummary">📊 汇总统计</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="taskId" label="任务ID" width="80" align="center" />
        <el-table-column prop="modelCode" label="模型编码" width="100" />
        <el-table-column prop="model" label="模型名" width="180" show-overflow-tooltip />
        <el-table-column prop="inputTokens" label="输入Token" width="100" align="right" />
        <el-table-column prop="outputTokens" label="输出Token" width="100" align="right" />
        <el-table-column prop="totalTokens" label="总Token" width="100" align="right">
          <template #default="{ row }">
            <b style="color:#409EFF">{{ row.totalTokens || (row.inputTokens||0) + (row.outputTokens||0) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="right">
          <template #default="{ row }">
            {{ row.durationMs != null ? row.durationMs + ' ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'SUCCESS'" type="success" effect="plain">成功</el-tag>
            <el-tag v-else type="danger" effect="dark">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="callTime" label="调用时间" width="160" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
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

    <el-dialog v-model="detailVisible" title="AI调用详情" width="70%" top="5vh">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ current?.taskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型编码">{{ current?.modelCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型名" :span="2">{{ current?.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="输入Token">{{ current?.inputTokens }}</el-descriptions-item>
        <el-descriptions-item label="输出Token">{{ current?.outputTokens }}</el-descriptions-item>
        <el-descriptions-item label="总Token">{{ current?.totalTokens }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current?.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag v-if="current?.status === 'SUCCESS'" type="success">成功</el-tag>
          <el-tag v-else type="danger">失败</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="调用时间" :span="2">{{ current?.callTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-tabs v-model="activeTab" style="margin-top: 18px;">
        <el-tab-pane label="Prompt（请求）" name="prompt">
          <pre class="pre-box">{{ current?.prompt || '-' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Response（响应）" name="response">
          <pre class="pre-box">{{ current?.response || '-' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="错误信息" name="error">
          <pre class="pre-box error-box">{{ current?.errorMsg || '（无错误）' }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { aiCallLogApi } from '@/api/ai'

const loading = ref(false)
const tableData = ref([])
const query = reactive({ taskId: undefined, modelCode: '', status: '', timeRange: [] })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const detailVisible = ref(false)
const activeTab = ref('prompt')
const current = ref(null)

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: page.pageNum, pageSize: page.pageSize, taskId: query.taskId, modelCode: query.modelCode, status: query.status }
    if (query.timeRange?.length === 2) { params.startTime = query.timeRange[0]; params.endTime = query.timeRange[1] }
    const res = await aiCallLogApi.list(params)
    if (res?.data) {
      if (Array.isArray(res.data)) { tableData.value = res.data; page.total = res.data.length }
      else { tableData.value = res.data.list || res.data.records || []; page.total = res.data.total ?? 0 }
    }
  } catch (e) { ElMessage.error('加载AI调用日志失败') }
  finally { loading.value = false }
}
function resetQuery() {
  query.taskId = undefined; query.modelCode = ''; query.status = ''; query.timeRange = []
  page.pageNum = 1; loadList()
}
function openDetail(row) { current.value = row; activeTab.value = 'prompt'; detailVisible.value = true }

function statisticSummary() {
  const list = tableData.value || []
  const totalCalls = list.length
  const success = list.filter(x => x.status === 'SUCCESS').length
  const fail = totalCalls - success
  const tokens = list.reduce((s, x) => s + ((x.totalTokens ?? (x.inputTokens||0) + (x.outputTokens||0)) || 0), 0)
  ElMessage({
    type: 'success',
    duration: 6000,
    message: `📊 本页汇总：共调用 ${totalCalls} 次 / 成功 ${success} 次 / 失败 ${fail} 次 / 累计消耗 ${tokens} tokens`
  })
}

onMounted(loadList)
</script>

<style scoped>
.page-container { padding: 16px; }
.mb-3 { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.pre-box {
  background: #f5f7fa; border: 1px solid #e4e7ed; padding: 12px;
  border-radius: 4px; max-height: 320px; overflow: auto;
  white-space: pre-wrap; word-break: break-all;
  font-size: 12px; line-height: 1.6;
}
.error-box { color: #f56c6c; background: #fef0f0; border-color: #fde2e2; }
</style>
