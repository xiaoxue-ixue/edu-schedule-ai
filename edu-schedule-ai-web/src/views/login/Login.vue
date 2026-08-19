<template>
  <div class="login-page">
    <div class="login-bg" />
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <el-icon class="header-icon"><School /></el-icon>
        <h1 class="header-title">智能教务排课与教学质量分析系统</h1>
        <p class="header-sub">AI Agent Smart Education Platform</p>
      </div>
      <el-tabs v-model="activeTab" stretch @tab-change="onTabChange">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
                clearable
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" class="tip-alert">
            <template #title>
              <div>默认账号：</div>
              <div>管理员 admin / admin</div>
              <div>教师 teacher01 / 123456</div>
              <div>学生 student01 / 123456</div>
            </template>
          </el-alert>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-alert
            v-if="registrationClosed"
            type="warning"
            :closable="false"
            show-icon
            class="closed-alert"
          >
            <template #title>管理员已关闭开放注册</template>
            请联系管理员在【用户管理】中为您创建账号。默认管理员账号：<b>admin / admin</b>
          </el-alert>

          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            :disabled="registrationClosed"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名（登录用）" :prefix-icon="User" size="large" clearable />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名（排课/评价实名显示）" :prefix-icon="UserFilled" size="large" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码（至少6位）" :prefix-icon="Lock" size="large" show-password clearable />
            </el-form-item>

            <!-- ===== 图形验证码（点击图片刷新，或点击右侧刷新按钮） ===== -->
            <el-form-item label="图形验证码" prop="captchaCode" required>
              <div class="captcha-row">
                <el-input
                  v-model="registerForm.captchaCode"
                  placeholder="请输入右侧图中字符（忽略大小写）"
                  size="large"
                  maxlength="6"
                  clearable
                />
                <div class="captcha-image-wrapper" :title="captchaLoading ? '' : '点击刷新验证码'" @click="refreshCaptcha">
                  <img
                    v-if="captchaImgSrc"
                    :src="captchaImgSrc"
                    :class="{ blurred: captchaLoading }"
                    class="captcha-image"
                    alt="验证码"
                    draggable="false"
                  />
                  <div v-else class="captcha-placeholder">
                    <el-icon :size="22" v-loading="captchaLoading"><Picture /></el-icon>
                  </div>
                  <el-icon class="captcha-refresh-icon" :size="16"><Refresh /></el-icon>
                </div>
              </div>
              <div class="captcha-tip">
                看不清？点击图片或右侧图标刷新（{{ captchaExpireSeconds }}s 内有效）
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="registerLoading"
                :disabled="registrationClosed"
                @click="handleRegister"
              >
                {{ registrationClosed ? '注册已关闭' : '注 册（默认学生角色）' }}
              </el-button>
            </el-form-item>
          </el-form>
          <div class="register-tip">
            注册成功后默认为<b>学生角色</b>。需要教师/管理员权限？请联系教务端管理员提升角色。
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { School, User, UserFilled, Lock, Picture, Refresh } from '@element-plus/icons-vue'
import { login, register, generateCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { roleHome } from '@/router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()
const loginLoading = ref(false)
const registerLoading = ref(false)

// ---------- 图形验证码状态 ----------
const captchaLoading = ref(false)
const captchaImgSrc = ref('')           // data:image/png;base64,xxx
const captchaExpireSeconds = ref(180)

const loginForm = reactive({
  username: 'admin',
  password: 'admin'
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 注册表单（加 2 个验证码字段）
const registerForm = reactive({
  username: '',
  realName: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }]
}

// 生产可能关注册（通过第一次请求 captcha/generate 不知道；通过 register 返回 4031 时置 true）
const registrationClosed = ref(false)

// ---------- 验证码刷新 ----------
async function refreshCaptcha() {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    const res = await generateCaptcha()
    registerForm.captchaKey = res.data.captchaKey
    captchaExpireSeconds.value = res.data.expireSeconds || 180
    captchaImgSrc.value = 'data:image/png;base64,' + res.data.imageBase64
    // 刷新后清空用户之前填的验证码（防止复用老的）
    registerForm.captchaCode = ''
  } catch (e) {
    // 429 限流 / 403 关闭注册 / 网络异常 都会被 request.js 弹 ElMessage
    captchaImgSrc.value = ''
    registerForm.captchaKey = ''
  } finally {
    captchaLoading.value = false
  }
}

// 切到注册 tab / 第一次打开页面：自动刷验证码
function onTabChange(tab) {
  if (tab === 'register' && !registrationClosed.value) {
    refreshCaptcha()
  }
}
watch(() => activeTab.value, onTabChange)
onMounted(() => {
  if (route.hash === '#register' || route.query.tab === 'register') {
    activeTab.value = 'register'
  }
})

// ---------- 登录 ----------
async function handleLogin() {
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loginLoading.value = true
    try {
      const res = await login({ ...loginForm })
      const { token, user } = res.data
      userStore.setToken(token)
      userStore.setUser(user)
      ElMessage.success(`欢迎回来，${user.realName || user.username}`)
      const redirect = route.query.redirect
      router.push(redirect || roleHome(user.role))
    } catch (e) {
      // 错误已由响应拦截器提示
    } finally {
      loginLoading.value = false
    }
  })
}

