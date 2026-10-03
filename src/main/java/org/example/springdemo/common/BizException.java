package org.example.springdemo.common;

/**
 * 业务异常：由业务代码主动抛出，表示「请求本身是合法的，但不满足业务规则」。
 *
 * <p>例：用户不存在、余额不足、重复收藏。
 *
 * <p><b>为什么不能直接 throw new RuntimeException？</b>
 * 因为全局异常处理器需要区分「业务预期的错误」和「代码 bug」：
 * <ul>
 *   <li>{@link BizException} → 返回 4xx + 给用户看的友好提示；</li>
 *   <li>其他 RuntimeException → 返回 500 + 记日志，因为那是代码出问题了。</li>
 * </ul>
 *
 * <p>用法：{@code throw new BizException(404, "用户不存在: id=" + id);}
 */
public class BizException extends RuntimeException {

    /** 业务状态码，会被写进 Result.code。 */
    private final int code;

    public BizException(int code, String message) {
        // 消息传给父类，这样日志里打印异常时能看到原因
        super(message);
        this.code = code;
    }

    /** 常用简写：404 资源不存在。 */
    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public int getCode() {
        return code;
    }
}
