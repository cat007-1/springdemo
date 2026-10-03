package org.example.springdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.springdemo.entity.SysUser;
import org.example.springdemo.mapper.SysUserMapper;
import org.example.springdemo.service.SysUserService;
import org.example.springdemo.vo.UserDeptVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public IPage<SysUser> pageUsers(long current, long size, String userName) {
        // LambdaQueryWrapper 用「方法引用」写列名，而不是手写字符串 "user_name"。
        // 好处：列名写错在编译期就会报错，重构实体属性时也不会漏改。
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 动态条件：只有传了关键字才拼这段，避免出现 WHERE user_name LIKE '%%'
        if (userName != null && !userName.isBlank()) {
            wrapper.like(SysUser::getUserName, userName);
        }
        wrapper.orderByAsc(SysUser::getId);

        // page(...) 来自 ServiceImpl，内部就是 baseMapper.selectPage。
        // 分页拦截器会在这里插入 COUNT 查询和 LIMIT。
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public List<UserDeptVO> listUserWithDept(Long deptId) {
        // 多表 SQL 在 XML 里，这里只负责转发
        return baseMapper.selectUserDeptByDeptId(deptId);
    }

    @Override
    public IPage<UserDeptVO> pageUserWithDept(long current, long size, Long deptId) {
        // 把 Page 作为第一个参数传给 Mapper 自定义方法，分页就生效了。
        // 返回值和入参是同一个 Page 对象（records / total 被插件填好）。
        Page<UserDeptVO> page = new Page<>(current, size);
        return baseMapper.selectUserDeptPage(page, deptId);
    }
}
