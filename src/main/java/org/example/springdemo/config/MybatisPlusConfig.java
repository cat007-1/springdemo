package org.example.springdemo.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p><b>分页插件必须手动注册</b>，这是新手最容易漏的一步：
 * 不注册 {@link PaginationInnerInterceptor}，调用 {@code selectPage} 不会报错，
 * 但返回的是<b>全部数据</b>（total 也不对），因为 SQL 里根本没拼 LIMIT。
 * 这种「不报错的错」最难查，所以务必确认本类被扫描到。
 *
 * <p>分页插件的 SQL 改写依赖 JSqlParser，因此 pom 里除了 starter，
 * 还要单独引 {@code mybatis-plus-jsqlparser}。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 总拦截器，分页、乐观锁、防全表更新等插件都挂在这里。
     *
     * <p>多个插件时注意顺序：分页插件要放在最后（官方要求）。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 指定数据库类型，插件据此生成对应方言的 LIMIT 语句
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.H2);
        // 单页最大条数限制，防止前端传 size=999999 把库拖垮
        pagination.setMaxLimit(500L);
        // 请求页码超过总页数时，是否回到第一页（false = 返回空列表）
        pagination.setOverflow(false);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
