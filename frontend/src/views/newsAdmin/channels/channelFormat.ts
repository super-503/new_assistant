import type { NewsSourceStat } from '@/api/newsChannel'

/** 统计数值：不足一万原样显示，如 321；一万起以「万」为单位保留一位小数，如 124000 显示为 12.4万；一亿起以「亿」为单位 */
export const formatStatValue = (value: number) => {
  if (value >= 100_000_000) return `${trimDecimal(value / 100_000_000)}亿`
  if (value >= 10_000) return `${trimDecimal(value / 10_000)}万`
  return String(value)
}

/** 保留一位小数，整数时去掉「.0」 */
const trimDecimal = (value: number) => {
  const fixed = (Math.floor(value * 10) / 10).toFixed(1)
  return fixed.endsWith('.0') ? fixed.slice(0, -2) : fixed
}

/** 统计项列表：「播放 12.4万 · 评论 321」 */
export const formatStats = (stats: NewsSourceStat[]) =>
  stats.map((stat) => `${stat.name} ${formatStatValue(stat.value)}`).join(' · ')
