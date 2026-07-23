<template>
  <div class="ai-chat">
    <div class="chat-container">
      <!-- 头部 -->
      <div class="chat-header">
        <div class="header-left">
          <div class="bot-avatar header-avatar">
            <el-icon :size="22"><Service /></el-icon>
          </div>
          <div>
            <p class="title">AI 智能客服 · 小星</p>
            <p class="subtitle">
              <span :class="['status-dot', statusClass]"></span>
              {{ statusText }}
            </p>
          </div>
        </div>
        <el-button text type="info" @click="clearChat">
          <el-icon><Delete /></el-icon>
          <span style="margin-left: 4px">新对话</span>
        </el-button>
      </div>

      <!-- 消息区 -->
      <div class="chat-messages" ref="messagesRef">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['msg-row', msg.role === 'user' ? 'msg-user' : 'msg-assistant']"
        >
          <div v-if="msg.role === 'assistant'" class="bot-avatar">
            <el-icon :size="18"><Service /></el-icon>
          </div>

          <div class="msg-body">
            <div :class="['bubble', msg.role, { 'bubble-error': msg.error }]">
              <template v-if="msg.streaming && !msg.content">
                <span class="typing-inline">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </span>
              </template>
              <template v-else>{{ msg.content }}</template>
            </div>

            <div v-if="msg.via === 'faq'" class="via-tag">来自本地知识库（FAQ 降级模式）</div>

            <!-- 相关问题 / 推荐问题 -->
            <div v-if="msg.related && msg.related.length" class="chips">
              <span class="chips-label">相关问题：</span>
              <el-tag
                v-for="r in msg.related"
                :key="r.question"
                class="chip"
                effect="plain"
                round
                @click="send(r.question)"
              >
                {{ r.question }}
              </el-tag>
            </div>
            <div v-if="msg.suggestions && msg.suggestions.length" class="chips">
              <el-tag
                v-for="s in msg.suggestions"
                :key="s"
                class="chip"
                type="primary"
                effect="plain"
                round
                @click="send(s)"
              >
                {{ s }}
              </el-tag>
            </div>

            <!-- 失败重试 -->
            <div v-if="msg.error && msg.retryQuestion" class="chips">
              <el-button size="small" type="warning" plain round @click="send(msg.retryQuestion)">
                <el-icon><RefreshRight /></el-icon>
                <span style="margin-left: 4px">点击重试</span>
              </el-button>
            </div>
          </div>

          <div v-if="msg.role === 'user'" class="user-avatar">
            <el-icon :size="18"><UserFilled /></el-icon>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="500"
          show-word-limit
          placeholder="请输入您的问题，例如：公司有哪些员工福利？（Enter 发送，Shift+Enter 换行）"
          @keydown.enter.exact.prevent="send()"
        />
        <el-button
          class="send-btn"
          type="primary"
          :disabled="!input.trim() || loading"
          :loading="loading"
          @click="send()"
        >
          <el-icon v-if="!loading"><Promotion /></el-icon>
          <span style="margin-left: 4px">发送</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Service, UserFilled, Delete, Promotion, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'

interface RelatedQuestion {
  question: string
  score: number
}

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  via?: 'llm' | 'faq'
  related?: RelatedQuestion[]
  suggestions?: string[]
  error?: boolean
  retryQuestion?: string
}

const WELCOME_TEXT =
  '您好！我是星辰科技的 AI 智能客服小星~\n可以问我公司简介、主营业务、员工福利、考勤请假制度，以及 OA 系统的使用问题哦。'

const router = useRouter()
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()
const openingSuggestions = ref<string[]>([])

// 大模型在线状态：null=检测中, true=在线, false=离线（FAQ 降级）
const aiOnline = ref<boolean | null>(null)
const ragReady = ref(false)
const modelName = ref('')

const statusClass = computed(() =>
  aiOnline.value === null ? 'checking' : aiOnline.value ? 'online' : 'offline',
)
const statusText = computed(() => {
  if (aiOnline.value === null) return '正在检测大模型状态…'
  if (aiOnline.value) {
    return `大模型在线（${modelName.value || '未知模型'}）${ragReady.value ? ' · RAG 知识检索' : ' · 无知识库'}`
  }
  return 'FAQ 离线模式 · 大模型未就绪'
})

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const pushWelcome = () => {
  messages.value.push({
    role: 'assistant',
    content: WELCOME_TEXT,
    suggestions: openingSuggestions.value.length ? [...openingSuggestions.value] : undefined,
  })
}

/** 探测 AI 微服务与本地大模型是否就绪 */
const checkHealth = async () => {
  try {
    const res = await axios.get('/api/v1/ai/chat/health', { timeout: 6000 })
    const d = res.data?.data
    aiOnline.value = !!(d && d.ollama && d.modelReady)
    ragReady.value = !!d?.ragReady
    modelName.value = d?.model || ''
  } catch {
    aiOnline.value = false
    ragReady.value = false
    modelName.value = ''
  }
}

