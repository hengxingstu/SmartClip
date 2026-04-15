# SmartClip MVP 版本汇报

## 当前进度

SmartClip 当前已完成 MVP 主干实现，项目可以作为本地单机剪贴板文本管理工具运行。已具备：

- Windows 系统剪贴板文本轮询采集。
- 空文本、短文本过滤。
- SHA-256 内容哈希去重，重复复制时累加 `copyCount`。
- `firstCopiedAt`、`lastCopiedAt` 和复制事件流水记录。
- URL、JSON、SQL、COMMAND、JAVA_EXCEPTION_LOG、FILE_PATH、CODE、TEXT 基础类型识别。
- SQLite 本地数据库，数据文件路径为 `data/smartclip.db`。
- Flyway 从第一版开始管理数据库迁移。
- 历史列表搜索、详情查看、再次复制、软删除、设置读写、健康检查 API。
- Vue 3 + Vite + Element Plus 前端工程，并已打包嵌入后端静态资源。
- 后端单元测试、Spring/Flyway/SQLite 上下文测试、剪贴板内容服务集成测试。

## 模块位置

### 后端入口与配置

- 应用入口：`src/main/java/com/smartclip/SmartClipApplication.java`
- 启动目录初始化：`src/main/java/com/smartclip/config/AppStartupConfig.java`
- SQLite 数据源配置：`src/main/java/com/smartclip/config/DataSourceConfig.java`
- MyBatis-Plus 配置：`src/main/java/com/smartclip/config/MyBatisPlusConfig.java`
- 应用配置：`src/main/resources/application.yml`

### 数据库与迁移

- 数据库目录占位：`data/.gitkeep`
- 数据库文件运行时位置：`data/smartclip.db`
- 初始表结构迁移：`src/main/resources/db/migration/V1__init_smartclip_schema.sql`
- 默认设置迁移：`src/main/resources/db/migration/V2__init_default_settings.sql`

### 剪贴板监听模块

- 系统剪贴板适配器：`src/main/java/com/smartclip/clipboard/SystemClipboardAdapter.java`
- 剪贴板读写服务：`src/main/java/com/smartclip/clipboard/ClipboardService.java`
- 定时轮询调度器：`src/main/java/com/smartclip/clipboard/ClipboardMonitorScheduler.java`

该模块负责读取系统剪贴板文本、写回文本，以及避免程序主动复制后的重复采集。

### 剪贴板内容业务模块

- REST 控制器：`src/main/java/com/smartclip/clip/controller/ClipItemController.java`
- 核心服务：`src/main/java/com/smartclip/clip/service/ClipItemService.java`
- 类型识别：`src/main/java/com/smartclip/clip/service/ClipTypeDetectService.java`
- 预览生成：`src/main/java/com/smartclip/clip/service/ClipPreviewService.java`
- 敏感内容识别：`src/main/java/com/smartclip/clip/service/SensitivityDetectService.java`
- 实体：`src/main/java/com/smartclip/clip/entity/ClipItem.java`
- 事件实体：`src/main/java/com/smartclip/clip/entity/ClipEvent.java`
- Mapper：`src/main/java/com/smartclip/clip/mapper/ClipItemMapper.java`、`src/main/java/com/smartclip/clip/mapper/ClipEventMapper.java`
- DTO：`src/main/java/com/smartclip/clip/dto`
- 枚举：`src/main/java/com/smartclip/clip/enums`

该模块是 MVP 的核心，负责文本保存、去重计数、历史查询、详情、再次复制和软删除。

### 设置模块

- REST 控制器：`src/main/java/com/smartclip/setting/controller/AppSettingController.java`
- 设置服务：`src/main/java/com/smartclip/setting/service/AppSettingService.java`
- 设置实体：`src/main/java/com/smartclip/setting/entity/AppSetting.java`
- Mapper：`src/main/java/com/smartclip/setting/mapper/AppSettingMapper.java`
- DTO：`src/main/java/com/smartclip/setting/dto`

当前支持设置项：

- 是否开启监听：`clipboard.listener.enabled`
- 轮询间隔：`clipboard.poll.interval.ms`
- 最短保存长度：`clipboard.min.text.length`
- 是否忽略敏感内容：`clipboard.ignore.sensitive.enabled`

### 通用基础模块

- 统一响应：`src/main/java/com/smartclip/common/api/ApiResponse.java`
- 分页响应：`src/main/java/com/smartclip/common/api/PageResponse.java`
- 健康检查：`src/main/java/com/smartclip/common/controller/HealthController.java`
- 全局异常处理：`src/main/java/com/smartclip/common/exception/GlobalExceptionHandler.java`
- SQLite 时间类型处理：`src/main/java/com/smartclip/common/mybatis/SqliteLocalDateTimeTypeHandler.java`
- 哈希工具：`src/main/java/com/smartclip/common/util/HashUtils.java`
- 时间工具：`src/main/java/com/smartclip/common/util/TimeUtils.java`

### 前端模块

- 前端工程目录：`frontend`
- Vue 入口：`frontend/src/main.ts`
- 主页面：`frontend/src/App.vue`
- API 封装：`frontend/src/api.ts`
- 样式：`frontend/src/styles.css`
- Vite 配置：`frontend/vite.config.ts`
- 后端静态资源输出目录：`src/main/resources/static`

前端当前包含历史列表、详情弹窗、再次复制、删除、设置页等 MVP 交互。

### 测试模块

- 应用上下文测试：`src/test/java/com/smartclip/SmartClipApplicationTests.java`
- 剪贴板内容服务集成测试：`src/test/java/com/smartclip/clip/ClipItemServiceIntegrationTest.java`
- 类型识别单元测试：`src/test/java/com/smartclip/clip/ClipTypeDetectServiceTest.java`
- 哈希工具单元测试：`src/test/java/com/smartclip/common/util/HashUtilsTest.java`

## 已验证内容

- `mvn test` 已通过。
- `mvn package` 已通过。
- `npm install` 已完成。
- `npm run build` 已通过，前端产物已写入 `src/main/resources/static`。
- `npm audit --omit=dev` 显示生产依赖无漏洞。
- Flyway 已在测试环境中成功迁移 SQLite 到版本 v2。

## 当前边界

当前版本保持 MVP 范围，没有实现以下能力：

- 登录注册、多用户。
- 云同步。
- AI 分类或总结。
- 图片识别。
- 桌面托盘、快捷键浮窗。
- 来源应用识别。
- 复杂自动归类。

## 下一步建议

- 增加 API 层集成测试，覆盖 `/api/clips` 和 `/api/settings` 的请求响应。
- 优化前端构建体积，按需引入 Element Plus 或做代码分包。
- 增加一键启动脚本，显式使用 JDK 17，避免本机默认 Java 1.8 造成启动失败。
- 手动验证真实 Windows 剪贴板采集和再次复制体验。
