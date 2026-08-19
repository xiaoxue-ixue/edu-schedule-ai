<template>
  <BaseCrudPage
    :api="timeSlotApi"
    title-suffix="时间段"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { timeSlotApi } from '@/api/baseData'

const columns = [
  { prop: 'slotName', label: '节次名称', width: 140 },
  { prop: 'slotNo', label: '节次序号', width: 100, align: 'center' },
  { prop: 'startTime', label: '开始时间', width: 120 },
  { prop: 'endTime', label: '结束时间', width: 120 }
]

const formColumns = [
  { prop: 'slotName', label: '节次名称', required: true, default: '' },
  { prop: 'slotNo', label: '节次序号', type: 'number', min: 1, default: 1 },
  { prop: 'startTime', label: '开始时间', default: '' },
  { prop: 'endTime', label: '结束时间', default: '' }
]
</script>
