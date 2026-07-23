<template>
  <div class="workspace-shell admin-workspace">
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
        管理控制台
        <b>ADMIN</b>
      </div>

      <nav class="workspace-nav" aria-label="管理员功能导航">
        <span class="nav-caption">工作台</span>
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/admin-home/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>数据面板</span>
          </el-menu-item>
          <span class="nav-caption nav-caption-group">组织与人事</span>
          <el-menu-item index="/admin-home/emp-list">
            <el-icon><User /></el-icon>
            <span>员工管理</span>
          </el-menu-item>
          <el-menu-item index="/admin-home/dept-manage">
            <el-icon><OfficeBuilding /></el-icon>
            <span>部门管理</span>
          </el-menu-item>
          <el-menu-item index="/admin-home/duty-manage">
            <el-icon><Briefcase /></el-icon>
            <span>职务管理</span>
          </el-menu-item>
          <span class="nav-caption nav-caption-group">运营洞察</span>
          <el-menu-item index="/admin-home/sign-list">
            <el-icon><Clock /></el-icon>
            <span>考勤管理</span>
          </el-menu-item>
          <el-menu-item index="/admin-home/unsigned-list">
            <el-icon><CircleClose /></el-icon>
            <span>未签到补签</span>
          </el-menu-item>
          <el-menu-item index="/admin-home/sign-statistics">
            <el-icon><PieChart /></el-icon>
            <span>考勤统计</span>
          </el-menu-item>
          <el-menu-item index="/admin-home/leave-manage">
            <el-icon><Document /></el-icon>
            <span>请假审批</span>
          </el-menu-item>
          <el-menu-item index="/admin-home/kb-manage">
            <el-icon><ChatDotRound /></el-icon>
            <span>知识库管理</span>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="workspace-sidebar-footer">
        <div class="system-indicator">
          <span></span>
          <div>
            <strong>服务运行正常</strong>
            <small>全部模块在线</small>
          </div>
        </div>
        <span class="system-version">NEXUS OS / 2.0</span>
      </div>
    </aside>

    <section class="workspace-stage">
      <header class="workspace-topbar">
        <div class="topbar-title">
          <span>ADMINISTRATION / {{ currentDate }}</span>
          <h1>{{ routeTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <div class="live-chip">
            <span></span>
            实时数据
          </div>
          <div class="profile-block">
            <div class="profile-avatar">{{ userInitial }}</div>
            <div class="profile-copy">
              <strong>{{ userInfo.name || '管理员' }}</strong>
              <small>超级管理员</small>
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
import {
  Briefcase,
  ChatDotRound,
  CircleClose,
  Clock,
  Document,
  Odometer,
  OfficeBuilding,
  PieChart,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import axios from 'axios'
import { gsap } from 'gsap'

const route = useRoute()
const router = useRouter()

interface AdminProfile {
  name?: string
}

const userInfo = ref<AdminProfile>({})

const routeTitles: Record<string, string> = {
  '/admin-home/dashboard': '数据面板',
  '/admin-home/emp-list': '员工管理',
  '/admin-home/dept-manage': '部门管理',
  '/admin-home/duty-manage': '职务管理',
  '/admin-home/sign-list': '考勤管理',
  '/admin-home/sign-statistics': '考勤统计',
  '/admin-home/leave-manage': '请假审批',
  '/admin-home/kb-manage': '知识库管理',
  '/admin-home/unsigned-list': '未签到补签'
}

const routeTitle = computed(() => routeTitles[route.path] || '管理控制台')
const currentDate = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short'
}).format(new Date())
const userInitial = computed(() => String(userInfo.value.name || '管').slice(0, 1))

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
    const response = await axios.get('/api/v1/admin/auth/profile')
    userInfo.value = response.data?.data || { name: '管理员' }
  } catch (error) {
    console.error('获取管理员信息失败:', error)
    userInfo.value = { name: '管理员' }
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
      await axios.post('/api/v1/admin/auth/logout')
      ElMessage.success('退出登录成功')
    } catch (error) {
      console.error('退出登录失败:', error)
      ElMessage.warning('退出登录失败，但将跳转到登录页')
    } finally {
      sessionStorage.removeItem('adminToken')
      userInfo.value = {}
      router.push('/admin-login')
    }
  } catch {
    ElMessage.info('已取消退出')
  }
}
</script>
