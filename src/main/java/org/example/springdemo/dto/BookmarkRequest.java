package org.example.springdemo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增收藏的请求体 DTO（Data Transfer Object）。
 *
 * <p><b>为什么不直接用 Entity 接收请求？</b>
 * 三个理由，每个都踩过坑：
 * <ol>
 *   <li><b>安全</b>：Entity 里有 {@code id}。用 Entity 接请求，前端就能传 id，
 *       可能覆盖别人的数据（越权）；DTO 里没有 id，前端想传也没地方放。</li>
 *   <li><b>校验规则不同</b>：新增时 id 必须为空（数据库生成），
 *       但查询返回时 id 必须有值。同一个类没法同时表达这两种约束。</li>
 *   <li><b>表结构和接口契约解耦</b>：数据库加一列，不应该自动变成接口参数。</li>
 * </ol>
 *
 * <p><b>DTO 和 VO 的区别</b>（这对概念最容易混）：
 * <ul>
 *   <li><b>DTO</b> = 进来的（请求体），Inbound；</li>
 *   <li><b>VO</b> = 出去的（响应体），Outbound，例如 {@code UserDeptVO}。</li>
 * </ul>
 *
 * <p><b>校验注解怎么生效？</b>
 * 光在字段上写注解<b>不会</b>生效，必须在 Controller 方法参数上加 {@code @Valid}。
 * 两者缺一不可，这是最常见的"校验没生效"原因。
 */
@Schema(description = "新增收藏的请求体")
public class BookmarkRequest {

    /**
     * 标题。
     *
     * <p>{@code @NotBlank} 比 {@code @NotNull} 严格：它会拒绝 null、空字符串、
     * 以及只有空格的字符串（"   " 也算空）。字符串校验一律优先用它。
     */
    @Schema(description = "标题", example = "Spring 官方文档", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题长度不能超过 50")
    private String title;

    /** 链接地址。允许为空，但填了就不能超过 200 字符。 */
    @Schema(description = "链接地址", example = "https://spring.io")
    @Size(max = 200, message = "链接长度不能超过 200")
    private String url;

    public BookmarkRequest() {
    }

    public BookmarkRequest(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
