# SmartClip 阶段性计划文档

## 1. 项目概述

### 1.1 项目名称
SmartClip

### 1.2 项目定位
SmartClip 是一个运行在 Windows 本地的智能剪贴板文本管理工具。

它的目标不是做一个复杂的平台，而是做一个可以在日常开发和办公中高频使用的轻量应用，帮助用户自动收集、识别、整理和检索复制过的文本内容。

### 1.3 目标用户
- Java 后端工程师
- 测试工程师
- 运维工程师
- 经常复制 URL、SQL、日志、命令、代码片段的办公用户

### 1.4 核心价值
解决以下问题：

1. 复制过的文本很快丢失，后续难以回查
2. 常用 SQL、命令、接口地址、日志片段无法沉淀
3. 重复复制相同内容，但没有统计与归类
4. 想做整理，却不想手工维护复杂笔记

---

## 2. 项目核心目标

### 2.1 核心目标
构建一个“开箱即用、零复杂配置、可持续迭代”的本地智能剪贴板工具。

### 2.2 产品原则
1. 先解决高频刚需，再逐步增强智能能力
2. 先做本地单机版，不做复杂分布式设计
3. 默认零配置可运行
4. 数据库必须支持后续字段和表的扩展
5. 任何数据库结构升级都不能要求用户手工执行 SQL

### 2.3 成功标准
当用户安装后，可以连续一周每天使用，并明显减少“刚复制过但找不到”的问题，就说明方向成立。

---

## 3. 非目标范围

当前阶段不做以下内容：

- 多用户系统
- 登录注册
- 云同步
- 多端同步
- OCR 图片识别
- 复杂 AI 自动总结
- 企业级权限系统
- 分布式部署
- 高并发架构
- 非 Windows 平台优先支持

---

## 4. 轻量化部署目标

### 4.1 部署目标
SmartClip 必须做到以下几点：

1. 用户无需安装 MySQL / PostgreSQL
2. 用户无需手工建库建表
3. 用户无需修改复杂配置参数
4. 用户可以在任意 Windows 机器轻量运行
5. 应用升级时数据库结构可自动迁移
6. 数据可以跟随应用目录整体迁移

### 4.2 部署设计原则
采用“本地服务 + 内嵌数据库 + 默认配置 + 自动初始化”的模式。

### 4.3 最终目标
让用户体验接近：

- 解压
- 双击启动
- 自动运行
- 自动存数据
- 自动升级数据库

---

## 5. 技术栈确定

### 5.1 后端技术栈
- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Scheduling
- Spring Validation
- MyBatis-Plus
- SQLite
- Flyway
- Lombok
- Hutool（可选）
- SLF4J + Logback

### 5.2 前端技术栈
- Vue 3
- Vite
- TypeScript
- Element Plus
- Axios
- Pinia

### 5.3 本地运行形态
- Spring Boot 作为本地后端服务
- Vue 打包后作为静态资源嵌入 Spring Boot
- 用户只需启动一个应用即可使用

### 5.4 选型理由
#### Java 17
长期支持版本，适合本地工具和后续打包。

#### Spring Boot
开发效率高，工程结构清晰，适合当前技术栈。

#### MyBatis-Plus
比 JPA 更直观，SQL 控制感更强，适合后续数据结构演进。

#### SQLite
单文件数据库，无需单独安装数据库服务，天然适合轻量部署。

#### Flyway
负责数据库版本迁移，保证后续增加字段或表时自动升级。

#### Vue 3 + Element Plus
足够轻量，能快速做出管理界面。

---

## 6. 系统架构设计

### 6.1 整体架构
SmartClip 采用单机本地架构：

1. 剪贴板监听模块
2. 内容处理模块
3. 类型识别模块
4. 去重与统计模块
5. 数据持久化模块
6. Web 管理界面
7. 设置与配置模块

### 6.2 架构说明

#### 1）剪贴板监听模块
负责定时轮询 Windows 系统剪贴板文本内容。

#### 2）内容处理模块
负责对采集到的文本进行预处理，例如：
- 去空白
- 截断预览
- 长度检查
- 敏感内容识别

