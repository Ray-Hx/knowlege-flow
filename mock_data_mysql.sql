-- Mock Data for KnowledgeFlow MySQL
SET FOREIGN_KEY_CHECKS=0;

-- Users
TRUNCATE TABLE sys_user;
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (1, '张伟_1', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user1@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-07-22 01:53:04', '2025-07-22 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (2, '王芳_2', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user2@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-06-01 01:53:04', '2025-06-01 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (3, '李娜_3', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user3@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-07-09 01:53:04', '2025-07-09 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (4, '刘洋_4', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user4@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-03-07 01:53:04', '2025-03-07 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (5, '陈静_5', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user5@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-03-23 01:53:04', '2025-03-23 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (6, '杨秀英_6', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user6@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2026-01-15 01:53:04', '2026-01-15 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (7, '王敏_7', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user7@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-02-26 01:53:04', '2025-02-26 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (8, '李强_8', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user8@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-10-24 01:53:04', '2025-10-24 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (9, '张丽_9', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user9@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-12-20 01:53:04', '2025-12-20 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (10, '王勇_10', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user10@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-05-11 01:53:04', '2025-05-11 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (11, '刘伟_11', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user11@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-04-28 01:53:04', '2025-04-28 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (12, '张敏_12', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user12@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-08-23 01:53:04', '2025-08-23 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (13, '李静_13', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user13@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-10-15 01:53:04', '2025-10-15 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (14, '王强_14', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user14@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-04-23 01:53:04', '2025-04-23 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (15, '李磊_15', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user15@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-07-12 01:53:04', '2025-07-12 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (16, '王磊_16', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user16@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-04-27 01:53:04', '2025-04-27 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (17, '刘燕_17', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user17@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-03-09 01:53:04', '2025-03-09 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (18, '张军_18', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user18@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-09-09 01:53:04', '2025-09-09 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (19, '李军_19', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user19@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-11-22 01:53:04', '2025-11-22 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (20, '王杰_20', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user20@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-10-10 01:53:04', '2025-10-10 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (21, '张杰_21', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user21@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-06-12 01:53:04', '2025-06-12 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (22, '李杰_22', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user22@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-11-22 01:53:04', '2025-11-22 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (23, '王伟_23', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user23@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-09-23 01:53:04', '2025-09-23 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (24, '李伟_24', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user24@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-05-24 01:53:04', '2025-05-24 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (25, '刘强_25', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user25@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2026-01-01 01:53:04', '2026-01-01 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (26, '张强_26', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user26@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-03-28 01:53:04', '2025-03-28 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (27, '王芳_27', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user27@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-02-04 01:53:04', '2025-02-04 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (28, '李芳_28', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user28@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-06-24 01:53:04', '2025-06-24 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (29, '刘芳_29', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user29@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-05-13 01:53:04', '2025-05-13 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (30, '张建华_30', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user30@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-07-28 01:53:04', '2025-07-28 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (31, '李建华_31', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user31@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-03-30 01:53:04', '2025-03-30 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (32, '王建华_32', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user32@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-12-24 01:53:04', '2025-12-24 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (33, '赵云_33', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user33@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-05-23 01:53:04', '2025-05-23 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (34, '钱学森_34', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user34@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-09-01 01:53:04', '2025-09-01 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (35, '孙悟空_35', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user35@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-02-03 01:53:04', '2025-02-03 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (36, '猪八戒_36', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user36@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-08-13 01:53:04', '2025-08-13 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (37, '沙和尚_37', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user37@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-07-18 01:53:04', '2025-07-18 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (38, '唐僧_38', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user38@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-10-11 01:53:04', '2025-10-11 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (39, '贾宝玉_39', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user39@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-04-24 01:53:04', '2025-04-24 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (40, '林黛玉_40', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user40@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-11-03 01:53:04', '2025-11-03 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (41, '薛宝钗_41', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user41@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2026-01-10 01:53:04', '2026-01-10 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (42, '王熙凤_42', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user42@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-04-21 01:53:04', '2025-04-21 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (43, '史湘云_43', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user43@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-03-29 01:53:04', '2025-03-29 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (44, '秦可卿_44', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user44@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-09-04 01:53:04', '2025-09-04 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (45, '妙玉_45', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user45@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-09-15 01:53:04', '2025-09-15 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (46, '迎春_46', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user46@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-08-02 01:53:04', '2025-08-02 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (47, '探春_47', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user47@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-11-24 01:53:04', '2025-11-24 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (48, '惜春_48', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user48@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-06-02 01:53:04', '2025-06-02 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (49, '李纨_49', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user49@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-12-16 01:53:04', '2025-12-16 01:53:04', 0);
INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES (50, '巧姐_50', '$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa', 'user50@example.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1, '2025-12-21 01:53:04', '2025-12-21 01:53:04', 0);

-- Content Items
TRUNCATE TABLE content_item;
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (1, 17, 23, 'GTD未来趋势', 'https://example.com/article/1', 'Author_76', '这是一个关于GTD未来趋势的摘要说明，内容非常精彩。', '这是GTD未来趋势的详细内容...

这里包含了很多关于GTD的知识。', 1, '2025-06-05 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (2, 85, 6, 'Data Structures实战演练', 'https://example.com/article/2', 'Author_51', '这是一个关于Data Structures实战演练的摘要说明，内容非常精彩。', '这是Data Structures实战演练的详细内容...

这里包含了很多关于Data Structures的知识。', 1, '2025-06-12 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (3, 50, 44, 'MongoDB面试题解析', 'https://example.com/article/3', 'Author_14', '这是一个关于MongoDB面试题解析的摘要说明，内容非常精彩。', '这是MongoDB面试题解析的详细内容...

这里包含了很多关于MongoDB的知识。', 1, '2025-12-18 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (4, 54, 22, '机器学习从入门到精通', 'https://example.com/article/4', 'Author_2', '这是一个关于机器学习从入门到精通的摘要说明，内容非常精彩。', '这是机器学习从入门到精通的详细内容...

这里包含了很多关于机器学习的知识。', 1, '2025-07-24 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (5, 34, 26, '人工智能学习笔记', 'https://example.com/article/5', 'Author_36', '这是一个关于人工智能学习笔记的摘要说明，内容非常精彩。', '这是人工智能学习笔记的详细内容...

这里包含了很多关于人工智能的知识。', 1, '2025-04-13 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (6, 4, 24, 'AWS面试题解析', 'https://example.com/article/6', 'Author_78', '这是一个关于AWS面试题解析的摘要说明，内容非常精彩。', '这是AWS面试题解析的详细内容...

这里包含了很多关于AWS的知识。', 1, '2025-06-05 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (7, 4, 32, 'Spring Boot未来趋势', 'https://example.com/article/7', 'Author_27', '这是一个关于Spring Boot未来趋势的摘要说明，内容非常精彩。', '这是Spring Boot未来趋势的详细内容...

这里包含了很多关于Spring Boot的知识。', 1, '2025-10-17 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (8, 66, 21, 'HTML5学习笔记', 'https://example.com/article/8', 'Author_84', '这是一个关于HTML5学习笔记的摘要说明，内容非常精彩。', '这是HTML5学习笔记的详细内容...

这里包含了很多关于HTML5的知识。', 1, '2025-07-18 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (9, 53, 31, 'Note Taking面试题解析', 'https://example.com/article/9', 'Author_49', '这是一个关于Note Taking面试题解析的摘要说明，内容非常精彩。', '这是Note Taking面试题解析的详细内容...

这里包含了很多关于Note Taking的知识。', 1, '2025-03-01 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (10, 47, 29, '基于Java的系统设计', 'https://example.com/article/10', 'Author_30', '这是一个关于基于Java的系统设计的摘要说明，内容非常精彩。', '这是基于Java的系统设计的详细内容...

这里包含了很多关于Java的知识。', 1, '2025-04-10 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (11, 76, 28, 'Spring Boot从入门到精通', 'https://example.com/article/11', 'Author_20', '这是一个关于Spring Boot从入门到精通的摘要说明，内容非常精彩。', '这是Spring Boot从入门到精通的详细内容...

这里包含了很多关于Spring Boot的知识。', 1, '2025-07-02 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (12, 27, 6, 'Encryption常见问题汇总', 'https://example.com/article/12', 'Author_66', '这是一个关于Encryption常见问题汇总的摘要说明，内容非常精彩。', '这是Encryption常见问题汇总的详细内容...

这里包含了很多关于Encryption的知识。', 1, '2025-08-18 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (13, 41, 39, '大语言模型学习笔记', 'https://example.com/article/13', 'Author_87', '这是一个关于大语言模型学习笔记的摘要说明，内容非常精彩。', '这是大语言模型学习笔记的详细内容...

这里包含了很多关于大语言模型的知识。', 1, '2025-08-08 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (14, 25, 38, '深入理解机器学习', 'https://example.com/article/14', 'Author_58', '这是一个关于深入理解机器学习的摘要说明，内容非常精彩。', '这是深入理解机器学习的详细内容...

这里包含了很多关于机器学习的知识。', 1, '2025-08-30 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (15, 98, 32, 'Productivity实战演练', 'https://example.com/article/15', 'Author_30', '这是一个关于Productivity实战演练的摘要说明，内容非常精彩。', '这是Productivity实战演练的详细内容...

这里包含了很多关于Productivity的知识。', 1, '2025-03-04 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (16, 43, 3, 'Vue.js从入门到精通', 'https://example.com/article/16', 'Author_44', '这是一个关于Vue.js从入门到精通的摘要说明，内容非常精彩。', '这是Vue.js从入门到精通的详细内容...

这里包含了很多关于Vue.js的知识。', 1, '2025-04-16 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (17, 51, 36, 'Prompt工程学习笔记', 'https://example.com/article/17', 'Author_4', '这是一个关于Prompt工程学习笔记的摘要说明，内容非常精彩。', '这是Prompt工程学习笔记的详细内容...

这里包含了很多关于Prompt工程的知识。', 1, '2025-07-29 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (18, 10, 16, 'DevOps与HTML5的结合', 'https://example.com/article/18', 'Author_83', '这是一个关于DevOps与HTML5的结合的摘要说明，内容非常精彩。', '这是DevOps与HTML5的结合的详细内容...

这里包含了很多关于DevOps的知识。', 1, '2025-10-04 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (19, 2, 15, 'Prompt工程源码分析', 'https://example.com/article/19', 'Author_68', '这是一个关于Prompt工程源码分析的摘要说明，内容非常精彩。', '这是Prompt工程源码分析的详细内容...

这里包含了很多关于Prompt工程的知识。', 1, '2025-10-14 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (20, 100, 48, 'Spring Cloud未来趋势', 'https://example.com/article/20', 'Author_32', '这是一个关于Spring Cloud未来趋势的摘要说明，内容非常精彩。', '这是Spring Cloud未来趋势的详细内容...

这里包含了很多关于Spring Cloud的知识。', 1, '2025-10-19 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (21, 45, 8, 'MongoDB面试题解析', 'https://example.com/article/21', 'Author_57', '这是一个关于MongoDB面试题解析的摘要说明，内容非常精彩。', '这是MongoDB面试题解析的详细内容...

这里包含了很多关于MongoDB的知识。', 1, '2025-09-04 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (22, 62, 42, '深入理解Note Taking', 'https://example.com/article/22', 'Author_53', '这是一个关于深入理解Note Taking的摘要说明，内容非常精彩。', '这是深入理解Note Taking的详细内容...

这里包含了很多关于Note Taking的知识。', 1, '2025-07-18 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (23, 58, 11, 'Design Patterns未来趋势', 'https://example.com/article/23', 'Author_34', '这是一个关于Design Patterns未来趋势的摘要说明，内容非常精彩。', '这是Design Patterns未来趋势的详细内容...

这里包含了很多关于Design Patterns的知识。', 1, '2025-08-10 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (24, 36, 15, '基于Kubernetes的系统设计', 'https://example.com/article/24', 'Author_17', '这是一个关于基于Kubernetes的系统设计的摘要说明，内容非常精彩。', '这是基于Kubernetes的系统设计的详细内容...

这里包含了很多关于Kubernetes的知识。', 1, '2025-12-28 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (25, 22, 10, 'Spring Cloud与Encryption的结合', 'https://example.com/article/25', 'Author_100', '这是一个关于Spring Cloud与Encryption的结合的摘要说明，内容非常精彩。', '这是Spring Cloud与Encryption的结合的详细内容...

这里包含了很多关于Spring Cloud的知识。', 1, '2025-06-23 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (26, 26, 47, 'Note Taking常见问题汇总', 'https://example.com/article/26', 'Author_95', '这是一个关于Note Taking常见问题汇总的摘要说明，内容非常精彩。', '这是Note Taking常见问题汇总的详细内容...

这里包含了很多关于Note Taking的知识。', 1, '2025-04-26 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (27, 27, 17, 'Blockchain学习笔记', 'https://example.com/article/27', 'Author_80', '这是一个关于Blockchain学习笔记的摘要说明，内容非常精彩。', '这是Blockchain学习笔记的详细内容...

这里包含了很多关于Blockchain的知识。', 1, '2025-06-11 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (28, 14, 21, 'Data Structures开发环境搭建', 'https://example.com/article/28', 'Author_44', '这是一个关于Data Structures开发环境搭建的摘要说明，内容非常精彩。', '这是Data Structures开发环境搭建的详细内容...

这里包含了很多关于Data Structures的知识。', 1, '2025-03-22 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (29, 46, 25, 'Redis入门指南', 'https://example.com/article/29', 'Author_55', '这是一个关于Redis入门指南的摘要说明，内容非常精彩。', '这是Redis入门指南的详细内容...

这里包含了很多关于Redis的知识。', 1, '2025-09-14 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (30, 96, 16, 'Frontend从入门到精通', 'https://example.com/article/30', 'Author_100', '这是一个关于Frontend从入门到精通的摘要说明，内容非常精彩。', '这是Frontend从入门到精通的详细内容...

这里包含了很多关于Frontend的知识。', 1, '2025-04-17 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (31, 26, 27, 'JWT性能优化', 'https://example.com/article/31', 'Author_82', '这是一个关于JWT性能优化的摘要说明，内容非常精彩。', '这是JWT性能优化的详细内容...

这里包含了很多关于JWT的知识。', 1, '2026-01-24 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (32, 33, 3, 'PostgreSQL入门指南', 'https://example.com/article/32', 'Author_40', '这是一个关于PostgreSQL入门指南的摘要说明，内容非常精彩。', '这是PostgreSQL入门指南的详细内容...

这里包含了很多关于PostgreSQL的知识。', 1, '2025-06-07 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (33, 74, 10, 'JWT与MongoDB的结合', 'https://example.com/article/33', 'Author_97', '这是一个关于JWT与MongoDB的结合的摘要说明，内容非常精彩。', '这是JWT与MongoDB的结合的详细内容...

这里包含了很多关于JWT的知识。', 1, '2025-09-18 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (34, 60, 28, 'GTD实战演练', 'https://example.com/article/34', 'Author_58', '这是一个关于GTD实战演练的摘要说明，内容非常精彩。', '这是GTD实战演练的详细内容...

这里包含了很多关于GTD的知识。', 1, '2025-10-23 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (35, 43, 24, '深入理解知识图谱', 'https://example.com/article/35', 'Author_58', '这是一个关于深入理解知识图谱的摘要说明，内容非常精彩。', '这是深入理解知识图谱的详细内容...

这里包含了很多关于知识图谱的知识。', 1, '2025-03-04 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (36, 15, 45, 'HTML5入门指南', 'https://example.com/article/36', 'Author_78', '这是一个关于HTML5入门指南的摘要说明，内容非常精彩。', '这是HTML5入门指南的详细内容...

这里包含了很多关于HTML5的知识。', 1, '2025-09-03 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (37, 37, 2, 'Frontend未来趋势', 'https://example.com/article/37', 'Author_21', '这是一个关于Frontend未来趋势的摘要说明，内容非常精彩。', '这是Frontend未来趋势的详细内容...

这里包含了很多关于Frontend的知识。', 1, '2025-03-19 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (38, 61, 30, 'Database学习笔记', 'https://example.com/article/38', 'Author_29', '这是一个关于Database学习笔记的摘要说明，内容非常精彩。', '这是Database学习笔记的详细内容...

这里包含了很多关于Database的知识。', 1, '2025-04-21 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (39, 69, 13, 'HTML5开发环境搭建', 'https://example.com/article/39', 'Author_61', '这是一个关于HTML5开发环境搭建的摘要说明，内容非常精彩。', '这是HTML5开发环境搭建的详细内容...

这里包含了很多关于HTML5的知识。', 1, '2025-10-04 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (40, 92, 5, 'Redis常见问题汇总', 'https://example.com/article/40', 'Author_60', '这是一个关于Redis常见问题汇总的摘要说明，内容非常精彩。', '这是Redis常见问题汇总的详细内容...

这里包含了很多关于Redis的知识。', 1, '2025-08-24 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (41, 18, 37, '深入理解Angular', 'https://example.com/article/41', 'Author_18', '这是一个关于深入理解Angular的摘要说明，内容非常精彩。', '这是深入理解Angular的详细内容...

这里包含了很多关于Angular的知识。', 1, '2025-02-08 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (42, 24, 34, 'Docker核心原理解密', 'https://example.com/article/42', 'Author_53', '这是一个关于Docker核心原理解密的摘要说明，内容非常精彩。', '这是Docker核心原理解密的详细内容...

这里包含了很多关于Docker的知识。', 1, '2025-04-26 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (43, 55, 30, 'Design Patterns入门指南', 'https://example.com/article/43', 'Author_34', '这是一个关于Design Patterns入门指南的摘要说明，内容非常精彩。', '这是Design Patterns入门指南的详细内容...

这里包含了很多关于Design Patterns的知识。', 1, '2025-08-25 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (44, 73, 23, 'Time Management源码分析', 'https://example.com/article/44', 'Author_30', '这是一个关于Time Management源码分析的摘要说明，内容非常精彩。', '这是Time Management源码分析的详细内容...

这里包含了很多关于Time Management的知识。', 1, '2025-03-01 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (45, 16, 50, 'Redis与Spring Boot的结合', 'https://example.com/article/45', 'Author_26', '这是一个关于Redis与Spring Boot的结合的摘要说明，内容非常精彩。', '这是Redis与Spring Boot的结合的详细内容...

这里包含了很多关于Redis的知识。', 1, '2025-02-23 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (46, 69, 44, 'GCP实战演练', 'https://example.com/article/46', 'Author_28', '这是一个关于GCP实战演练的摘要说明，内容非常精彩。', '这是GCP实战演练的详细内容...

这里包含了很多关于GCP的知识。', 1, '2025-02-17 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (47, 16, 10, 'Database开发环境搭建', 'https://example.com/article/47', 'Author_74', '这是一个关于Database开发环境搭建的摘要说明，内容非常精彩。', '这是Database开发环境搭建的详细内容...

这里包含了很多关于Database的知识。', 1, '2025-11-06 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (48, 85, 33, 'System Design面试题解析', 'https://example.com/article/48', 'Author_16', '这是一个关于System Design面试题解析的摘要说明，内容非常精彩。', '这是System Design面试题解析的详细内容...

这里包含了很多关于System Design的知识。', 1, '2025-09-10 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (49, 76, 43, 'System Design入门指南', 'https://example.com/article/49', 'Author_39', '这是一个关于System Design入门指南的摘要说明，内容非常精彩。', '这是System Design入门指南的详细内容...

这里包含了很多关于System Design的知识。', 1, '2025-04-03 01:53:04', 0);
INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES (50, 66, 46, 'OAuth2面试题解析', 'https://example.com/article/50', 'Author_40', '这是一个关于OAuth2面试题解析的摘要说明，内容非常精彩。', '这是OAuth2面试题解析的详细内容...

这里包含了很多关于OAuth2的知识。', 1, '2025-08-07 01:53:04', 0);

-- Notes Folders
TRUNCATE TABLE note_folder;
INSERT INTO note_folder (id, user_id, name, parent_id, sort_order, created_time, deleted) VALUES (1, 1, '前端开发', 0, 1, '2025-01-01 00:00:00', 0);
INSERT INTO note_folder (id, user_id, name, parent_id, sort_order, created_time, deleted) VALUES (2, 1, '后端架构', 0, 2, '2025-01-01 00:00:00', 0);
INSERT INTO note_folder (id, user_id, name, parent_id, sort_order, created_time, deleted) VALUES (3, 1, '人工智能', 0, 3, '2025-01-01 00:00:00', 0);
INSERT INTO note_folder (id, user_id, name, parent_id, sort_order, created_time, deleted) VALUES (4, 1, '随想杂记', 0, 4, '2025-01-01 00:00:00', 0);

-- Notes
TRUNCATE TABLE note_entry;
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (1, 1, 1, 'TypeScript面试题解析', '# TypeScript面试题解析

这里是关于 **TypeScript** 的笔记内容。
- 重点1
- 重点2', '关于TypeScript的笔记', 'Review,笔记', 'NOTE', 2, 1, '2025-12-25 01:53:04', '2025-12-25 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (2, 1, 3, 'Algorithms核心原理解密', '# Algorithms核心原理解密

这里是关于 **Algorithms** 的笔记内容。
- 重点1
- 重点2', '关于Algorithms的笔记', '学习,生活', 'NOTE', 2, 1, '2025-07-30 01:53:04', '2025-07-30 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (3, 2, 0, 'Prompt工程从入门到精通', '# Prompt工程从入门到精通

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '待办,技术', 'NOTE', 2, 0, '2025-03-27 01:53:04', '2025-03-27 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (4, 2, 0, '深入理解JavaScript', '# 深入理解JavaScript

这里是关于 **JavaScript** 的笔记内容。
- 重点1
- 重点2', '关于JavaScript的笔记', '重要,感悟', 'NOTE', 2, 1, '2025-04-13 01:53:04', '2025-04-13 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (5, 3, 0, 'Prompt工程性能优化', '# Prompt工程性能优化

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '感悟,学习', 'NOTE', 2, 0, '2025-03-24 01:53:04', '2025-03-24 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (6, 3, 0, '基于自然语言处理的系统设计', '# 基于自然语言处理的系统设计

这里是关于 **自然语言处理** 的笔记内容。
- 重点1
- 重点2', '关于自然语言处理的笔记', '生活,感悟', 'NOTE', 2, 1, '2025-10-02 01:53:04', '2025-10-02 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (7, 4, 0, '人工智能常见问题汇总', '# 人工智能常见问题汇总

这里是关于 **人工智能** 的笔记内容。
- 重点1
- 重点2', '关于人工智能的笔记', '工作,生活', 'NOTE', 2, 1, '2025-08-12 01:53:04', '2025-08-12 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (8, 4, 0, 'MongoDB性能优化', '# MongoDB性能优化

这里是关于 **MongoDB** 的笔记内容。
- 重点1
- 重点2', '关于MongoDB的笔记', '归档,生活', 'NOTE', 2, 1, '2025-06-17 01:53:04', '2025-06-17 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (9, 5, 0, 'Database面试题解析', '# Database面试题解析

这里是关于 **Database** 的笔记内容。
- 重点1
- 重点2', '关于Database的笔记', '学习,重要', 'NOTE', 2, 0, '2025-06-01 01:53:04', '2025-06-01 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (10, 5, 0, 'Security性能优化', '# Security性能优化

这里是关于 **Security** 的笔记内容。
- 重点1
- 重点2', '关于Security的笔记', '归档,技术', 'NOTE', 2, 0, '2025-07-28 01:53:04', '2025-07-28 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (11, 6, 0, 'GTD性能优化', '# GTD性能优化

这里是关于 **GTD** 的笔记内容。
- 重点1
- 重点2', '关于GTD的笔记', '笔记,感悟', 'NOTE', 2, 1, '2025-11-24 01:53:04', '2025-11-24 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (12, 6, 0, 'Vue.js学习笔记', '# Vue.js学习笔记

这里是关于 **Vue.js** 的笔记内容。
- 重点1
- 重点2', '关于Vue.js的笔记', '归档,工作', 'NOTE', 2, 1, '2025-08-09 01:53:04', '2025-08-09 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (13, 7, 0, 'MySQL最佳实践', '# MySQL最佳实践

这里是关于 **MySQL** 的笔记内容。
- 重点1
- 重点2', '关于MySQL的笔记', '重要,Review', 'NOTE', 2, 1, '2025-03-02 01:53:04', '2025-03-02 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (14, 7, 0, 'MongoDB核心原理解密', '# MongoDB核心原理解密

这里是关于 **MongoDB** 的笔记内容。
- 重点1
- 重点2', '关于MongoDB的笔记', '归档,重要', 'NOTE', 2, 1, '2025-12-19 01:53:04', '2025-12-19 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (15, 8, 0, 'Pomodoro未来趋势', '# Pomodoro未来趋势

这里是关于 **Pomodoro** 的笔记内容。
- 重点1
- 重点2', '关于Pomodoro的笔记', '生活,学习', 'NOTE', 2, 0, '2025-12-31 01:53:04', '2025-12-31 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (16, 8, 0, 'Azure最佳实践', '# Azure最佳实践

这里是关于 **Azure** 的笔记内容。
- 重点1
- 重点2', '关于Azure的笔记', '归档,Review', 'NOTE', 2, 0, '2025-10-18 01:53:04', '2025-10-18 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (17, 9, 0, '知识图谱学习笔记', '# 知识图谱学习笔记

这里是关于 **知识图谱** 的笔记内容。
- 重点1
- 重点2', '关于知识图谱的笔记', '学习,归档', 'NOTE', 2, 1, '2025-08-15 01:53:04', '2025-08-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (18, 9, 0, 'Algorithms核心原理解密', '# Algorithms核心原理解密

这里是关于 **Algorithms** 的笔记内容。
- 重点1
- 重点2', '关于Algorithms的笔记', '工作,笔记', 'NOTE', 2, 1, '2025-05-06 01:53:04', '2025-05-06 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (19, 10, 0, 'Docker最佳实践', '# Docker最佳实践

这里是关于 **Docker** 的笔记内容。
- 重点1
- 重点2', '关于Docker的笔记', '生活,归档', 'NOTE', 2, 1, '2025-06-11 01:53:04', '2025-06-11 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (20, 10, 0, 'AWS学习笔记', '# AWS学习笔记

这里是关于 **AWS** 的笔记内容。
- 重点1
- 重点2', '关于AWS的笔记', '笔记,技术', 'NOTE', 2, 1, '2025-05-06 01:53:04', '2025-05-06 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (21, 11, 0, '基于Encryption的系统设计', '# 基于Encryption的系统设计

这里是关于 **Encryption** 的笔记内容。
- 重点1
- 重点2', '关于Encryption的笔记', '笔记,工作', 'NOTE', 2, 1, '2025-05-29 01:53:04', '2025-05-29 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (22, 11, 0, 'Database未来趋势', '# Database未来趋势

这里是关于 **Database** 的笔记内容。
- 重点1
- 重点2', '关于Database的笔记', '技术,待办', 'NOTE', 2, 0, '2025-04-15 01:53:04', '2025-04-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (23, 12, 0, 'CSS3学习笔记', '# CSS3学习笔记

这里是关于 **CSS3** 的笔记内容。
- 重点1
- 重点2', '关于CSS3的笔记', '笔记,感悟', 'NOTE', 2, 0, '2025-12-30 01:53:04', '2025-12-30 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (24, 12, 0, 'React面试题解析', '# React面试题解析

这里是关于 **React** 的笔记内容。
- 重点1
- 重点2', '关于React的笔记', '技术,生活', 'NOTE', 2, 1, '2025-07-12 01:53:04', '2025-07-12 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (25, 13, 0, 'Neo4j实战演练', '# Neo4j实战演练

这里是关于 **Neo4j** 的笔记内容。
- 重点1
- 重点2', '关于Neo4j的笔记', '工作,生活', 'NOTE', 2, 1, '2025-08-28 01:53:04', '2025-08-28 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (26, 13, 0, '深度学习性能优化', '# 深度学习性能优化

这里是关于 **深度学习** 的笔记内容。
- 重点1
- 重点2', '关于深度学习的笔记', '生活,待办', 'NOTE', 2, 0, '2025-09-09 01:53:04', '2025-09-09 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (27, 14, 0, '深入理解DevOps', '# 深入理解DevOps

这里是关于 **DevOps** 的笔记内容。
- 重点1
- 重点2', '关于DevOps的笔记', '工作,待办', 'NOTE', 2, 1, '2025-05-03 01:53:04', '2025-05-03 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (28, 14, 0, 'Database核心原理解密', '# Database核心原理解密

这里是关于 **Database** 的笔记内容。
- 重点1
- 重点2', '关于Database的笔记', '待办,笔记', 'NOTE', 2, 0, '2025-08-18 01:53:04', '2025-08-18 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (29, 15, 0, 'DevOps与的结合', '# DevOps与的结合

这里是关于 **DevOps** 的笔记内容。
- 重点1
- 重点2', '关于DevOps的笔记', '待办,笔记', 'NOTE', 2, 1, '2025-06-08 01:53:04', '2025-06-08 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (30, 15, 0, '知识图谱未来趋势', '# 知识图谱未来趋势

这里是关于 **知识图谱** 的笔记内容。
- 重点1
- 重点2', '关于知识图谱的笔记', '笔记,待办', 'NOTE', 2, 0, '2025-04-28 01:53:04', '2025-04-28 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (31, 16, 0, 'Docker面试题解析', '# Docker面试题解析

这里是关于 **Docker** 的笔记内容。
- 重点1
- 重点2', '关于Docker的笔记', '感悟,生活', 'NOTE', 2, 1, '2025-06-10 01:53:04', '2025-06-10 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (32, 16, 0, 'Serverless实战演练', '# Serverless实战演练

这里是关于 **Serverless** 的笔记内容。
- 重点1
- 重点2', '关于Serverless的笔记', '技术,学习', 'NOTE', 2, 1, '2025-09-11 01:53:04', '2025-09-11 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (33, 17, 0, 'Serverless与的结合', '# Serverless与的结合

这里是关于 **Serverless** 的笔记内容。
- 重点1
- 重点2', '关于Serverless的笔记', '重要,感悟', 'NOTE', 2, 0, '2025-02-25 01:53:04', '2025-02-25 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (34, 17, 0, 'Algorithms未来趋势', '# Algorithms未来趋势

这里是关于 **Algorithms** 的笔记内容。
- 重点1
- 重点2', '关于Algorithms的笔记', '技术,工作', 'NOTE', 2, 1, '2025-07-23 01:53:04', '2025-07-23 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (35, 18, 0, 'JavaScript常见问题汇总', '# JavaScript常见问题汇总

这里是关于 **JavaScript** 的笔记内容。
- 重点1
- 重点2', '关于JavaScript的笔记', '工作,归档', 'NOTE', 2, 0, '2025-10-19 01:53:04', '2025-10-19 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (36, 18, 0, 'Kubernetes未来趋势', '# Kubernetes未来趋势

这里是关于 **Kubernetes** 的笔记内容。
- 重点1
- 重点2', '关于Kubernetes的笔记', '学习,工作', 'NOTE', 2, 1, '2025-07-18 01:53:04', '2025-07-18 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (37, 19, 0, 'Database面试题解析', '# Database面试题解析

这里是关于 **Database** 的笔记内容。
- 重点1
- 重点2', '关于Database的笔记', '笔记,感悟', 'NOTE', 2, 1, '2025-02-15 01:53:04', '2025-02-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (38, 19, 0, 'Spring Cloud面试题解析', '# Spring Cloud面试题解析

这里是关于 **Spring Cloud** 的笔记内容。
- 重点1
- 重点2', '关于Spring Cloud的笔记', '感悟,待办', 'NOTE', 2, 1, '2025-02-26 01:53:04', '2025-02-26 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (39, 20, 0, 'HTML5常见问题汇总', '# HTML5常见问题汇总

这里是关于 **HTML5** 的笔记内容。
- 重点1
- 重点2', '关于HTML5的笔记', '工作,待办', 'NOTE', 2, 1, '2025-09-15 01:53:04', '2025-09-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (40, 20, 0, 'Cloud Computing从入门到精通', '# Cloud Computing从入门到精通

这里是关于 **Cloud Computing** 的笔记内容。
- 重点1
- 重点2', '关于Cloud Computing的笔记', '学习,Review', 'NOTE', 2, 1, '2025-06-27 01:53:04', '2025-06-27 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (41, 21, 0, 'Prompt工程常见问题汇总', '# Prompt工程常见问题汇总

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '技术,生活', 'NOTE', 2, 1, '2025-12-31 01:53:04', '2025-12-31 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (42, 21, 0, 'Security入门指南', '# Security入门指南

这里是关于 **Security** 的笔记内容。
- 重点1
- 重点2', '关于Security的笔记', '工作,学习', 'NOTE', 2, 0, '2025-03-23 01:53:04', '2025-03-23 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (43, 22, 0, 'GTD性能优化', '# GTD性能优化

这里是关于 **GTD** 的笔记内容。
- 重点1
- 重点2', '关于GTD的笔记', '重要,生活', 'NOTE', 2, 0, '2025-04-12 01:53:04', '2025-04-12 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (44, 22, 0, '深度学习未来趋势', '# 深度学习未来趋势

这里是关于 **深度学习** 的笔记内容。
- 重点1
- 重点2', '关于深度学习的笔记', 'Review,工作', 'NOTE', 2, 0, '2025-08-06 01:53:04', '2025-08-06 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (45, 23, 0, 'CSS3学习笔记', '# CSS3学习笔记

这里是关于 **CSS3** 的笔记内容。
- 重点1
- 重点2', '关于CSS3的笔记', '笔记,归档', 'NOTE', 2, 1, '2025-08-24 01:53:04', '2025-08-24 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (46, 23, 0, 'Data Structures面试题解析', '# Data Structures面试题解析

这里是关于 **Data Structures** 的笔记内容。
- 重点1
- 重点2', '关于Data Structures的笔记', '感悟,归档', 'NOTE', 2, 0, '2025-09-11 01:53:04', '2025-09-11 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (47, 24, 0, 'Prompt工程从入门到精通', '# Prompt工程从入门到精通

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '工作,归档', 'NOTE', 2, 0, '2025-04-05 01:53:04', '2025-04-05 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (48, 24, 0, 'Prompt工程常见问题汇总', '# Prompt工程常见问题汇总

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '重要,笔记', 'NOTE', 2, 0, '2025-09-20 01:53:04', '2025-09-20 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (49, 25, 0, 'Spring Boot未来趋势', '# Spring Boot未来趋势

这里是关于 **Spring Boot** 的笔记内容。
- 重点1
- 重点2', '关于Spring Boot的笔记', '重要,待办', 'NOTE', 2, 1, '2025-03-21 01:53:04', '2025-03-21 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (50, 25, 0, '神经网络开发环境搭建', '# 神经网络开发环境搭建

这里是关于 **神经网络** 的笔记内容。
- 重点1
- 重点2', '关于神经网络的笔记', '归档,生活', 'NOTE', 2, 1, '2025-06-28 01:53:04', '2025-06-28 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (51, 26, 0, 'GTD面试题解析', '# GTD面试题解析

这里是关于 **GTD** 的笔记内容。
- 重点1
- 重点2', '关于GTD的笔记', 'Review,工作', 'NOTE', 2, 0, '2026-01-15 01:53:04', '2026-01-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (52, 26, 0, '机器学习未来趋势', '# 机器学习未来趋势

这里是关于 **机器学习** 的笔记内容。
- 重点1
- 重点2', '关于机器学习的笔记', '学习,归档', 'NOTE', 2, 0, '2025-10-23 01:53:04', '2025-10-23 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (53, 27, 0, '基于Prompt工程的系统设计', '# 基于Prompt工程的系统设计

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '归档,学习', 'NOTE', 2, 1, '2025-12-25 01:53:04', '2025-12-25 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (54, 27, 0, 'AWS核心原理解密', '# AWS核心原理解密

这里是关于 **AWS** 的笔记内容。
- 重点1
- 重点2', '关于AWS的笔记', '待办,技术', 'NOTE', 2, 0, '2026-01-01 01:53:04', '2026-01-01 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (55, 28, 0, 'Time Management核心原理解密', '# Time Management核心原理解密

这里是关于 **Time Management** 的笔记内容。
- 重点1
- 重点2', '关于Time Management的笔记', 'Review,归档', 'NOTE', 2, 0, '2025-11-15 01:53:04', '2025-11-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (56, 28, 0, 'Blockchain面试题解析', '# Blockchain面试题解析

这里是关于 **Blockchain** 的笔记内容。
- 重点1
- 重点2', '关于Blockchain的笔记', '重要,生活', 'NOTE', 2, 1, '2025-08-09 01:53:04', '2025-08-09 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (57, 29, 0, 'Pomodoro学习笔记', '# Pomodoro学习笔记

这里是关于 **Pomodoro** 的笔记内容。
- 重点1
- 重点2', '关于Pomodoro的笔记', '工作,感悟', 'NOTE', 2, 0, '2026-01-12 01:53:04', '2026-01-12 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (58, 29, 0, 'JavaScript面试题解析', '# JavaScript面试题解析

这里是关于 **JavaScript** 的笔记内容。
- 重点1
- 重点2', '关于JavaScript的笔记', '学习,技术', 'NOTE', 2, 0, '2025-07-09 01:53:04', '2025-07-09 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (59, 30, 0, 'CSS3从入门到精通', '# CSS3从入门到精通

这里是关于 **CSS3** 的笔记内容。
- 重点1
- 重点2', '关于CSS3的笔记', '学习,归档', 'NOTE', 2, 1, '2025-02-16 01:53:04', '2025-02-16 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (60, 30, 0, 'HTML5核心原理解密', '# HTML5核心原理解密

这里是关于 **HTML5** 的笔记内容。
- 重点1
- 重点2', '关于HTML5的笔记', 'Review,工作', 'NOTE', 2, 1, '2025-08-03 01:53:04', '2025-08-03 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (61, 31, 0, 'Data Structures常见问题汇总', '# Data Structures常见问题汇总

这里是关于 **Data Structures** 的笔记内容。
- 重点1
- 重点2', '关于Data Structures的笔记', '生活,技术', 'NOTE', 2, 0, '2025-05-06 01:53:04', '2025-05-06 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (62, 31, 0, 'Spring Boot面试题解析', '# Spring Boot面试题解析

这里是关于 **Spring Boot** 的笔记内容。
- 重点1
- 重点2', '关于Spring Boot的笔记', '感悟,归档', 'NOTE', 2, 0, '2025-08-01 01:53:04', '2025-08-01 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (63, 32, 0, 'TypeScript与的结合', '# TypeScript与的结合

这里是关于 **TypeScript** 的笔记内容。
- 重点1
- 重点2', '关于TypeScript的笔记', '归档,Review', 'NOTE', 2, 1, '2025-08-07 01:53:04', '2025-08-07 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (64, 32, 0, 'Prompt工程实战演练', '# Prompt工程实战演练

这里是关于 **Prompt工程** 的笔记内容。
- 重点1
- 重点2', '关于Prompt工程的笔记', '笔记,技术', 'NOTE', 2, 1, '2025-03-01 01:53:04', '2025-03-01 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (65, 33, 0, '大语言模型与的结合', '# 大语言模型与的结合

这里是关于 **大语言模型** 的笔记内容。
- 重点1
- 重点2', '关于大语言模型的笔记', '技术,工作', 'NOTE', 2, 0, '2025-09-11 01:53:04', '2025-09-11 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (66, 33, 0, 'Spring Cloud核心原理解密', '# Spring Cloud核心原理解密

这里是关于 **Spring Cloud** 的笔记内容。
- 重点1
- 重点2', '关于Spring Cloud的笔记', '生活,归档', 'NOTE', 2, 1, '2025-12-01 01:53:04', '2025-12-01 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (67, 34, 0, 'Time Management实战演练', '# Time Management实战演练

这里是关于 **Time Management** 的笔记内容。
- 重点1
- 重点2', '关于Time Management的笔记', '重要,技术', 'NOTE', 2, 1, '2025-08-24 01:53:04', '2025-08-24 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (68, 34, 0, '深入理解System Design', '# 深入理解System Design

这里是关于 **System Design** 的笔记内容。
- 重点1
- 重点2', '关于System Design的笔记', '笔记,感悟', 'NOTE', 2, 0, '2025-12-03 01:53:04', '2025-12-03 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (69, 35, 0, '自然语言处理核心原理解密', '# 自然语言处理核心原理解密

这里是关于 **自然语言处理** 的笔记内容。
- 重点1
- 重点2', '关于自然语言处理的笔记', '重要,待办', 'NOTE', 2, 0, '2025-09-17 01:53:04', '2025-09-17 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (70, 35, 0, 'PostgreSQL学习笔记', '# PostgreSQL学习笔记

这里是关于 **PostgreSQL** 的笔记内容。
- 重点1
- 重点2', '关于PostgreSQL的笔记', 'Review,工作', 'NOTE', 2, 1, '2025-11-10 01:53:04', '2025-11-10 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (71, 36, 0, 'Neo4j入门指南', '# Neo4j入门指南

这里是关于 **Neo4j** 的笔记内容。
- 重点1
- 重点2', '关于Neo4j的笔记', '学习,归档', 'NOTE', 2, 1, '2025-05-08 01:53:04', '2025-05-08 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (72, 36, 0, 'OAuth2从入门到精通', '# OAuth2从入门到精通

这里是关于 **OAuth2** 的笔记内容。
- 重点1
- 重点2', '关于OAuth2的笔记', '重要,感悟', 'NOTE', 2, 0, '2026-01-05 01:53:04', '2026-01-05 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (73, 37, 0, '深入理解Encryption', '# 深入理解Encryption

这里是关于 **Encryption** 的笔记内容。
- 重点1
- 重点2', '关于Encryption的笔记', '学习,归档', 'NOTE', 2, 1, '2025-07-11 01:53:04', '2025-07-11 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (74, 37, 0, 'Cloud Computing源码分析', '# Cloud Computing源码分析

这里是关于 **Cloud Computing** 的笔记内容。
- 重点1
- 重点2', '关于Cloud Computing的笔记', '生活,学习', 'NOTE', 2, 0, '2025-05-30 01:53:04', '2025-05-30 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (75, 38, 0, 'Elasticsearch入门指南', '# Elasticsearch入门指南

这里是关于 **Elasticsearch** 的笔记内容。
- 重点1
- 重点2', '关于Elasticsearch的笔记', '生活,待办', 'NOTE', 2, 0, '2025-02-28 01:53:04', '2025-02-28 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (76, 38, 0, 'Encryption源码分析', '# Encryption源码分析

这里是关于 **Encryption** 的笔记内容。
- 重点1
- 重点2', '关于Encryption的笔记', '学习,重要', 'NOTE', 2, 1, '2025-08-21 01:53:04', '2025-08-21 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (77, 39, 0, 'Pomodoro开发环境搭建', '# Pomodoro开发环境搭建

这里是关于 **Pomodoro** 的笔记内容。
- 重点1
- 重点2', '关于Pomodoro的笔记', '技术,笔记', 'NOTE', 2, 1, '2025-03-15 01:53:04', '2025-03-15 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (78, 39, 0, 'Vue.js常见问题汇总', '# Vue.js常见问题汇总

这里是关于 **Vue.js** 的笔记内容。
- 重点1
- 重点2', '关于Vue.js的笔记', '生活,笔记', 'NOTE', 2, 1, '2025-12-23 01:53:04', '2025-12-23 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (79, 40, 0, 'DevOps开发环境搭建', '# DevOps开发环境搭建

这里是关于 **DevOps** 的笔记内容。
- 重点1
- 重点2', '关于DevOps的笔记', '笔记,感悟', 'NOTE', 2, 1, '2025-09-27 01:53:04', '2025-09-27 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (80, 40, 0, 'Neo4j核心原理解密', '# Neo4j核心原理解密

这里是关于 **Neo4j** 的笔记内容。
- 重点1
- 重点2', '关于Neo4j的笔记', '感悟,笔记', 'NOTE', 2, 1, '2025-01-31 01:53:04', '2025-01-31 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (81, 41, 0, 'PostgreSQL开发环境搭建', '# PostgreSQL开发环境搭建

这里是关于 **PostgreSQL** 的笔记内容。
- 重点1
- 重点2', '关于PostgreSQL的笔记', '感悟,工作', 'NOTE', 2, 1, '2025-05-28 01:53:04', '2025-05-28 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (82, 41, 0, 'Pomodoro常见问题汇总', '# Pomodoro常见问题汇总

这里是关于 **Pomodoro** 的笔记内容。
- 重点1
- 重点2', '关于Pomodoro的笔记', '感悟,笔记', 'NOTE', 2, 0, '2025-06-06 01:53:04', '2025-06-06 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (83, 42, 0, 'React从入门到精通', '# React从入门到精通

这里是关于 **React** 的笔记内容。
- 重点1
- 重点2', '关于React的笔记', '重要,学习', 'NOTE', 2, 1, '2025-03-24 01:53:04', '2025-03-24 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (84, 42, 0, '神经网络开发环境搭建', '# 神经网络开发环境搭建

这里是关于 **神经网络** 的笔记内容。
- 重点1
- 重点2', '关于神经网络的笔记', '归档,工作', 'NOTE', 2, 0, '2025-02-23 01:53:04', '2025-02-23 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (85, 43, 0, '基于Spring Cloud的系统设计', '# 基于Spring Cloud的系统设计

这里是关于 **Spring Cloud** 的笔记内容。
- 重点1
- 重点2', '关于Spring Cloud的笔记', '重要,待办', 'NOTE', 2, 1, '2025-07-30 01:53:04', '2025-07-30 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (86, 43, 0, 'React与的结合', '# React与的结合

这里是关于 **React** 的笔记内容。
- 重点1
- 重点2', '关于React的笔记', '笔记,学习', 'NOTE', 2, 1, '2025-12-24 01:53:04', '2025-12-24 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (87, 44, 0, 'Cloud Computing源码分析', '# Cloud Computing源码分析

这里是关于 **Cloud Computing** 的笔记内容。
- 重点1
- 重点2', '关于Cloud Computing的笔记', '重要,工作', 'NOTE', 2, 0, '2025-11-03 01:53:04', '2025-11-03 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (88, 44, 0, 'JavaScript与的结合', '# JavaScript与的结合

这里是关于 **JavaScript** 的笔记内容。
- 重点1
- 重点2', '关于JavaScript的笔记', '工作,笔记', 'NOTE', 2, 1, '2025-11-29 01:53:04', '2025-11-29 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (89, 45, 0, 'TypeScript源码分析', '# TypeScript源码分析

这里是关于 **TypeScript** 的笔记内容。
- 重点1
- 重点2', '关于TypeScript的笔记', '待办,重要', 'NOTE', 2, 1, '2025-08-19 01:53:04', '2025-08-19 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (90, 45, 0, 'MySQL性能优化', '# MySQL性能优化

这里是关于 **MySQL** 的笔记内容。
- 重点1
- 重点2', '关于MySQL的笔记', '生活,学习', 'NOTE', 2, 1, '2025-12-17 01:53:04', '2025-12-17 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (91, 46, 0, 'System Design入门指南', '# System Design入门指南

这里是关于 **System Design** 的笔记内容。
- 重点1
- 重点2', '关于System Design的笔记', '工作,生活', 'NOTE', 2, 1, '2025-12-23 01:53:04', '2025-12-23 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (92, 46, 0, '自然语言处理学习笔记', '# 自然语言处理学习笔记

这里是关于 **自然语言处理** 的笔记内容。
- 重点1
- 重点2', '关于自然语言处理的笔记', '工作,重要', 'NOTE', 2, 0, '2025-02-04 01:53:04', '2025-02-04 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (93, 47, 0, 'OAuth2实战演练', '# OAuth2实战演练

这里是关于 **OAuth2** 的笔记内容。
- 重点1
- 重点2', '关于OAuth2的笔记', '重要,笔记', 'NOTE', 2, 1, '2025-02-09 01:53:04', '2025-02-09 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (94, 47, 0, 'Redis开发环境搭建', '# Redis开发环境搭建

这里是关于 **Redis** 的笔记内容。
- 重点1
- 重点2', '关于Redis的笔记', '学习,感悟', 'NOTE', 2, 1, '2025-03-31 01:53:04', '2025-03-31 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (95, 48, 0, 'Blockchain核心原理解密', '# Blockchain核心原理解密

这里是关于 **Blockchain** 的笔记内容。
- 重点1
- 重点2', '关于Blockchain的笔记', '工作,归档', 'NOTE', 2, 1, '2025-12-20 01:53:04', '2025-12-20 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (96, 48, 0, '深入理解Redis', '# 深入理解Redis

这里是关于 **Redis** 的笔记内容。
- 重点1
- 重点2', '关于Redis的笔记', '重要,感悟', 'NOTE', 2, 1, '2025-05-12 01:53:04', '2025-05-12 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (97, 49, 0, 'Redis开发环境搭建', '# Redis开发环境搭建

这里是关于 **Redis** 的笔记内容。
- 重点1
- 重点2', '关于Redis的笔记', '生活,学习', 'NOTE', 2, 0, '2025-03-27 01:53:04', '2025-03-27 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (98, 49, 0, 'Kubernetes核心原理解密', '# Kubernetes核心原理解密

这里是关于 **Kubernetes** 的笔记内容。
- 重点1
- 重点2', '关于Kubernetes的笔记', 'Review,笔记', 'NOTE', 2, 0, '2026-01-05 01:53:04', '2026-01-05 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (99, 50, 0, 'Java常见问题汇总', '# Java常见问题汇总

这里是关于 **Java** 的笔记内容。
- 重点1
- 重点2', '关于Java的笔记', '待办,归档', 'NOTE', 2, 1, '2025-07-07 01:53:04', '2025-07-07 01:53:04', 0);
INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES (100, 50, 0, 'Productivity面试题解析', '# Productivity面试题解析

这里是关于 **Productivity** 的笔记内容。
- 重点1
- 重点2', '关于Productivity的笔记', '归档,重要', 'NOTE', 2, 1, '2025-09-23 01:53:04', '2025-09-23 01:53:04', 0);

SET FOREIGN_KEY_CHECKS=1;
