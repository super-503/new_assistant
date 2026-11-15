<script setup lang="ts">
import { computed, ref } from 'vue'
import { User } from '@element-plus/icons-vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  listFeishuAuthorizedUsersApi,
  type NewsFeishuAppDraft,
  type NewsFeishuAuthorizedUser,
  type NewsFeishuRecipient
} from '@/api/newsSetting'
import FeishuAuthorizeButton from './FeishuAuthorizeButton.vue'
import FeishuProfileRow from './FeishuProfileRow.vue'

const props = defineProps<{
  apps: NewsFeishuAppDraft[]
  savedAppIds: string[]
  disabled: boolean
}>()
const recipients = defineModel<NewsFeishuRecipient[]>({ required: true })
const visible = ref(false)
const appId = ref('')
const users = ref<NewsFeishuAuthorizedUser[]>([])
const selected = ref<string[]>([])
const search = ref('')
const loading = ref(false)
const loadFailed = ref(false)
let pendingLoad = 0
const app = computed(() => props.apps.find((item) => item.appId === appId.value))
const candidates = computed(() => {
  const added = new Set(recipients.value.filter((user) => user.appId === appId.value).map((user) => user.openId))
  return users.value.filter((user) => !added.has(user.openId))
})
const visibleUsers = computed(() => {
  const query = search.value.trim().toLowerCase()
  return query ? candidates.value.filter((user) => user.name?.toLowerCase().includes(query)) : candidates.value
})

const loadUsers = async () => {
  const current = ++pendingLoad
  users.value = []
  selected.value = []
  search.value = ''
  loadFailed.value = false
  if (!appId.value) return
  loading.value = true
  try {
    const authorized = await listFeishuAuthorizedUsersApi(appId.value)
    if (pendingLoad === current) users.value = authorized
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    if (pendingLoad === current) loadFailed.value = true
  } finally {
    if (pendingLoad === current) loading.value = false
  }
}

const open = () => {
  appId.value = props.apps[0]?.appId ?? ''
  visible.value = true
  void loadUsers()
}

const onAuthorized = async (user: NewsFeishuAuthorizedUser) => {
  const currentApp = appId.value
  const previous = selected.value
  await loadUsers()
  if (!visible.value || currentApp !== appId.value) return
  const checked = new Set([...previous, user.openId])
  selected.value = candidates.value.filter((entry) => checked.has(entry.openId)).map((entry) => entry.openId)
}

const confirm = () => {
  if (!app.value || !selected.value.length || loading.value) return
  const checked = new Set(selected.value)
  const entries = candidates.value.filter((user) => checked.has(user.openId)).map((user) => ({
    ...user,
    appId: appId.value,
    avatarUrl: user.avatarUrl?.startsWith('https://') ? user.avatarUrl : null
  }))
  recipients.value = [...recipients.value, ...entries]
  visible.value = false
}

const appName = (key: string) => {
  const index = props.apps.findIndex((item) => item.appId === key)
  return props.apps[index]?.name || `飞书应用 ${index + 1}`
}

const remove = (entry: NewsFeishuRecipient) => {
  recipients.value = recipients.value.filter((recipient) => recipient !== entry)
}
</script>

<template>
  <section class="feishu-list" aria-labelledby="feishu-recipients-title">
    <header class="feishu-list-heading">
      <h3 id="feishu-recipients-title">推送人员 <small>{{ recipients.length }}</small></h3>
      <el-button type="primary" :disabled="disabled || !apps.length" @click="open">
        <SvgIcon icon-class="newsAdmin-add" size="18" /> 添加人员
      </el-button>
    </header>
    <div v-if="recipients.length" class="feishu-list-rows">
      <FeishuProfileRow
        v-for="(recipient, index) in recipients"
        :key="`${recipient.appId}:${recipient.openId}`"
        :name="recipient.name || `飞书接收人 ${index + 1}`"
        :detail="appName(recipient.appId)"
        :avatar-url="recipient.avatarUrl"
        :icon="recipient.name ? undefined : User"
      >
        <el-button text type="danger" :disabled="disabled" @click="remove(recipient)">移除</el-button>
      </FeishuProfileRow>
    </div>
    <p v-else class="feishu-empty">{{ apps.length ? '还没有推送人员。' : '请先添加一个飞书应用。' }}</p>
  </section>

  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog"
    title="添加推送人员"
    width="min(480px, calc(100vw - 24px))"
    destroy-on-close
  >
    <el-form label-position="top" require-asterisk-position="right" @submit.prevent="confirm">
      <el-form-item label="所属应用" required>
        <el-select
          v-model="appId"
          popper-class="news-admin-popper"
          @change="loadUsers"
        >
          <el-option v-for="item in apps" :key="item.appId" :label="appName(item.appId)" :value="item.appId" />
        </el-select>
      </el-form-item>
    </el-form>
    <div v-if="app" class="authorized-picker">
      <div class="picker-heading">
        <strong>已授权人员</strong>
        <FeishuAuthorizeButton
          :key="app.appId"
          :app-id="app.appId"
          :app-secret="app.appSecret ?? ''"
          :has-saved-secret="savedAppIds.includes(app.appId)"
          :disabled="disabled || loading"
          @authorized="onAuthorized"
        />
      </div>
      <p v-if="loading" role="status">正在加载已授权人员</p>
      <el-button v-else-if="loadFailed" plain @click="loadUsers">重新加载</el-button>
      <p v-else-if="!candidates.length">{{ users.length ? '已授权人员均已添加。' : '此应用还没有已授权人员。' }}</p>
      <template v-else>
        <el-input v-if="candidates.length > 5" v-model="search" clearable placeholder="搜索姓名" aria-label="搜索姓名" />
        <el-checkbox-group v-model="selected" class="picker-list" aria-label="选择已授权人员">
          <FeishuProfileRow
            v-for="user in visibleUsers"
            :key="user.openId"
            :name="user.name || '飞书用户'"
            :detail="appName(appId)"
            :avatar-url="user.avatarUrl"
            :icon="User"
          >
            <el-checkbox :value="user.openId" :aria-label="user.name || '飞书用户'" />
          </FeishuProfileRow>
          <p v-if="!visibleUsers.length">没有匹配人员。</p>
        </el-checkbox-group>
      </template>
    </div>
    <template #footer>
      <el-button plain @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="loading || !selected.length" @click="confirm">
        {{ selected.length ? `添加 ${selected.length} 人` : '添加人员' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.authorized-picker {
  display: grid;
  gap: var(--zg-space-md);

  p {
    margin: 0;
    color: var(--second-color);
  }
}

.picker-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-list {
  max-height: min(300px, 40vh);
  overflow: auto;
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-lg);
  line-height: 20px;

  :deep(.profile-row + .profile-row) { border-top: 1px solid var(--surface-border); }
  p { padding: var(--zg-space-lg); }
}
</style>
