-- =====================================================================
-- CampusShare 种子数据（sql/data.sql）
-- 用途：在 schema.sql 建表之后灌入少量测试数据（用户/分类/资源/系统配置）。
-- 执行：mysql -uroot -p campusshare < sql/data.sql
-- 说明：测试账号 admin / zhangsan / lisi，密码均为 123456（BCrypt 加密存储）。
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
