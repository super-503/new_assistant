# 资讯推荐助手开发指南

本仓库是前后端一体的 monorepo，`backend/` 是 Java 后端，`frontend/` 是 Vue 3 前端。本文件包含通用开发规则和前后端规范。**修改前端代码前必须先读本文的「前端项目概览」及其子节**，包括工具复用流程和工程质量要求。


## 项目定位

- 服务名为 `news-recommendation-assistant`，提供资讯采集、AI 加工、审核、资讯库、渠道设置和论文检索。
- Group ID：`com.zengame.ai`
- 后端使用 Java 11、Spring Boot 2.7.18、Spring MVC、Spring Security、MyBatis、PageHelper、MySQL 8 和 Redis。
- 默认监听 `127.0.0.1:8091`，可通过 `NEWS_BIND_ADDRESS` 和 `NEWS_PORT` 覆盖。
- Spring 配置位于 `backend/src/main/resources/application.yml`，部署参数通过 `NEWS_*` 环境变量传入。

## 常用命令

在 `backend/` 目录执行：

| 任务 | 命令 |
| --- | --- |
| 本地启动 | `mvn spring-boot:run` |
| 编译 | `mvn compile` |
| 前后端打包 | `pnpm --dir ../frontend build`，成功后执行 `mvn clean package` |
| 运行测试 | `mvn test -DskipTests=false` |
| 启动打包产物 | `java -jar target/news-recommendation-assistant-1.0.0.jar` |

运行测试时显式加 `-DskipTests=false`。首次打包前安装前端依赖，Maven 会将 `frontend/dist/` 一并打入 JAR。启动所需的数据库、Redis 和应用配置见 [README.md](README.md)。

## 目录结构

Java 包路径以下均相对于 `backend/src/main/java/com/zengame/ai/`。

- `App.java` 是 Spring Boot 启动入口。
- `business/` 按领域分包。`news/` 下有渠道插件、订阅源、AI 加工、审核、资讯库和设置，`paper/` 负责论文检索。
- `apiclient/` 下按来源和服务划分客户端，包括 `aibase`、`bilibili`、`wechatmp`、`x`、`model`、`paper` 和 `feishu`。
- `repository/ai/mapper/`：MyBatis Mapper 接口
- `repository/ai/po/`：数据库持久化对象
- `config/` 包含 Web 和安全配置以及 `properties/` 下的 `NewsProperties`。
- `infrastructure/`：通用封装、注解、切面、枚举、异常、登录态、工具类
- `task/` 中的 `NewsPullTask` 定时拉取资讯。
- `backend/src/main/resources/mapper/ai/` 存放 MyBatis XML。
- `backend/src/main/resources/application.yml` 存放 Spring 配置。
- `sql/new/`（仓库根目录）：新增的初始化、结构或配置变更、数据迁移 SQL 脚本

## 通用开发规则（前后端都适用）

- 动手写代码前，先在仓库内搜索是否已有同类或相似逻辑；能复用则复用，不能复用也采用相同写法，保证代码可读性和一致性。
- 代码、注释、重构方案、计划说明一律只写**现在的版本**：描述当前逻辑是什么、方案最终是什么样。不要写变更过程和取舍理由（如"因为 X 所以把这部分改成 Y"），这类内容属于 ADR / commit message，不混进代码、注释和计划本身。
- 分包/目录规范以本文的「目录结构」和「前端目录结构」为准，新增代码先确认归属的模块和目录，不要另起分包结构。
- 模块间只通过公开类型依赖，禁止复制实现形成双份逻辑
- 兜底处理，能力边界要考虑清楚，只在有必要时做。不要仅为了保证代码可运行，做太多兜底，导致代码隐藏关键问题，未被发现
- 新增的 SQL 脚本必须统一放在仓库根目录的 `sql/new/`，禁止放入 `resource` / `resources` 目录（包括 `backend/src/main/resources/sql/`）。一个变更或新功能所需的 SQL 按作用对象分成三类，必须分文件，不允许混在同一个文件里：
  - `<功能>_init.sql`：新建表、新功能从零初始化，用于测试 -> 线上。
  - `<功能>_change.sql` 包含已有表的结构变更（ALTER）和配置数据变更（例如 AI 字段和选项），用于测试 -> 线上。
  - `<功能>_migrate.sql`：一次性数据订正（回填、语义迁移、数据清洗），手动执行，跑完即可删除文件。

## 后端编码规则

