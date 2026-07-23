<template>
  <div class="emp-info">
    <div class="profile-summary">
      <span class="summary-index">PROFILE / 01</span>
      <div class="profile-monogram">{{ editFormData.name.slice(0, 1) || 'N' }}</div>
      <h2>{{ editFormData.name || '员工档案' }}</h2>
      <p>{{ editFormData.duty_name || '企业员工' }}</p>
      <div class="summary-line"><span>员工工号</span><strong>{{ editFormData.number || '--' }}</strong></div>
      <div class="summary-line"><span>所属部门</span><strong>{{ editFormData.dept_name || '--' }}</strong></div>
      <div class="summary-status"><i></i> 档案状态正常</div>
    </div>

    <div class="info-container">
      <div class="form-heading">
        <p class="info-title">个人信息</p>
        <span>更新你的基本档案信息</span>
      </div>

      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="rules"
        label-position="top"
        v-loading="loading"
      >
        <div class="form-grid">
          <el-form-item label="员工工号" prop="number">
            <el-input v-model="editFormData.number" :disabled="true" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="editFormData.name" />
          </el-form-item>
          <el-form-item label="出生日期" prop="birthday">
            <el-input v-model="editFormData.birthday" type="date" />
          </el-form-item>
          <el-form-item label="所在地址" prop="address">
            <el-input v-model="editFormData.address" />
          </el-form-item>
          <el-form-item label="所属部门" prop="dept_name">
            <el-input v-model="editFormData.dept_name" :disabled="true" />
          </el-form-item>
          <el-form-item label="当前职务" prop="duty_name">
            <el-input v-model="editFormData.duty_name" :disabled="true" />
          </el-form-item>
        </div>
        <div class="form-actions">
          <span>带有组织权限的字段由管理员维护</span>
          <el-button type="primary" @click="updateEmp" :loading="saving">保存修改</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const editFormRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

const editFormData = reactive({
  number: '',
  name: '',
  birthday: '',
  address: '',
  dept_name: '',
  duty_name: ''
})

const rules = {
  number: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  birthday: [
    { required: true, message: '请选择日期', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  dept_name: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ],
  duty_name: [
    { required: true, message: '请选择职务', trigger: 'change' }
  ]
}

const updateEmp = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const response = await axios.put('/api/v1/employee/profile', {
          number: parseInt(editFormData.number),
          name: editFormData.name,
          birthday: editFormData.birthday,
          address: editFormData.address
        })
        
        if (response.data && response.data.data) {
          ElMessage.success('信息更新成功')
          Object.assign(editFormData, response.data.data)
        } else {
          ElMessage.error('更新失败')
        }
      } catch (error) {
        console.error('更新错误:', error)
        ElMessage.error('更新失败，请检查网络连接')
      } finally {
        saving.value = false
      }
    }
  })
}

const getEmpInfo = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/profile')
    
    if (response.data && response.data.data) {
      Object.assign(editFormData, response.data.data)
    } else {
      ElMessage.error('获取信息失败')
    }
  } catch (error) {
    console.error('获取信息错误:', error)
    ElMessage.error('获取信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getEmpInfo()
})
</script>

<style scoped>
.emp-info {
  display: grid;
  min-height: 100%;
  grid-template-columns: 270px minmax(0, 760px);
  justify-content: center;
  gap: 18px;
}

.profile-summary,
.info-container {
  border: 1px solid #dfe5ea;
  background: #fff;
  box-shadow: 0 12px 34px rgba(20, 34, 46, 0.06);
}

.profile-summary {
  position: relative;
  overflow: hidden;
  padding: 28px 24px;
  color: #fff;
  background:
    linear-gradient(rgba(255,255,255,.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.045) 1px, transparent 1px),
    #18252e;
  background-size: 28px 28px;
}

.summary-index { color: #71838e; font-family: Consolas, monospace; font-size: 8px; }
.profile-monogram { display: grid; width: 72px; height: 72px; margin: 42px auto 18px; place-items: center; color: #0a1511; background: #e5ff4f; font-size: 28px; font-weight: 850; box-shadow: 10px 10px 0 rgba(0,168,120,.45); }
.profile-summary h2 { margin: 0; font-size: 22px; font-weight: 760; text-align: center; }
.profile-summary > p { margin: 7px 0 34px; color: #91a1aa; font-size: 11px; text-align: center; }
.summary-line { display: flex; padding: 12px 0; justify-content: space-between; border-top: 1px solid rgba(255,255,255,.08); }
.summary-line span { color: #798b95; font-size: 9px; }
.summary-line strong { color: #dce4e8; font-size: 10px; font-weight: 650; }
.summary-status { position: absolute; right: 20px; bottom: 18px; left: 20px; display: flex; padding-top: 14px; align-items: center; gap: 7px; border-top: 1px solid rgba(255,255,255,.08); color: #8ea19a; font-size: 9px; }
.summary-status i { width: 6px; height: 6px; border-radius: 50%; background: #00a878; box-shadow: 0 0 10px #00a878; }

.info-container {
  min-width: 0;
  padding: 28px 32px;
}

.info-title {
  margin: 0;
}

.form-heading { display: flex; padding-bottom: 19px; align-items: center; justify-content: space-between; border-bottom: 1px solid #e7ecef; }
.form-heading > span { color: #919ca6; font-size: 10px; }
.el-form { margin-top: 24px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.form-actions { display: flex; margin-top: 8px; padding-top: 20px; align-items: center; justify-content: space-between; border-top: 1px solid #e7ecef; }
.form-actions span { color: #9aa4ad; font-size: 9px; }
.form-actions .el-button { min-width: 112px; }

@media (max-width: 760px) {
  .emp-info { grid-template-columns: 1fr; }
  .profile-summary { min-height: 250px; }
  .profile-monogram { margin-top: 24px; }
  .profile-summary > p { margin-bottom: 22px; }
  .info-container { padding: 24px 20px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-heading, .form-actions { align-items: flex-start; flex-direction: column; gap: 12px; }
}
</style>
