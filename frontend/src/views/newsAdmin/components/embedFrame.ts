/**
 * 内嵌播放器 iframe 的 sandbox 权限：播放器需要脚本与自身同源存储，
 * 允许全屏演示和弹出新窗口（弹出的窗口不继承 sandbox）。
 * 正文里的 iframe 和播放区共用这一份。
 */
export const EMBED_FRAME_SANDBOX = 'allow-scripts allow-same-origin allow-presentation allow-popups allow-popups-to-escape-sandbox'
