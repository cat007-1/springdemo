package org.example.springdemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springdemo.common.BizException;
import org.example.springdemo.common.Result;
import org.example.springdemo.entity.SysUser;
import org.example.springdemo.mapper.SysUserMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教学演示接口：专门用来"故意出错"，观察骨架的护栏是否生效。
 *
 * <p>这些接口不承载真实业务，存在的意义是让你<b>亲手把系统弄坏</b>，
 * 从而看清「全局异常处理」和「事务」分别在什么时候起作用。
 *
 * <p>全部需要请求头 {@code token}。
 */
@Tag(name = "教学演示（故意出错）", description = "用于观察全局异常处理与事务回滚的行为")
@SecurityRequirement(name = "tokenAuth")
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final SysUserMapper sysUserMapper;

    public DemoController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 演示一：业务异常如何被全局处理器接住
     * 请求方式：GET
     * 请求地址：/api/demo/boom/{type}
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/demo/boom/biz"      → 404 统一格式
     * curl.exe -H "token: 123456" "http://localhost:8081/api/demo/boom/runtime"  → 500 模糊提示
     * curl.exe -H "token: 123456" "http://localhost:8081/api/demo/boom/npe"      → 500 模糊提示
     * </pre>
     *
     * <p><b>重点看两件事</b>：
     * <ol>
     *   <li>{@code biz} 和 {@code runtime} 返回的<b>消息不同</b>：
     *       业务异常把原因告诉用户，系统异常只说"服务器内部错误"；</li>
     *   <li>响应里<b>没有 Java 堆栈</b>，但控制台日志里<b>有完整堆栈</b>。
     *       这就是"日志里详细、响应里模糊"。</li>
     * </ol>
     */
    @Operation(summary = "故意抛异常",
            description = "type=biz 抛业务异常(404)；type=runtime 抛运行时异常(500)；type=npe 抛空指针(500)")
    @GetMapping("/boom/{type}")
    public Result<String> boom(
            @Parameter(description = "异常类型：biz / runtime / npe", example = "biz")
            @PathVariable String type) {

        switch (type) {
            case "biz":
                // 业务预期内的失败：告诉用户具体原因
                throw BizException.notFound("这是演示用的业务异常：数据不存在");
            case "runtime":
                // 代码 bug：不能把内部细节告诉前端
                throw new IllegalStateException("这是演示用的运行时异常，包含内部细节");
            case "npe":
                String s = null;
                return Result.ok(s.trim());   // 必然空指针
            default:
                return Result.ok("未匹配到类型，正常返回");
        }
    }

    /**
     * 演示二：事务回滚 —— 这是 {@code @Transactional} 存在的唯一理由
     * 请求方式：GET
     * 请求地址：/api/demo/tx/large-name
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/demo/tx/large-name"
     * </pre>
     *
     * <p><b>这个接口干了什么</b>：
     * 先成功插入一条名叫"事务测试"的记录，然后故意插入一个超长名字触发数据库报错。
     * 如果事务生效，第一条插入必须被<b>回滚</b>（表里查不到）；
     * 如果没加 {@code @Transactional}，第一条会留在表里 —— 这就是"改了一半的脏数据"。
     *
     * <p><b>怎么验证回滚生效了？</b>
     * 调用前后各查一次用户总数，比较是否一致：
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/page?current=1&amp;size=1"
     * curl.exe -H "token: 123456" "http://localhost:8081/api/demo/tx/large-name"
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/page?current=1&amp;size=1"
     * </pre>
     * 三次的 {@code total} 都应该是 <b>12</b>（表里没有多出一条"事务测试"）。
     *
     * <p><b>想亲眼看到"没有事务会怎样"？</b>
     * 把本方法上的 {@code @Transactional} 注释掉，重启，再走一遍上面三步。
     * 这时最后一步的 total 会变成 13，而第二步是报错的 —— 这就是脏数据。
     */
    @Operation(summary = "演示事务回滚",
            description = "先插入一条正常数据，再故意插入超长数据触发异常。事务生效则第一条被回滚，total 不变")
    @GetMapping("/tx/large-name")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> transactionDemo() {
        // 第一步：正常数据，会插入成功
        SysUser first = new SysUser(null, "事务测试", "13900000000", 30, 1L);
        sysUserMapper.insert(first);

        // 第二步：故意让它失败 —— user_name 列只有 VARCHAR(50)，这里给 100 个字符
        SysUser boom = new SysUser(null, "超长名字".repeat(50), "13900000001", 30, 1L);
        sysUserMapper.insert(boom);

        // 走不到这里：上面会抛异常，事务随之回滚
        return Result.ok("如果看到这句话，说明没有回滚，事务配置有问题");
    }

    /**
     * 演示三：对照实验 —— 同样的代码，但没有事务
     * 请求方式：GET
     * 请求地址：/api/demo/no-tx/large-name
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/demo/no-tx/large-name"
     * </pre>
     *
     * <p>和上面的方法<b>代码完全一样，只少了 {@code @Transactional}</b>。
     * 调用后去查用户列表，会发现多了一条"事务测试"，而接口本身是报错的 ——
     * 数据改了一半，这就是脏数据。
     *
     * <p><b>为什么用 GET 而不是 POST？</b> 纯粹为了方便你直接用浏览器/curl 点。
     * 真实项目里这种"会改数据"的操作必须是 POST，因为 GET 可能被浏览器预取或被爬虫触发。
     */
    @Operation(summary = "对照实验：没有事务会怎样",
            description = "与事务演示代码相同但去掉了 @Transactional，调用后会留下脏数据")
    @GetMapping("/no-tx/large-name")
    public Result<String> noTransactionDemo() {
        SysUser first = new SysUser(null, "事务测试", "13900000000", 30, 1L);
        sysUserMapper.insert(first);

        SysUser boom = new SysUser(null, "超长名字".repeat(50), "13900000001", 30, 1L);
        sysUserMapper.insert(boom);

        return Result.ok("不会执行到这里");
    }
}
