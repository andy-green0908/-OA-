<template>
  <div class="update-pwd">
    <aside class="security-panel">
      <span class="security-code">SECURITY / 04</span>
      <div class="security-shield">•••</div>
      <h2>守护账户安全</h2>
      <p>定期更新密码能够降低账户风险。修改成功后需要重新登录。</p>
      <ul>
        <li><i></i> 使用不易猜测的新密码</li>
        <li><i></i> 不要与他人共享登录凭据</li>
        <li><i></i> 修改后旧会话将自动结束</li>
      </ul>
    </aside>
    <div class="pwd-container">
      <div class="pwd-heading">
        <p class="pwd-title">修改密码</p>
        <span>CHANGE PASSWORD</span>
      </div>
      
      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="rules"
        label-position="top"
        status-icon
        v-loading="loading"
      >
        <el-form-item label="员工工号" prop="number">
          <el-input :disabled="true" v-model="editFormData.number" />
        </el-form-item>
        
        <el-form-item label="旧密码" prop="old_pwd">
          <el-input
            autocomplete="off"
            placeholder="请输入旧密码"
            type="password"
            v-model="editFormData.old_pwd"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="pass">
          <el-input
            autocomplete="off"
            placeholder="请输入新的密码"
            type="password"
            v-model="editFormData.pass"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="checkPass">
          <el-input
            autocomplete="off"
            placeholder="请再次输入新的密码以核对"
            type="password"
            v-model="editFormData.checkPass"
            show-password
          />
        </el-form-item>
        
        <div class="pwd-actions">
          <span>提交后将跳转至登录页</span>
          <el-button @click="updatePwd" type="primary" :loading="updating">更新密码</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const editFormRef = ref<FormInstance>()
const loading = ref(false)
const updating = ref(false)

const editFormData = reactive({
  number: '',
  old_pwd: '',
  pass: '',
  checkPass: '',
  pwd: '' // 用于存储原密码进行验证
})

// 自定义验证规则
const validatePass = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入新的密码'))
  } else {
    if (editFormData.checkPass !== '') {
      editFormRef.value?.validateField('checkPass')
    }
    callback()
  }
}

const validatePass2 = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入新的密码'))
  } else if (value !== editFormData.pass) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  pass: [
    { validator: validatePass, trigger: 'blur' }
  ],
  checkPass: [
    { validator: validatePass2, trigger: 'blur' }
  ]
}

const updatePwd = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        // 使用URL参数格式发送请求，因为后端使用了@RequestParam
        const params = new URLSearchParams()
        params.append('number', editFormData.number)
        params.append('pwd', editFormData.pass)
        params.append('oldPassword', editFormData.old_pwd)
        
        const response = await axios.put('/api/v1/employee/password?' + params.toString(), {
          number: parseInt(editFormData.number),
          pwd: editFormData.pass
        })
        
        if (response.data === 'true' || response.data === true) {
          ElMessage.success('密码修改成功')
          router.push('/emp-login')
        } else {
          ElMessage.error('密码修改失败，请检查旧密码是否正确')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('修改密码失败，请检查网络连接')
      } finally {
        updating.value = false
      }
    }
  })
}

const getEmpInfo = async () => {
  try {
    const response = await axios.get('/api/v1/employee/profile')
    if (response.data && response.data.data) {
      editFormData.number = response.data.data.number
      editFormData.pwd = response.data.data.pwd
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
    ElMessage.error('获取员工信息失败')
  }
}

onMounted(() => {
  getEmpInfo()
})
</script>

<style scoped>
.update-pwd {
  display: grid;
  min-height: 100%;
  grid-template-columns: 320px minmax(0, 520px);
  justify-content: center;
  gap: 18px;
}

.security-panel,
.pwd-container { border: 1px solid #dfe5ea; box-shadow: 0 12px 34px rgba(20,34,46,.06); }
.security-panel { padding: 30px 26px; color: #fff; background: #18252e; }
.security-code { color: #71838e; font-family: Consolas, monospace; font-size: 8px; }
.security-shield { display: grid; width: 74px; height: 82px; margin: 38px 0 26px; place-items: center; color: #07140f; background: #e5ff4f; clip-path: polygon(50% 0, 94% 17%, 84% 75%, 50% 100%, 16% 75%, 6% 17%); font-size: 17px; font-weight: 900; }
.security-panel h2 { margin: 0; font-size: 22px; font-weight: 760; }
.security-panel p { margin: 12px 0 26px; color: #8fa0aa; font-size: 11px; line-height: 1.8; }
.security-panel ul { margin: 0; padding: 0; list-style: none; }
.security-panel li { display: flex; padding: 10px 0; align-items: center; gap: 9px; border-top: 1px solid rgba(255,255,255,.07); color: #a7b4bc; font-size: 9px; }
.security-panel li i { width: 5px; height: 5px; background: #00a878; }

.pwd-container {
  min-width: 0;
  padding: 30px 34px;
  background: #fff;
}

.pwd-heading { display: flex; padding-bottom: 20px; align-items: center; justify-content: space-between; border-bottom: 1px solid #e7ecef; }
.pwd-title { margin: 0; }
.pwd-heading span { color: #9aa4ad; font-family: Consolas, monospace; font-size: 8px; }
.el-form { margin-top: 24px; }
.pwd-actions { display: flex; margin-top: 10px; padding-top: 20px; align-items: center; justify-content: space-between; border-top: 1px solid #e7ecef; }
.pwd-actions span { color: #9aa4ad; font-size: 9px; }

@media (max-width: 760px) {
  .update-pwd { grid-template-columns: 1fr; }
  .security-panel { padding: 24px; }
  .security-shield { width: 58px; height: 64px; margin: 24px 0 18px; }
  .pwd-container { padding: 24px 20px; }
}
</style>
