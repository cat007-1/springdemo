package org.example.springdemo.vo;

/**
 * 多表关联查询的结果对象（View Object）。
 *
 * <p>它<b>不是</b>数据库表的映射，而是「用户 + 所属部门」拼起来的展示对象。
 * 多表查询一定要有独立的 VO，不要把部门字段硬塞进 SysUser 实体
 * —— 那样实体和表结构就不一致了，后续维护会乱。
 *
 * <p>字段名与 Mapper XML 里的列别名一一对应，靠 map-underscore-to-camel-case 自动填充。
 */
public class UserDeptVO {

    /** 用户 id。 */
    private Long id;

    /** 姓名。 */
    private String userName;

    /** 手机号。 */
    private String phone;

    /** 年龄。 */
    private Integer age;

    /** 所属部门 id（来自 sys_user.dept_id）。 */
    private Long deptId;

    /** 所属部门名称 —— 这一列来自 sys_dept 表，是关联查询的重点。 */
    private String deptName;

    /** 部门所在城市，同样来自 sys_dept 表。 */
    private String city;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "UserDeptVO{id=" + id + ", userName=" + userName + ", phone=" + phone
                + ", age=" + age + ", deptId=" + deptId
                + ", deptName=" + deptName + ", city=" + city + "}";
    }
}
