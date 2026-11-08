/**
 * 资讯中心的时间、时长格式化。后端时间统一为 yyyy-MM-dd HH:mm:ss（本地时区），按固定位置截取。
 */

const pad = (value: number) => String(value).padStart(2, '0')

/** 本地日期 yyyy-MM-dd */
export const formatLocalDate = (date: Date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

/** 相对今天偏移若干天的本地日期 yyyy-MM-dd */
export const getOffsetDate = (offsetDays: number) => {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return formatLocalDate(date)
}

/** 列表时间：今天 10:32 / 昨天 20:15 / 11-02 09:05 / 2025-12-30 */
export const formatItemTime = (time: string) => {
  const day = time.slice(0, 10)
  const clock = time.slice(11, 16)
  if (day === getOffsetDate(0)) return `今天 ${clock}`
  if (day === getOffsetDate(-1)) return `昨天 ${clock}`
  if (day.slice(0, 4) === getOffsetDate(0).slice(0, 4)) return `${day.slice(5)} ${clock}`
  return day
}

/** 日期 yyyy-MM-dd */
export const formatDay = (time: string) => time.slice(0, 10)

/** 精确到分钟 yyyy-MM-dd HH:mm */
export const formatMinute = (time: string) => time.slice(0, 16)

/** 处理记录时间 MM-dd HH:mm */
export const formatLogTime = (time: string) => time.slice(5, 16)

/** 视频时长：12:36 / 1:02:09 */
export const formatDuration = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${minutes}:${pad(rest)}`
}