- 编码前先确认目标模块和已有实现，遵守当前分包边界，不把业务逻辑随意放进 `config`、`infrastructure` 或 Controller。
- Controller 只做参数接收、接口编排和统一返回，业务逻辑放到对应 `*Service`。
- HTTP 返回优先使用 `Result.ok(...)` / `Result.ok(PageInfo<T>)`，列表分页沿用 `BasePageInfo`、`ResultList`、`PageHelperUtil` 等既有封装。
- 数据访问优先复用已有 Mapper、PO、XML 和 SQL 风格。新增 SQL 时保持 Mapper 接口与 `backend/src/main/resources/mapper/ai/*.xml` 同步。
- 登录用户、权限、上下文等优先复用 `LoginUtils` 和现有基础设施，不自行解析 token 或重复实现上下文逻辑。
- 外部 API 调用放在 `apiclient/` 对应 provider 包内；业务层通过 client/service 编排，不在 Controller 中直接拼 HTTP 请求。
- 优先复用 `infrastructure/util`、通用枚举、异常和常量；不要重复造工具类。
- 通用操作优先使用已引入的工具库，能用现成工具则不手写：Apache Commons（`StringUtils`、`CollectionUtils` 等）、Guava（`Lists`、`Maps` 等）、Hutool、MapStruct（对象转换）；同一类操作在全项目保持同一种工具风格。
- 不要每个方法都 try-catch：项目已有全局异常处理，只在需要补偿逻辑、资源清理或补充上下文后重新抛出的地方捕获异常。
- 日志使用 Lombok `@Slf4j`，异常日志要带上下文，避免吞异常或用空 catch 掩盖问题。
- 日志一律用 `{}` 占位符传参，禁止先 `String.format` 拼好再 `log.error(msg, e)`：占位符在日志级别未开启时不做拼接，也便于结构化采集。异常对象作为**最后一个参数**传入（`log.error("xxx, traceId={}", traceId, e)`），不要再往文案里拼 `e.getMessage()`，堆栈里已经有了。
  仅当同一段文案还要喂给飞书告警等其它出口时，才额外 `String.format` 一份给那个出口，日志本身仍走占位符。
- 事务沿用 Spring `@Transactional(rollbackFor = Exception.class)`，需要隔离级别时参考现有 Service 写法。
- 注释风格参考 JavaDoc：类和公开方法写明用途、异常点、参数和返回；复杂业务分支用简短注释说明意图。
- 类和方法注释只写业务语义，呈现输入输出: 不解释代码怎么写（精度、边界处理、计算步骤等），这些实现细节属于封装内部，由代码本身表达，或者内部简短注释说明，不暴露作为类/方法说明。
- `business` 包内的 Controller、Service、策略、转换器及关键私有流程方法必须编写有业务含义的 JavaDoc，说明用途、参数、返回值和必要的权限/异常语义。禁止使用“处理数据”“执行操作”等无法辅助排障的占位注释。简单 DTO/VO 由 Lombok 生成的访问器不要求逐项写方法注释。
- 保持 Java 11 兼容，不引入高版本 JDK API。
- 周期参数统一用 `List` 接收，再转换成 `DatePeriod`。
- 默认值通过注解在 `domain` 里做，不在 `Service` 里加
- 默认值通过注解做，不要在service里加这个
- 业务层面传递的各种数据，除非是一个字段的，都需要建 **BO**(business object) 不要用 Map 做，可读性非常差
- 字段名/表名/方法名 参考类似其他模块的**命名风格**，需要有 语义明确/简洁/规范化 的命名风格，不明确时，可学习 **Spring** 项目的命名风格
- 代码可以多组合各种**设计模式**，学习 **Spring** 项目代码设计

### 分层职责 后端分层

