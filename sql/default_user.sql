USE ai_drama;

-- 默认登录用户:
-- username: admin
-- password: 123456
-- password hash: BCrypt strength=12
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `status`, `deleted`)
VALUES (1, 'admin', '$2b$12$8j4MzrG3ZDiVQra98hZFbuOl/w8RVftFoxPyBIw1Bzq1CmGFG8/Iq', '管理员', NULL, 1, 0)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `nickname` = VALUES(`nickname`),
    `status` = VALUES(`status`),
    `deleted` = VALUES(`deleted`);
