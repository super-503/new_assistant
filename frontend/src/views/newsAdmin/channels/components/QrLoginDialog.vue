<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { pollQrLoginApi, startQrLoginApi, type NewsChannel, type NewsQrLoginPoll } from '@/api/newsChannel'
import NewsSpinner from '../../components/NewsSpinner.vue'
import { notifySuccess } from '../../newsNotify'

/** 扫码登录弹窗：打开即发起扫码并轮询，扫码确认后自动关闭；二维码过期时可刷新 */
const props = defineProps<{
  channel: NewsChannel
}>()

const emit = defineEmits<{
  /** 登录成功，凭证已保存 */
  confirmed: []
}>()

/** 轮询间隔 */
const POLL_INTERVAL_MS = 2000

const visible = defineModel<boolean>({ required: true })

const qrImage = ref('')
const pollState = ref<NewsQrLoginPoll | null>(null)
const isStarting = ref(false)
/** 发起或轮询失败的原因 */
const errorText = ref('')

let sessionId = ''
let pollTimer: ReturnType<typeof setTimeout> | undefined
/** 关闭或刷新后，进行中的轮询结果作废 */
let sessionSeq = 0

const stopPolling = () => {
  clearTimeout(pollTimer)
  sessionSeq++
}

const poll = async (seq: number) => {
  let result: NewsQrLoginPoll
  try {
    result = await pollQrLoginApi(props.channel.code, sessionId)
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    if (seq === sessionSeq) errorText.value = error.message
    return
  }
  if (seq !== sessionSeq) return
  pollState.value = result
  if (result.status === 'CONFIRMED') {
    notifySuccess(result.account ? `登录成功：${result.account}` : '登录成功')
    visible.value = false
    emit('confirmed')
    return
  }
  if (result.status !== 'EXPIRED') pollTimer = setTimeout(() => poll(seq), POLL_INTERVAL_MS)
}

const start = async () => {
  stopPolling()
  const seq = sessionSeq
  qrImage.value = ''
  pollState.value = null
  errorText.value = ''
  isStarting.value = true
  try {
    const result = await startQrLoginApi(props.channel.code)
    if (seq !== sessionSeq) return
    sessionId = result.sessionId
    qrImage.value = result.qrImage
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    if (seq === sessionSeq) errorText.value = error.message
    return
  } finally {
    isStarting.value = false
  }
  pollTimer = setTimeout(() => poll(seq), POLL_INTERVAL_MS)
}

onBeforeUnmount(stopPolling)
</script>

<template>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog qr-dialog"
    :title="`${props.channel.name}扫码登录`"
    width="360px"
    append-to-body
    :show-close="false"
    @open="start"
    @closed="stopPolling"
  >
    <div class="qr-body">
      <div class="qr-frame">
        <NewsSpinner v-if="isStarting" :size="32" />
        <img v-else-if="qrImage" class="qr-image" :src="qrImage" alt="登录二维码" />
        <div v-if="pollState?.status === 'EXPIRED'" class="qr-expired">
          <span>二维码已过期</span>
          <el-button @click="start">刷新</el-button>
        </div>
      </div>
      <p v-if="errorText" class="qr-error" role="alert">
        {{ errorText }}
        <el-button text type="primary" @click="start">重试</el-button>
      </p>
      <p v-else-if="pollState && pollState.status !== 'EXPIRED'" class="qr-status" role="status">
        {{ pollState.statusDesc }}
      </p>
      <p v-else-if="qrImage && !pollState" class="qr-status" role="status">等待扫码</p>
    </div>

    <template #footer>
      <el-button text type="primary" @click="visible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.qr-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zg-space-lg);
}

.qr-frame {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 220px;
  height: 220px;
  overflow: hidden;
  border-radius: var(--zg-radius-lg);
  background: var(--surface-container-highest);
  color: var(--second-color);
}

.qr-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-expired {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--zg-space-lg);
  background: var(--surface-container-highest);
  color: var(--main-color);
  font-weight: var(--zg-weight-medium);
  animation: qr-fade 200ms linear;
}

.qr-status {
  margin: 0;
  color: var(--second-color);
}

.qr-error {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xs);
  margin: 0;
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
}

@keyframes qr-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
