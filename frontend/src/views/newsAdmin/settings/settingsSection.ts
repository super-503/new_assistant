export const FEISHU_SECTIONS = [
  { key: 'apps', label: '应用' },
  { key: 'recipients', label: '人员' },
  { key: 'card', label: '卡片' }
] as const

export type FeishuSection = (typeof FEISHU_SECTIONS)[number]['key']
export type SettingsSection = FeishuSection | 'plans' | 'ai' | 'rejectReasons'
