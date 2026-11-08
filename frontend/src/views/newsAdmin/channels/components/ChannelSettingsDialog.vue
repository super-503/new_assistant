<script setup lang="ts">
import { computed, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  saveChannelSettingsApi,
  saveTokenApi,
  verifyCredentialApi,
  verifyTokenApi,
  type NewsChannel,
  type NewsCredentialVerify
} from '@/api/newsChannel'
import { formatMinute } from '../../components/newsFormat'
import NewsSpinner from '../../components/NewsSpinner.vue'
import { confirmDiscard, notifySuccess } from '../../newsNotify'
import QrLoginDialog from './QrLoginDialog.vue'

/**
 * 渠道设置弹窗（520px）：启用渠道；凭证区按凭证方式显示扫码登录或 Token 输入，无需凭证时不显示；验证连接。
 */
const props = defineProps<{
  /** 当前设置的渠道，凭证更新后由父组件传入最新数据 */
  channel: NewsChannel
}>()

const emit = defineEmits<{
  /** 渠道启用状态或凭证已变化，渠道列表需要刷新 */
  changed: []
}>()

const visible = defineModel<boolean>({ required: true })

const enabled = ref(true)
const token = ref('')
/** Token 校验失败的原因 */
const tokenError = ref('')
const isSaving = ref(false)
const isVerifying = ref(false)
/** 验证结果；验证接口本身失败时为 { valid: false, message } */
const verifyResult = ref<NewsCredentialVerify | null>(null)
const qrVisible = ref(false)

const mode = computed(() => props.channel.credentialMode)
const credentialInvalid = computed(() => props.channel.credentialStatus === 'INVALID')
const isDirty = computed(() => enabled.value !== props.channel.enabled || token.value.trim() !== '')

const onOpen = () => {
  enabled.value = props.channel.enabled
  token.value = ''
  tokenError.value = ''
  verifyResult.value = null
}

/** 输入框有 Token 时验证这个 Token（不保存），否则验证渠道已保存的凭证 */
const verify = async () => {
  const tokenText = token.value.trim()
  isVerifying.value = true
  verifyResult.value = null
  try {
    verifyResult.value =
      mode.value === 'TOKEN' && tokenText !== ''
        ? await verifyTokenApi(props.channel.code, tokenText)
        : await verifyCredentialApi(props.channel.code)
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    verifyResult.value = { valid: false, account: null, message: error.message }
  } finally {
    isVerifying.value = false
  }
}

/** 改了 Token 后，上一次的校验与验证结果不再对应输入框 */
const onTokenInput = () => {
  tokenError.value = ''
  verifyResult.value = null
}

const onQrConfirmed = () => {
  verifyResult.value = null
  emit('changed')
}

const onBeforeClose = async (done: () => void) => {
  if (isDirty.value && !(await confirmDiscard())) return
  done()
}

const close = () => onBeforeClose(() => (visible.value = false))

