package org.example.springdemo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import org.example.springdemo.entity.Dept;

/**
 * 部门服务。
 *
 * <p>继承 {@code IService<Dept>} 后，除了 Mapper 的单条 CRUD，
 * 还多了批量操作（saveBatch / listByIds / removeByIds）等常用方法。
 */
public interface DeptService extends IService<Dept> {

    /**
     * 分页查询部门。
     *
     * @param current 当前页，从 1 开始
     * @param size    每页条数
     */
    IPage<Dept> pageDepts(long current, long size);
}
