<template>
  <div class="page-container">
    <el-card shadow="never" class="tip-card">
      <div class="tip">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            点击下方网格中的单元格可快速切换「可用 / 不可用」状态。绿色为可用、灰色为不可用。
            <span v-if="myTeacherId" class="me-tag">当前教师：{{ userStore.realName }} (ID: {{ myTeacherId }})</span>
            <el-tag v-else type="warning">未绑定教师ID，将按用户名匹配，保存时默认 teacherId=1</el-tag>
          </template>
        </el-alert>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>📅 我的可用时间（周视图）</span>
          <div>
            <el-button type="primary" :icon="Refresh" @click="loadData">🔄 刷新</el-button>
            <el-button type="success" :icon="Check" @click="fillAllWeek">✅ 整周全部可用</el-button>
            <el-button type="danger" :icon="Close" @click="clearAllWeek">❌ 整周清空</el-button>
            <el-button type="warning" :icon="MagicStick" @click="fillWeekday">🕘 工作日可用</el-button>
          </div>
        </div>
      </template>

      <div class="grid-wrapper" v-loading="loading">
        <table class="week-grid" border="1" cellpadding="0" cellspacing="0">
          <thead>
            <tr>
              <th class="col-head first-col">时间段 / 星期</th>
              <th v-for="d in weekDays" :key="d.day" :class="['col-head', d.cls]">
                {{ d.label }}
                <el-tag size="small" :type="d.cls" effect="plain" style="margin-left:6px;">{{ d.hint }}</el-tag>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="slot in slotOptions" :key="slot.value">
              <td class="row-head">{{ slot.label }}</td>
              <td v-for="d in weekDays" :key="`${d.day}-${slot.value}`"
                  :class="['cell', cellClass(d.day, slot.value)]"
                  @click="toggleCell(d.day, slot.value)">
                <div class="cell-inner">
                  <span v-if="isAvailable(d.day, slot.value)" class="mark">✔ 可用</span>
                  <span v-else class="mark dim">— 不可用</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <el-divider />

      <div class="list-title">
        <span>📋 已保存的可用时间明细（共 {{ myRecords.length }} 条）</span>
        <el-button type="primary" :icon="Plus" size="small" @click="openAddDialog">➕ 单条添加</el-button>
      </div>
      <el-table :data="myRecords.slice(0,20)" border size="small">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="dayOfWeek" label="星期" width="100" align="center">
          <template #default="{ row }">{{ dayText(row.dayOfWeek) }}</template>
        </el-table-column>
        <el-table-column prop="slotId" label="时间段ID" width="110" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="removeRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="myRecords.length === 0" class="empty-tip">
        <el-empty description="暂无可用时间记录，点击上方网格添加" />
      </div>
    </el-card>

    <el-dialog v-model="addDialogVisible" title="添加可用时间" width="420px">
      <el-form :model="addForm" label-width="90px">
        <el-form-item label="星期">
          <el-select v-model="addForm.dayOfWeek" style="width: 100%">
            <el-option v-for="d in weekDays" :key="d.day" :label="d.label" :value="d.day" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间段">
          <el-select v-model="addForm.slotId" style="width: 100%">
            <el-option v-for="s in slotOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus, Check, Close, MagicStick } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { teacherAvailableTimeApi } from '@/api/baseData'
import { teacherPortalApi } from '@/api/schedule'
import request from '@/utils/request'

const userStore = useUserStore()

const loading = ref(false)
const allRecords = ref([])
const slotOptions = ref([
  { label: '第 1-2 节 (8:00-9:40)', value: 1 },
  { label: '第 3-4 节 (10:00-11:40)', value: 2 },
  { label: '第 5-6 节 (14:00-15:40)', value: 3 },
  { label: '第 7-8 节 (16:00-17:40)', value: 4 },
  { label: '第 9-10 节 (19:00-20:40)', value: 5 }
])

const weekDays = [
  { day: 1, label: '周一', cls: '', hint: '工作日' },
  { day: 2, label: '周二', cls: '', hint: '工作日' },
  { day: 3, label: '周三', cls: '', hint: '工作日' },
  { day: 4, label: '周四', cls: '', hint: '工作日' },
  { day: 5, label: '周五', cls: '', hint: '工作日' },
  { day: 6, label: '周六', cls: 'warning', hint: '周末' },
  { day: 7, label: '周日', cls: 'danger', hint: '周末' }
]