// ---------- 注册 ----------
async function handleRegister() {
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    // 提前拦截：验证码 key 没拿到（可能刷不出图，前端就不让提交）
    if (!registerForm.captchaKey) {
      ElMessage.warning('验证码尚未加载完成，请稍等或点击刷新')
      return
    }
    registerLoading.value = true
    try {
      await register({
        username: registerForm.username,
        realName: registerForm.realName,
        password: registerForm.password,
        captchaKey: registerForm.captchaKey,
        captchaCode: registerForm.captchaCode
      })
      ElMessage.success('注册成功！默认学生角色，请使用新账号登录')
      activeTab.value = 'login'
      loginForm.username = registerForm.username
      loginForm.password = ''
    } catch (e) {
      // request.js 已弹 message；这里做业务层面副作用：
      const code = e?.response?.data?.code ?? (e?.message?.includes('管理员已关闭') ? 4031 : null)
      // 1) 验证码错误/过期 → 自动刷新 + 清输入
      if (code === 40001 || /验证码|captcha/i.test(e?.response?.data?.message || e?.message || '')) {
        registerForm.captchaCode = ''
        await refreshCaptcha()
      }
      // 2) 管理员关闭注册 → 锁住整个注册 UI，防止再尝试
      if (code === 4031 || /已关闭开放注册|REGISTRATION_CLOSED/i.test(e?.response?.data?.message || e?.message || '')) {
        registrationClosed.value = true
      }
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
}
.login-card {
  width: 420px;
  max-width: 92vw;
  border-radius: 12px;
  z-index: 1;
  padding: 8px 12px;
}
.login-header {
  text-align: center;
  margin-bottom: 12px;
}
.header-icon {
  font-size: 42px;
  color: #409eff;
}
.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 8px 0 4px;
}
.header-sub {
  font-size: 12px;
  color: #909399;
  letter-spacing: 1px;
}
.submit-btn {
  width: 100%;
}
.tip-alert {
  margin-top: 8px;
  line-height: 1.8;
}
.closed-alert {
  margin-bottom: 12px;
  line-height: 1.7;
}
.register-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

/* ---------- 验证码行样式 ---------- */
.captcha-row {
  display: flex;
  gap: 8px;
  align-items: stretch;
}
.captcha-row .el-input {
  flex: 1;
}
.captcha-image-wrapper {
  position: relative;
  width: 130px;
  height: 40px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}
.captcha-image-wrapper:hover {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
  transform: translateY(-1px);
}
.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: fill;
  user-select: none;
  -webkit-user-drag: none;
}
.captcha-image.blurred {
  filter: blur(1px) opacity(0.6);
}
.captcha-placeholder {
  color: #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-refresh-icon {
  position: absolute;
  right: 4px;
  bottom: 2px;
  color: #909399;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  padding: 2px;
  pointer-events: none;
}
.captcha-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>