/** 开场推荐问题（来自 AI 服务加载的 kb_doc 热门知识，加载失败静默跳过） */
const loadSuggestions = async () => {
  try {
    const res = await axios.get('/api/v1/ai/chat/suggestions')
    if (res.data?.code === 200 && Array.isArray(res.data.data)) {
      openingSuggestions.value = res.data.data
      const first = messages.value[0]
      if (first && first.role === 'assistant' && !first.suggestions) {
        first.suggestions = [...openingSuggestions.value]
      }
    }
  } catch {
    /* 静默降级 */
  }
}

/** 大模型流式对话（SSE 解析，逐 token 打字机渲染） */
const streamChat = async (question: string): Promise<void> => {
  const assistantMsg: ChatMessage = { role: 'assistant', content: '', streaming: true, via: 'llm' }
  messages.value.push(assistantMsg)
  scrollToBottom()

  let gotToken = false
  try {
    const employeeToken = sessionStorage.getItem('employeeToken')
    const res = await fetch('/api/v1/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(employeeToken ? { 'X-Emp-Token': employeeToken } : {}),
      },
      credentials: 'include',
      body: JSON.stringify({ message: question }),
    })
    if (!res.ok || !res.body) throw new Error('HTTP ' + res.status)

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 帧以空行分隔；一帧内可能有多条 data: 行
      const frames = buffer.split('\n\n')
      buffer = frames.pop() ?? ''
      for (const frame of frames) {
        const dataStr = frame
          .split('\n')
          .filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5))
          .join('\n')
        if (!dataStr.trim()) continue
        try {
          const payload = JSON.parse(dataStr)
          if (payload.c) {
            assistantMsg.content += payload.c
            gotToken = true
            scrollToBottom()
          } else if (payload.error) {
            throw new Error(payload.error)
          }
        } catch (err) {
          if (err instanceof SyntaxError) continue // 不完整 JSON 帧，忽略
          throw err
        }
      }
    }
    if (!gotToken) throw new Error('大模型无响应')
    assistantMsg.streaming = false
  } catch (err) {
    // 流式失败：移除占位消息，交由上层降级到 FAQ
    const idx = messages.value.indexOf(assistantMsg)
    if (idx >= 0 && !gotToken) {
      messages.value.splice(idx, 1)
      throw err
    }
    // 已输出部分内容则保留并追加提示
    assistantMsg.streaming = false
    assistantMsg.content += '\n\n（回答中断，请重试）'
  }
}

/** FAQ 降级：调用 OA-2 自建向量库检索式问答 */
const faqChat = async (question: string): Promise<void> => {
  try {
    const res = await axios.post('/api/v1/employee/chat/ask', { question })
    const body = res.data
    if (body && body.code === 200 && body.data) {
      messages.value.push({
        role: 'assistant',
        content: body.data.answer,
        via: 'faq',
        related: body.data.related?.length ? body.data.related : undefined,
        suggestions: body.data.suggestions?.length ? body.data.suggestions : undefined,
      })
    } else {
      throw new Error(body?.message || 'FAQ 服务异常')
    }
  } catch (error: unknown) {
    const status = (error as { response?: { status?: number } })?.response?.status
    if (status === 401) {
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/emp-login')
      return
    }
    messages.value.push({
      role: 'assistant',
      content: 'AI 客服暂时不可用，请检查网络后重试',
      error: true,
      retryQuestion: question,
    })
  }
}

