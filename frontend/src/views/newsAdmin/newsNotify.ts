import { ElMessage, ElMessageBox } from 'element-plus'

/** 底部提示条，如「已加入拉取队列」「已保存，仅影响后续加工」 */
export const notifySuccess = (message: string) =>
  ElMessage({ type: 'success', message, customClass: 'news-admin-message', placement: 'bottom' })

/** 底部提示条：操作失败的原因 */
export const notifyError = (message: string) =>
  ElMessage({ type: 'error', message, customClass: 'news-admin-message', placement: 'bottom' })

/**
 * 确认弹窗，用户确认时返回 true，取消或关闭返回 false。
 * danger 为不可逆操作，确认按钮用 error 色。
 */
export const confirmAction = (options: {
  title: string
  message: string
  confirmText: string
  cancelText?: string
  danger?: boolean
}) =>
  ElMessageBox.confirm(options.message, options.title, {
    customClass: `dark-message-box news-admin-message-box${options.danger ? ' is-danger' : ''}`,
    confirmButtonText: options.confirmText,
    cancelButtonText: options.cancelText ?? '取消'
  }).then(
    () => true,
    () => false
  )

/** 有未保存的修改时关闭弹窗前的确认 */
export const confirmDiscard = () =>
  confirmAction({
    title: '放弃未保存的修改？',
    message: '表单内容尚未保存。',
    confirmText: '放弃修改',
    cancelText: '继续编辑',
    danger: true
  })
