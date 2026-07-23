<template>
  <div class="leave-page">
    <div class="page-header">
      <b class="page-title">请假申请</b>
      <el-button @click="fetchList" :loading="loading">刷新</el-button>
    </div>

    <el-form :model="formData" :rules="rules" ref="formRef" label-width="90px" class="leave-form">
      <el-form-item label="请假时间" prop="range">
        <el-date-picker
          v-model="formData.range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="请假事由" prop="reason">
        <el-input v-model="formData.reason" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitLeave" :loading="submitting">提交申请</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" row-key="id">
      <el-table-column label="开始时间" prop="startDate" min-width="170" align="center" />
      <el-table-column label="结束时间" prop="endDate" min-width="170" align="center" />
      <el-table-column label="事由" prop="reason" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button size="small" :disabled="row.status !== '待审批'" @click="cancelLeave(row)">撤回</el-button>
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
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import axios from 'axios'

interface LeaveRow {
  id: string
  startDate: string
  endDate: string
  reason: string
  status: string
}

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const tableData = ref<LeaveRow[]>([])

const formData = reactive({
  range: [] as string[],
  reason: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const rules = {
  range: [{ required: true, message: '请选择请假时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }]
}

const statusType = (status: string) => {
  if (status === '已批准') return 'success'
  if (status === '已拒绝') return 'danger'
  if (status === '已撤回') return 'info'
  return 'warning'
}

const fetchList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/leave/my-records', {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
    })
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '获取请假记录失败')
      return
    }
    tableData.value = response.data?.data || []
    pagination.total = response.data?.total || 0
    pagination.currentPage = response.data?.pageNum || pagination.currentPage
  } catch (error) {
    console.error('获取请假记录失败:', error)
    ElMessage.error('获取请假记录失败')
  } finally {
    loading.value = false
  }
}

const submitLeave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  if (formData.range.length !== 2) {
    ElMessage.error('请选择完整请假时间')
    return
  }

  submitting.value = true
  try {
    const response = await axios.post('/api/v1/employee/leave/apply', {
      startDate: formData.range[0],
      endDate: formData.range[1],
      reason: formData.reason
    })
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '提交失败')
      return
    }
    ElMessage.success('提交成功')
    resetForm()
    pagination.currentPage = 1
    fetchList()
  } catch (error) {
    console.error('提交请假申请失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const cancelLeave = async (row: LeaveRow) => {
  try {
    await ElMessageBox.confirm('确定撤回这条请假申请吗？', '撤回确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await axios.put(`/api/v1/employee/leave/${row.id}/cancel`)
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '撤回失败')
      return
    }
    ElMessage.success('撤回成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回请假申请失败:', error)
      ElMessage.error('撤回失败')
    }
  }
}

const resetForm = () => {
  formData.range = []
  formData.reason = ''
  formRef.value?.resetFields()
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
.leave-page {
  min-height: 100%;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-title {
  color: #c0392b;
  font-size: 20px;
}

.leave-form {
  max-width: 760px;
  margin-bottom: 24px;
  padding: 20px;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
  background: #fff;
}
</style>