const send = async (text?: string) => {
  const question = (text ?? input.value).trim()
  if (!question || loading.value) return

  input.value = ''
  messages.value.push({ role: 'user', content: question })
  loading.value = true
  scrollToBottom()

  try {
    if (aiOnline.value !== false) {
      try {
        await streamChat(question)
      } catch {
        // 大模型不可用：标记离线并降级 FAQ
        aiOnline.value = false
        await faqChat(question)
      }
    } else {
      await faqChat(question)
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const clearChat = async () => {
  messages.value = []
  pushWelcome()
  // 清空服务端会话记忆并重新探测大模型（静默执行）
  axios.delete('/api/v1/ai/chat/session').catch(() => {})
  checkHealth()
  ElMessage.success('已开启新对话')
}

onMounted(() => {
  pushWelcome()
  checkHealth()
  loadSuggestions()
})
</script>

<style scoped>
.ai-chat {
  height: 100%;
  display: flex;
  justify-content: center;
}

.chat-container {
  width: 100%;
  max-width: 1000px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(125, 120, 120, 0.1);
  overflow: hidden;
}

/* 头部 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #ebeef5;
  background: linear-gradient(135deg, #033363 0%, #0a4d8f 100%);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header .title {
  margin: 0;
  font-size:18px;
  font-weight: bold;
  color: white;
}

.chat-header .subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  display: flex;
  align-items: center;
  gap: 5px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background-color: #67c23a;
  box-shadow: 0 0 4px #67c23a;
}

.status-dot.offline {
  background-color: #e6a23c;
}

.status-dot.checking {
  background-color: #909399;
  animation: blink 1s infinite;
}

@keyframes blink {
  50% {
    opacity: 0.3;
  }
}

.chat-header .el-button {
  color: rgba(255, 255, 255, 0.85);
}

/* 头像 */
.bot-avatar,
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}

.bot-avatar {
  background: linear-gradient(135deg, #409eff 0%, #1d6fd1 100%);
}

.header-avatar {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.15);
}

.user-avatar {
  background-color: #909399;
}

/* 消息区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg-user {
  justify-content: flex-end;
}

.msg-body {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}

.msg-user .msg-body {
  align-items: flex-end;
}

/* 气泡 */
.bubble {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble.assistant {
  background-color: white;
  color: #303133;
  border: 1px solid #ebeef5;
  border-top-left-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.bubble.user {
  background: linear-gradient(135deg, #409eff 0%, #1d6fd1 100%);
  color: white;
  border-top-right-radius: 2px;
}

.bubble-error {
  background-color: #fef0f0 !important;
  border-color: #fbc4c4 !important;
  color: #f56c6c !important;
}

.via-tag {
  margin-top: 4px;
  font-size: 11px;
  color: #c0c4cc;
}

/* 相关/推荐问题 chips */
.chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.chips-label {
  font-size: 12px;
  color: #909399;
}

.chip {
  cursor: pointer;
}

.chip:hover {
  opacity: 0.8;
}

/* 气泡内打字动画 */
.typing-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 40px;
}

.typing-inline .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #c0c4cc;
  animation: typing-bounce 1.2s infinite ease-in-out;
}

.typing-inline .dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-inline .dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

/* 输入区 */
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #ebeef5;
  background-color: white;
  flex-shrink: 0;
}

.chat-input .el-textarea {
  flex: 1;
}

.send-btn {
  height: 54px;
  padding: 0 22px;
}
</style>

<style scoped>
.ai-chat {
  height: 100%;
  min-height: 560px;
  justify-content: stretch;
}

.chat-container {
  max-width: none;
  height: 100%;
  overflow: hidden;
  border: 1px solid #dfe5ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 38px rgba(20,34,46,.08);
}

.chat-header {
  position: relative;
  min-height: 72px;
  padding: 13px 20px;
  border-bottom: 0;
  background:
    linear-gradient(rgba(255,255,255,.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.04) 1px, transparent 1px),
    #18252e;
  background-size: 26px 26px;
}

.chat-header::after { position: absolute; right: 0; bottom: 0; width: 32%; height: 3px; background: #e5ff4f; content: ''; }
.chat-header .title { font-size: 15px; font-weight: 750; }
.chat-header .subtitle { color: #8da0aa; font-size: 9px; }
.header-avatar { width: 42px; height: 42px; border-radius: 4px; color: #07140f; background: #e5ff4f; }
.bot-avatar { border-radius: 4px; color: #fff; background: #00a878; }
.user-avatar { border-radius: 4px; background: #263844; }
.chat-messages {
  padding: 24px;
  gap: 18px;
  background:
    linear-gradient(rgba(16,24,32,.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16,24,32,.025) 1px, transparent 1px),
    #f5f7f8;
  background-size: 30px 30px;
}
.bubble { padding: 12px 15px; border-radius: 6px; font-size: 12px; line-height: 1.75; }
.bubble.assistant { border-color: #dfe5ea; border-top-left-radius: 0; color: #344154; background: #fff; box-shadow: 0 7px 22px rgba(20,34,46,.05); }
.bubble.user { border-top-right-radius: 0; color: #06140f; background: #e5ff4f; }
.via-tag { color: #97a2ab; font-family: Consolas, monospace; font-size: 8px; }
.chip { border-color: #cdd7dc; color: #536271; background: rgba(255,255,255,.8); }
.chip:hover { border-color: #00a878; color: #008d65; opacity: 1; }
.chat-input { padding: 14px 18px; border-top-color: #dfe5ea; background: #fff; }
.chat-input :deep(.el-textarea__inner) { min-height: 54px !important; padding: 12px 14px; resize: none; }
.send-btn { width: 52px; height: 54px; padding: 0; }
.status-dot.online { background: #e5ff4f; box-shadow: 0 0 8px #e5ff4f; }

@media (max-width: 720px) {
  .ai-chat { min-height: calc(100vh - 184px); }
  .chat-header { padding: 12px 14px; }
  .chat-messages { padding: 16px 12px; }
  .msg-body { max-width: 82%; }
  .chat-input { padding: 10px; gap: 8px; }
  .send-btn { width: 46px; flex: 0 0 46px; }
}
</style>
