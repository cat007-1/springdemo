-- ============================================================
-- 练习用表：收藏夹
-- 这个文件是为「你从零写一个接口」练习准备的，独立成文件，
-- 不掺进已有的 schema.sql，方便你对照学习时一眼看出哪些是新东西。
--
-- 已经在 application.properties 里通过逗号挂到
-- spring.sql.init.schema-locations 上了，启动时会自动执行。
-- ============================================================

DROP TABLE IF EXISTS bookmark;

CREATE TABLE bookmark (
    id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '收藏 id',
    title    VARCHAR(50)  NOT NULL                COMMENT '标题',
    url      VARCHAR(200)                         COMMENT '链接地址',
    PRIMARY KEY (id)
);

-- 预置 12 条，和 sys_user 一样故意超过 10 条，方便一眼看出分页效果
INSERT INTO bookmark (id, title, url) VALUES
    (1,  'Spring 官方文档',    'https://spring.io/projects/spring-boot'),
    (2,  'MyBatis-Plus 官网',  'https://baomidou.com'),
    (3,  'Swagger 规范',       'https://swagger.io/specification'),
    (4,  'MDN Web 文档',       'https://developer.mozilla.org'),
    (5,  'GitHub',             'https://github.com'),
    (6,  'Gitee 码云',         'https://gitee.com'),
    (7,  'Maven 中央仓库',     'https://repo1.maven.org'),
    (8,  '阿里云 Maven 镜像',  'https://maven.aliyun.com'),
    (9,  'H2 数据库',          'https://h2database.com'),
    (10, 'Java SE 文档',       'https://docs.oracle.com/javase'),
    (11, 'Stack Overflow',     'https://stackoverflow.com'),
    (12, 'Spring 中文社区',    'https://spring4all.com');
