<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  getFeishuAuthorizationResultApi,
  startFeishuAuthorizationApi,
  type NewsFeishuAuthorizedUser
} from '@/api/newsSetting'
import { notifyError } from '../../newsNotify'

const props = defineProps<{
  appId: string
  appSecret: string
  hasSavedSecret: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  authorized: [user: NewsFeishuAuthorizedUser]
}>()

const POLL_INTERVAL_MS = 2000
const authorizing = ref(false)
const canAuthorize = computed(() =>
  !props.disabled && !authorizing.value && !!props.appId.trim() && (!!props.appSecret.trim() || props.hasSavedSecret)
)

let popup: Window | null = null
let pollTimer: ReturnType<typeof setTimeout> | undefined
let attempt = 0

const stop = () => {
  attempt++
  clearTimeout(pollTimer)
  popup?.close()
  popup = null
  authorizing.value = false
}

const fail = (error: unknown, current: number) => {
  if (current !== attempt) return
  stop()
  if (!(error instanceof NewsApiError)) throw error
  notifyError(error.message)
}

const poll = async (current: number, state: string, appId: string, deadline: number) => {
  if (current !== attempt) return
  if (Date.now() >= deadline) {
    stop()
    notifyError('飞书授权已过期，请重新授权')
    return
  }
  try {
    const user = await getFeishuAuthorizationResultApi(state)
    if (current !== attempt) return
    if (!user) {
      pollTimer = setTimeout(() => poll(current, state, appId, deadline), POLL_INTERVAL_MS)
      return
    }
    stop()
    if (props.appId.trim() !== appId) {
      notifyError('App ID 已变更，请重新授权')
      return
    }
    emit('authorized', user)
  } catch (error) {
    fail(error, current)
  }
}

const authorize = async () => {
  if (!canAuthorize.value) return
  const authorizationWindow = window.open('', '_blank', 'popup,width=540,height=720')
  if (!authorizationWindow) {
    notifyError('请允许浏览器弹出飞书授权窗口后重试')
    return
  }
  authorizationWindow.opener = null
  popup = authorizationWindow
  authorizing.value = true
  const current = ++attempt
  const appId = props.appId.trim()
  try {
    const start = await startFeishuAuthorizationApi({ appId, appSecret: props.appSecret.trim() || null })
    if (current !== attempt) return
    if (authorizationWindow.closed) {
      stop()
      return
    }
    authorizationWindow.location.replace(start.authorizationUrl)
    const deadline = Date.now() + start.expiresIn * 1000
    pollTimer = setTimeout(() => poll(current, start.state, appId, deadline), POLL_INTERVAL_MS)
  } catch (error) {
    fail(error, current)
  }
}

onBeforeUnmount(stop)
</script>

<template>
  <el-button v-if="authorizing" plain @click="stop">取消授权</el-button>
  <el-button v-else type="primary" :disabled="!canAuthorize" @click="authorize">授权飞书</el-button>
</template>
