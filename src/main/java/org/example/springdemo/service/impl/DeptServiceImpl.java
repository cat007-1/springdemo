package org.example.springdemo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.springdemo.entity.Dept;
import org.example.springdemo.mapper.DeptMapper;
import org.example.springdemo.service.DeptService;
import org.springframework.stereotype.Service;

/**
 * 部门服务实现。
 *
 * <p>{@code ServiceImpl<DeptMapper, Dept>} 把 Mapper 注入进来并提供实现，
 * 所以本类连 Mapper 字段都不用声明，直接就能用 {@code baseMapper} / {@code getById} 等方法。
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Override
    public IPage<Dept> pageDepts(long current, long size) {
        // Page 是 IPage 的默认实现；new Page<>(页码, 每页条数) 即可。
        // 不传任何条件时 selectPage 会查全表分页，等价于 SELECT * FROM sys_dept LIMIT ?, ?
        return page(new Page<>(current, size));
    }
}
