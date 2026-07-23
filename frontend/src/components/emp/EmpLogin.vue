<template>
  <div ref="pageRoot" class="auth-page auth-employee">
    <section class="auth-visual" aria-label="Nexus Office 企业办公空间">
      <NexusWebGL variant="auth" />
      <div class="auth-grid"></div>
      <div class="auth-scanline"></div>
      <div class="auth-vector-line" data-line-reveal></div>
      <div class="auth-brand" data-reveal>
        <div class="brand-symbol"><span>N</span></div>
        <div class="brand-copy">
          <strong>NEXUS <em>OFFICE</em></strong>
          <small>企业数字办公中枢</small>
        </div>
      </div>

      <div class="auth-telemetry" aria-hidden="true">
        <span>NEURAL WORKSPACE</span>
        <b>01.824</b>
        <small>LATENCY / 08MS</small>
      </div>
      <div class="auth-coordinates" aria-hidden="true"><i></i> X 113.42 / Y 39.27</div>

      <div class="auth-story">
        <div class="auth-status" data-reveal><span></span> NEXUS NETWORK / ONLINE</div>
        <p class="auth-kicker" data-reveal>WORKPLACE, REIMAGINED</p>
        <h1 data-reveal>让每一次工作<br /><span>都清晰发生。</span></h1>
        <p class="auth-lead" data-reveal>连接人员、考勤与企业知识，让日常办公自然流动。</p>
        <div class="auth-metrics" data-reveal>
          <div><strong>01</strong><span>统一工作台</span></div>
          <div><strong>24/7</strong><span>智能服务</span></div>
          <div><strong>100%</strong><span>实时协同</span></div>
        </div>
      </div>

      <div class="auth-index">EMP / 01</div>
    </section>

    <main class="auth-panel-wrap">
      <button class="auth-role-switch" type="button" data-magnetic data-cursor @click="goToAdminLogin">
        <el-icon><Setting /></el-icon>
        管理员入口
        <el-icon><ArrowRight /></el-icon>
      </button>

      <div class="auth-panel" data-reveal>
        <div class="auth-panel-heading">
          <span>EMPLOYEE ACCESS</span>
          <h2>欢迎回来</h2>
          <p>登录你的工作空间，继续今天的进度。</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="员工工号" prop="number">
            <el-input
              v-model="loginForm.number"
              type="text"
              inputmode="numeric"
              autocomplete="username"
              placeholder="请输入员工工号"
              size="large"
            >
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="初始密码为 123"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="auth-submit"
            data-magnetic
            data-cursor
            @click="handleLogin"
          >
            进入工作台
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="auth-assurance">
          <span><el-icon><CircleCheck /></el-icon> 安全会话</span>
          <span><el-icon><Connection /></el-icon> 服务在线</span>
        </div>
      </div>

      <p class="auth-copyright">NEXUS OFFICE · ENTERPRISE WORKSPACE</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { ArrowRight, CircleCheck, Connection, Lock, Setting, User } from '@element-plus/icons-vue'
import axios from 'axios'
import NexusWebGL from '../effects/NexusWebGL.vue'
import { useNexusMotion } from '../../composables/useNexusMotion'

const router = useRouter()
const pageRoot = ref<HTMLDivElement>()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

useNexusMotion(pageRoot)

const loginForm = reactive({
  number: '',
  password: ''
})

const rules = {
  number: [{ required: true, message: '请输入您的账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入您的密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/v1/employee/login', {
          number: parseInt(loginForm.number),
          pwd: loginForm.password
        }, {
          headers: { 'Content-Type': 'application/json' }
        })

        const token = response.data?.data?.token
        if (token) {
          sessionStorage.setItem('employeeToken', token)
          ElMessage.success('登录成功')
          router.push('/emp-home/info')
        } else {
          ElMessage.error('登录失败，请检查用户名和密码')
        }
      } catch (error) {
        console.error('登录错误:', error)
        ElMessage.error('登录失败，请检查网络连接')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToAdminLogin = () => {
  router.push('/admin-login')
}
</script>