- Controller 只负责路由、参数校验、当前用户提取、调用 Service 和封装返回值；不得编写业务规则、数据库操作或跨服务编排。
- Service 承载业务校验、事务、流程编排和领域对象转换。
- Repository 层只负责持久化查询和数据映射，不承载业务决策。
- 持久层统一使用 MyBatis。`repository/ai/po/` 中的 PO 与数据库字段一一对应，`repository/ai/mapper/` 中的 `@Mapper` 接口只声明数据访问方法，由 Mapper XML 提供 SQL。PO 与业务模型之间的转换沿用所属业务模块 `cover/` 下的转换器。
- 业务查询和写入 SQL 必须维护在 `backend/src/main/resources/mapper/**/*.xml` 中。初始化、结构或配置变更、数据迁移等独立 SQL 脚本统一放在仓库根目录的 `sql/new/`。生产 Java 代码禁止使用 `JdbcTemplate`、`JdbcClient`、Mapper 注解 SQL 或字符串拼接 SQL。
- 数据库自增主键在 PO 中使用 `Long`，Mapper XML 使用 `useGeneratedKeys="true" keyProperty="id"` 回填。
- Spring Bean 使用 `@RequiredArgsConstructor` 和 `private final` 依赖注入；禁止手写注入构造方法和字段 `@Autowired`。
- 请求边界允许接收字符串，但进入 Service 后必须通过 `CodeEnumUtils` 转为枚举；PO、Record、VO 和 Mapper 参数尽量保持枚举类型，仅在外部协议、日志标签等边界调用 `getCode()`。
- 字符串空值判断统一使用 Apache Commons `StringUtils`，集合和 Map 空值判断统一使用 `CollectionUtils`、`MapUtils`，普通对象空值判断统一使用`ObjectUtils`；禁止重复书写 `value == null || value.isBlank()`、`collection == null || collection.isEmpty()`。
- PO、Record、DTO、VO 之间的大多数同构属性转换统一使用项目现有的 MapStruct Converter。Service 只保留包含业务语义的聚合与派生字段计算。Spring 配置绑定对象、框架对象以及简单值对象创建不强制使用 MapStruct。
  - **禁止用 `BeanUtils.copyProperties` 或手写 `new VO()` + 逐个 setter 做对象转换**，即使只有一两个派生字段也要走 MapStruct。
  - 转换器放在所属模块的 `cover/` 包下，命名为 `XxxConvert`。参考 `business/news/review/cover/NewsItemConvert.java`，使用 `@Mapper` 接口和 `INSTANCE = Mappers.getMapper(...)`，同名字段自动映射。
  - 派生字段（枚举转描述、多字段拼接等）用 `@Mapping(qualifiedByName = ...)` + `@Named` 的 `default` 方法表达，不要把这类逻辑留在 Service 里。
- 数据约束，比如非空、默认值等，需要在 **DDL** 设计时考虑到，在数据库层面就规范好数据质量，避免数据异常
- 可空值的取值、转换、过滤和默认值统一用 `Optional` 链式表达，少用三元表达式，禁止嵌套三元表达式。

### 接口与异常

- 管理端 REST API 成功响应统一使用 `Result<T>`，格式为 `code/ret/msg/data`。
- 业务 Controller 默认直接返回 `Result<T>`。只有实时调用异步上游、流式处理等确有必要的接口可以使用 Spring MVC 支持的异步响应方式。普通数据库接口保持同步。
- 业务失败抛出 `BusinessException`，由 `WebExceptionAdapter` 转换为 `code/msg/errors`。
- Controller 不捕获并吞掉业务异常；未知异常由全局异常处理记录。


## 配置与敏感信息

- 不要提交真实密钥、token、账号密码、内网凭据或生产连接串。
- 修改 `backend/src/main/resources/application.yml` 或 `NEWS_*` 环境变量时，先确认配置是否只适用于目标环境。
- Redis、MySQL、定时拉取和外部 AI 服务等配置应走现有 Spring 配置类和 properties 结构。

## 验证要求

- 文档或注释改动：至少确认文件路径和内容正确。
- Java 代码改动：优先运行 `mvn compile`。
- Mapper/XML/SQL 改动：运行 `mvn compile`，并检查 Mapper 方法、`@Param` 名称、XML namespace 和 SQL 字段是否一致。
- 涉及业务分支、分页、外部 API 回调、异步任务或定时任务：补充或运行相关测试；测试命令使用 `mvn test -DskipTests=false`。
- 如果因为本地缺少 MySQL、Redis 或外部服务导致无法完整验证，需要在交付说明中明确未验证项和原因。

## 变更纪律

- 只改与需求直接相关的后端文件；不要混入前端、脚本或无关格式化。
- 不要删除或重命名现有接口、字段、枚举值、SQL 字段，除非需求明确要求并已检查调用方。
- 修改公共封装、登录态、异常处理、线程池、配置类、Mapper 基类等共享能力时，要扩大回归检查范围。
- 保持与现有命名风格一致：类名 `PascalCase`，方法/变量 `camelCase`，常量 `UPPER_SNAKE_CASE`。

## 前端项目概览（`frontend/`）

