<template>
  <div class="workspace-shell employee-workspace">
    <aside class="workspace-sidebar">
      <div class="workspace-brand" aria-label="Nexus Office">
        <div class="brand-symbol"><span>N</span></div>
        <div class="brand-copy">
          <strong>NEXUS <em>OFFICE</em></strong>
          <small>企业数字办公中枢</small>
        </div>
      </div>

      <div class="workspace-edition">
        <span class="edition-pulse"></span>
        员工工作台
        <b>WORK</b>
      </div>

      <nav class="workspace-nav" aria-label="员工功能导航">
        <span class="nav-caption">我的工作</span>
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/emp-home/info">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/emp-home/sign-in">
            <el-icon><Clock /></el-icon>
            <span>员工签到</span>
          </el-menu-item>
          <el-menu-item index="/emp-home/sign-message">
            <el-icon><Document /></el-icon>
            <span>签到记录</span>
          </el-menu-item>
          <el-menu-item index="/emp-home/leave">
            <el-icon><Document /></el-icon>
            <span>请假申请</span>
          </el-menu-item>
          <span class="nav-caption nav-caption-group">智能服务</span>
          <el-menu-item index="/emp-home/ai-chat">
            <el-icon><Service /></el-icon>
            <span>AI 办公助手</span>
            <i class="nav-ai-badge">AI</i>
          </el-menu-item>
          <el-menu-item index="/emp-home/update-pwd">
            <el-icon><Lock /></el-icon>
            <span>账户安全</span>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="workspace-sidebar-footer">
        <div class="system-indicator">
          <span></span>
          <div>
            <strong>办公服务在线</strong>
            <small>随时为你响应</small>
          </div>
        </div>
        <span class="system-version">NEXUS OS / 2.0</span>
      </div>
    </aside>

    <section class="workspace-stage">
      <header class="workspace-topbar">
        <div class="topbar-title">
          <span>MY WORKSPACE / {{ currentDate }}</span>
          <h1>{{ routeTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <div class="live-chip employee-live">
            <span></span>
            工作中
          </div>
          <div class="profile-block">
            <div class="profile-avatar">{{ userInitial }}</div>
            <div class="profile-copy">
              <strong>{{ userInfo.name || '员工' }}</strong>
              <small>{{ userInfo.dept_name || '企业员工' }}</small>
            </div>
          </div>
          <el-tooltip content="退出登录" placement="bottom">
            <button class="icon-command" type="button" aria-label="退出登录" @click="logout">
              <el-icon><SwitchButton /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </header>

      <main class="workspace-main">
        <router-view v-slot="{ Component }">
          <transition mode="out-in" appear :css="false" @enter="handlePageEnter" @leave="handlePageLeave">
            <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Document, Lock, Service, SwitchButton, User } from '@element-plus/icons-vue'
import axios from 'axios'
import { gsap } from 'gsap'

const route = useRoute()
const router = useRouter()

interface EmployeeProfile {
  name?: string
  dept_name?: string
}

const userInfo = ref<EmployeeProfile>({})

const routeTitles: Record<string, string> = {
  '/emp-home/info': '个人信息',
  '/emp-home/sign-in': '今日签到',
  '/emp-home/sign-message': '签到记录',
  '/emp-home/leave': '请假申请',
  '/emp-home/update-pwd': '账户安全',
  '/emp-home/ai-chat': 'AI 办公助手'
}

const routeTitle = computed(() => routeTitles[route.path] || '员工工作台')
const currentDate = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short'
}).format(new Date())
const userInitial = computed(() => String(userInfo.value.name || '员').slice(0, 1))

const handlePageEnter = (element: Element, done: () => void) => {
  gsap.fromTo(element, {
    opacity: 0,
    y: 20,
    scale: 0.992,
    filter: 'blur(9px)'
  }, {
    opacity: 1,
    y: 0,
    scale: 1,
    filter: 'blur(0px)',
    duration: 0.72,
    ease: 'expo.out',
    clearProps: 'transform,filter',
    onComplete: done
  })
}

const handlePageLeave = (element: Element, done: () => void) => {
  gsap.to(element, {
    opacity: 0,
    y: -10,
    scale: 0.995,
    filter: 'blur(5px)',
    duration: 0.24,
    ease: 'power2.in',
    onComplete: done
  })
}

onMounted(async () => {
  try {
    const response = await axios.get('/api/v1/employee/profile')
    if (response.data?.data) {
      userInfo.value = response.data.data
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
  }
})

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    try {
      await axios.post('/api/v1/employee/logout')
      ElMessage.success('退出登录成功')
    } catch (error) {
      console.error('退出登录失败:', error)
      ElMessage.warning('退出登录失败，但将跳转到登录页')
    } finally {
      sessionStorage.removeItem('employeeToken')
      userInfo.value = {}
      router.push('/emp-login')
    }
  } catch {
    ElMessage.info('已取消退出')
  }
}
</script>
