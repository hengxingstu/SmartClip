# SmartClip

SmartClip 是一个 **本地优先（local-first）的 Windows 剪贴板文本历史管理工具**。它会在本机后台采集你复制过的文本内容，自动识别类型、去重统计，并提供一个电脑端 Web 界面用于搜索、收藏、查看详情和再次复制。

`Local-first` `Windows` `Spring Boot` `Vue 3` `SQLite` `MIT License`

![SmartClip 桌面端历史页截图](docs/images/img.png)

## 为什么做 SmartClip

日常开发和办公中，我们经常复制 URL、SQL、命令、日志、接口地址、代码片段和临时文本。系统剪贴板通常只保留最后一次复制内容，刚刚复制过的东西很容易丢失。

SmartClip 的目标是做一个轻量、可持续迭代、默认本地运行的剪贴板文本管理工具：不需要云服务，不需要登录，不需要安装 MySQL 或 PostgreSQL，数据保存在本机 SQLite 文件中。

## 功能特性

- 自动采集 Windows 系统剪贴板中的文本内容。
- 过滤空文本和过短文本，减少低价值记录。
- 使用内容哈希去重，相同内容不会重复新增，而是累计复制次数。
- 记录首次复制时间、最后复制时间和复制事件流水。
- 自动识别 URL、JSON、SQL、命令、Java 异常日志、文件路径、代码片段和普通文本。
- 支持历史、收藏、高频、忽略四种列表视图。
- 支持关键词搜索、类型筛选、详情查看、再次复制、忽略和恢复。
- 支持设置监听开关、轮询间隔、最短保存长度和敏感内容忽略策略。
- 使用 SQLite 单文件数据库和 Flyway migration，方便本地迁移和后续升级。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Scheduling
- Spring Validation
- MyBatis-Plus
- SQLite
- Flyway
- Lombok

### 前端

- Vue 3
- TypeScript
- Vite
- Element Plus
- Axios
- Pinia

## 快速开始

### 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+ 和 npm（仅在重新构建前端时需要）

### 启动本地服务

在 Windows PowerShell 中执行：

```powershell
$env:JAVA_HOME='E:\TOOL\Env\Java\jdk-17.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

启动后访问：

```text
http://localhost:8080
```

### 打包运行

```powershell
$env:JAVA_HOME='E:\TOOL\Env\Java\jdk-17.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn package
java -Djava.awt.headless=false -jar target\smartclip-0.1.0-SNAPSHOT.jar
```

`-Djava.awt.headless=false` 用于确保 Java AWT 可以访问 Windows 系统剪贴板。

## 前端开发

前端工程位于 `frontend` 目录。开发时可以使用 Vite 代理访问后端 API：

```bash
cd frontend
npm install
npm run dev
```

构建前端并写入后端静态资源目录：

```bash
cd frontend
npm run build
```

构建产物会输出到：

```text
src/main/resources/static
```

## 测试与构建

后端测试：

```powershell
mvn test
```

完整打包：

```powershell
mvn package
```

前端构建：

```bash
cd frontend
npm run build
```

## 项目结构

```text
SmartClip/
├─ docs/                             # 项目文档与截图资源
├─ frontend/                         # Vue 3 前端工程
├─ src/main/java/com/smartclip/       # Spring Boot 后端源码
├─ src/main/resources/db/migration/   # Flyway 数据库迁移脚本
├─ src/main/resources/static/         # 前端构建后的静态资源
├─ src/test/                          # 后端测试
├─ data/                              # SQLite 数据文件目录
├─ pom.xml                            # Maven 配置
└─ README.md
```

## 核心模块

- 剪贴板监听：定时读取系统剪贴板文本，并避免程序主动复制造成重复采集。
- 内容处理：负责文本预处理、敏感内容识别、类型识别、预览生成和哈希去重。
- 历史管理：提供搜索、详情、再次复制、收藏、忽略、恢复和高频排序。
- 设置管理：提供监听开关、轮询间隔、最短保存长度、敏感内容策略等配置。
- Web 界面：提供桌面端历史列表、视图切换、搜索筛选和设置入口。

## 当前状态

SmartClip 当前已经完成 MVP 主链路，并包含部分增强能力：

- 自动采集文本
- 基础类型识别
- 去重与复制次数统计
- 历史查询与详情查看
- 再次复制到系统剪贴板
- 收藏、高频和忽略视图
- 本地 SQLite 持久化
- Flyway 自动迁移
- Vue 桌面端 Web 管理界面

暂未实现：

- 登录注册和多用户
- 云同步
- AI 分类或总结
- 图片/OCR 识别
- 系统托盘
- 快捷键浮窗
- Windows 安装包

## 路线图

- 增加 API 层集成测试，覆盖列表视图、设置和剪贴板操作接口。
- 优化前端构建体积，按需引入 Element Plus 或做代码分包。
- 增加一键启动脚本，降低本地启动成本。
- 补充真实项目截图和发布说明。
- 后续探索系统托盘、快捷键唤起和 Windows 安装包。

## 贡献

欢迎提交 issue、功能建议和 pull request。

贡献时请注意：

- 数据库结构变更必须新增 Flyway migration，不直接修改已发布的 migration。
- 尽量保持本地优先和轻量部署原则。
- 新增功能应补充必要测试或验证说明。
- 不要在仓库中提交本地敏感数据或个人剪贴板内容。

## 联系方式

- 维护者：HengxingStu
- 邮箱：hengxingstu@gmail.com

## License

SmartClip 基于 MIT License 开源，详见 [LICENSE](LICENSE)。
