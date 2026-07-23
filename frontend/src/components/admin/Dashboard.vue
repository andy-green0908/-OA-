<template>
  <div ref="pageRoot" class="dashboard">
    <section class="dashboard-hero">
      <NexusWebGL variant="dashboard" />
      <div class="hero-grid"></div>
      <div class="hero-copy">
        <div class="hero-eyebrow" data-reveal><span></span> ENTERPRISE COMMAND CENTER</div>
        <h2 data-reveal>组织运行，<em>一屏掌控。</em></h2>
        <p data-reveal>实时汇总人事、组织与考勤状态，让每一次管理判断都有数据依据。</p>
        <div class="hero-timestamp" data-reveal>
          <el-icon><Clock /></el-icon>
          {{ currentTime }}
        </div>
      </div>
      <div class="hero-radar" data-reveal aria-hidden="true">
        <span class="radar-ring ring-one"></span>
        <span class="radar-ring ring-two"></span>
        <span class="radar-cross cross-x"></span>
        <span class="radar-cross cross-y"></span>
        <strong>LIVE</strong>
        <small>SYNCED</small>
      </div>
      <div class="hero-spectrum" aria-hidden="true">
        <i v-for="index in 18" :key="index" :style="{ '--bar': index }"></i>
      </div>
      <div class="hero-code">NX / OPS / 2026</div>
      <div class="hero-stream" aria-hidden="true">
        <span>NEXUS CORE ONLINE</span><i></i><span>ORGANIZATION SYNCED</span><i></i>
        <span>ATTENDANCE LIVE</span><i></i><span>KNOWLEDGE CONNECTED</span>
      </div>
    </section>

    <section class="stat-grid" aria-label="核心运营指标">
      <el-card class="stat-card stat-green" data-reveal data-tilt v-loading="loading" shadow="never">
        <div class="stat-topline"><span>EMPLOYEES</span><b>01</b></div>
        <div class="stat-main">
          <div class="stat-icon"><el-icon><User /></el-icon></div>
          <div class="stat-info">
            <strong>{{ stats.employeeCount }}</strong>
            <span>员工总数</span>
          </div>
        </div>
        <div class="stat-footer"><i></i> 人员数据已同步</div>
      </el-card>

      <el-card class="stat-card stat-blue" data-reveal data-tilt v-loading="loading" shadow="never">
        <div class="stat-topline"><span>DEPARTMENTS</span><b>02</b></div>
        <div class="stat-main">
          <div class="stat-icon"><el-icon><OfficeBuilding /></el-icon></div>
          <div class="stat-info">
            <strong>{{ stats.departmentCount }}</strong>
            <span>部门数量</span>
          </div>
        </div>
        <div class="stat-footer"><i></i> 组织架构运行正常</div>
      </el-card>

      <el-card class="stat-card stat-coral" data-reveal data-tilt v-loading="loading" shadow="never">
        <div class="stat-topline"><span>ATTENDANCE</span><b>03</b></div>
        <div class="stat-main">
          <div class="stat-icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-info">
            <strong>{{ stats.todayAttendance }}</strong>
            <span>今日签到</span>
          </div>
        </div>
        <div class="stat-footer"><i></i> 今日考勤实时更新</div>
      </el-card>

      <el-card class="stat-card stat-yellow" data-reveal data-tilt v-loading="loading" shadow="never">
        <div class="stat-topline"><span>POSITIONS</span><b>04</b></div>
        <div class="stat-main">
          <div class="stat-icon"><el-icon><Briefcase /></el-icon></div>
          <div class="stat-info">
            <strong>{{ stats.dutyCount }}</strong>
            <span>职务数量</span>
          </div>
        </div>
        <div class="stat-footer"><i></i> 职务体系已连接</div>
      </el-card>
    </section>

    <section class="dashboard-lower">
      <div class="command-panel" data-reveal>
        <div class="panel-heading">
          <div>
            <span>QUICK ACTIONS</span>
            <h3>快速开始</h3>
          </div>
          <small>常用管理入口</small>
        </div>
        <div class="command-grid">
          <button type="button" data-magnetic data-cursor @click="$router.push('/admin-home/emp-list')">
            <i class="command-icon green"><el-icon><User /></el-icon></i>
            <span><strong>员工管理</strong><small>新增、编辑与维护员工档案</small></span>
            <el-icon class="command-arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" data-magnetic data-cursor @click="$router.push('/admin-home/dept-manage')">
            <i class="command-icon blue"><el-icon><OfficeBuilding /></el-icon></i>
            <span><strong>部门管理</strong><small>维护企业组织架构</small></span>
            <el-icon class="command-arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" data-magnetic data-cursor @click="$router.push('/admin-home/sign-list')">
            <i class="command-icon coral"><el-icon><Clock /></el-icon></i>
            <span><strong>考勤管理</strong><small>查看今日员工出勤状态</small></span>
            <el-icon class="command-arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" data-magnetic data-cursor @click="$router.push('/admin-home/kb-manage')">
            <i class="command-icon yellow"><el-icon><ChatDotRound /></el-icon></i>
            <span><strong>知识库</strong><small>训练企业 AI 服务能力</small></span>
            <el-icon class="command-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </div>

      <aside class="system-panel" data-reveal data-tilt>
        <div class="panel-heading system-heading">
          <div>
            <span>SYSTEM HEALTH</span>
            <h3>系统状态</h3>
          </div>
          <span class="health-label" :class="{ unhealthy: systemHealth.status !== 'HEALTHY' }">
            {{ systemHealth.status }}
          </span>
        </div>
        <div class="system-score">
          <div class="score-ring" :class="{ unhealthy: systemHealth.status !== 'HEALTHY' }">
            <strong>{{ systemHealth.availability }}</strong><small>%</small>
          </div>
          <div><b>服务可用率</b><span>{{ healthSummary }}</span></div>
        </div>
        <div class="system-lines">
          <div><span><el-icon><Connection /></el-icon> 网关连接</span><b>{{ systemHealth.gatewayConnected ? '正常' : '异常' }}</b></div>
          <div><span><el-icon><Cpu /></el-icon> 数据服务</span><b>{{ systemHealth.dataService ? '正常' : '异常' }}</b></div>
          <div><span><el-icon><User /></el-icon> 在线管理员</span><b>{{ stats.onlineAdmins }} 人</b></div>
          <div><span><el-icon><User /></el-icon> 在线员工</span><b>{{ stats.onlineEmployees }} 人</b></div>
        </div>
        <div class="version-line"><span>当前版本</span><b>OA SYSTEM v2.0</b></div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  ArrowRight,
  Briefcase,
  ChatDotRound,
  Clock,
  Connection,
  Cpu,
  OfficeBuilding,
  User
} from '@element-plus/icons-vue'
import axios from 'axios'
import NexusWebGL from '../effects/NexusWebGL.vue'
import { useNexusMotion } from '../../composables/useNexusMotion'

