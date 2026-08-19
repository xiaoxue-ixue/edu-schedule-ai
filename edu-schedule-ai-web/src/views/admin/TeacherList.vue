<template>
  <BaseCrudPage
    :api="teacherApi"
    title-suffix="教师"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { teacherApi } from '@/api/baseData'

const columns = [
  { prop: 'teacherNo', label: '工号', width: 120 },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'title', label: '职称', width: 120 },
  { prop: 'campusName', label: '所属校区', width: 140 },
  { prop: 'phone', label: '联系电话', width: 140 }
]

const formColumns = [
  { prop: 'teacherNo', label: '工号', required: true, default: '' },
  { prop: 'name', label: '姓名', required: true, default: '' },
  { prop: 'title', label: '职称', default: '' },
  { prop: 'campusId', label: '所属校区', type: 'number', default: 1 },
  { prop: 'phone', label: '联系电话', default: '' }
]
</script>
