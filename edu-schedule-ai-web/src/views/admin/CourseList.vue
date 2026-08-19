<template>
  <BaseCrudPage
    :api="courseApi"
    title-suffix="课程"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { courseApi } from '@/api/baseData'

const columns = [
  { prop: 'courseCode', label: '课程编码', width: 140 },
  { prop: 'courseName', label: '课程名称', width: 180 },
  { prop: 'credit', label: '学分', width: 90, align: 'center' },
  { prop: 'hours', label: '学时', width: 90, align: 'center' },
  { prop: 'teacherName', label: '主讲教师', width: 120 }
]

const formColumns = [
  { prop: 'courseCode', label: '课程编码', required: true, default: '' },
  { prop: 'courseName', label: '课程名称', required: true, default: '' },
  { prop: 'credit', label: '学分', type: 'number', min: 0, default: 0 },
  { prop: 'hours', label: '学时', type: 'number', min: 0, default: 0 },
  { prop: 'teacherId', label: '主讲教师ID', type: 'number', default: 1 }
]
</script>
