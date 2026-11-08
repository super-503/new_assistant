# 资讯中心（/news-admin）

资讯推荐助手的管理端使用下列视觉规则。适用范围是 `.news-admin-page` 及其浮层。

## 1. 配色

资讯中心整体采用 Material Design 3 暗色方案（种子色品牌蓝 #3e66ef），不用全站的靛蓝→紫品牌色与恒定暗色档的灰阶。

- 原语：`--zg-m3-*`（M3 角色色）与 `--zg-tag-{1..5}-{bg,text}`（标签循环配色），见 `primitives.css`。
- 皮肤：`semantic.css` 的「资讯中心皮肤」块，把通用语义 token 映射到 M3 角色，另有一组**只在本档定义**的 token：

| Token | M3 角色 | 用途 |
| --- | --- | --- |
| `--second-bg-color` | surface | 页面底色 |
| `--third-bg-color` | surface-container-low | 导航栏、表格容器、顶栏 |
| `--surface-container` | surface-container | AI 结构化结果卡片底、代码块 |
| `--surface-container-high` | surface-container-high | 搜索框底 |
| `--surface-container-highest` | surface-container-highest | 输入框、胶囊按钮、头像底 |
| `--main-color` / `--second-color` | on-surface / on-surface-variant | 主文字 / 次要文字与图标 |
| `--surface-border` | outline-variant | 分割线、常规描边 |
| `--surface-outline` | outline | 筛选 chip 与描边按钮的描边、占位符 |
| `--row-hover-overlay` | on-surface 4% | 列表行悬停，叠在行底色上（`background-image`） |
| `--text-disabled` | on-surface 38% | 禁用文字 |
| `--disabled-container` | on-surface 12% | 禁用的实底按钮底色 |
| `--state-layer-hover` | on-surface 8% | 菜单项、下拉选项悬停 |
| `--media-badge-bg` / `--media-badge-text` | — | 缩略图上的时长角标 |
| `--focus-ring` | secondary | 键盘聚焦框 |
| `--brand-indigo` / `--on-brand` | primary / on-primary | 主操作实底按钮、选中态文字、链接 |
| `--brand-container` / `--on-brand-container` | primary-container / on-primary-container | 主色容器、链接悬停色 |
| `--brand-tint` / `--brand-tint-border` | primary 6% / 16% | AI 结构化结果卡片的淡底与细描边 |
| `--selected-bg` / `--selected-text` | secondary-container / on-secondary-container | 导航胶囊、选中的选项 chip、次要实底按钮 |
| `--status-{approved,pending,rejected}` | success / warning / error | 状态文字与图标 |
| `--status-*-container` / `--on-status-*-container` | 对应 container / on-container | 「已通过」标记、警示条、失败提示 |
| `--control-error-border` | error | 表单出错描边 |
| `--badge-bg` / `--badge-text` | error / on-error | 数量角标 |
| `--snackbar-bg` / `--snackbar-text` / `--snackbar-action` | inverse-surface / inverse-on-surface / inverse-primary | 底部提示 |
| `--tag-cycle-{1..5}-{bg,text}` | — | 标签循环配色 |

- 强调色只用于主操作、选中态，以及失败、拒绝这类需要注意的状态；头像、图标用中性色。
- 「显示为标签」的字段按选项顺序循环使用 5 组标签配色（`NewsTagChip` 传 `colorIndex`）；岗位分类用中性描边样式
  （透明底、`--surface-border` 描边、`--second-color` 文字）。

## 2. 浮层

皮肤选择器已列入以下类名，资讯中心的浮层必须带上对应类名：

| 浮层 | 类名 | 写法 |
| --- | --- | --- |
| 抽屉 | `news-admin-drawer` | `<el-drawer class="news-admin-drawer">` |
| 表单弹窗 | `news-admin-dialog` | `<el-dialog class="dark-dialog news-admin-dialog">` |
| 确认 / 输入弹窗 | `news-admin-message-box` | `customClass: 'dark-message-box news-admin-message-box'` |
| 消息 | `news-admin-message` | `ElMessage({ customClass: 'news-admin-message' })` |
| 下拉 / 弹出层 | `news-admin-popper` | `popper-class="news-admin-popper"` |

弹窗沿用 MASTER §6 的预设版式，再叠加本档类名：皮肤块排在恒定暗色档之后，同时带两个类名时取本档取值。

### Element Plus

