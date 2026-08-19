import request from '@/utils/request'
import { createCrud } from './baseData'

// AI 模型配置
export const aiModelConfigApi = createCrud('/ai-model-config')
export function testAiModelConfig(id) {
  return request({ url: `/ai-model-config/${id}/test`, method: 'post' })
}

// Prompt 模板
export const promptTemplateApi = createCrud('/prompt-template')

// AI 调用日志
export const aiCallLogApi = createCrud('/ai-call-log')

// AI 教学质量分析
export function generateQualityAnalysis(data) {
  return request({ url: '/ai-quality-analysis/generate', method: 'post', data })
}