#### 3）类型识别模块
基于规则判断文本类型：
- URL
- JSON
- SQL
- 命令
- Java 异常日志
- 文件路径
- 代码片段
- 普通文本

#### 4）去重与统计模块
通过内容哈希判断唯一性，实现：
- 相同内容合并
- copyCount 计数
- firstCopiedAt 和 lastCopiedAt 维护

#### 5）数据持久化模块
使用 SQLite 保存数据，并通过 Flyway 做数据库迁移。

#### 6）Web 管理界面
提供：
- 历史列表
- 搜索
- 筛选
- 详情
- 收藏
- 设置

#### 7）设置与配置模块
允许用户调整少量必要参数，例如：
- 是否开启监听
- 最短保存长度
- 是否保存敏感文本
- 轮询间隔

---

## 7. 轻量部署方案设计

### 7.1 部署形式
第一阶段使用：

- Spring Boot fat jar
- SQLite 本地数据库文件
- `start.bat` 启动脚本

第二阶段可以演进为：

- Windows exe / msi 安装包
- 捆绑 JRE 运行时

### 7.2 应用目录建议

```text
smartclip/
├─ smartclip.jar
├─ start.bat
├─ config/
│  └─ application.yml
├─ data/
│  └─ smartclip.db
├─ logs/
│  └─ smartclip.log
```

### 7.3 目录说明
- `config/`：可选外部配置，没有也能启动
- `data/`：SQLite 数据文件目录
- `logs/`：日志目录
- 启动时自动检查并创建目录

### 7.4 零配置原则
程序必须自带默认配置，例如：

- 默认端口：8086
- 默认数据库路径：`./data/smartclip.db`
- 默认轮询间隔：1000ms
- 默认最短保存长度：3

这意味着绝大多数用户无需修改任何配置。

---

## 8. 数据库设计原则

### 8.1 数据库选型
使用 SQLite。

### 8.2 为什么使用 SQLite
1. 单文件数据库，部署极轻
2. 无需安装数据库服务
3. 非常适合本地单机工具
4. 数据迁移简单，复制数据库文件即可
5. 支持后续结构扩展

### 8.3 后续扩展要求
虽然 SQLite 轻量，但必须从一开始支持以下能力：

- 新增字段
- 新增表
- 新增索引
- 老版本数据库自动升级
- 应用升级后数据库无感迁移

### 8.4 版本迁移方案
使用 Flyway 管理数据库版本。

#### 迁移原则
1. 所有数据库改动必须通过 migration 脚本完成
2. 不直接手工改用户数据库
3. 不修改已发布的旧 migration
4. 每次变更追加新版本脚本

#### 示例
```text
V1__init.sql
V2__add_is_favorite.sql
V3__create_tag_table.sql
V4__add_source_app.sql
```

---

## 9. 核心数据模型设计

### 9.1 ClipItem
表示一条唯一文本内容。

字段建议：

- id
- content
- contentHash
- type
- subType
- title
- previewText
- copyCount
- firstCopiedAt
- lastCopiedAt
- isFavorite
- isIgnored
- sensitivityLevel
- createdAt
- updatedAt

### 9.2 ClipEvent
表示一次复制事件。

字段建议：

- id
- clipItemId
- copiedAt
- rawPreview

### 9.3 AppSetting
保存系统设置。

字段建议：

- id
- settingKey
- settingValue
- valueType
- description
- updatedAt

### 9.4 Tag（后续版本）
字段建议：

- id
- name
- createdAt

### 9.5 ClipItemTag（后续版本）
字段建议：

- clipItemId
- tagId

---

## 10. 核心功能规划

### 10.1 第一原则
从 MVP 开始，只做真正高频、必要、可落地的功能。

### 10.2 核心功能定义

#### 核心功能一：自动采集文本
后台自动检测剪贴板变化并读取文本。

#### 核心功能二：自动识别类型
系统自动识别基础类型。

#### 核心功能三：去重与计数
相同内容不重复新增，改为累计复制次数。

