<template>
  <div class="sign-statistics">
    <div class="header-section">
      <h1 class="chart-title">
        <el-icon class="title-icon"><TrendCharts /></el-icon>
        近五日签到情况统计视图
      </h1>
      <el-button 
        @click="refreshChart" 
        :loading="loading"
        type="primary" 
        class="refresh-btn"
      >
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>
    
    <div class="chart-grid">
      <el-card class="chart-card" v-loading="loading" element-loading-text="正在加载图表数据...">
        <div ref="checkInChartContainer" class="chart-container"></div>
      </el-card>
      
      <el-card class="chart-card" v-loading="loading" element-loading-text="正在加载图表数据...">
        <div ref="checkOutChartContainer" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 空数据状态 -->
    <div v-if="!loading && hasError" class="empty-state">
        <el-icon class="empty-icon"><DocumentRemove /></el-icon>
        <p class="empty-text">暂无统计数据</p>
        <el-button @click="refreshChart" type="primary">重新获取</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, Refresh, DocumentRemove } from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts'

const checkInChartContainer = ref<HTMLElement>()
const checkOutChartContainer = ref<HTMLElement>()
const loading = ref(false)
const hasError = ref(false)
let checkInChart: echarts.ECharts | null = null
let checkOutChart: echarts.ECharts | null = null

const buildChartOption = (
  title: string,
  labels: [string, string, string],
  dates: string[] = [],
  signedData: number[] = [],
  unsignedData: number[] = [],
  totalData: number[] = []
) => ({
  title: {
    text: title,
    textStyle: {
      color: '#333'
    }
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  legend: {
    data: labels
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: dates
  },
  yAxis: {
    type: 'value',
    boundaryGap: [0, 0.01]
  },
  series: [
    {
      name: labels[0],
      type: 'bar',
      data: signedData,
      itemStyle: {
        color: '#67C23A'
      }
    },
    {
      name: labels[1],
      type: 'bar',
      data: unsignedData,
      itemStyle: {
        color: '#F56C6C'
      }
    },
    {
      name: labels[2],
      type: 'bar',
      data: totalData,
      itemStyle: {
        color: '#409EFF'
      }
    }
  ]
})

const resizeCharts = () => {
  checkInChart?.resize()
  checkOutChart?.resize()
}

const initChart = () => {
  if (checkInChartContainer.value) {
    checkInChart = echarts.init(checkInChartContainer.value)
    checkInChart.setOption(buildChartOption('近五日上班签到统计', ['已签到人数', '未签到人数', '应签到人数']))
  }
  if (checkOutChartContainer.value) {
    checkOutChart = echarts.init(checkOutChartContainer.value)
    checkOutChart.setOption(buildChartOption('近五日下班签退统计', ['已签退人数', '未签退人数', '应签退人数']))
  }

  window.addEventListener('resize', resizeCharts)
}

const loadChartData = async () => {
  loading.value = true
  hasError.value = false
  
  try {
    const response = await axios.get('/api/v1/admin/attendance/statistics/chart')
    
    if (response.data) {
      const payload = response.data.data || {}
      const dates = payload.dates || []
      const checkIn = payload.checkIn || {}
      const checkOut = payload.checkOut || {}
      
      // 检查数据是否有效
      if (dates.length === 0) {
        console.warn('日期数据为空')
        hasError.value = true
        ElMessage.warning('暂无统计数据')
        return
      }
      
      if (checkInChart && checkOutChart) {
        checkInChart.setOption(buildChartOption(
          '近五日上班签到统计',
          ['已签到人数', '未签到人数', '应签到人数'],
          dates,
          checkIn.signed || [],
          checkIn.unsigned || [],
          checkIn.total || []
        ))
        checkOutChart.setOption(buildChartOption(
          '近五日下班签退统计',
          ['已签退人数', '未签退人数', '应签退人数'],
          dates,
          checkOut.signed || [],
          checkOut.unsigned || [],
          checkOut.total || []
        ))
        console.log('图表已更新')
      } else {
        console.error('图表实例不存在')
        hasError.value = true
      }
    } else {
      hasError.value = true
      ElMessage.error('获取统计数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    hasError.value = true
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新图表数据
const refreshChart = async () => {
  await loadChartData()
}

onMounted(async () => {
  await nextTick()
  initChart()
  loadChartData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  checkInChart?.dispose()
  checkOutChart?.dispose()
})
</script>

<style scoped>
.sign-statistics {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chart-title {
  display: flex;
  align-items: center;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.title-icon {
  margin-right: 8px;
  color: #409EFF;
}

.refresh-btn {
  margin-left: 16px;
}

.chart-card {
  position: relative;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chart-container {
  width: 100%;
  height: 600px;
  min-height: 400px;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-content {
  text-align: center;
}

.loading-icon {
  font-size: 48px;
  color: #409EFF;
  animation: rotate 2s linear infinite;
  margin-bottom: 16px;
}

.loading-text {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #C0C4CC;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #909399;
  margin-bottom: 16px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

<style scoped>
.sign-statistics {
  min-height: 100%;
  padding: 0;
  background: transparent;
}

.header-section {
  position: relative;
  overflow: hidden;
  min-height: 112px;
  margin-bottom: 18px;
  padding: 25px 28px;
  border-radius: 0;
  color: #fff;
  background:
    linear-gradient(rgba(255,255,255,.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.045) 1px, transparent 1px),
    #18252e;
  background-size: 28px 28px;
  box-shadow: none;
}

.header-section::after { position: absolute; right: 0; bottom: 0; width: 34%; height: 4px; background: #e5ff4f; content: ''; }
.chart-title { color: #fff; font-size: 24px; font-weight: 760; }
.title-icon { color: #e5ff4f; }
.refresh-btn { position: relative; z-index: 2; }
.chart-card { border: 1px solid #dfe5ea; border-radius: 8px; box-shadow: 0 12px 34px rgba(20,34,46,.06); }
.chart-card :deep(.el-card__body) { padding: 22px; }
.chart-container { height: min(590px, calc(100vh - 270px)); min-height: 430px; }
.loading-overlay { background: rgba(255,255,255,.9); backdrop-filter: blur(8px); }
.loading-icon { color: #00a878; }
.empty-icon { color: #aab5bd; }

@media (max-width: 720px) {
  .header-section { min-height: 146px; padding: 23px 20px; align-items: flex-start; flex-direction: column; gap: 18px; }
  .chart-title { font-size: 20px; }
  .refresh-btn { margin-left: 0; }
  .chart-card :deep(.el-card__body) { padding: 12px; }
  .chart-container { height: 460px; min-height: 400px; }
}
</style>
