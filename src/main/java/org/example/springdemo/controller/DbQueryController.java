package org.example.springdemo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.springdemo.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springdemo.entity.Dept;
import org.example.springdemo.entity.SysUser;
import org.example.springdemo.service.DeptService;
import org.example.springdemo.service.SysUserService;
import org.example.springdemo.vo.UserDeptVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MyBatis-Plus 教学接口：单表分页、多表关联、多表关联 + 分页。
 *
 * <p>数据来自 H2 内存库（见 resources/db/schema.sql 与 data.sql）。
 * 控制台会打印真实 SQL，可以直接对照观察分页 LIMIT 是怎么被拼出来的。
 *
 * <p>所有接口都需要请求头 {@code token}（由 LoginInterceptor 校验）。
 */
@Tag(name = "数据库查询（MyBatis-Plus）", description = "单表分页、多表关联查询、多表关联分页")
@SecurityRequirement(name = "tokenAuth")
@RestController
@RequestMapping("/api/db")
public class DbQueryController {

    private final SysUserService sysUserService;
    private final DeptService deptService;

    /** 构造器注入：依赖用 final + 构造器，便于测试，也避免循环依赖隐患。 */
    public DbQueryController(SysUserService sysUserService, DeptService deptService) {
        this.sysUserService = sysUserService;
        this.deptService = deptService;
    }

    /**
     * 【单表分页】分页查询用户
     * 请求方式：GET
     * 请求地址：/api/db/users/page?current=1&size=5&userName=张
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/page?current=1&amp;size=5"
     * </pre>
     */
    @Operation(summary = "单表分页查询用户",
            description = "MyBatis-Plus 自带分页，不写 SQL。current 从 1 开始，size 最大 500")
    @ApiResponse(responseCode = "200", description = "分页成功")
    @GetMapping("/users/page")
    public Result<Map<String, Object>> pageUsers(
            @Parameter(description = "当前页，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页条数", example = "5")
            @RequestParam(defaultValue = "5") long size,
            @Parameter(description = "姓名模糊查询，可不传")
            @RequestParam(required = false) String userName) {

        IPage<SysUser> page = sysUserService.pageUsers(current, size, userName);
        return toPageResult(page);
    }

    /**
     * 【多表关联】用户 + 所属部门，不分页
     * 请求方式：GET
     * 请求地址：/api/db/users/with-dept?deptId=1
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/with-dept"
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/with-dept?deptId=1"
     * </pre>
     */
    @Operation(summary = "多表关联查询（不分页）",
            description = "INNER JOIN sys_dept，返回用户及其部门名称和城市；deptId 可选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/users/with-dept")
    public Result<List<UserDeptVO>> listWithDept(
            @Parameter(description = "部门 id，不传则查全部", example = "1")
            @RequestParam(required = false) Long deptId) {

        return Result.ok(sysUserService.listUserWithDept(deptId));
    }

    /**
     * 【多表关联 + 分页】JOIN 的同时分页
     * 请求方式：GET
     * 请求地址：/api/db/users/with-dept/page?current=1&size=5&deptId=1
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/with-dept/page?current=1&amp;size=5"
     * </pre>
     */
    @Operation(summary = "多表关联 + 分页",
            description = "JOIN 查询带分页，分页插件会自动生成 count SQL 并追加 LIMIT")
    @ApiResponse(responseCode = "200", description = "分页成功")
    @GetMapping("/users/with-dept/page")
    public Result<Map<String, Object>> pageWithDept(
            @Parameter(description = "当前页，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页条数", example = "5")
            @RequestParam(defaultValue = "5") long size,
            @Parameter(description = "部门 id，不传则查全部", example = "1")
            @RequestParam(required = false) Long deptId) {

        IPage<UserDeptVO> page = sysUserService.pageUserWithDept(current, size, deptId);
        return toPageResult(page);
    }

    /**
     * 【单表分页】分页查询部门
     * 请求方式：GET
     * 请求地址：/api/db/depts/page?current=1&size=2
     *
     * <pre>
     * curl.exe -H "token: 123456" "http://localhost:8081/api/db/depts/page?current=1&amp;size=2"
     * </pre>
     */
    @Operation(summary = "单表分页查询部门", description = "分页查询 sys_dept")
    @ApiResponse(responseCode = "200", description = "分页成功")
    @GetMapping("/depts/page")
    public Result<Map<String, Object>> pageDepts(
            @Parameter(description = "当前页，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页条数", example = "2")
            @RequestParam(defaultValue = "2") long size) {

        IPage<Dept> page = deptService.pageDepts(current, size);
        return toPageResult(page);
    }

    /**
     * 把 MyBatis-Plus 的 IPage 摊平成前端友好的结构，再套进统一的 {@link Result}。
     *
     * <p>为什么分两层包装？
     * <ul>
     *   <li>内层（records/total/pages）解决「分页信息怎么给前端」；</li>
     *   <li>外层 Result 解决「所有接口格式统一」，这是骨架约定，所有接口都要遵守。</li>
     * </ul>
     *
     * <p>为什么不直接返回 IPage？它的默认 JSON 里有一堆前端用不到的字段
     * （orders、optimizeCountSql、searchCount、maxLimit 等），而且属性顺序不稳定。
     *
     * <p>用 LinkedHashMap 是为了让字段顺序可预测（Jackson 按插入顺序输出）。
     */
    private Result<Map<String, Object>> toPageResult(IPage<?> page) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", page.getRecords());   // 当前页数据
        data.put("total", page.getTotal());       // 总记录数
        data.put("current", page.getCurrent());   // 当前页
        data.put("size", page.getSize());         // 每页条数
        data.put("pages", page.getPages());       // 总页数
        return Result.ok(data);
    }
}
