package org.example.springdemo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 文档配置。
 *
 * <p>用注解方式声明文档元信息，不需要写 @Bean（少一个类、少一份配置）。
 *
 * <p>版本要点：<b>Spring Boot 4 必须配 springdoc 3.x</b>。
 * 老的 springfox（Swagger 2）和 springdoc 2.x 只支持 Boot 3，加到 Boot 4 里会启动失败。
 * 本文档用的是 {@code springdoc-openapi-starter-webmvc-ui:3.1.1}。
 *
 * <p>启动后访问：
 * <ul>
 *   <li>Swagger UI 页面：<a href="http://localhost:8081/swagger-ui.html">http://localhost:8081/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON：<a href="http://localhost:8081/v3/api-docs">http://localhost:8081/v3/api-docs</a></li>
 * </ul>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Springdemo 接口文档",
                version = "v1",
                description = """
                        这是 Springdemo 学习项目的接口文档，由 springdoc-openapi 自动从控制器注解生成。

                        包含两组示例接口：
                        - 用户管理 /api/users：RESTful 风格增删改查
                        - 文件上传 /api/files：上传与下载

                        注意：/api/** 接口需要在页面右上角 Authorize 里填入 token，或请求时带 token 请求头。
                        """
        )
)
@SecurityScheme(
        name = "tokenAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "token",
        description = "登录后拿到的 token，放在请求头 token 里。教学环境任意非空值即可，例如 123456"
)
public class OpenApiConfig {
}
