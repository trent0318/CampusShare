-- =====================================================================
-- CampusShare 校园资源共享与预约平台 —— 数据库初始化脚本
-- 目标数据库：MySQL 8.x / 9.x（本机 9.5 兼容 8 语法）
-- 执行方式：mysql -uroot -p --default-character-set=utf8mb4 < CampusShare.sql
-- 作用：建库 + 建 8 张核心表 + 少量测试数据（Phase 1 只需验证查询链路）
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 建库
CREATE DATABASE IF NOT EXISTS campusshare
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE campusshare;

-- 2. 先删旧表（保证脚本可重复执行），再按依赖顺序重建
--    说明：采用「逻辑外键」——列上只声明引用关系并建索引，不加数据库层 FOREIGN KEY 约束。
--    原因：① 与 NewBee Mall / 苍穹外卖等主流项目一致，避免跨表锁与高并发下的死锁风险；
--         ② 引用完整性由 Service 层业务代码保证。
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS resource_image;
DROP TABLE IF EXISTS resource;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS `user`;

-- ---------------------------------------------------------------------
-- 2.1 用户表
-- ---------------------------------------------------------------------
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录名，唯一',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码，绝不存明文',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常 / 0 禁用',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ---------------------------------------------------------------------
-- 2.2 分类表
-- ---------------------------------------------------------------------
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名，如：摄影器材',
    `type`        VARCHAR(20) NOT NULL COMMENT '所属类型：ITEM / VENUE',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序号，小的在前',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 启用 / 0 停用',
    `create_time` DATETIME    NOT NULL COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分类表';

-- ---------------------------------------------------------------------
-- 2.3 资源表（物品 ITEM / 场地 VENUE，用 type 字段区分，不拆表）
-- ---------------------------------------------------------------------
CREATE TABLE `resource` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(100) NOT NULL COMMENT '资源名',
    `category_id`  BIGINT       NOT NULL COMMENT '所属分类 id（逻辑外键 → category.id）',
    `type`         VARCHAR(20)  NOT NULL COMMENT '类型：ITEM 物品 / VENUE 场地',
    `description`  TEXT         COMMENT '描述',
    `image`        VARCHAR(255) DEFAULT NULL COMMENT '封面图 URL',
    `location`     VARCHAR(255) DEFAULT NULL COMMENT '存放位置 / 场地地址',
    `total_count`  INT          NOT NULL DEFAULT 1 COMMENT '可用数量，场地一般填 1',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待审核 / 1 已上架 / 2 已下架 / 3 已驳回',
    `audit_reason` VARCHAR(255) DEFAULT NULL COMMENT '审核驳回原因',
    `create_time`  DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资源表（物品/场地）';

-- ---------------------------------------------------------------------
-- 2.4 资源图片表（一个资源多张展示图）
-- ---------------------------------------------------------------------
CREATE TABLE `resource_image` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id` BIGINT       NOT NULL COMMENT '所属资源 id（逻辑外键 → resource.id）',
    `url`         VARCHAR(255) NOT NULL COMMENT '图片地址',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    PRIMARY KEY (`id`),
    KEY `idx_resource` (`resource_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资源图片表';

