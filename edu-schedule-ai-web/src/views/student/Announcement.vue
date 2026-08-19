<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>📢 教务公告</span>
          <span class="sub-title">共 {{ total }} 条已发布公告</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索标题或内容关键词" clearable style="width: 260px" :prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="query.type" placeholder="公告类型" clearable style="width: 150px" @change="handleSearch">
          <el-option label="📢 系统公告" value="SYSTEM" />
          <el-option label="📚 教学通知" value="TEACHING" />
          <el-option label="📅 排课通知" value="SCHEDULE" />
          <el-option label="🔧 维护公告" value="MAINTENANCE" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <div class="announcement-list" v-loading="loading">
        <div v-if="list.length === 0" class="empty-wrap">
          <el-empty description="暂无公告" />
        </div>
        <div v-for="item in list" :key="item.id" class="ann-card" @click="openDetail(item)">
          <div class="ann-left">
            <div class="ann-title-row">
              <el-tag v-if="item.priority === 'HIGH'" type="danger" effect="dark" size="small">🔥 置顶</el-tag>
              <el-tag v-else-if="item.priority === 'MEDIUM'" type="warning" size="small">普通</el-tag>
              <el-tag v-else type="info" effect="plain" size="small">一般</el-tag>
              <el-tag :type="typeTagType(item.type)" effect="plain" size="small" style="margin-left: 8px;">{{ typeText(item.type) }}</el-tag>
              <span class="ann-title" :class="{ pinned: item.priority === 'HIGH' }">{{ item.title }}</span>
            </div>
            <div class="ann-summary">{{ item.content && item.content.length > 120 ? item.content.slice(0, 120) + '……' : (item.content || '（无正文）') }}</div>
            <div class="ann-meta">
              <el-icon><User /></el-icon>
              <span>{{ item.createBy || item.publisher || '管理员' }}</span>
              <el-icon style="margin-left: 14px;"><Clock /></el-icon>
              <span>{{ item.publishTime || item.createTime || '-' }}</span>
            </div>
          </div>
          <div class="ann-right">
            <el-button type="primary" link>查看详情 →</el-button>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="current?.title || '公告详情'" width="720px" top="6vh">
      <template v-if="current">
        <div class="detail-header">
          <el-tag :type="typeTagType(current.type)" effect="plain">{{ typeText(current.type) }}</el-tag>
          <el-tag v-if="current.priority === 'HIGH'" type="danger" effect="dark" style="margin-left: 6px;">🔥 置顶</el-tag>
          <span class="detail-meta">
            <el-icon><User /></el-icon> {{ current.createBy || current.publisher || '管理员' }}
            &nbsp;·&nbsp;
            <el-icon><Clock /></el-icon> {{ current.publishTime || current.createTime || '-' }}
          </span>
        </div>
        <el-divider />
        <div class="detail-content">{{ current.content || '（无正文）' }}</div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, User, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { announcementApi } from '@/api/schedule'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', type: null, page: 1, size: 10 })

const detailVisible = ref(false)
const current = ref(null)

function typeText(t) {
  return { SYSTEM: '📢 系统公告', TEACHING: '📚 教学通知', SCHEDULE: '📅 排课通知', MAINTENANCE: '🔧 维护公告' }[t] || t || '公告'
}
function typeTagType(t) {
  return { SYSTEM: '', TEACHING: 'success', SCHEDULE: 'warning', MAINTENANCE: 'info' }[t] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await announcementApi.list({ page: query.page, size: query.size, title: query.keyword, content: query.keyword, type: query.type })
    const raw = res?.data || []
    const all = Array.isArray(raw) ? raw : (raw.list || raw.records || [])
    const published = all.filter(x => x.status == null || String(x.status) === '1' || x.status === true)
    published.sort((a, b) => {
      const pa = a.priority === 'HIGH' ? 0 : a.priority === 'MEDIUM' ? 1 : 2
      const pb = b.priority === 'HIGH' ? 0 : b.priority === 'MEDIUM' ? 1 : 2
      if (pa !== pb) return pa - pb
      const ta = new Date(a.publishTime || a.createTime || 0).getTime()
      const tb = new Date(b.publishTime || b.createTime || 0).getTime()
      return tb - ta
    })
    const start = (query.page - 1) * query.size
    const end = start + query.size
    total.value = published.length
    list.value = published.slice(start, end)
  } catch (e) { console.error(e); list.value = []; total.value = 0; ElMessage.error('加载公告失败') }
  finally { loading.value = false }
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.keyword = ''; query.type = null; query.page = 1; loadData() }
function openDetail(item) { current.value = item; detailVisible.value = true }

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.sub-title { font-size: 13px; color: #909399; font-weight: normal; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.announcement-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.empty-wrap { padding: 40px 0; }
.ann-card {
  display: flex; justify-content: space-between; align-items: stretch;
  padding: 14px 18px; border: 1px solid #ebeef5; border-radius: 8px;
  background: #fff; cursor: pointer; transition: all .18s;
}
.ann-card:hover { border-color: #409EFF; box-shadow: 0 2px 12px 0 rgba(64,158,255,.12); transform: translateY(-1px); }
.ann-left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.ann-right { display: flex; align-items: center; padding-left: 14px; }
.ann-title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.ann-title { font-size: 15px; font-weight: 600; color: #303133; margin-left: 4px; }
.ann-title.pinned { color: #f56c6c; }
.ann-summary { color: #606266; font-size: 13px; line-height: 1.6; white-space: pre-wrap; }
.ann-meta { display: flex; align-items: center; color: #909399; font-size: 12px; gap: 4px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.detail-header { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.detail-meta { margin-left: auto; color: #909399; font-size: 13px; display: flex; align-items: center; gap: 3px; }
.detail-content {
  white-space: pre-wrap; line-height: 1.8; font-size: 14px; color: #303133;
  background: #fafbfc; border: 1px solid #f0f2f5; border-radius: 6px; padding: 18px 20px;
  min-height: 140px;
}
</style>