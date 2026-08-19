<template>
  <BaseCrudPage
    :api="promptTemplateApi"
    title-suffix="Prompt模板"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { promptTemplateApi } from '@/api/ai'

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'code', label: '模板编码', width: 160 },
  { prop: 'name', label: '模板名称', width: 180 },
  {
    prop: 'category', label: '分类', width: 140,
    dict: {
      SCHEDULE: { text: '排课', type: 'primary' },
      EVALUATION: { text: '评价', type: 'success' },
      ANALYSIS: { text: '分析', type: 'warning' },
      OTHER: { text: '其他', type: 'info' }
    }
  },
  { prop: 'variables', label: '变量列表', width: 200, showOverflowTooltip: true },
  { prop: 'content', label: '模板内容', minWidth: 300, showOverflowTooltip: true },
  {
    prop: 'enabled', label: '启用', width: 90, align: 'center',
    dict: {
      1: { text: '启用', type: 'success' },
      0: { text: '停用', type: 'info' }
    }
  }
]

const formColumns = [
  { prop: 'code', label: '模板编码', required: true, default: '' },
  { prop: 'name', label: '模板名称', required: true, default: '' },
  {
    prop: 'category', label: '分类', type: 'select', required: true, default: 'SCHEDULE',
    options: [
      { label: '排课', value: 'SCHEDULE' },
      { label: '评价', value: 'EVALUATION' },
      { label: '分析', value: 'ANALYSIS' },
      { label: '其他', value: 'OTHER' }
    ]
  },
  { prop: 'variables', label: '变量列表(逗号分隔)', default: '' },
  { prop: 'content', label: '模板内容(支持{变量}占位)', type: 'textarea', required: true, default: '' },
  {
    prop: 'enabled', label: '是否启用', type: 'select', default: 1,
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ]
  }
]
</script>