-- ---------------------------------------------------------------------
-- 2.5 预约表（核心）
--     索引是并发预约的命脉：
--       uk_user_slot      唯一约束兜底防重复提交
--       idx_resource_time 冲突检测走这个索引
--       idx_user          查询「我的预约」
-- ---------------------------------------------------------------------
CREATE TABLE `reservation` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '预约人 id（逻辑外键 → user.id）',
    `resource_id`   BIGINT       NOT NULL COMMENT '资源 id（逻辑外键 → resource.id）',
    `reserve_date`  DATE         NOT NULL COMMENT '预约日期',
    `start_time`    DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`      DATETIME     NOT NULL COMMENT '结束时间',
    `status`        VARCHAR(20)  NOT NULL COMMENT '状态：PENDING/CONFIRMED/IN_USE/COMPLETED/CANCELLED/EXPIRED',
    `checkin_time`  DATETIME     DEFAULT NULL COMMENT '签到时间',
    `finish_time`   DATETIME     DEFAULT NULL COMMENT '完成时间',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_slot` (`user_id`, `resource_id`, `reserve_date`, `start_time`, `end_time`),
    KEY `idx_resource_time` (`resource_id`, `start_time`, `end_time`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预约表（核心）';

-- ---------------------------------------------------------------------
-- 2.6 评价表
--     reservation_id 加 UNIQUE：数据库层面保证「一次预约只能评一次」
-- ---------------------------------------------------------------------
CREATE TABLE `review` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       NOT NULL COMMENT '评价人 id（逻辑外键 → user.id）',
    `resource_id`    BIGINT       NOT NULL COMMENT '被评资源 id（逻辑外键 → resource.id）',
    `reservation_id` BIGINT       NOT NULL COMMENT '对应预约 id（逻辑外键 → reservation.id），唯一 = 一次预约只能评一次',
    `rating`         TINYINT      NOT NULL COMMENT '评分 1~5',
    `content`        VARCHAR(500) DEFAULT NULL COMMENT '评论内容',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reservation` (`reservation_id`),
    KEY `idx_resource` (`resource_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价表';

-- ---------------------------------------------------------------------
-- 2.7 操作日志表（管理员操作，可追溯）
-- ---------------------------------------------------------------------
CREATE TABLE `operation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       DEFAULT NULL COMMENT '操作者 id',
    `username`       VARCHAR(50)  DEFAULT NULL COMMENT '操作者用户名（冗余，删用户后日志仍可读）',
    `operation_type` VARCHAR(50)  NOT NULL COMMENT '操作类型，如 CREATE_RESOURCE',
    `target_type`    VARCHAR(50)  DEFAULT NULL COMMENT '操作对象类型，如 RESOURCE/USER/RESERVATION',
    `target_id`      BIGINT       DEFAULT NULL COMMENT '操作对象 id',
    `detail`         VARCHAR(500) DEFAULT NULL COMMENT '详情',
    `result`         VARCHAR(20)  DEFAULT NULL COMMENT '结果：SUCCESS / FAIL',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表';

-- ---------------------------------------------------------------------
-- 2.8 系统配置表（预约上限、签到时间窗等可调参数）
-- ---------------------------------------------------------------------
CREATE TABLE `system_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   VARCHAR(50)  NOT NULL COMMENT '配置键，如 max_active_reservations',
    `config_value` VARCHAR(255) DEFAULT NULL COMMENT '配置值，如 3',
    `description`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
    `update_time`  DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表';

-- =====================================================================
-- 3. 测试数据（Phase 1 仅需验证查询链路，数据量保持少量）
-- =====================================================================

-- 用户。password 为 BCrypt 加密后的明文 "123456"
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES
(1, 'admin',    '$2a$10$Me7TwXeY6KCFQuHqRZb.O.y0bv1TveCULgpFV6iUHlFseCmdfljQW', '管理员', NULL, '13800000001', 'ADMIN', 1, NOW(), NOW()),
(2, 'zhangsan', '$2a$10$Me7TwXeY6KCFQuHqRZb.O.y0bv1TveCULgpFV6iUHlFseCmdfljQW', '张三',   NULL, '13800000002', 'USER',  1, NOW(), NOW()),
(3, 'lisi',     '$2a$10$Me7TwXeY6KCFQuHqRZb.O.y0bv1TveCULgpFV6iUHlFseCmdfljQW', '李四',   NULL, '13800000003', 'USER',  1, NOW(), NOW());

-- 分类
INSERT INTO `category` (`id`, `name`, `type`, `sort`, `status`, `create_time`, `update_time`) VALUES
(1, '摄影器材', 'ITEM',  1, 1, NOW(), NOW()),
(2, '运动器材', 'ITEM',  2, 1, NOW(), NOW()),
(3, '自习室',   'VENUE', 1, 1, NOW(), NOW()),
(4, '会议室',   'VENUE', 2, 1, NOW(), NOW());

-- 资源（status = 1 已上架）
INSERT INTO `resource` (`id`, `name`, `category_id`, `type`, `description`, `image`, `location`, `total_count`, `status`, `audit_reason`, `create_time`, `update_time`) VALUES
(1, '佳能单反相机', 1, 'ITEM',  '佳能 EOS 系列单反，配 18-55mm 镜头', NULL, '器材室 A 柜',   5,  1, NULL, NOW(), NOW()),
(2, '羽毛球拍',     2, 'ITEM',  '尤尼克斯羽毛球拍',                    NULL, '体育馆器材室',   10, 1, NULL, NOW(), NOW()),
(3, '图书馆自习室', 3, 'VENUE', '图书馆三楼安静自习区，可预约单间',     NULL, '图书馆 3 楼',    1,  1, NULL, NOW(), NOW());

-- 资源图片
INSERT INTO `resource_image` (`id`, `resource_id`, `url`, `sort`) VALUES
(1, 1, 'https://example.com/img/camera-1.jpg',     1),
(2, 2, 'https://example.com/img/racket-1.jpg',     1),
(3, 3, 'https://example.com/img/study-room-1.jpg', 1);

-- 系统配置（Phase 4 / Phase 6 用到的可调参数）
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `description`, `update_time`) VALUES
(1, 'max_active_reservations', '3',  '用户同时最多可持有的有效预约数',        NOW()),
(2, 'checkin_early_minutes',   '15', '允许提前签到的时间窗（分钟）',           NOW()),
(3, 'checkin_grace_minutes',   '30', '开始后多久未签到则自动过期（分钟）',     NOW());
