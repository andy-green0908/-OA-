<template>
  <div ref="pageRoot" class="auth-page auth-admin">
    <section class="auth-visual" aria-label="Nexus Office 管理空间">
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
        <span>COMMAND LAYER</span>
        <b>02.360</b>
        <small>SECURE / AES-256</small>
      </div>
      <div class="auth-coordinates" aria-hidden="true"><i></i> X 88.19 / Y 202.61</div>

      <div class="auth-story">
        <div class="auth-status" data-reveal><span></span> CONTROL PLANE / SECURE</div>
        <p class="auth-kicker" data-reveal>OPERATIONS, IN FOCUS</p>
        <h1 data-reveal>洞察组织脉搏，<br /><span>让决策先一步。</span></h1>
        <p class="auth-lead" data-reveal>从人员到考勤，从数据到智能知识，一站掌控企业运行。</p>
        <div class="auth-metrics" data-reveal>
          <div><strong>360°</strong><span>组织视图</span></div>
          <div><strong>LIVE</strong><span>实时数据</span></div>
          <div><strong>AI</strong><span>知识中枢</span></div>
        </div>
      </div>

      <div class="auth-index">ADM / 02</div>
    </section>

    <main class="auth-panel-wrap">
      <button class="auth-role-switch" type="button" data-magnetic data-cursor @click="goToEmpLogin">
        <el-icon><User /></el-icon>
        员工入口
        <el-icon><ArrowRight /></el-icon>
      </button>

      <div class="auth-panel" data-reveal>
        <div class="auth-panel-heading">
          <span>ADMIN ACCESS</span>
          <h2>管理控制台</h2>
          <p>使用管理员凭据进入企业运营中心。</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="管理员账号" prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              autocomplete="username"
              placeholder="请输入管理员账号"
              size="large"
            >
              <template #prefix><el-icon><UserFilled /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入登录密码"
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
            进入管理中心
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <button class="auth-register-link" type="button" @click="showRegisterDialog">
            首次使用？创建管理员账户
          </button>
        </el-form>

        <div class="auth-assurance">
          <span><el-icon><CircleCheck /></el-icon> 权限隔离</span>
          <span><el-icon><Connection /></el-icon> 加密连接</span>
        </div>
      </div>

      <p class="auth-copyright">NEXUS OFFICE · ADMINISTRATION SYSTEM</p>
    </main>

    <el-dialog
      v-model="registerDialogVisible"
      title="创建管理员账户"
      width="440px"
      @close="resetRegisterForm"
    >
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="管理员姓名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="设置密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetRegisterForm">重置</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">
          创建账户
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { ArrowRight, CircleCheck, Connection, Lock, User, UserFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import NexusWebGL from '../effects/NexusWebGL.vue'
import { useNexusMotion } from '../../composables/useNexusMotion'

const router = useRouter()
const pageRoot = ref<HTMLDivElement>()
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registerLoading = ref(false)
const registerDialogVisible = ref(false)

useNexusMotion(pageRoot)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/v1/admin/auth/login', {
          name: loginForm.username,
          pwd: loginForm.password
        }, {
          headers: { 'Content-Type': 'application/json' }
        })

        const token = response.data?.data?.token
        if (token) {
          sessionStorage.setItem('adminToken', token)
          ElMessage.success('登录成功')
          router.push('/admin-home/dashboard')
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

const showRegisterDialog = () => {
  registerDialogVisible.value = true
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      registerLoading.value = true
      try {
        const response = await axios.post('/api/v1/admin/auth/register', {
          name: registerForm.username,
          pwd: registerForm.password
        }, {
          headers: { 'Content-Type': 'application/json' }
        })

        if (response.data === 'true' || response.data === true) {
          ElMessage.success('注册成功')
          registerDialogVisible.value = false
          resetRegisterForm()
        } else {
          ElMessage.error('注册失败，用户名重复')
        }
      } catch (error) {
        console.error('注册错误:', error)
        ElMessage.error('注册失败，请检查网络连接')
      } finally {
        registerLoading.value = false
      }
    }
  })
}

const resetRegisterForm = () => {
  registerForm.username = ''
  registerForm.password = ''
  registerFormRef.value?.resetFields()
}

const goToEmpLogin = () => {
  router.push('/emp-login')
}
</script>
