<template>
  <BaseCrudPage
    :api="termApi"
    title-suffix="学期"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { termApi } from '@/api/baseData'

const columns = [
  { prop: 'termName', label: '学期名称', width: 160 },
  { prop: 'termCode', label: '学期编码', width: 140 },
  { prop: 'startDate', label: '开始日期', width: 140 },
  { prop: 'endDate', label: '结束日期', width: 140 },
  {
    prop: 'status', label: '状态', width: 100, align: 'center',
    dict: {
      1: { text: '启用', type: 'success' },
      0: { text: '停用', type: 'info' }
    }
  }
]

const formColumns = [
  { prop: 'termName', label: '学期名称', required: true, default: '' },
  { prop: 'termCode', label: '学期编码', required: true, default: '' },
  { prop: 'startDate', label: '开始日期', default: '' },
  { prop: 'endDate', label: '结束日期', default: '' },
  {
    prop: 'status', label: '状态', type: 'select', default: 1,
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ]
  }
]
</script>
