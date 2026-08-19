<template>
  <BaseCrudPage
    :api="roomApi"
    title-suffix="教室"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { roomApi } from '@/api/baseData'

const columns = [
  { prop: 'roomName', label: '教室名称', width: 160 },
  { prop: 'roomCode', label: '教室编码', width: 140 },
  { prop: 'campusName', label: '所在校区', width: 140 },
  { prop: 'capacity', label: '容量', width: 100, align: 'center' },
  { prop: 'roomType', label: '类型', width: 120, align: 'center' }
]

const formColumns = [
  { prop: 'roomName', label: '教室名称', required: true, default: '' },
  { prop: 'roomCode', label: '教室编码', required: true, default: '' },
  { prop: 'campusId', label: '所在校区ID', type: 'number', default: 1 },
  { prop: 'capacity', label: '容量', type: 'number', min: 0, default: 0 },
  {
    prop: 'roomType', label: '类型', type: 'select', default: '普通',
    options: [
      { label: '普通教室', value: '普通' },
      { label: '实验室', value: '实验室' },
      { label: '多媒体', value: '多媒体' },
      { label: '机房', value: '机房' }
    ]
  }
]
</script>