defineOptions({ name: 'AdminDashboard' })

const pageRoot = ref<HTMLDivElement>()
const currentTime = ref('')
const loading = ref(false)
let clockTimer: ReturnType<typeof setInterval> | null = null
let healthTimer: ReturnType<typeof setInterval> | null = null

const stats = reactive({
  employeeCount: 0,
  departmentCount: 0,
  todayAttendance: 0,
  dutyCount: 0,
  onlineAdmins: 0,
  onlineEmployees: 0,
  onlineUsers: 0
})

const systemHealth = reactive({
  status: 'CHECKING',
  availability: '0.0',
  gatewayConnected: false,
  dataService: false
})

useNexusMotion(pageRoot)

const healthSummary = computed(() => {
  if (systemHealth.status === 'CHECKING') return '正在检测核心服务'
  if (systemHealth.status === 'HEALTHY') return '全部核心模块运行正常'
  return '部分核心模块不可用'
})

const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN')
}

const loadStats = async () => {
  loading.value = true
  try {
    const [employeesRes, departmentsRes, dutiesRes, todaySignedRes] = await Promise.all([
      axios.get('/api/v1/admin/employees', { params: { currentPage: 1, pageSize: 1 } }),
      axios.get('/api/v1/admin/departments'),
      axios.get('/api/v1/admin/duties'),
      axios.get('/api/v1/admin/attendance/today/signed', { params: { currentPage: 1, pageSize: 1 } })
    ])

    if (employeesRes.data?.total !== undefined) stats.employeeCount = employeesRes.data.total
    if (departmentsRes.data?.data) stats.departmentCount = departmentsRes.data.data.length
    if (dutiesRes.data?.data) stats.dutyCount = dutiesRes.data.data.length
    if (todaySignedRes.data?.total !== undefined) stats.todayAttendance = todaySignedRes.data.total
  } catch (error) {
    console.error('加载统计数据失败:', error)
    stats.employeeCount = 0
    stats.departmentCount = 0
    stats.todayAttendance = 0
    stats.dutyCount = 0
  } finally {
    loading.value = false
  }
}

