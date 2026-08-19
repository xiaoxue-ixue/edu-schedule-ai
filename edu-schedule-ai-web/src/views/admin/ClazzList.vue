<template>
  <BaseCrudPage
    :api="clazzApi"
    title-suffix="班级"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { clazzApi } from '@/api/baseData'

const columns = [
  { prop: 'className', label: '班级名称', width: 160 },
  { prop: 'classCode', label: '班级编码', width: 140 },
  { prop: 'grade', label: '年级', width: 100 },
  { prop: 'studentCount', label: '人数', width: 100, align: 'center' },
  { prop: 'campusName', label: '所属校区', width: 140 }
]

const formColumns = [
  { prop: 'className', label: '班级名称', required: true, default: '' },
  { prop: 'classCode', label: '班级编码', required: true, default: '' },
  { prop: 'grade', label: '年级', default: '' },
  { prop: 'studentCount', label: '人数', type: 'number', min: 0, default: 0 },
  { prop: 'campusId', label: '所属校区', type: 'number', default: 1 }
]
</script>
