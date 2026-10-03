# MyBatis-Plus：单表 CRUD、多表关联、分页查询 —— 完整教学

> 项目：`D:\StackLab\Springdemo`（Spring Boot **4.1.1** / Java 17 / MyBatis-Plus **3.5.17** / H2 内存库 / 端口 8081）
> 本文所有接口、SQL、状态码均在本机实测通过。

---

## 〇、先说结论：Boot 4 下必须用哪几个坐标

| 组件 | 坐标 | 版本 | 说明 |
|---|---|---|---|
| MyBatis-Plus | `com.baomidou:mybatis-plus-spring-boot4-starter` | 3.5.17 | **必须是 boot4 版**，boot3 版在 Boot 4 下不自动装配 |
| 分页插件依赖 | `com.baomidou:mybatis-plus-jsqlparser` | 3.5.17 | 3.5.9 起从主包拆出，**不引就运行时报 ClassNotFoundException** |
| 数据库 | `com.h2database:h2` | 由 Boot 管理 | 内存库，免装 MySQL |

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>3.5.17</version>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>3.5.17</version>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 一、⚠️ 最大的坑：3.5.17 的包名和 Boot 3 教程完全不一样

网上（以及大模型记忆里）绝大多数 MyBatis-Plus 教程是 Boot 3 + MP 3.5.x 前期的写法。
**照着抄，编译直接过不去。** 3.5.17 把几个核心类挪了位置：

| 类 | 老写法（网上教程 / Boot 3） | **本项目正确写法（3.5.17）** |
|---|---|---|
| `IService` | `com.baomidou.mybatisplus.extension.service.IService` | `com.baomidou.mybatisplus.spring.service.IService` |
| `ServiceImpl` | `com.baomidou.mybatisplus.extension.service.impl.ServiceImpl` | `com.baomidou.mybatisplus.spring.service.impl.ServiceImpl` |
| `Page` | `com.baomidou.mybatisplus.extension.plugins.pagination.Page` | **同一个包，没变** |
| `IPage` | `com.baomidou.mybatisplus.core.metadata.IPage` | 没变 |
| `MybatisPlusInterceptor` | `...extension.plugins.MybatisPlusInterceptor` | 没变 |
| `PaginationInnerInterceptor` | `...extension.plugins.inner.PaginationInnerInterceptor` | 没变（但在 jsqlparser 包里） |

我是怎么确认的（这个手法值得记住）：**别猜，直接翻 jar**。

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
Get-ChildItem "$env:USERPROFILE\.m2\repository\com\baomidou" -Recurse -Filter *.jar |
  Where-Object { $_.Name -notmatch 'sources|javadoc' } | ForEach-Object {
    $z = [System.IO.Compression.ZipFile]::OpenRead($_.FullName)
    $hit = $z.Entries | Where-Object { $_.FullName -match 'IService\.class|/Page\.class$' }
    if ($hit) { Write-Host "$($_.Name): $($hit.FullName -join ', ')" }
    $z.Dispose()
  }
```

结果一目了然：`IService` 和 `ServiceImpl` 在 **`mybatis-plus-spring-3.5.17.jar`** 的
`com/baomidou/mybatisplus/spring/service/` 下，根本不在 `extension.service` 里。

> 教训：升级依赖后如果报「找不到符号 / 程序包不存在」，先怀疑**包名变更**，
> 而不是怀疑依赖没下下来。翻 jar 比搜索快且权威。

---

## 二、数据源与建表脚本

### 2.1 application.properties

```properties
# ===== 数据源：H2 内存库 =====
# DB_CLOSE_DELAY=-1 让库在最后一个连接关闭后仍保留，否则连接池一回收数据就没了
# MODE=MySQL 让 H2 兼容 MySQL 语法，方便以后换 MySQL 时不改写 SQL
spring.datasource.url=jdbc:h2:mem:springdemo;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# 每次启动都重建表结构并灌入演示数据
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:db/schema.sql
spring.sql.init.data-locations=classpath:db/data.sql
spring.sql.init.continue-on-error=false