const loadSystemHealth = async () => {
  try {
    const response = await axios.get('/api/v1/admin/system/health')
    const data = response.data?.data || {}
    systemHealth.gatewayConnected = !!data.gatewayConnected
    systemHealth.dataService = !!data.dataService
    systemHealth.status = data.status || 'DEGRADED'
    systemHealth.availability = Number(data.availability || 0).toFixed(1)
    stats.onlineAdmins = Number(data.onlineAdmins || 0)
    stats.onlineEmployees = Number(data.onlineEmployees || 0)
    stats.onlineUsers = Number(data.onlineUsers || 0)
  } catch (error) {
    console.error('加载系统健康状态失败:', error)
    systemHealth.gatewayConnected = false
    systemHealth.dataService = false
    systemHealth.status = 'DEGRADED'
    systemHealth.availability = '0.0'
    stats.onlineAdmins = 0
    stats.onlineEmployees = 0
    stats.onlineUsers = 0
  }
}

const refreshSystemHealthOnVisible = () => {
  if (document.visibilityState === 'visible') {
    loadSystemHealth()
  }
}

onMounted(async () => {
  updateTime()
  clockTimer = setInterval(updateTime, 1000)
  healthTimer = setInterval(loadSystemHealth, 5000)
  window.addEventListener('focus', loadSystemHealth)
  document.addEventListener('visibilitychange', refreshSystemHealthOnVisible)
  await Promise.all([loadStats(), loadSystemHealth()])
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (healthTimer) clearInterval(healthTimer)
  window.removeEventListener('focus', loadSystemHealth)
  document.removeEventListener('visibilitychange', refreshSystemHealthOnVisible)
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-hero {
  position: relative;
  min-height: 248px;
  overflow: hidden;
  padding: 38px 42px;
  color: #fff;
  background: #111c24;
}

.dashboard-hero::after {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 38%;
  height: 5px;
  background: #e5ff4f;
  content: '';
}

.hero-grid {
  position: absolute;
  inset: 0;
  opacity: 0.45;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(90deg, transparent, #000 55%);
}

.hero-copy {
  position: relative;
  z-index: 2;
  max-width: 590px;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #9aacb6;
  font-family: Consolas, monospace;
  font-size: 9px;
  font-weight: 700;
}

.hero-eyebrow span {
  width: 24px;
  height: 2px;
  background: #00a878;
}

.hero-copy h2 {
  margin: 22px 0 10px;
  font-size: 34px;
  font-weight: 780;
}

.hero-copy p {
  max-width: 520px;
  margin: 0;
  color: #aebac2;
  font-size: 13px;
  line-height: 1.8;
}

.hero-timestamp {
  display: inline-flex;
  margin-top: 26px;
  padding: 8px 11px;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #d7dfe3;
  background: rgba(255, 255, 255, 0.04);
  font-family: Consolas, monospace;
  font-size: 10px;
}

.hero-radar {
  position: absolute;
  top: 50%;
  right: 9%;
  width: 162px;
  height: 162px;
  border: 1px solid rgba(229, 255, 79, 0.36);
  border-radius: 50%;
  transform: translateY(-50%);
}

.radar-ring,
.radar-cross {
  position: absolute;
  display: block;
}

.radar-ring {
  top: 50%;
  left: 50%;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.ring-one { width: 116px; height: 116px; }
.ring-two { width: 70px; height: 70px; }
.radar-cross { background: rgba(255, 255, 255, 0.1); }
.cross-x { top: 50%; left: -18px; width: calc(100% + 36px); height: 1px; }
.cross-y { top: -18px; left: 50%; width: 1px; height: calc(100% + 36px); }

.hero-radar strong,
.hero-radar small {
  position: absolute;
  left: 50%;
  display: block;
  transform: translateX(-50%);
}

.hero-radar strong {
  top: 61px;
  color: #e5ff4f;
  font-family: Consolas, monospace;
  font-size: 16px;
}

.hero-radar small {
  top: 84px;
  color: #748894;
  font-size: 7px;
}

.hero-code {
  position: absolute;
  right: 20px;
  bottom: 20px;
  color: #61727c;
  font-family: Consolas, monospace;
  font-size: 8px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  background: var(--stat-color);
  content: '';
}

.stat-card :deep(.el-card__body) { padding: 18px 19px 15px; }
.stat-green { --stat-color: #00a878; --stat-soft: #e6f7f1; }
.stat-blue { --stat-color: #2878ff; --stat-soft: #eaf1ff; }
.stat-coral { --stat-color: #ff6b4a; --stat-soft: #fff0ec; }
.stat-yellow { --stat-color: #b5cb22; --stat-soft: #f5f9dc; }

.stat-topline {
  display: flex;
  justify-content: space-between;
  color: #9aa5ae;
  font-family: Consolas, monospace;
  font-size: 8px;
}

.stat-topline b { color: #c4cbd0; }

.stat-main {
  display: flex;
  margin-top: 18px;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  color: var(--stat-color);
  background: var(--stat-soft);
  font-size: 20px;
}

.stat-info strong,
.stat-info span { display: block; }
.stat-info strong { color: #142131; font-size: 27px; font-weight: 800; line-height: 1; }
.stat-info span { margin-top: 7px; color: #6f7b87; font-size: 11px; }

.stat-footer {
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px solid #edf0f2;
  color: #8a959f;
  font-size: 9px;
}

.stat-footer i {
  display: inline-block;
  width: 5px;
  height: 5px;
  margin-right: 6px;
  border-radius: 50%;
  background: var(--stat-color);
  vertical-align: 1px;
}

.dashboard-lower {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(300px, 0.75fr);
  gap: 16px;
}

.command-panel,
.system-panel {
  border: 1px solid #dfe5ea;
  background: #fff;
  box-shadow: 0 10px 30px rgba(20, 34, 46, 0.05);
}

.command-panel { padding: 22px; }
.system-panel { padding: 22px; color: #fff; border-color: #18252e; background: #18252e; }

.panel-heading {
  display: flex;
  margin-bottom: 18px;
  align-items: flex-end;
  justify-content: space-between;
}

.panel-heading span { color: #95a0a9; font-family: Consolas, monospace; font-size: 8px; font-weight: 700; }
.panel-heading h3 { margin: 5px 0 0; color: #162334; font-size: 17px; font-weight: 750; }
.panel-heading > small { color: #9aa4ad; font-size: 9px; }
.system-heading h3 { color: #fff; }

.health-label {
  padding: 4px 7px;
  color: #07140f !important;
  background: #e5ff4f;
}

.health-label.unhealthy {
  color: #fff !important;
  background: #ff6b4a;
}

.command-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.command-grid button {
  display: flex;
  min-height: 74px;
  padding: 13px;
  align-items: center;
  gap: 12px;
  border: 1px solid #e5eaed;
  border-radius: 5px;
  color: #172333;
  background: #fafbfb;
  text-align: left;
  cursor: pointer;
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease;
}

.command-grid button:hover { border-color: #b8c4ca; box-shadow: 0 10px 24px rgba(16, 30, 41, 0.08); transform: translateY(-2px); }
.command-icon { display: grid; width: 38px; height: 38px; flex: 0 0 38px; place-items: center; font-size: 17px; font-style: normal; }
.command-icon.green { color: #008d65; background: #e4f6f0; }
.command-icon.blue { color: #2878ff; background: #eaf1ff; }
.command-icon.coral { color: #ff6b4a; background: #fff0ec; }
.command-icon.yellow { color: #8b9d12; background: #f5f9dc; }
.command-grid button > span { min-width: 0; flex: 1; }
.command-grid strong, .command-grid small { display: block; }
.command-grid strong { font-size: 12px; font-weight: 750; }
.command-grid small { margin-top: 5px; overflow: hidden; color: #8a95a0; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.command-arrow { color: #9aa5ae; }

.system-score { display: flex; padding: 9px 0 22px; align-items: center; gap: 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.09); }
.score-ring { display: grid; width: 68px; height: 68px; flex: 0 0 68px; border: 5px solid #00a878; border-right-color: rgba(255, 255, 255, 0.12); border-radius: 50%; place-content: center; transform: rotate(-20deg); }
.score-ring.unhealthy { border-color: #ff6b4a; border-right-color: rgba(255, 255, 255, 0.12); }
.score-ring strong, .score-ring small { transform: rotate(20deg); }
.score-ring strong { font-size: 16px; }
.score-ring small { color: #778a94; font-size: 7px; text-align: center; }
.system-score > div:last-child b, .system-score > div:last-child span { display: block; }
.system-score > div:last-child b { font-size: 12px; }
.system-score > div:last-child span { margin-top: 6px; color: #778994; font-size: 9px; }
.system-lines { padding: 13px 0; }
.system-lines > div, .version-line { display: flex; padding: 8px 0; align-items: center; justify-content: space-between; }
.system-lines span { display: flex; align-items: center; gap: 7px; color: #94a3ac; font-size: 10px; }
.system-lines b { color: #d9e3e7; font-size: 9px; }
.version-line { margin-top: 4px; padding-top: 12px; border-top: 1px solid rgba(255, 255, 255, 0.09); color: #788a94; font-family: Consolas, monospace; font-size: 8px; }
.version-line b { color: #e5ff4f; }

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dashboard-lower { grid-template-columns: 1fr; }
}

@media (max-width: 720px) {
  .dashboard-hero { min-height: 270px; padding: 28px 24px; }
  .hero-copy { max-width: 70%; }
  .hero-copy h2 { font-size: 27px; }
  .hero-copy p { font-size: 11px; }
  .hero-radar { right: -32px; width: 130px; height: 130px; opacity: 0.72; }
  .ring-one { width: 92px; height: 92px; }
  .ring-two { width: 54px; height: 54px; }
  .hero-radar strong { top: 48px; }
  .hero-radar small { top: 69px; }
  .stat-grid, .command-grid { grid-template-columns: 1fr; }
}
</style>

<style scoped>
.dashboard {
  perspective: 1200px;
}

.dashboard-hero {
  min-height: 312px;
  padding-top: 46px;
  isolation: isolate;
  background: #08141b;
  box-shadow: 0 24px 60px rgba(8, 20, 27, 0.22);
}

.dashboard-hero :deep(.nexus-webgl) {
  z-index: 1;
}

.dashboard-hero::before {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(90deg, rgba(8,20,27,.96) 0%, rgba(8,20,27,.78) 43%, rgba(8,20,27,.18) 100%);
  content: '';
  pointer-events: none;
}

.dashboard-hero::after {
  z-index: 5;
  bottom: 30px;
  animation: hero-energy 3.8s ease-in-out infinite;
}

.hero-grid { z-index: 2; opacity: .28; }
.hero-copy { z-index: 4; }
.hero-copy h2 { font-size: 39px; }
.hero-copy h2 em { color: #e5ff4f; font-style: normal; filter: drop-shadow(0 0 18px rgba(229,255,79,.2)); }
.hero-radar { z-index: 4; border-color: rgba(229,255,79,.6); box-shadow: 0 0 60px rgba(0,168,120,.1), inset 0 0 38px rgba(0,168,120,.08); animation: radar-float 4.2s ease-in-out infinite; }
.ring-one { animation: ring-spin 7s linear infinite; }
.ring-two { animation: ring-spin-reverse 5s linear infinite; }
.hero-code { top: 18px; right: 18px; bottom: auto; z-index: 5; color: #78909b; }

.hero-spectrum {
  position: absolute;
  right: 4%;
  bottom: 55px;
  z-index: 4;
  display: flex;
  height: 38px;
  align-items: flex-end;
  gap: 4px;
  opacity: .66;
}

.hero-spectrum i {
  width: 2px;
  height: calc(6px + var(--bar) * 1.4px);
  max-height: 34px;
  background: linear-gradient(to top, #00a878, #e5ff4f);
  box-shadow: 0 0 7px rgba(229,255,79,.28);
  transform-origin: bottom;
  animation: spectrum 1.2s ease-in-out infinite alternate;
  animation-delay: calc(var(--bar) * -70ms);
}

.hero-stream {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 6;
  display: flex;
  height: 30px;
  padding: 0 18px;
  align-items: center;
  gap: 18px;
  overflow: hidden;
  color: #8ca0aa;
  background: rgba(3,11,15,.76);
  font-family: Consolas, monospace;
  font-size: 7px;
  white-space: nowrap;
  backdrop-filter: blur(12px);
}

.hero-stream::after {
  position: absolute;
  top: 0;
  left: -20%;
  width: 20%;
  height: 1px;
  background: #e5ff4f;
  box-shadow: 0 0 10px #e5ff4f;
  content: '';
  animation: stream-scan 3.2s linear infinite;
}

.hero-stream i { width: 4px; height: 4px; flex: 0 0 4px; border: 1px solid #00e8ac; transform: rotate(45deg); }

.stat-card {
  transform-style: preserve-3d;
  will-change: transform;
}

.stat-card::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 20%, rgba(255,255,255,.88) 45%, transparent 68%);
  content: '';
  opacity: 0;
  pointer-events: none;
  transform: translateX(-110%);
  transition: opacity .25s ease, transform .65s ease;
}

.stat-card:hover::after { opacity: .58; transform: translateX(110%); }
.stat-card :deep(.el-card__body) { position: relative; z-index: 2; transform: translateZ(18px); }
.stat-icon { box-shadow: inset 0 0 0 1px rgba(0,0,0,.035), 0 8px 22px rgba(20,34,46,.07); }
.command-panel, .system-panel { position: relative; overflow: hidden; }
.command-panel::before { position: absolute; top: 0; left: 0; width: 28%; height: 2px; background: linear-gradient(90deg, #00a878, #e5ff4f); content: ''; animation: panel-scan 4s ease-in-out infinite alternate; }
.command-grid button { will-change: transform; }
.system-panel { transform-style: preserve-3d; will-change: transform; }
.system-panel::before { position: absolute; inset: 0; background-image: linear-gradient(rgba(255,255,255,.035) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.035) 1px, transparent 1px); background-size: 22px 22px; content: ''; mask-image: linear-gradient(transparent, #000); pointer-events: none; }
.system-panel > * { position: relative; z-index: 1; transform: translateZ(14px); }

@keyframes hero-energy {
  0%, 100% { width: 22%; opacity: .5; }
  50% { width: 62%; opacity: 1; }
}

@keyframes radar-float {
  0%, 100% { transform: translateY(-50%) scale(1); }
  50% { transform: translateY(-53%) scale(1.04); }
}

@keyframes ring-spin {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes ring-spin-reverse {
  to { transform: translate(-50%, -50%) rotate(-360deg); }
}

@keyframes spectrum {
  from { transform: scaleY(.2); opacity: .3; }
  to { transform: scaleY(1); opacity: 1; }
}

@keyframes stream-scan {
  to { left: 120%; }
}

@keyframes panel-scan {
  from { left: 0; }
  to { left: 72%; }
}

@media (max-width: 720px) {
  .dashboard-hero { min-height: 310px; padding-top: 30px; }
  .hero-copy { max-width: 92%; }
  .hero-copy h2 { font-size: 27px; }
  .hero-copy h2 em { display: block; white-space: nowrap; }
  .hero-spectrum { right: 2%; bottom: 42px; opacity: .45; }
  .hero-radar { right: -68px; z-index: 3; opacity: .62; }
}
</style>
