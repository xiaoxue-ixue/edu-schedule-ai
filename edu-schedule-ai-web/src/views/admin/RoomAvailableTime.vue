<template>
  <BaseCrudPage
    :api="roomAvailableTimeApi"
    title-suffix="教室可用时间"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { roomAvailableTimeApi } from '@/api/baseData'

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'roomId', label: '教室ID', width: 100 },
  {
    prop: 'dayOfWeek', label: '星期', width: 100, align: 'center',
    dict: {
      1: { text: '周一', type: '' },
      2: { text: '周二', type: '' },
      3: { text: '周三', type: '' },
      4: { text: '周四', type: '' },
      5: { text: '周五', type: '' },
      6: { text: '周六', type: 'warning' },
      7: { text: '周日', type: 'danger' }
    }
  },
  { prop: 'slotId', label: '时间段ID', width: 120 }
]

const formColumns = [
  { prop: 'roomId', label: '教室ID', type: 'number', required: true, default: 1 },
  {
    prop: 'dayOfWeek', label: '星期', type: 'select', required: true, default: 1,
    options: [
      { label: '周一', value: 1 },
      { label: '周二', value: 2 },
      { label: '周三', value: 3 },
      { label: '周四', value: 4 },
      { label: '周五', value: 5 },
      { label: '周六', value: 6 },
      { label: '周日', value: 7 }
    ]
  },
  { prop: 'slotId', label: '时间段ID', type: 'number', required: true, default: 1 }
]
</script>