<template>
  <div class="page-container">
    <el-card shadow="never" class="placeholder-card">
      <div class="placeholder-inner">
        <el-icon class="placeholder-icon"><Document /></el-icon>
        <h2 class="placeholder-title">{{ title }}</h2>
        <p class="placeholder-desc">该模块正在建设中，敬请期待。</p>
        <p class="placeholder-tip">对应后端接口：<code>{{ apiPath || '—' }}</code></p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, default: '页面' },
  apiPath: { type: String, default: '' }
})
</script>

<style scoped>
.placeholder-card {
  border-radius: 8px;
}
.placeholder-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}
.placeholder-icon {
  font-size: 64px;
  color: #c0c4cc;
  margin-bottom: 20px;
}
.placeholder-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.placeholder-desc {
  color: #909399;
  margin-bottom: 8px;
}
.placeholder-tip {
  color: #909399;
  font-size: 13px;
}
.placeholder-tip code {
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 4px;
  color: #67c23a;
}
</style>
