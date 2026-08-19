<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">个人信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户ID">{{ user?.id || '—' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user?.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ user?.realName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="roleTagType" effect="plain">{{ roleText }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-button type="primary" :icon="Refresh" :loading="loading" style="margin-top: 16px" @click="loadInfo">
        刷新信息
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getInfo } from '@/api/auth'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const user = ref(userStore.user)
const loading = ref(false)

const roleText = computed(() => {
  const map = { admin: '管理员', teacher: '教师', student: '学生' }
  return map[user.value?.role] || '—'
})
const roleTagType = computed(() => {
  const map = { admin: 'primary', teacher: 'success', student: 'warning' }
  return map[user.value?.role] || 'info'
})

async function loadInfo() {
  loading.value = true
  try {
    const res = await getInfo()
    user.value = res.data
    userStore.setUser(res.data)
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadInfo()
})
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