#### 核心功能四：搜索与回查
用户可以通过界面快速搜索找回历史片段。

#### 核心功能五：再次复制
从历史记录中一键复制回剪贴板。

---

## 11. MVP 设计

### 11.1 MVP 定义
MVP = Minimum Viable Product，最小可行产品。

对于 SmartClip，MVP 不是“做一个全功能剪贴板平台”，而是：

> 自动采集文本 + 基础识别 + 去重计数 + 历史查询 + 再次复制

### 11.2 MVP 核心目标
只验证一件事：

> 用户愿不愿意日常持续使用这个工具。

### 11.3 MVP 功能范围

#### 必做功能
1. 定时检测 Windows 剪贴板文本变化
2. 自动保存文本
3. 基础规则识别类型
4. 相同内容合并并累计 `copyCount`
5. 历史列表查询
6. 关键词搜索
7. 查看详情
8. 再次复制内容到剪贴板
9. 支持少量设置项

#### MVP 不做
- AI 识别
- 智能标签推荐
- 自动归组
- 来源应用识别
- 云同步
- 登录系统
- 图片采集
- 快捷键唤起浮窗

### 11.4 MVP 页面设计

#### 页面 1：历史列表页
展示：
- 类型
- 标题或预览
- `copyCount`
- 最后复制时间

操作：
- 搜索
- 查看详情
- 再次复制
- 删除

#### 页面 2：详情页
展示：
- 完整内容
- 类型
- 首次复制时间
- 最后复制时间
- 复制次数

#### 页面 3：设置页
支持：
- 是否启用监听
- 轮询间隔
- 最短保存长度
- 敏感内容策略

---

## 12. 开发版本规划

### V0.1 - MVP 最小可用版本
#### 目标
跑通核心链路。

#### 功能
1. 剪贴板轮询采集
2. 文本保存
3. 基础类型识别
4. 去重与 `copyCount`
5. 历史列表
6. 搜索
7. 详情
8. 再次复制
9. SQLite 持久化
10. Flyway 初始化迁移

#### 验收标准
- 可持续自动记录复制文本
- 常见类型能正确识别
- 相同内容不会无限重复新增
- 能快速找回并再次复制

### V0.2 - 好用版
#### 目标
提升实用性。

#### 功能
1. 收藏功能
2. 类型筛选
3. 最近使用排序
4. 高频片段页
5. 删除与忽略功能
6. 设置页增强

#### 验收标准
- 用户能更快回查高频内容
- 能对历史内容做基本管理

### V0.3 - 结构增强版
#### 目标
为后续智能化打基础。

#### 功能
1. Tag 表与标签能力
2. `subType` 细分
3. 标题自动生成
4. 索引优化
5. 数据迁移脚本持续演进

#### 验收标准
- 数据结构可持续扩展
- 检索效率更高
- 分类更清晰

### V0.4 - 智能增强版
#### 目标
让系统开始主动辅助整理。

#### 功能
1. 智能推荐标签
2. 自动归组常用片段
3. 相似文本聚合
4. 可选 AI 兜底识别

#### 验收标准
- 用户维护成本明显下降
- 查找效率进一步提升

### V0.5 - 桌面体验版
#### 目标
增强桌面应用体验。

#### 功能
1. 系统托盘
2. 快捷键唤起
3. 最近片段悬浮窗
4. 开机自启动
5. exe/msi 安装包

#### 验收标准
- 工具可以像常驻桌面助手一样运行

---

## 13. 后端模块划分

建议包结构：

```text
com.smartclip
├─ SmartClipApplication
├─ common
│  ├─ config
│  ├─ constant
│  ├─ exception
│  ├─ response
│  └─ util
├─ clipboard
│  ├─ listener
│  ├─ service
│  └─ scheduler
├─ clip
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ entity
│  ├─ dto
│  └─ vo
├─ classifier
│  ├─ rule
│  ├─ model
│  └─ service
├─ setting
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  └─ entity
└─ infrastructure
   ├─ persistence
   ├─ migration
   └─ startup
```

---

## 14. 关键后端设计点

