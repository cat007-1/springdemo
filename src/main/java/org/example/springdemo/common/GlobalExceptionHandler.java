package org.example.springdemo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 —— 整个应用的「统一异常出口」。
 *
 * <p><b>它解决什么问题？</b>
 * 没有它的时候，两种糟糕情况会同时出现：
 * <ol>
 *   <li><b>异常直接泄漏给前端</b>：Spring 默认返回一大坨 Java 异常 JSON
 *       （带 {@code trace} 字段、类名、甚至源码路径）；</li>
 *   <li><b>每个方法都要写 try-catch</b>：Controller 里堆满
 *       {@code try { ... } catch (Exception e) { return ... } }，业务代码被淹没。</li>
 * </ol>
 *
 * <p><b>原理</b>：{@code @RestControllerAdvice} 会给所有 Controller 套一层代理，
 * 任何 Controller 方法抛出的异常都会被这里"截住"，转成统一的 {@link Result} 格式。
 * 所以 Controller 里<b>不需要也不应该</b>写 try-catch —— 直接让异常往上抛即可。
 *
 * <p><b>三种状态码要分清</b>（这是最容易混的地方）：
 * <table border="1">
 *   <tr><th>异常</th><th>含义</th><th>HTTP 状态码</th></tr>
 *   <tr><td>{@code BizException}</td><td>业务预期内的失败，如"用户不存在"</td><td>按异常自带的 code（通常 4xx）</td></tr>
 *   <tr><td>参数校验失败</td><td>请求方传错了参数</td><td>400</td></tr>
 *   <tr><td>其他 Exception</td><td>代码 bug，如空指针、SQL 语法错</td><td>500（并且必须记日志）</td></tr>
 * </table>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：请求合法但不符合业务规则。
     *
     * <p>状态码用异常自己带的（如 404），这样 HTTP 状态码和 {@code Result.code} 保持一致，
     * 前端既可以用 HTTP 状态码判断，也可以用 body 里的 code，两边都不冲突。
     */
    @ExceptionHandler(BizException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> handleBizException(BizException e) {
        // 业务异常是可预期的，用 warn 级别，不需要堆栈
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return org.springframework.http.ResponseEntity.status(status)
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * {@code @Valid} 校验失败（请求体是 JSON 对象时抛这个）。
     *
     * <p>{@code MethodArgumentNotValidException} 里装着<b>所有</b>校验失败的字段，
     * 这里把它们拼成「字段名: 提示」的形式一次返回，而不是只报第一个错，
     * 这样前端能一次性提示用户所有问题。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("@Valid 校验失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 表单/查询参数的绑定校验失败（非 JSON 请求体时抛这个）。
     *
     * <p>和上面的区别：{@code MethodArgumentNotValidException} 针对 {@code @RequestBody}，
     * {@code BindException} 针对表单和 URL 参数。两个都要处理，否则其中一个会漏成 500。
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定校验失败: {}", message);
        return Result.fail(400, message);
    }

    /** 请求体不是合法 JSON，或类型对不上（例如该传数字传了字符串）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(400, "请求体格式错误，请检查 JSON 是否合法");
    }

    /** 缺少必填的 URL 参数，例如要求传 age 但没传。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(400, "缺少必需参数: " + e.getParameterName());
    }

    /** URL 参数类型不匹配，例如 {@code /api/users/abc} 而 id 要求是数字。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(400, "参数类型错误: " + e.getName());
    }

    /**
     * 访问了不存在的路径。
     *
     * <p><b>注意这里有两个长得很像的异常，必须都处理</b>（这是我实际踩到的坑）：
     * <ul>
     *   <li>{@code NoHandlerFoundException} —— 路径没有匹配到任何 Controller 方法。
     *       本次实际触发的就是这个，只处理 {@code NoResourceFoundException} 时它会落到 500 兜底，
     *       于是"接口不存在"被误报成"服务器内部错误"；</li>
     *   <li>{@code NoResourceFoundException} —— 路径匹配到了静态资源处理器，但文件不存在。</li>
     * </ul>
     *
     * <p>教训：写异常处理器时不要靠猜异常类型，要<b>看日志里真实的异常全限定名</b>。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.fail(404, "接口不存在: " + e.getRequestURL());
    }

    /**
     * 静态资源不存在。
     *
     * <p>没有这个处理器时，Spring 会返回默认的错误 JSON（带 trace 字段）。
     * 加上它，404 也能保持统一的响应格式 —— 前端就不用为"出错"写两套解析了。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.fail(404, "资源不存在: " + e.getResourcePath());
    }

    /** HTTP 方法用错，例如用 GET 访问只支持 POST 的接口。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(405, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 兜底：所有没被上面接住的异常。
     *
     * <p><b>两个关键点</b>：
     * <ol>
     *   <li>必须 {@code log.error} 并带上异常对象，把<b>完整堆栈</b>记进日志 ——
     *       这是排 bug 的唯一线索；</li>
     *   <li>返回给前端的消息<b>不能带堆栈细节</b>，也不能带 SQL、类名等内部信息，
     *       否则等于把系统内部结构告诉攻击者。前端只需要知道"服务器开小差了"。</li>
     * </ol>
     * 「日志里详细，响应里模糊」是生产环境的基本纪律。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常（未预期）", e);
        return Result.fail(500, "服务器内部错误，请联系管理员");
    }
}
