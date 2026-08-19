<template>
  <BaseCrudPage
    :api="scheduleRuleApi"
    title-suffix="排课规则"
    :columns="columns"
    :form-columns="formColumns"
  />
</template>

<script setup>
import BaseCrudPage from '@/components/BaseCrudPage.vue'
import { scheduleRuleApi } from '@/api/baseData'

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'ruleName', label: '规则名称', width: 180 },
  {
    prop: 'ruleType', label: '规则类型', width: 140,
    dict: {
      HARD: { text: '硬约束', type: 'danger' },
      SOFT: { text: '软约束', type: 'warning' },
      PREFER: { text: '偏好', type: 'primary' }
    }
  },
  { prop: 'priority', label: '优先级', width: 90, align: 'center' },
  { prop: 'triggerCondition', label: '触发条件', minWidth: 200, showOverflowTooltip: true },
  { prop: 'action', label: '处理动作', minWidth: 180, showOverflowTooltip: true },
  {
    prop: 'enabled', label: '启用', width: 90, align: 'center',
    dict: {
      1: { text: '启用', type: 'success' },
      0: { text: '停用', type: 'info' }
    }
  }
]

const formColumns = [
  { prop: 'ruleName', label: '规则名称', required: true, default: '' },
  {
    prop: 'ruleType', label: '规则类型', type: 'select', required: true, default: 'HARD',
    options: [
      { label: '硬约束', value: 'HARD' },
      { label: '软约束', value: 'SOFT' },
      { label: '偏好', value: 'PREFER' }
    ]
  },
  { prop: 'priority', label: '优先级', type: 'number', default: 100 },
  { prop: 'triggerCondition', label: '触发条件', type: 'textarea', default: '' },
  { prop: 'action', label: '处理动作', type: 'textarea', default: '' },
  {
    prop: 'enabled', label: '是否启用', type: 'select', default: 1,
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ]
  }
]
</script>