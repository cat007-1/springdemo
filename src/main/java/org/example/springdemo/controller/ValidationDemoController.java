package org.example.springdemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.springdemo.common.Result;
import org.example.springdemo.dto.BookmarkRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教学演示接口：参数校验（{@code @Valid} 骨架）。
 *
 * <p><b>为什么单独放一个控制器？</b>
 * 校验演示不需要碰数据库，独立出来最干净。等你按练习任务卡写出真正的
 * {@code BookmarkController} 时，把 {@code @Valid} 和这个 DTO 直接拿过去用即可。
 *
 * <p><b>{@code @Valid} 生效的两个条件（缺一不可，这是最常见的"校验没生效"原因）</b>：
 * <ol>
 *   <li>DTO 的字段上写了约束注解，如 {@code @NotBlank}；</li>
 *   <li><b>Controller 方法参数上加了 {@code @Valid}</b> ← 最容易漏的就是这个。</li>
 * </ol>
 *
 * <p>校验失败时，Spring 抛 {@code MethodArgumentNotValidException}，
 * 被 {@code GlobalExceptionHandler} 接住，转成统一的 400 响应。
 */
@Tag(name = "教学演示（参数校验）", description = "观察 @Valid 如何拦截非法参数")
@SecurityRequirement(name = "tokenAuth")
@RestController
@RequestMapping("/api/demo")
public class ValidationDemoController {

    /**
     * 演示四：参数校验
     * 请求方式：POST
     * 请求地址：/api/demo/validate
     *
     * <pre>
     * 合法请求（200）：
     * '{"title":"我的收藏","url":"https://example.com"}' | Set-Content -Path "$env:TEMP/bm.json" -Encoding utf8 -NoNewline
     * curl.exe -i -X POST -H "token: 123456" -H "Content-Type: application/json" --data-binary "@$env:TEMP/bm.json" "http://localhost:8081/api/demo/validate"
     *
     * 非法请求（400，且一次返回所有错误）：
     * '{"title":"","url":"..."}' | Set-Content -Path "$env:TEMP/bad.json" -Encoding utf8 -NoNewline
     * curl.exe -i -X POST -H "token: 123456" -H "Content-Type: application/json" --data-binary "@$env:TEMP/bad.json" "http://localhost:8081/api/demo/validate"
     * </pre>
     *
     * <p><b>重点观察</b>：方法体里<b>一行校验代码都没有</b>，却没有非法数据能进来。
     * 对比一下：如果不加校验，你得在每个 Controller 里写
     * {@code if (title == null || title.isBlank()) return ...;}，
     * 十个接口就要写十遍。
     */
    @Operation(summary = "演示参数校验", description = "title 为空或超长时返回 400，并一次列出所有错误字段")
    @PostMapping("/validate")
    public Result<BookmarkRequest> validate(@Valid @RequestBody BookmarkRequest request) {
        // 能执行到这里，说明校验已经通过（校验发生在进入方法之前）
        return Result.ok(request);
    }
}