- 全局变量（`--el-color-primary`、`--el-text-color-*`、`--el-bg-color*`、`--el-border-color*`、`--el-fill-color*` 等）在皮肤块里映射到 M3 角色。
- 组件级变量和尺寸、形态补丁在 `views/newsAdmin/newsAdminElement.scss`（由 `NewsAdminPage.vue` 引入），覆盖输入框、文本域、
  选择器、表单项、按钮、开关、复选框、表单弹窗、确认弹窗、抽屉、下拉菜单与消息。
- 按钮写法：`<el-button type="primary">` 主色实底，`<el-button>` 次要实底，`<el-button text type="primary">` 文字按钮，
  `<el-button plain>` 描边按钮，`<el-button text type="danger">` 危险文字按钮。
- 搜索框：`<el-input class="news-search-input">`。
- 表单：`<el-form label-position="top" require-asterisk-position="right">`；锁定的表单项整体 `opacity: 0.6`。

## 3. 尺寸（紧凑规格）

| 元素 | 取值 |
| --- | --- |
| 按钮 | 高 32px、14px 字、全圆角 |
| 搜索框 | 高 36px、宽 240px（最窄 160px）、全圆角、`--surface-container-high` 底 |
| 表单输入框 | 高 40px |
| 筛选 chip | 高 28px |
| 开关 | 36 × 20 |
| 侧栏导航项 | 高 40px |
| 标签页 | 高 44px，选中项下方 3px 主色指示条 |
| 左侧导航栏 | 宽 80px，图标下方 12px 标签，选中项图标带 56 × 32 胶囊底 |
| 分类栏 / 渠道栏 | 宽 220px |
| 列表内容区 | 居中，最宽 960px |

## 4. 圆角

| 元素 | 取值 |
| --- | --- |
| 标签 | 6px（`--zg-radius-sm`） |
| chip、输入框 | 8px（`--zg-radius-md`） |
| 卡片、图片、播放区 | 12px（`--zg-radius-lg`） |
| 侧边栏 | 16px（`--zg-radius-xl`） |
| 弹窗 | 28px（`--zg-radius-2xl`） |
| 按钮、导航胶囊、搜索框 | 全圆角（`--zg-radius-pill`） |

## 5. 字号

| 元素 | 取值 |
| --- | --- |
| 页面标题 | 20px / 28px、600 |
| 标题旁的数据 | 13px、`--second-color` |
| 卡片标题 | 15px、500 |
| 正文 | 14px / 20px |
| 阅读区正文 | 15px / 26px，字距 0.5px（`NewsReader` 内置） |
| 导航标签、来源信息、标签 | 12px |

页面上不放副标题、身份说明、操作提示这类说明文字。

## 6. 表单

全站只用这一种输入框，替代 MASTER 的描边控件：

- 标签在输入框上方，13px、`--second-color`；必填项标 `--status-rejected` 色的「*」。
- 输入框用填充底色 `--control-bg`，无静态描边；聚焦显示 `--control-focus-border` 描边，出错时描边为 `--control-error-border`。
- 示例文字只作框内占位；报错在输入框下方，12px、`--status-rejected`。
- 下拉框右侧统一放箭头；只读或锁定的字段整体变淡。
- 筛选 chip 宽度跟随文字，未筛选时显示维度名，选中后显示值并加对勾；文字最长 128px，超出截断；chip 间距 8px，放不下时换行。

## 7. 动效

缓动统一用 M3 曲线，替代 MASTER 的 `--zg-ease-standard`：

| Token | 曲线 | 用途 |
| --- | --- | --- |
| `--zg-ease-m3-standard` | `cubic-bezier(0.2, 0, 0, 1)` | 状态层、标签页指示条、开关、chip |
| `--zg-ease-m3-emphasized-decelerate` | `cubic-bezier(0.05, 0.7, 0.1, 1)` | 进场：弹窗缩放、侧边栏滑入、底部提示上浮、列表依次入场 |
| `--zg-ease-m3-emphasized-accelerate` | `cubic-bezier(0.3, 0, 0.8, 0.15)` | 离场：审核卡片处理后向右滑出（220ms） |

- 可点元素悬停有状态层（`currentColor` 8%），按下有涟漪。
- 复选框勾选描画勾号，筛选 chip 对勾弹出，开关滑块平移，标签页下划线伸缩。
- 搜索订阅源显示骨架屏；重新加工、验证连接、立即拉取显示环形进度。
- 键盘聚焦框 2px `--focus-ring`，鼠标点击时不显示（`:focus-visible`）。
- 阴影用 `--zg-shadow-m3-{1,2,3}`：卡片悬停 / 菜单 / 弹窗与底部提示。
