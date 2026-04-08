# SmartClip

SmartClip 是一个 **本地优先（local-first）的 Windows 剪贴板文本历史管理工具（MVP版本）**。  
项目采用以下技术栈：

- 后端：Spring Boot
- 数据库：SQLite
- 数据迁移：Flyway
- ORM：MyBatis-Plus
- 前端：Vue 3（构建后嵌入后端静态资源）

---

## 环境要求

在运行项目前，请确保已安装以下环境：

- JDK 17
- Maven 3.9 及以上
- Node.js 20+ 和 npm（仅在重新构建前端时需要）

---

## 启动后端服务

在 Windows PowerShell 中执行：

```powershell
$env:JAVA_HOME='E:\TOOL\Env\Java\jdk-17.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
