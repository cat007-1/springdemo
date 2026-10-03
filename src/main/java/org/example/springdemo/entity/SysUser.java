package org.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户实体，对应表 sys_user。
 *
 * <p>说明：原来的示范代码 {@code UserController} 用的是内存 Map + record，
 * 这里换成数据库实体。实体类必须是普通 POJO（无参构造 + getter/setter），
 * 因为 MyBatis 要靠反射创建对象再逐列 set，record 做不到。
 */
@TableName("sys_user")
public class SysUser {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 姓名，对应列 user_name。 */
    private String userName;

    /** 手机号。 */
    private String phone;

    /** 年龄。 */
    private Integer age;

    /** 所属部门 id，对应列 dept_id。 */
    private Long deptId;

    public SysUser() {
    }

    public SysUser(Long id, String userName, String phone, Integer age, Long deptId) {
        this.id = id;
        this.userName = userName;
        this.phone = phone;
        this.age = age;
        this.deptId = deptId;
    }

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

    @Override
    public String toString() {
        return "SysUser{id=" + id + ", userName=" + userName + ", phone=" + phone
                + ", age=" + age + ", deptId=" + deptId + "}";
    }
}