### 14.1 剪贴板监听策略
第一版采用定时轮询，不直接接 Windows 原生事件。

原因：
- 实现简单
- Java 纯后端可完成
- 足以支撑 MVP

### 14.2 去重策略
使用 `contentHash` 判断唯一性。

处理逻辑：
1. 读取文本
2. 预处理
3. 计算 hash
4. 查找是否已有 `ClipItem`
5. 有则更新 `copyCount` 和 `lastCopiedAt`
6. 无则新增 `ClipItem`
7. 同时新增 `ClipEvent`

### 14.3 类型识别策略
采用“规则优先”的方式。

首期支持：
- URL
- JSON
- SQL
- COMMAND
- JAVA_EXCEPTION_LOG
- FILE_PATH
- CODE
- TEXT

返回结构建议：
- `type`
- `subType`
- `confidence`

### 14.4 敏感数据策略
必须预留敏感内容处理能力，例如：
- JWT
- Authorization
- Cookie
- Password
- SecretKey

MVP 可先支持：
- 识别后忽略
- 或识别后脱敏保存

---

## 15. 数据库迁移设计

### 15.1 核心原则
从第一版开始就接入 Flyway。

### 15.2 为什么必须现在接入
因为项目虽然轻量，但后续一定会增加：
- 字段
- 表
- 索引
- 配置项

如果没有迁移机制，用户升级时会遇到数据库结构不一致的问题。

### 15.3 迁移示例
```text
V1__init.sql
V2__add_is_favorite_to_clip_item.sql
V3__create_app_setting_table.sql
V4__create_tag_tables.sql
```

### 15.4 迁移规范
1. 已发布的 migration 不允许修改
2. 新改动只能追加新文件
3. migration 文件命名必须清晰表达变更含义
4. 开发、测试、发布环境都走同一套脚本

---

## 16. 配置设计

### 16.1 配置目标
- 默认能跑
- 少量可调
- 支持外部覆盖

### 16.2 建议配置项
- `server.port`
- `smartclip.poll-interval-ms`
- `smartclip.min-save-length`
- `smartclip.ignore-sensitive-content`
- `smartclip.db-path`

### 16.3 配置优先级建议
1. 程序内置默认值
2. 外部 `config/application.yml`
3. 启动参数

---

## 17. 交付与发布策略

### 17.1 第一阶段
交付形式：
- `smartclip.jar`
- `start.bat`
- 默认配置文件
- 自动初始化目录

### 17.2 第二阶段
交付形式：
- Windows 安装包
- 内置运行时
- 自动创建快捷方式

---

## 18. 项目第一阶段开发顺序

### 第 1 步
初始化 Spring Boot 项目，确定依赖与目录结构

### 第 2 步
集成 SQLite 与 Flyway，完成 V1 初始化脚本

### 第 3 步
完成 `ClipItem` / `ClipEvent` / `AppSetting` 数据模型

### 第 4 步
实现剪贴板轮询监听

### 第 5 步
实现文本预处理与去重逻辑

### 第 6 步
实现基础类型识别模块

### 第 7 步
实现历史列表、详情、搜索 API

### 第 8 步
完成 Vue 页面：历史列表页 + 详情页 + 设置页

### 第 9 步
实现“再次复制”功能

### 第 10 步
完成打包、自启动脚本、目录自动初始化

---

## 19. Codex 开发用 MVP 提示词

