# RESTful 服务与 Swagger 配置 —— 完整教学

> ⚠️ **本文的响应示例是「补全骨架之前」的格式，已过期。**
> 项目后来加了统一响应 `Result` 和全局异常处理，所有接口的响应现在都被
> `{"code":..., "message":..., "data":...}` 包了一层。
> 例如本文写的 `{"message":"用户不存在: id=999"}`，现在是
> `{"code":404,"message":"用户不存在: id=999","data":null}`。
>
> **RESTful 的设计原则（名词做路径、方法表动作、状态码语义）没有变，本文主体仍然有效**，
> 只有「响应体长什么样」变了。最新格式与前后对比见
> `项目骨架-各层如何配合与串联.md` 第八节。

> 项目：`D:\StackLab\Springdemo`（Spring Boot 4.1.1 / Java 17 / Tomcat 11 / 端口 8081）
> 本文所有接口与配置均在本机实测通过。

---

## 一、RESTful 是什么：把「动作」交给 HTTP 方法

初学者最容易写成 RPC 风格：

```
GET /getUserList        ❌ 路径里出现动词
POST /addUser           ❌
POST /deleteUserById    ❌
```

RESTful 的核心是**用名词指代资源，用 HTTP 方法表达动作**：

```
GET    /api/users        ✅ 查列表
GET    /api/users/1      ✅ 查单个
POST   /api/users        ✅ 新增
PUT    /api/users/1      ✅ 整体更新
DELETE /api/users/1      ✅ 删除
```

| 动作 | 方法 | 路径 | 成功状态码 |
|---|---|---|---|
| 查列表 | GET | `/api/users` | **200** |
| 查单个 | GET | `/api/users/{id}` | 200 / **404** |
| 新增 | POST | `/api/users` | **201** + `Location` 头 |
| 更新 | PUT | `/api/users/{id}` | 200 / 404 |
| 删除 | DELETE | `/api/users/{id}` | **204**（无响应体）/ 404 |

**状态码不是随便填的**，客户端和网关依赖它做判断：

- 新增成功必须是 **201**，并在 `Location` 头里给出新资源地址，这样客户端不用猜 id；
- 删除成功用 **204**，表示「成功但没有响应体」；
- 资源不存在用 **404**，不要用 200 包一个 `{"code":404}` —— 那样监控和重试机制全都失效。

---

## 二、代码落地

文件：`src/main/java/org/example/springdemo/controller/UserController.java`

### 2.1 类级路径

```java
@RestController
@RequestMapping("/api/users")     // 类级前缀，避免每个方法重复写
public class UserController { }
```

### 2.2 方法级声明

```java
@GetMapping                          // 不带值 = 用类级路径，即 GET /api/users
public ResponseEntity<List<User>> list() { ... }

@GetMapping("/{id}")
public ResponseEntity<?> getById(@PathVariable Long id) { ... }   // 路径变量

@PostMapping
public ResponseEntity<?> create(@RequestBody UserRequest request) { ... }   // JSON body

@PutMapping("/{id}")
public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UserRequest request) { ... }

@DeleteMapping("/{id}")
public ResponseEntity<?> delete(@PathVariable Long id) { ... }
```

### 2.3 用 ConcurrentHashMap 做内存存储

教学项目不接数据库，但**并发安全仍要写对**：

```java
private final Map<Long, User> store = new ConcurrentHashMap<>();   // 不是 HashMap
private final AtomicLong idGenerator = new AtomicLong(0);          // 不是 int i++
```

`HashMap` 在多线程下扩容可能死循环；`int` 自增不是原子操作，并发下会产生重复 id。

### 2.4 请求体与响应体用 record

```java
public record User(Long id, String name, String phone) { }         // 响应：带 id
public record UserRequest(String name, String phone) { }           // 请求：不带 id
```

**区分两个 record 是刻意的**：id 由服务端生成，不能让客户端传进来（否则客户端能伪造/覆盖别人的 id）。

访问器是 `user.id()`，**不是** `getId()` —— record 没有 getter。

### 2.5 关键实现细节

```java
// 201 的正确写法：用 created()，它会自动设置 Location 头
return ResponseEntity.created(URI.create("/api/users/" + id)).body(user);

// 204 的正确写法
return ResponseEntity.noContent().build();

// 404 带消息体
return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("message", "用户不存在: id=" + id));
```

**PUT 的语义陷阱**：资源不存在时要返回 404，**不要顺手创建**。
「不存在就创建」是 POST 或 PATCH 的语义，PUT 是「整体替换」。

---

## 三、配置 Swagger（springdoc-openapi）

### 3.1 ⚠️ 版本是最大的坑

**Spring Boot 4 必须用 springdoc 3.x**：

