# SmartClip Bug 记录

## 1. IDEA Debug 启动时剪贴板读取为空

### 发现

在 IDEA 中使用 Debug 启动项目后，`ClipboardMonitorScheduler#pollClipboard` 调用 `clipboardService.readText()` 一直返回 `Optional.empty()`。

进一步在 `SystemClipboardAdapter#readText` 中调试发现，代码执行到：

```java
Toolkit.getDefaultToolkit().getSystemClipboard()
```

时抛出了 `HeadlessException`。

同时通过 PowerShell 执行：

```powershell
Get-Clipboard -Raw
```

可以正常读到剪贴板文本，说明 Windows 系统剪贴板本身有内容，问题不在用户复制行为，也不在数据库入库逻辑。

### 起因

Spring Boot 默认会以 headless 模式启动应用，`java.awt.headless` 为 `true` 时，Java AWT 无法访问系统剪贴板。

SmartClip 使用的是：

```java
java.awt.Toolkit
java.awt.datatransfer.Clipboard
```

这套 API 需要非 headless 环境才能访问桌面剪贴板。因此在 headless 模式下调用 `Toolkit.getDefaultToolkit().getSystemClipboard()` 会抛出 `HeadlessException`。

项目原先在 `application.yml` 中加入了：

```yml
spring:
  main:
    headless: false
```

但在 IDEA Debug 场景中仍然遇到问题。原因是 AWT headless 状态属于 JVM/SpringApplication 启动早期行为，配置文件加载时序可能晚于该状态被设置或读取的时机，因此仅依赖 `application.yml` 不够稳。

### 解决方式

在应用入口中显式关闭 SpringApplication 的 headless 模式：

```java
public static void main(String[] args) {
    SpringApplication application = new SpringApplication(SmartClipApplication.class);
    application.setHeadless(false);
    application.run(args);
}
```

修改位置：

```text
src/main/java/com/smartclip/SmartClipApplication.java
```

这样项目通过 IDEA Debug、`mvn spring-boot:run` 或 `java -jar` 启动时，都会默认以非 headless 模式运行。

### 临时兜底方案

如果仍然出现 headless 问题，可以在 IDEA 的 Run/Debug Configuration 的 VM options 中加入：

```text
-Djava.awt.headless=false
```

正式 jar 启动时也可以使用：

```powershell
java -Djava.awt.headless=false -jar target\smartclip-0.1.0-SNAPSHOT.jar
```

需要注意：如果启动命令、IDEA 配置或环境变量中显式传入了：

```text
-Djava.awt.headless=true
```

则必须删除或改为 `false`。

### 验证

修复后执行：

```powershell
mvn -q test
```

测试通过。

IDEA Debug 启动后可在断点中检查：

```java
java.awt.GraphicsEnvironment.isHeadless()
```

期望返回：

```text
false
```