# ===== MyBatis-Plus =====
# 控制台打印真实 SQL，学习时能直接看到分页 LIMIT 是怎么被拼出来的
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
# 数据库列名 user_name -> 实体属性 userName 的自动驼峰映射
mybatis-plus.configuration.map-underscore-to-camel-case=true
# 手写 XML 的位置（多表关联查询）
mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml
# 主键策略改成数据库自增，便于对照学习；默认是雪花算法（19 位长整型）
mybatis-plus.global-config.db-config.id-type=auto
```

### 2.2 表结构：一对多

`src/main/resources/db/schema.sql`

```sql
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;

CREATE TABLE sys_dept (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL,
    city      VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE TABLE sys_user (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(50) NOT NULL,
    phone     VARCHAR(20),
    age       INT,
    dept_id   BIGINT,                 -- 外键，指向 sys_dept.id
    PRIMARY KEY (id)
);
```

`data.sql` 灌了 **3 个部门 + 12 个用户**。为什么是 12 条？因为 **12 > 10**，
分页效果一眼就能看出来（3 页 vs 2 页），数据太少反而看不出问题。

> 脚本先 `DROP TABLE IF EXISTS` 再 `CREATE`，配合 `spring.sql.init.mode=always`，
> 保证重启可重复执行。内存库本身重启即清空，这一步是为了防止中途改表结构导致数据错乱。

---

## 三、分页插件：必须手动注册

`src/main/java/org/example/springdemo/config/MybatisPlusConfig.java`

```java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.H2);
        pagination.setMaxLimit(500L);    // 单页上限，防止 size=999999 拖垮库
        pagination.setOverflow(false);   // 页码越界时返回空列表，而不是回到第一页

        // 多个插件时，分页插件要放最后（官方要求）
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
```

### 最容易踩的坑：不注册插件**不会报错**

不注册 `PaginationInnerInterceptor`，`selectPage` 照样返回 200，
但返回的是**全部数据**，`total` 也不对 —— 因为 SQL 里根本没拼 LIMIT。

这种「静默失效」最难查。**验证方法只有一个：看控制台打印的 SQL 里有没有 LIMIT。**
本项目已经开了 `StdOutImpl`，直接就能看到，见第六节的实测日志。

---

## 四、分层与代码

```
entity/SysUser.java          实体，对应 sys_user 表
entity/Dept.java             实体，对应 sys_dept 表
vo/UserDeptVO.java           多表查询结果对象（不是表映射！）
mapper/SysUserMapper.java    继承 BaseMapper + 3 个手写多表方法
mapper/DeptMapper.java       继承 BaseMapper
resources/mapper/SysUserMapper.xml   多表 SQL
service/SysUserService.java          接口
service/DeptService.java             接口
service/impl/*ServiceImpl.java       实现，继承 ServiceImpl
controller/DbQueryController.java    REST 接口
```

### 4.1 实体：为什么不能用 record

```java
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)      // 数据库自增
    private Long id;
    private String userName;          // 自动映射到列 user_name
    private Integer age;
    private Long deptId;              // 自动映射到列 dept_id
    // 必须有 无参构造 + getter/setter
}
```

**实体必须是普通 POJO**。前面几节的教学代码用 record 接收请求体没问题，
但实体不行：MyBatis 要靠反射「先 new 无参对象，再逐列 set」，
record 没有无参构造也没有 setter，做不到。

`@TableName` 不能省：不写的话 MP 把类名转下划线，`Dept` 会去找 `dept` 表，
而实际表名是 `sys_dept`。

### 4.2 Mapper：继承 BaseMapper 白拿 CRUD

```java
@Mapper
public interface DeptMapper extends BaseMapper<Dept> { }
```

一行 SQL 都不用写，就有 `insert / selectById / selectList / updateById / deleteById`。

`@Mapper` 不能省：本项目没在启动类上加 `@MapperScan`，不写这注解 MyBatis
不会生成实现类，注入时报找不到 Bean。

### 4.3 多表查询：VO + XML

**为什么要单独的 VO？** 多表结果里有 `sys_dept` 的列（部门名、城市），
塞进 `SysUser` 实体的话，实体就和表结构不一致了，后面维护必乱。

`vo/UserDeptVO.java` 字段：`id / userName / phone / age / deptId / deptName / city`

`mapper/SysUserMapper.java`

```java
List<UserDeptVO> selectUserDeptList();                                     // 不分页
List<UserDeptVO> selectUserDeptByDeptId(@Param("deptId") Long deptId);     // 动态条件
IPage<UserDeptVO> selectUserDeptPage(IPage<UserDeptVO> page,
                                     @Param("deptId") Long deptId);        // 分页
```

`resources/mapper/SysUserMapper.xml`

```xml
<mapper namespace="org.example.springdemo.mapper.SysUserMapper">

    <!-- 公共片段：用户表关联部门表 -->
    <sql id="userDeptColumns">
        SELECT u.id AS id, u.user_name AS userName, u.phone AS phone,
               u.age AS age, u.dept_id AS deptId,
               d.dept_name AS deptName, d.city AS city
        FROM sys_user u
        INNER JOIN sys_dept d ON d.id = u.dept_id
    </sql>

    <!-- 多表查询（不分页） -->
    <select id="selectUserDeptList" resultType="org.example.springdemo.vo.UserDeptVO">
        <include refid="userDeptColumns"/>
        ORDER BY u.id
    </select>

    <!-- 多表 + 动态条件 -->
    <select id="selectUserDeptByDeptId" resultType="org.example.springdemo.vo.UserDeptVO">
        <include refid="userDeptColumns"/>
        <where>
            <if test="deptId != null">
                u.dept_id = #{deptId}
            </if>
        </where>
        ORDER BY u.id
    </select>

    <!-- 多表 + 分页：千万不要自己写 LIMIT！ -->
    <select id="selectUserDeptPage" resultType="org.example.springdemo.vo.UserDeptVO">
        <include refid="userDeptColumns"/>
        <where>
            <if test="deptId != null">
                u.dept_id = #{deptId}
            </if>
        </where>
        ORDER BY u.id
    </select>
</mapper>
```

**四个要点**：

1. `namespace` 必须等于 Mapper 接口全限定名，否则绑定不上；
2. 用 `resultType` + **列别名与 VO 属性同名**（两边都写驼峰），
   这样不依赖 `map-underscore-to-camel-case` 开关，最稳；
3. `INNER JOIN` 会丢掉 `dept_id` 在部门表里找不到的用户。要保留就换 `LEFT JOIN`；
4. **分页的 SQL 里绝对不能写 LIMIT，末尾也不要加分号** ——
   分页拦截器会自己追加，手写会和插件拼出来的冲突，页数错乱。

### 4.4 Service：单表分页用 Wrapper，多表分页传 Page

```java
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    // 单表分页：LambdaQueryWrapper 用方法引用写列名，写错编译期就报
    @Override
    public IPage<SysUser> pageUsers(long current, long size, String userName) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (userName != null && !userName.isBlank()) {
            wrapper.like(SysUser::getUserName, userName);   // 动态条件
        }
        wrapper.orderByAsc(SysUser::getId);
        return page(new Page<>(current, size), wrapper);
    }

    // 多表分页：把 Page 作为第一个参数传给 Mapper 方法
    @Override
    public IPage<UserDeptVO> pageUserWithDept(long current, long size, Long deptId) {
        Page<UserDeptVO> page = new Page<>(current, size);
        return baseMapper.selectUserDeptPage(page, deptId);
    }
}
```

**多表分页的原理**：第一个参数是 `IPage`，拦截器识别到它就自动做两件事 ——
① 生成并执行 count 语句填 `total`；② 给原 SQL 拼上 `LIMIT`。
不需要你自己写 count 查询。

`Service<Dept>` → `ServiceImpl<DeptMapper, Dept>`：Mapper 由父类注入，
所以实现类里连 Mapper 字段都不用声明，直接用 `baseMapper` / `page(...)` / `getById(...)`。

### 4.5 Controller：分页结果要摊平

```java
@GetMapping("/users/page")
public Map<String, Object> pageUsers(
        @RequestParam(defaultValue = "1") long current,
        @RequestParam(defaultValue = "5") long size,
        @RequestParam(required = false) String userName) {
    return toPageResult(sysUserService.pageUsers(current, size, userName));
}

private Map<String, Object> toPageResult(IPage<?> page) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("records", page.getRecords());   // 当前页数据
    result.put("total", page.getTotal());       // 总记录数
    result.put("current", page.getCurrent());
    result.put("size", page.getSize());
    result.put("pages", page.getPages());       // 总页数
    return result;
}
```

**为什么不直接返回 `IPage`？** 它序列化出来带一堆前端用不到的内部字段
（`orders`、`optimizeCountSql`、`searchCount`、`maxLimit`…），字段顺序也不稳定。
摊平成 `records / total / pages` 更清晰。用 `LinkedHashMap` 是为了字段顺序可预测。

---

## 五、接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/db/users/page?current=1&size=5&userName=张` | 单表分页 + 模糊查询 |
| GET | `/api/db/users/with-dept?deptId=1` | 多表关联，不分页 |
| GET | `/api/db/users/with-dept/page?current=2&size=5&deptId=1` | 多表关联 + 分页 |
| GET | `/api/db/depts/page?current=1&size=2` | 单表分页（部门） |

全部需要请求头 `token`（由 `LoginInterceptor` 校验），Swagger 里已自动收录。

---

## 六、实测结果

### 6.1 接口返回

```
1) GET /api/db/users/page?current=1&size=5
   total=12 pages=3 current=1 size=5 本页条数=5
   id=1 张三 age=28 deptId=1
   id=2 李四 age=32 deptId=1
   ...（共 5 条）

2) GET /api/db/users/page?current=3&size=5      ← 最后一页
   current=3 本页条数=2
   id=11 陈十三 / id=12 杨十四

3) GET /api/db/users/page?userName=张            ← 分页 + 模糊
   total=1  →  id=1 张三

4) GET /api/db/users/with-dept                   ← 多表关联
   返回 12 条，id=1 张三 -> deptName=研发部 city=杭州

5) GET /api/db/users/with-dept?deptId=1          ← 多表 + 条件
   返回 5 条，部门均为: 研发部

6) GET /api/db/users/with-dept/page?current=2&size=5   ← 多表 + 分页
   total=12 pages=3 current=2 本页条数=5
   id=6 周八 -> 市场部/上海
   id=8 郑十 -> 人事部/北京

7) GET /api/db/depts/page?size=2
   total=3 pages=2
   id=1 研发部 杭州 / id=2 市场部 上海
```

### 6.2 控制台打印的真实 SQL（关键证据）

单表分页（第 1 页）：

```sql
==>  Preparing: SELECT COUNT(*) AS total FROM sys_user
==>  Preparing: SELECT id,user_name,phone,age,dept_id FROM sys_user ORDER BY id ASC LIMIT ?
==> Parameters: 5(Long)
```

单表分页（第 3 页，注意 OFFSET）：

```sql
==>  Preparing: SELECT COUNT(*) AS total FROM sys_user
==>  Preparing: SELECT id,user_name,phone,age,dept_id FROM sys_user ORDER BY id ASC LIMIT ? OFFSET ?
==> Parameters: 5(Long), 10(Long)
```

分页 + 模糊查询：

```sql
==>  Preparing: SELECT COUNT(*) AS total FROM sys_user WHERE (user_name LIKE ?)
==> Parameters: %张%(String)
==>  Preparing: SELECT ... FROM sys_user WHERE (user_name LIKE ?) ORDER BY id ASC LIMIT ?
```

多表关联：

```sql
==>  Preparing: SELECT u.id AS id, u.user_name AS userName, u.phone AS phone, u.age AS age,
                         u.dept_id AS deptId, d.dept_name AS deptName, d.city AS city
                  FROM sys_user u INNER JOIN sys_dept d ON d.id = u.dept_id ORDER BY u.id
```

多表关联 + 分页：

```sql
==>  Preparing: SELECT COUNT(*) AS total FROM sys_user u INNER JOIN sys_dept d ON d.id = u.dept_id
==>  Preparing: SELECT u.id AS id, ... FROM sys_user u INNER JOIN sys_dept d ON d.id = u.dept_id
                ORDER BY u.id LIMIT ? OFFSET ?
==> Parameters: 5(Long), 5(Long)
```

**这三条现象一起说明分页配置是对的**：

1. 每次分页查询前都先执行了一条 `SELECT COUNT(*)` → 插件自动生成了 count；
2. 主查询末尾被追加了 `LIMIT ? OFFSET ?` → 插件改写了 SQL；
3. 多表 JOIN 的 count 语句也正确保留了 JOIN 条件 → 不是简单套 `COUNT(*)` 全表。

> 顺带解释一个疑问：日志里 `<== Columns: id, username, deptid, deptname` 是小写，
> 看着和别名 `userName / deptName` 对不上。这是 **H2 把未加引号的别名统一转成了小写**。
> MyBatis 是按「结果集列标签」匹配属性名的，映射依然成功 —— 接口返回的
> `deptName=研发部` 就是证明。

---

## 七、H2 控制台在 Boot 4 里没了

Boot 3 时代可以配 `spring.h2.console.enabled=true` 然后用浏览器查表。
**Boot 4.1.1 已移除该自动配置**，属性和自动配置类都不存在了，配了也是 404。

验证方式（翻 jar 找元数据）：

```powershell
# spring-boot-autoconfigure-4.1.1.jar 的 spring-configuration-metadata.json 里
# 搜 "spring.h2.console" —— 结果为空，说明属性已被移除
```

所以本项目**没有**配置 H2 控制台，注释里也写明了原因，避免留下一条无效配置误导后来人。
想看数据就直接调 `/api/db/**` 接口，或打开 Swagger 页面点。

### 换成 MySQL

1. `pom.xml`：把 `h2` 依赖换成 `com.mysql:mysql-connector-j`；
2. `application.properties` 改连接串：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/springdemo?serverTimezone=Asia/Shanghai
   spring.datasource.username=root
   spring.datasource.password=你的密码
   ```
3. `MybatisPlusConfig` 里 `DbType.H2` 改成 `DbType.MYSQL`；
4. 建表脚本里的 `AUTO_INCREMENT`、`COMMENT` 本来就是 MySQL 语法，可直接用。

---

## 八、速查与排错

| 现象 | 原因 | 解决 |
|---|---|---|
| 编译「程序包 `...extension.service` 不存在」 | 3.5.17 包名变了 | 改用 `...spring.service` / `...spring.service.impl` |
| 编译「程序包 `...extension.plugins.pagination` 不存在」 | `Page` 在 extension 的 `plugins.pagination` 下 | 确认 import 完整路径 |
| 运行时报 `ClassNotFoundException: PaginationInnerInterceptor` | 没引 `mybatis-plus-jsqlparser` | 补依赖 |
| 分页返回全部数据、total 也不对 | 分页插件没注册 | 检查 `MybatisPlusConfig` 是否被扫描到 |
| 注入 Mapper 报找不到 Bean | 缺 `@Mapper` 且没 `@MapperScan` | 加 `@Mapper` |
| 实体报「没有无参构造」 | 用了 record 当实体 | 换成普通 POJO |
| 多表查询某字段为 null | 列名和 VO 属性名对不上 | 加列别名，两边都写驼峰 |
| 页数错乱 | XML 里自己写了 LIMIT | 删掉，交给插件 |

```powershell
# 编译 + 测试
.\mvnw.cmd -q test

# 启动（控制台会打印真实 SQL）
.\mvnw.cmd spring-boot:run

# 手工验证
curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/page?current=1&size=5"
curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/with-dept?deptId=1"
curl.exe -H "token: 123456" "http://localhost:8081/api/db/users/with-dept/page?current=2&size=5"
```
