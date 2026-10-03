package org.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springdemo.entity.Dept;

/**
 * 部门 Mapper。
 *
 * <p>继承 {@code BaseMapper<Dept>} 就直接拥有单表 CRUD 能力（insert / selectById /
 * selectList / updateById / deleteById ...），一行 SQL 都不用写。
 *
 * <p>{@code @Mapper} 不能省：本工程没有在启动类上加 {@code @MapperScan}，
 * 不加这个注解 MyBatis 不会为接口生成实现类，注入时会报找不到 Bean。
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
}
