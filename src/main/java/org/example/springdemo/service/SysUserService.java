package org.example.springdemo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import org.example.springdemo.entity.SysUser;
import org.example.springdemo.vo.UserDeptVO;

import java.util.List;

/**
 * 用户服务。
 *
 * <p>分层约定：Controller 只做参数接收与响应，查询逻辑放 Service，
 * SQL 放 Mapper。Controller 里不要直接调 Mapper。
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 【单表分页】用 MyBatis-Plus 自带能力分页，不写 SQL。
     *
     * @param current  当前页，从 1 开始
     * @param size     每页条数
     * @param userName 姓名模糊匹配，可为 null
     */
    IPage<SysUser> pageUsers(long current, long size, String userName);

    /**
     * 【多表查询】用户 + 所属部门，不分页，可传部门 id 过滤。
     *
     * @param deptId 部门 id，null 表示查全部
     */
    List<UserDeptVO> listUserWithDept(Long deptId);

    /**
     * 【多表查询 + 分页】JOIN 查询同时分页。
     *
     * @param current 当前页，从 1 开始
     * @param size    每页条数
     * @param deptId  部门 id，null 表示查全部
     */
    IPage<UserDeptVO> pageUserWithDept(long current, long size, Long deptId);
}