function dayText(d) {
  return weekDays.find(x => x.day === Number(d))?.label || `周${d}`
}

const myTeacherId = computed(() => {
  const u = userStore.user || {}
  return u.teacherId ?? u.id ?? u.userId ?? null
})
function getSaveTeacherId() {
  return myTeacherId.value != null ? myTeacherId.value : 1
}

async function loadSlots() {
  try {
    const res = await request.get('/time-slot', { params: { page: 1, size: 100 } })
    allRecords.value = res?.data || []
    const list = Array.isArray(raw) ? raw : (raw.list || raw.records || [])
    if (list?.length) {
      slotOptions.value = list.map(x => ({
        label: `${x.name || x.slotName || `时间段${x.id}`}${x.startTime?` (${x.startTime}-${x.endTime})`:''}`,
        value: x.id
      }))
    }
  } catch (e) {}
}

async function loadData() {
  loading.value = true
  try {
    const res = await teacherPortalApi.myAvailableTime()
    allRecords.value = res?.data || []
  } catch (e) { allRecords.value = [] }
  finally { loading.value = false }
}

const myRecords = computed(() => {
  const list = allRecords.value || []
  const tid = myTeacherId.value
  const name = (userStore.user?.realName || userStore.user?.username || '').trim()
  return list.filter(x => {
    if (tid != null) {
      if (x.teacherId === tid || x.teacherID === tid) return true
    }
    if (name && x.teacherName === name) return true
    if (tid == null && !name) return true
    return false
  })
})

const availabilityMap = computed(() => {
  const map = new Map()
  for (const r of myRecords.value) {
    const d = Number(r.dayOfWeek); const s = Number(r.slotId)
    map.set(`${d}-${s}`, r)
  }
  return map
})
function isAvailable(day, slotId) { return availabilityMap.value.has(`${day}-${slotId}`) }
function cellClass(day, slotId) {
  const ok = isAvailable(day, slotId)
  if (ok) return ['available', ...(day>=6?['weekend']:[])]
  return ['unavailable', ...(day>=6?['weekend']:[])]
}

async function toggleCell(day, slotId) {
  const key = `${day}-${slotId}`
  const exist = availabilityMap.value.get(key)
  if (exist) {
    try {
      await teacherAvailableTimeApi.delete ? teacherAvailableTimeApi.delete(exist.id)
                                            : (teacherAvailableTimeApi.remove ? teacherAvailableTimeApi.remove(exist.id) : null)
      ElMessage.success(`已取消 ${dayText(day)} ${slotLabel(slotId)} 的可用时间`)
    } catch (e) { ElMessage.error('删除失败') }
  } else {
    try {
      await teacherAvailableTimeApi.create({ teacherId: getSaveTeacherId(), dayOfWeek: day, slotId })
      ElMessage.success(`已保存 ${dayText(day)} ${slotLabel(slotId)} 可用`)
    } catch (e) { ElMessage.error('保存失败') }
  }
  loadData()
}
function slotLabel(sid) { return slotOptions.value.find(x=>x.value===sid)?.label || `时间段${sid}` }

