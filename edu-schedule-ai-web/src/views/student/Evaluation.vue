<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">教学评教</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
        <el-form-item label="课程" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称">
            <template #append>
              <span v-if="form.courseId">ID: {{ form.courseId }}</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="教师" prop="teacherName">
          <el-input v-model="form.teacherName" placeholder="请输入教师姓名">
            <template #append>
              <span v-if="form.teacherId">ID: {{ form.teacherId }}</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-input v-model="form.className" placeholder="请输入班级" />
        </el-form-item>

        <el-divider content-position="left">维度评分（1-5星，系统自动换算为百分制）</el-divider>

        <el-form-item
          v-for="dim in dimensions"
          :key="dim.prop"
          :label="dim.label"
        >
          <div class="rate-row">
            <el-rate v-model="form[dim.prop]" :max="5" show-text :texts="rateTexts" />
            <span class="rate-desc">{{ dim.desc }}</span>
          </div>
        </el-form-item>

        <el-divider content-position="left">文字反馈</el-divider>

        <el-form-item label="总体评价" prop="feedback">
          <el-input
            v-model="form.feedback"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入您对该课程/教师的总体评价与建议"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交评教</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { submitEvaluation } from '@/api/student'

const formRef = ref()
const submitLoading = ref(false)

const rateTexts = ['很差', '较差', '一般', '较好', '很好']

const dimensions = [
  { prop: 'attitudeScore', label: '教学态度', desc: '教师教学态度是否认真负责' },
  { prop: 'contentScore', label: '教学内容', desc: '教学内容是否充实、条理清晰' },
  { prop: 'methodScore', label: '教学方法', desc: '教学方法是否灵活、易于理解' },
  { prop: 'effectScore', label: '教学效果', desc: '整体学习效果与收获' }
]

const form = reactive({
  courseId: '',
  courseName: '',
  teacherId: '',
  teacherName: '',
  classId: '',
  className: '',
  attitudeScore: 0,
  contentScore: 0,
  methodScore: 0,
  effectScore: 0,
  feedback: ''
})

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherName: [{ required: true, message: '请输入教师姓名', trigger: 'blur' }]
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const hasScore = dimensions.some((d) => form[d.prop] > 0)
    if (!hasScore) {
      ElMessage.warning('请至少完成一项维度评分')
      return
    }
    submitLoading.value = true
    try {
      const payload = {
        ...form,
        attitudeScore: form.attitudeScore ? Number(form.attitudeScore) * 20 : null,
        contentScore: form.contentScore ? Number(form.contentScore) * 20 : null,
        methodScore: form.methodScore ? Number(form.methodScore) * 20 : null,
        effectScore: form.effectScore ? Number(form.effectScore) * 20 : null
      }
      await submitEvaluation(payload)
      ElMessage.success('评教提交成功，感谢您的反馈！')
      handleReset()
    } catch (e) {
      // ignore
    } finally {
      submitLoading.value = false
    }
  })
}

function handleReset() {
  formRef.value && formRef.value.resetFields()
  Object.assign(form, {
    attitudeScore: 0,
    contentScore: 0,
    methodScore: 0,
    effectScore: 0,
    feedback: ''
  })
}
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.rate-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.rate-desc {
  font-size: 12px;
  color: #909399;
}
</style>