| 方案 | 支持版本 | 结论 |
|---|---|---|
| springfox（Swagger 2） | Spring Boot 2.x | ❌ 早停更，Boot 4 用不了 |
| springdoc-openapi **2.x** | Spring Boot **3.x** | ❌ 加到 Boot 4 会启动失败 |
| springdoc-openapi **3.x** | Spring Boot **4.x** | ✅ 本项目的选择 |

本项目用 `springdoc-openapi-starter-webmvc-ui:3.1.1`（从 Maven 中央仓库/阿里云镜像实测可下载）。

`pom.xml` 里的依赖（**只需这一个**，它已内含 swagger-ui 网页，不要再单独引 swagger-ui）：

```xml
<!--
    Spring Boot 4 必须用 springdoc 3.x（3.x 起进入 Boot 4 版本线），
    老的 springfox 2.x / springdoc 2.x 只支持 Boot 3，加进来会启动失败。
    该 starter 已内含 swagger-ui 网页，无需再单独引 swagger-ui 依赖。
-->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.1.1</version>
</dependency>
```

> 顺带一个 Boot 4 的连带影响：Boot 4 用的是 **Jackson 3**（包名 `tools.jackson.*`），
> 而 springdoc 内部仍带 Jackson 2（`com.fasterxml.jackson.*`）。两者可共存，但你自己写
> `ObjectMapper` 配置类时要看清包名，别照抄 Boot 3 的文章。

### 3.2 配置类：注解方式，不写 @Bean

文件：`src/main/java/org/example/springdemo/config/OpenApiConfig.java`

```java
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Springdemo 接口文档",
                version = "v1",
                description = "这是 Springdemo 学习项目的接口文档……"
        )
)
@SecurityScheme(
        name = "tokenAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "token",
        description = "登录后拿到的 token，放在请求头 token 里"
)
public class OpenApiConfig { }
```

`@SecurityScheme` 的作用：在 Swagger UI 右上角生成 **Authorize** 按钮，
测试需要登录的接口时不用手动敲请求头。

控制器上用 `@Tag` / `@Operation` / `@ApiResponse` / `@SecurityRequirement` 补充说明：

```java
@Tag(name = "用户管理", description = "RESTful 风格的用户增删改查示例接口")
@SecurityRequirement(name = "tokenAuth")          // 引用上面定义的方案
@RestController
public class UserController {

    @Operation(summary = "新增用户", description = "创建成功返回 201 与新建的资源")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数不合法")
    })
    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserRequest request) { ... }
}
```

### 3.3 ⚠️ 必须放行 Swagger 路径

本项目已有 `LoginInterceptor` 拦截所有 `/**`。**不放行的话，Swagger 页面本身会被拦成 401。**

`src/main/java/org/example/springdemo/config/WebMvcConfig.java`：

```java
registry.addInterceptor(loginInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
                "/api/files/upload",
                "/api/files/download/**",
                "/login",
                "/error",
                // Swagger UI 与其数据源必须放行，否则页面和 JSON 都会被拦成 401
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/static/**", "/css/**", "/js/**", "/images/**"
        );
```

> 注意：放行的是**文档页面**，不是你的业务接口。
> `/api/users/**` 仍然需要 token —— 这是正确的：文档能看，接口照样要鉴权。

### 3.4 application.properties

```properties
# ===== Swagger / OpenAPI（springdoc 3.1.1，Spring Boot 4 专用版本线）=====
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=alpha
springdoc.api-docs.enabled=true
```

生产环境要关掉文档：`springdoc.api-docs.enabled=false` 和 `springdoc.swagger-ui.enabled=false`。

---

## 四、实测结果

### 4.1 RESTful CRUD（全部实测通过）

```powershell
$H = @{ token = "123456" }

# 查列表
Invoke-RestMethod "http://localhost:8081/api/users" -Headers $H

# 新增（注意中文用 UTF-8 字节发送）
$body = '{"name":"王五","phone":"13800000003"}'
Invoke-WebRequest "http://localhost:8081/api/users" -Method Post -Headers $H `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -UseBasicParsing

# 查单个 / 更新 / 删除
Invoke-RestMethod "http://localhost:8081/api/users/1" -Headers $H
Invoke-WebRequest "http://localhost:8081/api/users/1" -Method Put -Headers $H `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes('{"name":"张三改","phone":"13900000000"}')) -UseBasicParsing
Invoke-WebRequest "http://localhost:8081/api/users/2" -Method Delete -Headers $H -UseBasicParsing
```

实测状态码：

| 请求 | 结果 |
|---|---|
| 无 token 访问 `/api/users` | **401** |
| GET `/api/users` | **200** `[{"id":1,...},{"id":2,...}]` |
| POST `/api/users` | **201**，`Location: /api/users/3` |
| GET `/api/users/1` | **200** |
| GET `/api/users/999` | **404** `{"message":"用户不存在: id=999"}` |
| PUT `/api/users/1` | **200** |
| DELETE `/api/users/2` | **204**，响应体长度 0 |
| 再 DELETE `/api/users/2` | **404** |

