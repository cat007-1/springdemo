package org.example.springdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 部门实体，对应表 sys_dept。
 *
 * <p>{@code @TableName} 指定表名；不写的话 MyBatis-Plus 会把类名转成下划线（Dept 对应 dept），
 * 和实际表名 sys_dept 对不上，所以要显式声明。
 */
@TableName("sys_dept")
public class Dept {

    /**
     * 主键。{@code IdType.AUTO} 表示用数据库自增，
     * 插入时不带 id，靠 JDBC 的 getGeneratedKeys 回填。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 部门名称。属性名 deptName 会自动映射到列 dept_name（开启驼峰映射时）。 */
    private String deptName;

    /** 所在城市。 */
    private String city;

    public Dept() {
    }

    public Dept(Long id, String deptName, String city) {
        this.id = id;
        this.deptName = deptName;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return "Dept{id=" + id + ", deptName=" + deptName + ", city=" + city + "}";
    }
}
