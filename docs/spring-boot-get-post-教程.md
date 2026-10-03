# Spring Boot 里发 GET / POST 请求 —— 完整教学

> 项目：`D:\StackLab\Springdemo`（Spring Boot 4.1.1，Java 17，Tomcat 11，端口 **8081**）
> 本文所有命令都在本机实测通过。

---

## 0. 先记三条铁律

| 铁律 | 说明 |
|---|---|
| **HTTP 方法 ↔ 注解必须配对** | `@GetMapping` 只接 GET，`@PostMapping` 只接 POST。用 GET 去访问 POST 接口 → `405 Method Not Allowed` |
| **GET 参数在 URL 上，POST 参数在请求体里** | GET 走 query string（`?a=1&b=2`），POST 走 body |
| **POST 必须声明 Content-Type** | 表单：`application/x-www-form-urlencoded`；JSON：`application/json`。写错 → `415 Unsupported Media Type` |

---

## 1. 先启动项目

```powershell
# 在 D:\StackLab\Springdemo 目录下
.\mvnw.cmd spring-boot:run
```

启动成功的标志（控制台出现这两行）：

```
Tomcat initialized with port 8081 (http)
Tomcat started on port(s): 8081 (http) with context path ''
```

> ⚠️ **易踩坑**：如果报 `Port 8081 was already in use`，说明已经有一个实例在跑了（IDEA 里点过 Run，或上一个终端没关）。
> 查是谁占着：
> ```powershell
> netstat -ano | Select-String ":8081"
> # 最后一列是 PID，然后结束它：
> Stop-Process -Id <PID> -Force
> ```

---

## 2. 本项目的三个接口

代码在 `src/main/java/org/example/springdemo/controller/helloController.java`：

| 方法 | 路径 | 参数位置 | 接收方式 |
|---|---|---|---|
| GET | `/hello` | URL 查询串 | 方法形参自动绑定 |
| POST | `/hello/form` | 请求体（表单） | `@RequestParam` |
| POST | `/hello/json` | 请求体（JSON） | `@RequestBody` + record |

### 2.1 GET —— 参数写在 URL 上

```java
@GetMapping("/hello")
public String hello(String nickname, String phone) {
    System.out.println("GET  ->  nickname=" + nickname + ", phone=" + phone);
    return "Hello" + nickname;
}
```

参数名和方法形参名一致时，Spring 会**自动绑定**，不需要写 `@RequestParam`。
参数名不一致或想设默认值时才写：`@RequestParam(name = "nickname", defaultValue = "游客") String name`。

**怎么发：**

```powershell
# ① 最简单：浏览器地址栏直接敲
#    http://localhost:8081/hello?nickname=Tom&phone=13800000000

# ② curl（Windows 10+ 自带 curl.exe，注意要写 .exe，否则是 PowerShell 的别名）
curl.exe "http://localhost:8081/hello?nickname=Tom&phone=13800000000"

# ③ 带状态码看清楚 HTTP 响应
curl.exe -i "http://localhost:8081/hello?nickname=Tom&phone=13800000000"

# ④ PowerShell 原生写法
Invoke-RestMethod "http://localhost:8081/hello?nickname=Tom&phone=13800000000"
```

**实测结果：**

```
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 8

HelloTom
```

> 结论：`nickname=Tom` 成功注入方法参数。注意**不传参数不会报错**，`nickname` 为 `null`，返回 `Hellonull` —— 这就是为什么真实项目要加参数校验。

### 2.2 POST 之一 —— 表单提交

```java
@PostMapping("/hello/form")
public String helloForm(@RequestParam String nickname, @RequestParam String phone) {
    System.out.println("POST(form)  ->  nickname=" + nickname + ", phone=" + phone);
    return "Hello " + nickname + ", phone=" + phone;
}
```

参数放在请求体里，格式是 `key=value&key=value`（和 URL 查询串长得一样，但在 body 里）。
这里 `@RequestParam` **必须写**，因为 Spring 无法从形参名推断「从 body 的表单里取值」。

**怎么发：**

```powershell
# -d 会自动把 Content-Type 设成 application/x-www-form-urlencoded
curl.exe -i -X POST "http://localhost:8081/hello/form" -d "nickname=Jerry&phone=13900000000"

# 显式指定 Content-Type 也一样
curl.exe -i -X POST "http://localhost:8081/hello/form" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "nickname=Jerry&phone=13900000000"
```

**实测结果：**

```
HTTP/1.1 200

Hello Jerry, phone=13900000000
```

### 2.3 POST 之二 —— JSON 请求体（前后端分离最常用）

```java
@PostMapping("/hello/json")
public String helloJson(@RequestBody User user) {
    System.out.println("POST(json)  ->  " + user);
    return "Hello " + user.nickname() + ", phone=" + user.phone();
}

public record User(String nickname, String phone) { }   // JSON 字段名必须和 record 参数名一致
```

**版本注意**：Spring Boot 4 用 **Jackson 3**（包名 `tools.jackson.*`），Spring Boot 3 是 Jackson 2（`com.fasterxml.jackson.*`）。
平时写业务代码无感，但如果你从网上抄了 Jackson 2 的 `ObjectMapper` 配置类，会编译不过。

