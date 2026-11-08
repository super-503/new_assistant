# 资讯推荐助手

面向管理员的资讯采集和审核工作台，包含资讯审核、资讯库、渠道配置和基础配置。支持 AIBase、公众号、B 站和 X，采集后进行 AI 字段生成，审核通过的内容进入资讯库。论文搜索提供中国知网、arXiv、Crossref 和 Europe PMC 的在线检索。

## 环境

- JDK 11 或兼容版本、Maven 3.9、Node.js 22 或更新版本、pnpm 11
- MySQL 8、Redis
- AI 加工使用 OpenAI 兼容的 Chat Completions 接口

## 初始化数据库

首次部署时，在项目根目录用有建库权限的 MySQL 账号依次执行以下脚本，初始化 `news_assistant`、AI 模型配置、日报发送记录和通知渠道。

```bash
mysql --default-character-set=utf8mb4 -u root -p < sql/new/news_init.sql
mysql --default-character-set=utf8mb4 -u root -p news_assistant < sql/new/news_ai_model_init.sql
mysql --default-character-set=utf8mb4 -u root -p news_assistant < sql/new/news_digest_change.sql
mysql --default-character-set=utf8mb4 -u root -p news_assistant < sql/new/news_feishu_notification_init.sql
```

在 PowerShell 7 中执行以下命令。

```powershell
Get-Content -Raw -Encoding UTF8 sql/new/news_init.sql | mysql --default-character-set=utf8mb4 -u root -p
Get-Content -Raw -Encoding UTF8 sql/new/news_ai_model_init.sql | mysql --default-character-set=utf8mb4 -u root -p news_assistant
Get-Content -Raw -Encoding UTF8 sql/new/news_digest_change.sql | mysql --default-character-set=utf8mb4 -u root -p news_assistant
Get-Content -Raw -Encoding UTF8 sql/new/news_feishu_notification_init.sql | mysql --default-character-set=utf8mb4 -u root -p news_assistant
```

已有数据库升级时执行 `news_ai_model_init.sql` 和 `news_feishu_notification_init.sql`。尚未添加日报发送记录的库执行一次 `news_digest_change.sql`。旧库也可按需执行 `news_ai_field_change.sql`，补齐「划重点」「提及产品」和岗位分类提示词。新库初始化已包含这三项。

## 启动

先在 `backend/src/main/resources/application.yml` 的 `spring` 段直接填写数据库、Redis 和管理员配置。数据库账号需提前创建，并对 `news_assistant` 库有查询和增删改权限。管理员密码留空时，Spring Boot 会在启动日志中输出临时密码。

在项目根目录构建前端和后端。Maven 会把配置和前端构建产物打进应用 JAR。

```powershell
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend build
mvn -f backend/pom.xml clean package
```

启动应用。

```powershell
java -jar backend/target/news-recommendation-assistant-1.0.0.jar
```

默认地址为 <http://127.0.0.1:8091/>。本地开发可分别运行 `mvn -f backend/pom.xml spring-boot:run` 和 `pnpm --dir frontend dev`，通过 Vite 打开的页面登录。若修改后端端口，启动前端前同步设置 `NEWS_BACKEND_URL`。

## 配置

| 环境变量 | 用途 |
| --- | --- |
| `NEWS_MODEL_URL`、`NEWS_MODEL`、`NEWS_MODEL_API_KEY` | 可选的默认模型配置。页面中保存的模型配置优先使用 |
| `NEWS_PULL_CRON`、`NEWS_TIME_ZONE` | 拉取计划和时区，默认为上海时间每天 08:00 和 20:00。设为 `-` 可暂停定时拉取 |
| `NEWS_X_PROXY_HOST`、`NEWS_X_PROXY_PORT` | X 渠道的可选 HTTP 代理 |
| `NEWS_FEISHU_WEBHOOK`、`NEWS_ADMIN_URL` | 飞书机器人 Webhook 和渠道页面完整地址，用于凭证失效和岗位分类通知 |
| `NEWS_DIGEST_FEISHU_WEBHOOK` | 未保存飞书应用机器人配置时，日报可使用此飞书群机器人 Webhook |
| `NEWS_FEISHU_AUTHORIZATION_REDIRECT_URI` | 飞书接收人授权的回调地址。默认使用 `127.0.0.1` 和 `NEWS_PORT`，其他访问域名需配置完整回调地址 |
| `NEWS_DIGEST_CRON` | 日报推送计划，默认 `0 5 21 * * *`，即每天 21:05，使用 `NEWS_TIME_ZONE`。设为 `-` 可暂停定时推送 |
| `NEWS_JOB_FAMILY_ENABLED` | 岗位分类开关，个人版默认关闭。设为 `true` 后重启可启用 |
| `NEWS_BIND_ADDRESS`、`NEWS_PORT` | 监听地址和端口，默认 `127.0.0.1:8091` |