改前端代码前必须先读本节及下列前端规则。涉及资讯管理页面的组件和样式时，参考 [资讯中心视觉规范](frontend/design-system/pages/news-admin.md)。

- 项目名为 `news-recommendation-assistant`。
- 前端使用 Vue 3（`<script setup lang="ts">`）、TypeScript、Vite 6、pnpm、Pinia、Vue Router、Element Plus、CSS、SCSS 和 Axios。
- Vite 开发代理通过 `NEWS_BACKEND_URL` 指向后端，默认 `http://127.0.0.1:8091`。生产构建产物随应用 JAR 发布。

### 前端常用命令

在 `frontend/` 目录执行：

| 任务 | 命令 |
| --- | --- |
| 安装依赖 | `pnpm install --frozen-lockfile` |
| 本地开发 | `pnpm dev` |
| 构建 | `pnpm build`（含 `vue-tsc` 类型检查） |
| 仅类型检查 | `pnpm type-check` |

注意：前端目前没有配置测试框架，验证以 `vue-tsc` 类型检查和实际页面为准。

### 前端目录结构

- `src/api/`：按功能划分的接口模块，请求/响应类型就近定义
- `src/axios/request.ts`：Axios 封装，业务代码统一使用其导出的 `get` / `post` / `putRequest` / `deleteRequest`
- `src/components/common/` 存放全局通用组件。资讯管理的共享组件位于 `src/views/newsAdmin/components/`。
- `src/views/newsAdmin/` 下按审核、资讯库、渠道和设置分模块，可复用的业务逻辑以 `use*.ts` 就近组织。
- `src/stores/`：Pinia 状态模块
- `src/views/` 存放路由页面入口，路由在 `src/main.ts` 注册。
- 通用工具函数按用途放在现有模块中。**复用前必须按下文执行 Shortlist → Confirm → Verdict 流程**，新增通用工具导出后同步更新工具索引。
- `src/icons/svg/`：SVG 图标（`vite-plugin-svg-icons` 管理）
- `src/styles/design-system/` 存放全局设计变量和样式。

### 前端工具复用流程

1. **Shortlist**。先搜索 `frontend/src/`，从工具索引、同类模块和现有调用中列出候选。
2. **Confirm**。阅读候选实现及调用方，核对输入、输出和适用范围。
3. **Verdict**。符合需求时复用。没有合适的实现时，在对应模块按现有写法新增。

| 用途 | 工具入口 |
| --- | --- |
| HTTP 请求 | `src/axios/request.ts` |
| 资讯日期、时间和视频时长格式化 | `src/views/newsAdmin/components/newsFormat.ts` |
| 资讯字段值和表单校验辅助 | `src/views/newsAdmin/components/newsFieldValue.ts` |
| 资讯管理消息和确认弹窗 | `src/views/newsAdmin/newsNotify.ts` |

### 前端编码要点

- 组件用 `PascalCase`，变量/函数用 `camelCase`；格式遵循 `.prettierrc.json`（2 空格、无分号、单引号、120 列）
- 严格 TypeScript，避免 `any`；Props 用 `defineProps<{ ... }>()`
- Vue、Vue Router 和 Pinia 的 API 已配置自动导入（见 `auto-imports.d.ts`），`src/components/common/` 和 Element Plus 组件自动注册（见 `components.d.ts`）。
- 用户可见错误用 Element Plus 的 `ElMessage` 反馈
- 改动 `.vue` / `.ts` 后运行 `pnpm type-check` 验证。不要把无关重构混入需求改动。

### 前端组件与数据流规则

- `el-tabs` 的标签页内容要懒加载（`el-tab-pane` 加 `lazy`），避免一次性渲染所有 tab。
- 动作和其功能封装到同一个组件：例如按钮和它触发的逻辑（弹窗、请求、状态变更）封装成一个组件，父组件只关心布局，不关心逻辑。
- 单个组件尽量不超过 600 行，超过时按上一条的思路拆分。
- 数据变化流程保持清晰：在事件触发处修改主数据，在回调中修改其他关联数据，避免数据在多处交叉修改。
- 弹窗（Dialog/Drawer 等）样式复用全局样式，不要在组件内重新定义一套弹窗样式。
- 页面长期不变的数据（字典、下拉选项、配置等）用 Pinia 缓存，需要跨刷新保留时使用 Pinia 持久化插件。参考 `src/stores/newsAdmin/newsSettingStore.ts` 的按需加载和更新方式，避免每次进页面都重新请求。
- 用**项目组件库**去做原型，保证还原时的一致/准确性