function fillAllWeek() {
  ElMessageBox.confirm('确定为所有星期和时间段设置可用吗？', '批量设置', { type: 'success' }).then(async () => {
    loading.value = true
    let ok = 0, fail = 0
    const tid = getSaveTeacherId()
    try {
      for (const d of weekDays) {
        for (const s of slotOptions.value) {
          if (!isAvailable(d.day, s.value)) {
            try { await teacherAvailableTimeApi.create({ teacherId: tid, dayOfWeek: d.day, slotId: s.value }); ok++ }
            catch { fail++ }
          }
        }
      }
      ElMessage.success(`新增 ${ok} 条成功${fail?`，${fail} 条失败`:''}`)
      loadData()
    } finally { loading.value = false }
  }).catch(()=>{})
}
function clearAllWeek() {
  ElMessageBox.confirm('确定清空我的所有可用时间吗？', '批量清空', { type: 'warning' }).then(async () => {
    loading.value = true
    let ok = 0, fail = 0
    try {
      for (const r of myRecords.value) {
        try {
          if (teacherAvailableTimeApi.delete) await teacherAvailableTimeApi.delete(r.id)
          else if (teacherAvailableTimeApi.remove) await teacherAvailableTimeApi.remove(r.id)
          ok++
        } catch { fail++ }
      }
      ElMessage.success(`删除 ${ok} 条成功${fail?`，${fail} 条失败`:''}`)
      loadData()
    } finally { loading.value = false }
  }).catch(()=>{})
}
function fillWeekday() {
  ElMessageBox.confirm('设置工作日（周一~周五）全部时间段可用吗？', '快捷设置', { type: 'info' }).then(async () => {
    loading.value = true
    let ok = 0
    const tid = getSaveTeacherId()
    try {
      for (const d of weekDays.filter(x=>x.day<=5)) {
        for (const s of slotOptions.value) {
          if (!isAvailable(d.day, s.value)) {
            try { await teacherAvailableTimeApi.create({ teacherId: tid, dayOfWeek: d.day, slotId: s.value }); ok++ } catch {}
          }
        }
      }
      ElMessage.success(`新增 ${ok} 条工作日可用时间`)
      loadData()
    } finally { loading.value = false }
  }).catch(()=>{})
}

async function removeRecord(row) {
  await ElMessageBox.confirm('确认删除该可用时间？', '删除', { type: 'warning' })
  try {
    if (teacherAvailableTimeApi.delete) await teacherAvailableTimeApi.delete(row.id)
    else if (teacherAvailableTimeApi.remove) await teacherAvailableTimeApi.remove(row.id)
    ElMessage.success('删除成功'); loadData()
  } catch (e) { ElMessage.error('删除失败') }
}

const addDialogVisible = ref(false)
const addForm = reactive({ dayOfWeek: 1, slotId: 1 })
function openAddDialog() { addForm.dayOfWeek = 1; addForm.slotId = slotOptions.value[0]?.value ?? 1; addDialogVisible.value = true }
async function submitAdd() {
  try {
    if (isAvailable(addForm.dayOfWeek, addForm.slotId)) { ElMessage.warning('该可用时间已存在'); return }
    await teacherAvailableTimeApi.create({ teacherId: getSaveTeacherId(), dayOfWeek: addForm.dayOfWeek, slotId: addForm.slotId })
    ElMessage.success('添加成功'); addDialogVisible.value = false; loadData()
  } catch (e) { ElMessage.error('添加失败') }
}

onMounted(() => { loadSlots(); loadData() })
</script>

<style scoped>
.page-container { padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.tip-card { margin-bottom: 0; }
.tip .me-tag { margin-left: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.grid-wrapper { overflow-x: auto; }
.week-grid { width: 100%; border-collapse: collapse; font-size: 13px; min-width: 780px; }
.week-grid th, .week-grid td { border: 1px solid #ebeef5; padding: 0; text-align: center; }
.week-grid .col-head { background: #f5f7fa; padding: 10px 6px; font-weight: 600; color: #303133; }
.week-grid .col-head.warning { background: #fdf6ec; }
.week-grid .col-head.danger { background: #fef0f0; }
.week-grid .row-head { background: #fafafa; padding: 10px 12px; font-weight: 500; color: #606266; min-width: 180px; text-align: left; }
.week-grid .first-col { min-width: 180px; }
.week-grid .cell { height: 56px; cursor: pointer; transition: all .15s; }
.week-grid .cell:hover { transform: scale(1.02); z-index: 1; position: relative; box-shadow: 0 0 0 2px #409EFF inset; }
.week-grid .cell.available { background: #f0f9eb; }
.week-grid .cell.available.weekend { background: #e1f3d8; }
.week-grid .cell.unavailable { background: #ffffff; }
.week-grid .cell.unavailable.weekend { background: #fafafa; }
.cell-inner { padding: 8px; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; }
.mark { font-weight: 600; color: #67c23a; font-size: 12px; }
.mark.dim { color: #c0c4cc; }
.list-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-weight: 600; color: #303133; }
.empty-tip { margin-top: 10px; }
</style>