响应体中文编码也验证过原始字节，`张三改` 是标准 UTF-8：

```
7B 22 69 64 22 3A 31 2C 22 6E 61 6D 65 22 3A 22 E5 BC A0 E4 B8 89 E6 94 B9 22 ...
{"id":1,"name":"张三改","phone":"13900000000"}
```

### 4.2 Swagger

| 资源 | 结果 |
|---|---|
| `/swagger-ui.html` | **200** |
| `/v3/api-docs` | **200**，`openapi: 3.1.0` |
| `/swagger-ui/swagger-ui.css` | **200** `text/css` 185784 bytes |
| `/swagger-ui/swagger-ui-bundle.js` | **200** `text/javascript` 1553809 bytes |
| `/v3/api-docs/swagger-config` | **200** |

文档里自动收录了全部 7 个路径：`/api/users`、`/api/users/{id}`、`/hello`、`/hello/form`、`/hello/json`、`/api/files/upload`、`/api/files/download/{filename}`。

每个操作的中文摘要也正确落进了 JSON（不是乱码）：

```
GET     summary=查询用户列表   tags=用户管理
POST    summary=新增用户       tags=用户管理
GET     summary=查询单个用户
PUT     summary=更新用户
DELETE  summary=删除用户
```

安全方案也生效了：

```json
"securitySchemes": {
  "tokenAuth": { "type": "apiKey", "name": "token", "in": "header" }
}
```

> **控制台中文显示是乱码，但文件/响应是对的**：PowerShell 控制台是 GBK 代码页，
> 显示 UTF-8 中文会花屏。判断中文对不对要看**原始字节**或改用浏览器看 Swagger 页面，
> 不要被控制台吓到。

---

## 五、这次真正卡住的坑：`\u` 在 Java 里是转义前缀

这是本次唯一真正让编译失败、且最反直觉的问题。

我在 Javadoc 里写了一条示例：

```java
* curl.exe ... --data-binary "@$env:TEMP\user.json" "http://localhost:8081/api/users"
```

编译直接失败：

```
错误: 非法的 Unicode 字符
```

**原因**：Java 编译器在**词法分析之前**有一个 Unicode 转义预处理阶段，它会扫描整个源文件里的
「反斜杠 + 字母 u」，并试图把它当成 `\uXXXX` 转义序列。

- `$env:TEMP` 后面的 `\user.json` → 反斜杠紧跟 `u` → 被当成转义序列的开始；
- 后面不是合法的四位十六进制 → 编译期报「非法的 Unicode 字符」；
- **注释和 Javadoc 里也不例外**，因为这一步发生在语法分析之前，编译器还不知道那是注释。

**两个解法**：

1. 路径改用正斜杠（Windows 同样可用）：`$env:TEMP/user.json`
2. 如果确实要写反斜杠，别让 `\` 紧跟 `u`（例如写 `C:\temp\user.json` 就没问题）

更坑的是：我为了**解释**这个坑，又在注释里写了一个字面量的 `\uXXXX`，于是再次编译失败 ——
同一个坑踩了两次。

### 排查手法（可复用）

控制台报错中文乱码时，别猜，直接看字节：

```powershell
# 1) 打印报错行的每个字符码位，找出 \ (U+005C) 后面是不是 u (U+0075)
$text = [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($path))
$lines = $text -split "`n"
$lines[108].ToCharArray() | ForEach-Object { "U+{0:X4}" -f [int]$_ }

# 2) 全项目扫描残留
Get-ChildItem -Recurse -File src -Filter *.java | ForEach-Object {
  $t = [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($_.FullName))
  if ($t -match '\\u') { Write-Host "$($_.Name) 命中" }
}
```

### 附带修掉的一个隐患：源码编码

`pom.xml` 补上了：

```xml
<properties>
    <java.version>17</java.version>
    <!--
        必须显式声明源码是 UTF-8。本机 Java 平台默认编码是 GB2312，
        不声明的话 javac 会拿 GB2312 去解 UTF-8 源码。
    -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>
```

旧代码没暴露这个问题，是因为它的中文只出现在**注释**里 —— 解码错了只是显示乱码，不报错。
一旦中文出现在**字符串字面量**里（比如 `@Operation(summary = "新增用户")`），
GB2312 解码到某些字节序列时会把后面的引号一起吃掉，导致字符串不闭合而编译失败。
显式声明 UTF-8 后，这类问题从根上消失。

---

## 六、快速验证清单

```powershell
# 1) 编译 + 测试
.\mvnw.cmd -q test

# 2) 启动
.\mvnw.cmd spring-boot:run

# 3) 浏览器打开文档
start http://localhost:8081/swagger-ui.html

# 4) 点右上角 Authorize，填入 123456，然后就能直接在页面上试 /api/users 了

# 5) 文档 JSON（可用于生成前端 SDK）
curl.exe -s http://localhost:8081/v3/api-docs
```
