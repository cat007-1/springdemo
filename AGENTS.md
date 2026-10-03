# AGENTS.md — Springdemo

这是 agent 在本仓库工作时的约定。更具体的规则优先于全局偏好。

## 项目定位

Spring Boot **学习/示范**项目，用来验证各类 HTTP 接口的写法，堆栈刻意保持最小。

当前形态：

| 项 | 值 |
|---|---|
| Spring Boot | 4.1.1（不是 3.x，写法有差异） |
| Java | 17 |
| Web 依赖 | `spring-boot-starter-webmvc`（Boot 4 的新名，**没有** `spring-boot-starter-web`） |
| 测试依赖 | `spring-boot-starter-webmvc-test` |
| 端口 | 8081（`src/main/resources/application.properties`） |
| devtools | 已启用热重载（quiet-period 2s / poll-interval 3s，Boot 4 要求 poll > quiet） |
| 包名 | `org.example.springdemo` |
| 现有代码 | `controller/helloController.java` + `SpringdemoApplication` + 一个 `contextLoads` 测试 |

## 构建与验证

在仓库根目录：

```powershell
.\mvnw.cmd -q test        # 全量测试（默认要求）
.\mvnw.cmd -q -Dtest=类名 test
.\mvnw.cmd -q compile     # 仅编译
```

改完代码必须跑一次编译/测试，不要只改不验。

## 代码风格（照现状，不要另起风格）

- **所有注释和方法说明用中文**，并在注释里给出可直接复制的调用示例（`curl` 或浏览器 URL）——这是本仓库已有的风格。
- 控制器用 `@RestController`，路径用 `@GetMapping`/`@PostMapping` 明确声明。
- 请求体用 `record` 接收（见 `helloController.User`），字段名与 JSON 字段一致，不额外引 Lombok。
- 现有方法用 `System.out.println` 打印入参，属于示范用途；**新写的非示范代码不要照抄这一点**，改用日志。
- 类名保持现状风格（`helloController` 小写开头是既有事实，新类按常规 `PascalCase`，不要为了统一去重命名旧类）。

## 边界

- 这是学习项目：**不要为了"生产级"擅自加**安全框架、数据库、Docker、CI 配置。需要时先问。
- 不要改动 `application.properties` 的端口（8081）——本机其他工具依赖它。
- 不要新增与当前学习主题无关的依赖。
