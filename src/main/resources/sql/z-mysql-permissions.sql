-- MySQL用户权限初始化脚本
-- 这个脚本会在MySQL容器首次启动时执行

-- 确保diary用户有足够的权限
GRANT ALL PRIVILEGES ON diary_db.* TO 'diary'@'%';
GRANT ALL PRIVILEGES ON diary_db.* TO 'diary'@'localhost'; 

-- 刷新权限
FLUSH PRIVILEGES;