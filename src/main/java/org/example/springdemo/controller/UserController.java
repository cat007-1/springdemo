package org.example.springdemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.example.springdemo.common.BizException;
import org.example.springdemo.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RESTful 风格的用户资源接口（教学用，数据存在内存里，重启即清空）。
 *
 * <p>REST 的三条核心约定，本类全部按此实现：
 * <ol>
 *   <li><b>用名词做路径，不用动词</b>：是 {@code /api/users}，不是 {@code /api/getUserList}；</li>
 *   <li><b>用 HTTP 方法表达动作</b>：GET 查、POST 增、PUT 改、DELETE 删；</li>
 *   <li><b>用状态码表达结果</b>：200 成功、201 新建成功、404 资源不存在。</li>
 * </ol>
 *
 * <p><b>已接入统一骨架</b>：
 * <ul>
 *   <li>返回值统一用 {@link Result} 包装，前端只需判断一次 {@code code}；</li>
 *   <li>失败时<b>直接抛 {@link BizException}</b>，不再自己拼错误 Map ——
 *       由 {@code GlobalExceptionHandler} 统一转成响应。这就是全局异常处理器的价值：
 *       Controller 里再也看不到 try-catch 和手工构造的错误体。</li>
 * </ul>
 *
 * <p>所有接口都需要请求头 {@code token}（由 {@link org.example.springdemo.interceptor.LoginInterceptor} 校验）。
 *
 * <p><b>注意</b>：本类只是「骨架改造」的演示，教学项目里删除操作直接用 GET/DELETE + 内存 Map，
 * 真实项目应当走 Service + Mapper + 事务。
 */
@Tag(name = "用户管理", description = "RESTful 风格的用户增删改查示例接口，数据存内存，重启即清空")
@SecurityRequirement(name = "tokenAuth")
@RestController
@RequestMapping("/api/users")
public class UserController {

    /** 内存存储，key 为用户 id。用 ConcurrentHashMap 保证并发安全。 */
    private final Map<Long, User> store = new ConcurrentHashMap<>();

    /** 自增主键。用 AtomicLong 保证并发下 id 不重复。 */
    private final AtomicLong idGenerator = new AtomicLong(0);

    /** 启动时塞两条演示数据，方便打开 Swagger 直接试。 */
    @PostConstruct
    public void initDemoData() {
        create(new UserRequest("张三", "13800000001"));
        create(new UserRequest("李四", "13800000002"));
    }

    /**
     * 查询用户列表
     * 请求方式：GET
     * 请求地址：/api/users
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/users"
     * </pre>
     */
    @Operation(summary = "查询用户列表", description = "返回全部用户，按 id 升序排列，外层统一 Result 包装")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public Result<List<User>> list() {
        List<User> users = new ArrayList<>(store.values());
        users.sort(Comparator.comparingLong(User::id));
        return Result.ok(users);
    }

    /**
     * 根据 id 查询单个用户
     * 请求方式：GET
     * 请求地址：/api/users/{id}
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/users/1"
     * </pre>
     */
    @Operation(summary = "查询单个用户", description = "按 id 查询，不存在时由全局异常处理器返回 404")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{id}")
    public Result<User> getById(
            @Parameter(description = "用户 id", example = "1") @PathVariable Long id) {
        User user = store.get(id);
        if (user == null) {
            // 对比改造前：这里原来是 return notFound(id)，自己拼 Map + 设状态码。
            // 现在只管抛，格式和状态码都由全局处理器保证一致。
            throw BizException.notFound("用户不存在: id=" + id);
        }
        return Result.ok(user);
    }

    /**
     * 新增用户
     * 请求方式：POST
     * 请求地址：/api/users
     * 请求体：{"name":"王五","phone":"13800000003"}
     *
     * <pre>
     * 把 JSON 写进文件再传，避免 PowerShell 引号转义问题。
     * 注意：这里路径用正斜杠 $env:TEMP/user.json。若写成反斜杠，反斜杠紧跟字母 u 时，
     * 会被 Java 当成 Unicode 转义序列处理（Unicode 转义在词法分析之前处理，注释里也不例外），
     * 因为后面不是合法的四位十六进制，编译期就会报「非法的 Unicode 字符」。
     * '{"name":"王五","phone":"13800000003"}' | Set-Content -Path "$env:TEMP/user.json" -Encoding utf8 -NoNewline
     * curl.exe -X POST -H "token: 123456" -H "Content-Type: application/json" --data-binary "@$env:TEMP/user.json" "http://localhost:8081/api/users"
     * </pre>
     */
    @Operation(summary = "新增用户", description = "创建成功返回 200 与新建的资源（含服务端生成的 id）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数不合法（name 为空）")
    })
    @PostMapping
    public Result<User> create(@RequestBody UserRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            // 参数类的错误统一 400，消息会被放进 Result.message
            throw new BizException(400, "name 不能为空");
        }

        long id = idGenerator.incrementAndGet();
        User user = new User(id, request.name(), request.phone());
        store.put(id, user);
        return Result.ok(user);
    }

    /**
     * 整体更新用户
     * 请求方式：PUT
     * 请求地址：/api/users/{id}
     * 请求体：{"name":"王五改","phone":"13900000000"}
     *
     * <pre>
     * curl.exe -X PUT -H "token: 123456" -H "Content-Type: application/json" --data-binary "@$env:TEMP/user.json" "http://localhost:8081/api/users/1"
     * </pre>
     */
    @Operation(summary = "更新用户", description = "按 id 整体覆盖，目标不存在时返回 404")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PutMapping("/{id}")
    public Result<User> update(
            @Parameter(description = "用户 id", example = "1") @PathVariable Long id,
            @RequestBody UserRequest request) {
        // PUT 语义是"整体替换"：资源不存在时应返回 404，而不是顺手创建（那是 POST/PATCH 的事）。
        if (!store.containsKey(id)) {
            throw BizException.notFound("用户不存在: id=" + id);
        }
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BizException(400, "name 不能为空");
        }

        User updated = new User(id, request.name(), request.phone());
        store.put(id, updated);
        return Result.ok(updated);
    }

    /**
     * 删除用户
     * 请求方式：DELETE
     * 请求地址：/api/users/{id}
     *
     * <pre>
     * curl.exe -X DELETE -H "token: 123456" "http://localhost:8081/api/users/1"
     * </pre>
     */
    @Operation(summary = "删除用户", description = "删除成功返回 200，目标不存在时返回 404")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "用户 id", example = "1") @PathVariable Long id) {
        User removed = store.remove(id);
        if (removed == null) {
            throw BizException.notFound("用户不存在: id=" + id);
        }
        // 改造前这里返回 204 无响应体；为了统一格式，现在返回 200 + Result.ok()（data 为 null）。
        return Result.ok();
    }

    /** 对外返回的用户资源，id 由服务端生成。 */
    public record User(Long id, String name, String phone) {
    }

    /** 新增/更新时的请求体。字段名与 JSON 字段一致，不额外引 Lombok。 */
    public record UserRequest(String name, String phone) {
    }
}
