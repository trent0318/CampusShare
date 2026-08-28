-- =====================================================================
-- CampusShare 建表脚本（sql/schema.sql）
-- 用途：在已建好的 campusshare 数据库上创建 8 张表。
-- 执行：mysql -uroot -p campusshare < sql/schema.sql
-- 说明：先执行本文件建表，再执行 sql/data.sql 灌入测试数据；
--       也可用项目根目录 CampusShare.sql 一键完成建库建表 + 数据。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `user` (
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

CREATE TABLE IF NOT EXISTS `category` (
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

CREATE TABLE IF NOT EXISTS `resource` (
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

CREATE TABLE IF NOT EXISTS `resource_image` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id` BIGINT       NOT NULL COMMENT '所属资源 id（逻辑外键 → resource.id）',
    `url`         VARCHAR(255) NOT NULL COMMENT '图片地址',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    PRIMARY KEY (`id`),
    KEY `idx_resource` (`resource_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资源图片表';

CREATE TABLE IF NOT EXISTS `reservation` (
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

CREATE TABLE IF NOT EXISTS `review` (
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

CREATE TABLE IF NOT EXISTS `operation_log` (
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

CREATE TABLE IF NOT EXISTS `system_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   VARCHAR(50)  NOT NULL COMMENT '配置键，如 max_active_reservations',
    `config_value` VARCHAR(255) DEFAULT NULL COMMENT '配置值，如 3',
    `description`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
    `update_time`  DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表';