```md
你现在是我的项目协作开发助手，请帮助我开发一个本地运行的智能剪贴板工具 SmartClip。

# 一、项目背景
SmartClip 是一个运行在 Windows 本地的智能剪贴板文本管理工具。

它的目标是：
1. 自动检测系统剪贴板中的文本变化
2. 自动保存复制过的文本内容
3. 自动识别文本类型
4. 对重复复制的内容做去重和计数
5. 提供一个 Web 页面用于搜索、查看详情和再次复制

这是一个强调“轻量部署、零复杂配置、本地单机运行”的项目。

# 二、当前阶段目标
当前只开发 MVP（最小可行版本），不要扩展到复杂平台能力。

MVP 核心目标：
- 自动采集文本
- 基础类型识别
- 去重统计
- 历史查询
- 再次复制

# 三、技术栈
后端：
- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Scheduling
- Spring Validation
- MyBatis-Plus
- SQLite
- Flyway
- Lombok

前端：
- Vue 3
- Vite
- TypeScript
- Element Plus
- Axios
- Pinia

# 四、部署要求
1. 不依赖 MySQL 或 PostgreSQL
2. 数据库使用 SQLite
3. 数据文件放在本地 data 目录
4. 启动时自动创建目录
5. 使用 Flyway 做数据库版本迁移
6. 默认配置即可运行，不要求用户手工配置复杂参数
7. 前端打包后嵌入后端作为静态资源提供

# 五、核心业务规则
1. 只处理剪贴板中的文本内容
2. 空文本不保存
3. 长度小于 3 的文本默认忽略
4. 相同内容不重复新增，而是累计 copyCount
5. 需要记录 firstCopiedAt 和 lastCopiedAt
6. 需要支持再次复制到剪贴板
7. 需要支持关键词搜索
8. 需要支持查看历史详情
9. 需要基础类型识别：
   - URL
   - JSON
   - SQL
   - COMMAND
   - JAVA_EXCEPTION_LOG
   - FILE_PATH
   - CODE
   - TEXT

# 六、数据库设计要求
请围绕以下实体设计：

## 1. ClipItem
字段建议：
- id
- content
- contentHash
- type
- subType
- title
- previewText
- copyCount
- firstCopiedAt
- lastCopiedAt
- isFavorite
- isIgnored
- sensitivityLevel
- createdAt
- updatedAt

## 2. ClipEvent
字段建议：
- id
- clipItemId
- copiedAt
- rawPreview

## 3. AppSetting
字段建议：
- id
- settingKey
- settingValue
- valueType
- description
- updatedAt

数据库必须支持后续扩展，未来可以增加字段或新增表，所以必须从第一版开始接入 Flyway。

# 七、MVP 页面要求
## 页面 1：历史列表页
- 支持关键词搜索
- 支持列表展示
- 字段包括：类型、预览、copyCount、最后复制时间
- 支持操作：查看详情、再次复制、删除

## 页面 2：详情页
- 展示完整内容
- 展示类型
- 展示首次复制时间、最后复制时间、复制次数

## 页面 3：设置页
- 是否开启监听
- 轮询间隔
- 最短保存长度
- 是否忽略敏感内容

# 八、当前任务
先不要一次性生成整个项目全部代码，请按顺序完成以下任务：

1. 输出项目整体设计方案
2. 输出后端项目目录结构
3. 输出数据库表设计
4. 输出 Flyway migration 规划
5. 输出核心实体类设计
6. 输出后端 API 设计
7. 输出 MVP 开发任务拆分

# 九、边界要求
当前阶段不要做以下内容：
- 不要做登录注册
- 不要做多用户
- 不要做云同步
- 不要做 AI 功能
- 不要做图片识别
- 不要做复杂自动归类
- 不要做桌面托盘
- 不要做快捷键浮窗
- 不要做来源应用识别

# 十、输出要求
请严格按以下顺序输出：
1. 项目整体架构说明
2. 后端目录结构
3. 数据库表设计
4. Flyway migration 文件规划
5. Java 实体设计
6. REST API 设计
7. MVP 分阶段开发清单

请先输出设计和规划，不要直接生成全部实现代码。
```

---

## 20. 当前结论

SmartClip 的第一阶段应该以“轻量、稳定、可升级”为核心，而不是一开始追求复杂智能化。

### 当前确定方案
- 后端：Java 17 + Spring Boot + MyBatis-Plus
- 前端：Vue 3 + TypeScript + Element Plus
- 数据库：SQLite
- 迁移：Flyway
- 部署：本地 fat jar，后续可打 exe/msi
- MVP：自动采集 + 基础识别 + 去重统计 + 搜索回查 + 再次复制

这套方案能保证：
1. 轻量部署
2. 用户无复杂配置
3. 数据结构可持续扩展
4. 后续版本升级平滑