AIBase 随库初始化一个订阅源。公众号和 B 站在渠道设置中扫码登录，X 在渠道设置中填写凭证。可从渠道配置页手动拉取单个订阅源。

个人版默认隐藏岗位分类的设置、标签和筛选，并暂停 AI 生成、人工编辑和职位族同步。旧库中的字段配置、资讯标签和历史记录会保留。

启用岗位分类后，选项使用 `news_ai_field` 中 `job_family` 的 `options_json` 配置，填写字符串数组。若已有 `user_center.feishu_user` 和职位族数据，可同时设 `NEWS_SYNC_JOB_FAMILIES=true`，定时拉取时会同步在职人员的职位族。仅在该外部表缺少职位族字段时使用 `feishu_user_job_family_change.sql`，并为应用账号授予查询权限。

## 个人飞书日报（B 站示例）

1. 在飞书开放平台创建自建应用并添加机器人能力。应用身份权限开通「以应用的身份发消息」（`im:message:send_as_bot`）或「获取与发送单聊、群组消息」（`im:message`）之一，将接收人加入应用可用范围并发布。取得 App ID、App Secret 和接收人在这个应用内的 Open ID。[飞书发送消息权限](https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/reference/im-v1) · [获取 Open ID](https://open.feishu.cn/document/faq/trouble-shooting/how-to-obtain-openid)
2. 启动资讯推荐助手，在「基础配置 → 通知发送渠道」填写三项配置。点击「发送测试消息」可检查飞书单聊是否收到，点击「保存配置」应用到日报任务。编辑同一个 App ID 时，App Secret 留空可沿用已保存的密钥。
3. 在「渠道配置 → B 站 → 渠道设置」扫码登录。用昵称或 UID 搜索并添加订阅源，选择「立即拉取」。启用的订阅源也会由定时任务拉取。
4. 在「资讯审核」打开待审视频并通过。日报在下一次运行时发送已通过且尚未发送的资讯，包含原标题、作者和原文链接。

首次运行会包含资讯库中已有的已通过资讯。没有待发内容时跳过推送，飞书确认接收后记录发送时间，失败的批次在下一次运行时重试。

### 授权飞书填写接收人

在飞书开发者后台打开日报应用的「安全设置」，添加以下重定向 URL。本地访问资讯推荐助手时使用 `http://127.0.0.1:8091`。[飞书重定向 URL 配置](https://open.feishu.cn/document/uAjLw4CM/ugTN1YjL4UTN24CO1UjN/trouble-shooting/how-to-resolve-the-authorization-page-20029-error)

```text
http://127.0.0.1:8091/ai/api/web/v1/news/setting/notification-channel/authorization/callback
```

在「通知发送渠道」输入 App ID 和 App Secret 后，点击「授权飞书」。授权成功会填入该账号的 Open ID 并显示飞书姓名。确认后可发送测试消息，再保存配置。

## 资讯 AI 模型

在「基础配置 → AI 模型」填写完整的 Chat Completions 接口地址、模型 ID 和服务要求的 API 密钥。本地服务允许密钥留空。「测试连接」会发送一条测试消息。保存后即用于后续加工，密钥保存在服务端，页面只显示密钥设置状态。

模型未配置时，AI 字段、标签、筛选和手动生成入口停用。新采集的资讯直接进入待审，审核使用原始内容。设置模型后，之前未生成的待审资讯可在详情中手动重新加工。

## 资讯媒体与论文检索

资讯封面、正文媒体和视频使用来源地址。外站限制访问或地址过期时，可打开资讯原文。已有记录继续按库中保存的地址显示。

从左侧「论文搜索」选择来源后输入题名或关键词。结果展示来源提供的作者、发表信息、摘要和 DOI，点击题名可打开来源页。全文访问权限以来源平台为准。

- [知网空间](https://search.cnki.com.cn/)提供中文文献题录，页面也提供直达知网的搜索入口。
- [arXiv](https://info.arxiv.org/help/api/user-manual.html)提供预印本检索。
- [Crossref](https://www.crossref.org/documentation/retrieve-metadata/rest-api/)提供带 DOI 的期刊、会议、学位论文和预印本题录。
- [Europe PMC](https://europepmc.org/RestfulWebService)提供生命科学和医学文献检索。

论文搜索需要服务器能够访问所选来源，每次读取一页。论文题录由上游实时返回。

## 验证命令

```powershell
mvn -f backend/pom.xml test -DskipTests=false
pnpm --dir frontend type-check
pnpm --dir frontend build
```