`@RequestBody` 的作用：让 Spring 用 Jackson 把 body 里的 JSON **反序列化**成 `User` 对象。
既然是反序列化，Java 类**必须有对应字段的 getter/构造函数**：
- 普通类 → 需要 `getNickname()` / `setNickname()` 或无参构造 + setter
- record → 自动生成，但访问器是 `user.nickname()`，**不是** `getNickname()`（这里我第一次就写错，编译报「找不到符号」）

**怎么发（重点：转义坑）：**

```powershell
# ❌ 新手最常见的失败写法（在 PowerShell 里）
curl.exe -X POST "http://localhost:8081/hello/json" -H "Content-Type: application/json" `
  -d "{\"nickname\":\"Alice\"}"
# 反斜杠会被 PowerShell 原样传给 curl，服务端收到的不是合法 JSON → 400

# ✅ 正确姿势：把 JSON 写进文件，用 --data-binary "@文件" 传
'{"nickname":"Alice","phone":"13700000000"}' | Set-Content -Path "$env:TEMP\user.json" -Encoding utf8 -NoNewline
curl.exe -i -X POST "http://localhost:8081/hello/json" `
  -H "Content-Type: application/json" `
  --data-binary "@$env:TEMP\user.json"
```

**实测结果：**

```
HTTP/1.1 200

Hello Alice, phone=13700000000
```

**PowerShell 原生写法（不用 curl，不会有转义问题，推荐）：**

```powershell
$body = @{ nickname = "Alice"; phone = "13700000000" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/hello/json" `
  -Method Post -ContentType "application/json" -Body $body
```

> `ConvertTo-Json` 会自动处理转义；`Invoke-RestMethod` 会自动解析返回的 JSON。

---

## 3. 不离开 IDEA 也能发请求：`.http` 文件

在 IDEA 里新建 `src/test/resources/api-test.http`（或任意 `.http` 文件），贴进下面内容：

```http
### 变量
@host = http://localhost:8081

### GET 测试
GET {{host}}/hello?nickname=Tom&phone=13800000000

### POST 表单测试
POST {{host}}/hello/form
Content-Type: application/x-www-form-urlencoded

nickname=Jerry&phone=13900000000

### POST JSON 测试
POST {{host}}/hello/json
Content-Type: application/json

{
  "nickname": "Alice",
  "phone": "13700000000"
}
```

每个 `###` 左边会出现绿色 ▶ 箭头，点一下直接发请求并显示响应 —— 调接口最快的方式。

---

## 4. 看懂返回的状态码（排错地图）

| 状态码 | 含义 | 本项目里怎么复现 | 怎么修 |
|---|---|---|---|
| **200** | 成功 | 正常请求 | — |
| **400** | 请求体解析失败 | JSON 格式错/转义错 | 检查 JSON 语法、Content-Type |
| **404** | 路径不存在 | 把 `/hello/form` 拼成 `/hello/forms` | 对照接口路径 |
| **405** | 方法不匹配 | 浏览器直接访问 `/hello/json`（GET 打了 POST 接口） | 换对方法 |
| **415** | Content-Type 不支持 | POST JSON 却不带 `Content-Type: application/json` | 补上请求头 |
| **500** | 服务端异常 | 代码抛异常 | 看控制台堆栈 |

实测 405 的响应：

```
HTTP/1.1 405
{"status":405,"error":"Method Not Allowed","message":"Method 'GET' is not supported.","path":"/hello/json"}
```

---

## 5. 关于热部署（devtools）

`application.properties` 里已经配好：

```properties
spring.devtools.restart.quiet-period=2s
spring.devtools.restart.poll-interval=3s
```

devtools 监测的是**编译输出目录 `target/classes`**，不是 `src`。
所以：
- 在 IDEA 里按 `Ctrl+F9`（Build Project）→ 几秒后自动重启，**不用手动重启**
- 但在终端改了 `.java` 文件后，只跑 `spring-boot:run` 之外什么都没做的话，**必须重新编译**：

```powershell
.\mvnw.cmd -DskipTests compile
```

本次实测就是靠这条命令触发热重启，POST 接口才生效的。
另外 `poll-interval` 必须大于 `quiet-period`（Boot 4 强制要求），否则启动报配置错误。

---

## 6. 一张速查表

```powershell
# GET，参数在 URL
curl.exe -i "http://localhost:8081/hello?nickname=Tom&phone=138"

# POST 表单，-d 自动带 x-www-form-urlencoded
curl.exe -i -X POST "http://localhost:8081/hello/form" -d "nickname=Jerry&phone=139"

# POST JSON，用文件避开转义地狱
curl.exe -i -X POST "http://localhost:8081/hello/json" -H "Content-Type: application/json" --data-binary "@$env:TEMP\user.json"

# POST JSON，PowerShell 原生（推荐）
Invoke-RestMethod -Uri "http://localhost:8081/hello/json" -Method Post -ContentType "application/json" -Body (@{nickname="Alice";phone="137"} | ConvertTo-Json)

# 查端口占用
netstat -ano | Select-String ":8081"
```
