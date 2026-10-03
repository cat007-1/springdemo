-- ============================================================
-- 演示表结构：部门 1 : N 用户，用于演示多表关联查询
-- H2 内存库，每次启动都会重新执行本脚本（先 DROP 再 CREATE）
-- 语法刻意保持 MySQL 兼容（MODE=MySQL），换 MySQL 时基本可直接用
-- ============================================================

DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;

-- 部门表
CREATE TABLE sys_dept (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门 id',
    dept_name VARCHAR(50)  NOT NULL                COMMENT '部门名称',
    city      VARCHAR(50)                          COMMENT '所在城市',
    PRIMARY KEY (id)
);

-- 用户表，dept_id 是外键，指向 sys_dept.id
CREATE TABLE sys_user (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 id',
    user_name VARCHAR(50)  NOT NULL                COMMENT '姓名',
    phone     VARCHAR(20)                          COMMENT '手机号',
    age       INT                                  COMMENT '年龄',
    dept_id   BIGINT                               COMMENT '所属部门 id',
    PRIMARY KEY (id)
);
