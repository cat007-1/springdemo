package org.example.springdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springdemo.entity.SysUser;
import org.example.springdemo.vo.UserDeptVO;

import java.util.List;

/**
 * 用户 Mapper。
 *
 * <p>分两类方法：
 * <ol>
 *   <li><b>单表</b>：继承 {@code BaseMapper} 白拿的方法，以及用 Wrapper 拼条件的查询；</li>
 *   <li><b>多表关联</b>：SQL 写在 {@code resources/mapper/SysUserMapper.xml} 里。</li>
 * </ol>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 【多表查询 · 不分页】用户列表连带部门名称（INNER JOIN）。
     *
     * <p>SQL 见 SysUserMapper.xml。返回 VO 而不是实体，因为结果里含 sys_dept 的列。
     */
    List<UserDeptVO> selectUserDeptList();

    /**
     * 【多表查询 · 带条件】按部门 id 过滤，仍走 JOIN。
     *
     * <p>{@code @Param} 指定 XML 里引用的参数名。多参数时<b>必须</b>加，
     * 否则 XML 里只能用 arg0/param1 这种难看且易错的名字。
     *
     * @param deptId 部门 id，传 null 表示不过滤
     */
    List<UserDeptVO> selectUserDeptByDeptId(@Param("deptId") Long deptId);

    /**
     * 【多表查询 + 分页】JOIN 查询也能分页。
     *
     * <p>关键点：第一个参数是 {@code IPage}，MyBatis-Plus 的分页拦截器会识别它，
     * 自动做两件事：① 生成并执行 count 语句填充 total；② 给原 SQL 拼上 LIMIT。
     * 返回的 {@code IPage} 就是入参那个对象（被填好了 records 和 total）。
     *
     * <p>注意 XML 里<b>不要</b>自己写 LIMIT，也不要写分号结尾。
     *
     * @param page   分页参数（页码、每页条数）
     * @param deptId 部门 id，传 null 表示不过滤
     */
    IPage<UserDeptVO> selectUserDeptPage(IPage<UserDeptVO> page, @Param("deptId") Long deptId);
}
