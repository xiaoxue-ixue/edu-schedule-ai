<template>
  <BaseCrudPage
    :api="functionToolApi"
    title-suffix="Function 工具注册"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { functionToolApi } from '@/api/baseData'

const categoryDict = {
  DATA_QUERY: { text: '数据查询', type: 'primary' },
  WRITE: { text: '写入排课', type: 'success' },
  CONFLICT: { text: '冲突检测', type: 'warning' },
  VALIDATE: { text: '规则校验', type: 'info' },
  OTHER: { text: '其他', type: '' }
}

const enabledDict = {
  1: { text: '启用', type: 'success' },
  0: { text: '停用', type: 'info' }
}

const columns = [
  { prop: 'id', label: 'ID', width: 70, align: 'center' },
  { prop: 'toolCode', label: '工具编码', width: 150 },
  { prop: 'toolName', label: '工具名称', width: 150 },
  { prop: 'category', label: '分类', width: 110, align: 'center', dict: categoryDict },
  { prop: 'description', label: '工具描述', minWidth: 260, showOverflowTooltip: true },
  { prop: 'enabled', label: '启用', width: 90, align: 'center', dict: enabledDict }
]

const formColumns = [
  { prop: 'toolCode', label: '工具编码', required: true, default: '', placeholder: '例：queryCourses' },
  { prop: 'toolName', label: '工具名称', required: true, default: '', placeholder: '例：查询课程列表' },
  {
    prop: 'category', label: '分类', type: 'select', required: true, default: 'DATA_QUERY',
    options: [
      { label: '数据查询', value: 'DATA_QUERY' },
      { label: '写入排课', value: 'WRITE' },
      { label: '冲突检测', value: 'CONFLICT' },
      { label: '规则校验', value: 'VALIDATE' },
      { label: '其他', value: 'OTHER' }
    ]
  },
  { prop: 'description', label: '工具描述（给 Agent 看的说明）', type: 'textarea', required: true, default: '', rows: 3 },
  { prop: 'parameters', label: '参数 JSON Schema（选填）', type: 'textarea', default: '{}', rows: 4, placeholder: 'JSON格式，描述工具入参结构' },
  {
    prop: 'enabled', label: '是否启用', type: 'select', default: 1,
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ]
  }
]
</script>