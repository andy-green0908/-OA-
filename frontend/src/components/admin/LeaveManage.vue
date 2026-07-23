<template>
  <div class="leave-manage">
    <div class="page-header">
      <b class="page-title">请假审批</b>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="工号/姓名" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="待审批" value="待审批" />
          <el-option label="已批准" value="已批准" />
          <el-option label="已拒绝" value="已拒绝" />
          <el-option label="已撤回" value="已撤回" />
        </el-select>
        <el-button @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" row-key="id">
      <el-table-column label="工号" prop="number" width="90" align="center" />
      <el-table-column label="姓名" prop="name" width="100" align="center" />
      <el-table-column label="部门" prop="dept_name" width="120" align="center" />
      <el-table-column label="开始时间" prop="startDate" min-width="170" align="center" />
      <el-table-column label="结束时间" prop="endDate" min-width="170" align="center" />
      <el-table-column label="事由" prop="reason" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" :disabled="row.status !== '待审批'" @click="approve(row)">批准</el-button>
          <el-button size="small" type="danger" :disabled="row.status !== '待审批'" @click="reject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[8, 10, 20]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px;"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

interface LeaveRow {
  id: string
  number: number
  name: string
  dept_name: string
  startDate: string
  endDate: string
  reason: string
  status: string
}

const loading = ref(false)
const keyword = ref('')
const status = ref('')
const tableData = ref<LeaveRow[]>([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const statusType = (value: string) => {
  if (value === '已批准') return 'success'
  if (value === '已拒绝') return 'danger'
  if (value === '已撤回') return 'info'
  return 'warning'
}

const listParams = () => ({
  currentPage: pagination.currentPage,
  pageSize: pagination.pageSize,
  status: status.value || undefined,
  keyword: keyword.value.trim() || undefined
})

const fetchList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/leave', { params: listParams() })
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '获取请假列表失败')
      return
    }
    tableData.value = response.data?.data || []
    pagination.total = response.data?.total || 0
    pagination.currentPage = response.data?.pageNum || pagination.currentPage
  } catch (error) {
    console.error('获取请假列表失败:', error)
    ElMessage.error('获取请假列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchList()
}

const approve = (row: LeaveRow) => changeStatus(row, 'approve', '批准')

const reject = (row: LeaveRow) => changeStatus(row, 'reject', '拒绝')

const changeStatus = async (row: LeaveRow, action: string, text: string) => {
  try {
    await ElMessageBox.confirm(`确定${text} ${row.name} 的请假申请吗？`, `${text}确认`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await axios.put(`/api/v1/admin/leave/${row.id}/${action}`)
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || `${text}失败`)
      return
    }
    ElMessage.success(`${text}成功`)
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${text}请假申请失败:`, error)
      ElMessage.error(`${text}失败`)
    }
  }
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  fetchList()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.leave-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  color: #c0392b;
  font-size: 20px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