/** 先保存 Token（校验不通过则不保存任何项），再保存启用状态 */
const save = async () => {
  const tokenText = token.value.trim()
  tokenError.value = ''
  /** Token 校验通过后的登录账号 */
  let account: string | null = null
  isSaving.value = true
  try {
    if (mode.value === 'TOKEN' && tokenText !== '') {
      try {
        account = (await saveTokenApi(props.channel.code, tokenText)).credentialAccount
        token.value = ''
      } catch (error) {
        if (!(error instanceof NewsApiError)) throw error
        tokenError.value = error.message
        return
      }
    }
    if (enabled.value !== props.channel.enabled) await saveChannelSettingsApi(props.channel.code, enabled.value)
  } finally {
    isSaving.value = false
  }
  notifySuccess(account ? `已保存渠道设置，登录账号：${account}` : '已保存渠道设置')
  visible.value = false
  emit('changed')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog channel-settings-dialog"
    :title="`${props.channel.name}渠道设置`"
    width="520px"
    append-to-body
    :show-close="false"
    :before-close="onBeforeClose"
    @open="onOpen"
  >
    <div class="settings-body">
      <section class="settings-section" aria-labelledby="channel-collect-title">
        <h3 id="channel-collect-title" class="section-title">采集</h3>
        <div class="inline-field">
          <span id="channel-enabled" class="inline-label">启用渠道</span>
          <el-switch v-model="enabled" aria-labelledby="channel-enabled" />
        </div>
      </section>

      <section v-if="mode !== 'NONE'" class="settings-section" aria-labelledby="channel-credential-title">
        <div class="section-head">
          <h3 id="channel-credential-title" class="section-title">凭证</h3>
          <span class="credential-state">
            <span class="state-dot" :class="credentialInvalid ? 'is-error' : 'is-ok'" aria-hidden="true"></span>
            {{ props.channel.credentialStatusDesc }}
          </span>
        </div>

        <dl class="credential-info">
          <div class="info-row">
            <dt>登录账号</dt>
            <dd>{{ props.channel.credentialAccount ?? '—' }}</dd>
          </div>
          <div class="info-row">
            <dt>更新时间</dt>
            <dd class="is-number">
              {{ props.channel.credentialUpdateTime ? formatMinute(props.channel.credentialUpdateTime) : '—' }}
            </dd>
          </div>
        </dl>

        <el-form v-if="mode === 'TOKEN'" label-position="top" @submit.prevent>
          <el-form-item label="Token" :error="tokenError">
            <el-input
              v-model="token"
              class="token-input"
              type="password"
              show-password
              autocomplete="off"
              placeholder="粘贴新的 Token，保存时校验"
              @input="onTokenInput"
            />
          </el-form-item>
        </el-form>

        <div class="credential-actions">
          <el-button v-if="mode === 'QR'" @click="qrVisible = true">扫码登录</el-button>
          <el-button plain :disabled="isVerifying" @click="verify">
            <NewsSpinner v-if="isVerifying" />
            {{ isVerifying ? '验证中' : '验证连接' }}
          </el-button>
          <span
            v-if="verifyResult"
            class="verify-result"
            :class="verifyResult.valid ? 'is-ok' : 'is-error'"
            role="status"
          >
            {{
              verifyResult.valid
                ? `连接正常${verifyResult.account ? `：${verifyResult.account}` : ''}`
                : verifyResult.message
            }}
          </span>
        </div>
      </section>
    </div>

    <template #footer>
      <el-button text type="primary" @click="close">取消</el-button>
      <el-button text type="primary" :loading="isSaving" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <QrLoginDialog v-if="mode === 'QR'" v-model="qrVisible" :channel="props.channel" @confirmed="onQrConfirmed" />
</template>

<style scoped lang="scss">
.settings-body {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xl);
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xl);

  .el-form-item {
    margin-bottom: 0;
  }
}

.section-head {
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
}

.section-title {
  flex: 1;
  margin: 0;
  color: var(--brand-indigo);
  font-size: var(--zg-text-base);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.1px;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xl);
  min-height: 40px;
}

.inline-label {
  flex: 1;
  min-width: 0;
}

.credential-state {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-md);
}

.state-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.is-ok {
    background: var(--status-approved);
  }
  &.is-error {
    background: var(--status-rejected);
  }
}

.credential-info {
  display: grid;
  gap: var(--zg-space-md);
  margin: 0;
}

.info-row {
  display: flex;
  gap: var(--zg-space-xl);

  dt {
    flex: 0 0 72px;
    color: var(--second-color);
  }

  dd {
    min-width: 0;
    margin: 0;
    overflow-wrap: anywhere;
  }
}

.is-number {
  font-variant-numeric: tabular-nums;
}

.token-input :deep(.el-input__inner) {
  font-family: 'Roboto Mono', ui-monospace, Menlo, monospace;
}

.credential-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md);

  .el-button + .el-button {
    margin-left: 0;
  }
}

.verify-result {
  animation: verify-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;

  &.is-ok {
    color: var(--status-approved);
  }
  &.is-error {
    color: var(--status-rejected);
  }
}

@keyframes verify-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
