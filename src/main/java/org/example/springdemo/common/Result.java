package org.example.springdemo.common;

/**
 * 统一响应结构。
 *
 * <p><b>为什么需要它？</b>
 * 没有包装类时，每个接口的返回格式都是「碰运气」：有的返回裸数组、有的返回 Map、
 * 有的返回字符串。前端要为每个接口写一套解析逻辑。
 * 有了统一结构，前端只需要判断一次 {@code code}：
 *
 * <pre>
 * {
 *   "code": 200,
 *   "message": "成功",
 *   "data": { ... 具体业务数据 ... }
 * }
 * </pre>
 *
 * <p><b>三个字段的分工</b>：
 * <ul>
 *   <li>{@code code} —— 业务状态码，前端据此判断成败，是全套接口的统一契约；</li>
 *   <li>{@code message} —— 给人看的提示，成功时是「成功」，失败时说明原因；</li>
 *   <li>{@code data} —— 真正的业务数据，失败时为 null。</li>
 * </ul>
 *
 * <p><b>注意</b>：泛型 {@code <T>} 不能省。有了它，{@code Result<User>} 里的 data
 * 就是 User 类型，Swagger 也能推导出正确的响应结构；写成 Object 就全丢了。
 *
 * @param <T> 业务数据类型
 */
public class Result<T> {

    /** 业务状态码。成功固定 200，失败与 HTTP 状态码保持一致。 */
    private int code;

    /** 提示信息。 */
    private String message;

    /** 业务数据，失败时为 null。 */
    private T data;

    /** Jackson 反序列化需要无参构造，保留一个。 */
    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功，带数据。例：Result.ok(userList) —— 这是最常用的一个。 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "成功", data);
    }

    /** 成功，不带数据。例：删除成功后 Result.ok()。 */
    public static <T> Result<T> ok() {
        return new Result<>(200, "成功", null);
    }

    /** 失败，自定义状态码与消息。例：Result.fail(404, "用户不存在")。 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 失败，默认 500。 */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
