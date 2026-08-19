<template>
  <BaseCrudPage
    :api="campusApi"
    title-suffix="校区"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { campusApi } from '@/api/baseData'

const columns = [
  { prop: 'campusName', label: '校区名称', width: 160 },
  { prop: 'campusCode', label: '校区编码', width: 140 },
  { prop: 'address', label: '校区地址', width: 220 }
]

const formColumns = [
  { prop: 'campusName', label: '校区名称', required: true, default: '' },
  { prop: 'campusCode', label: '校区编码', required: true, default: '' },
  { prop: 'address', label: '校区地址', default: '' }
]
</script>
