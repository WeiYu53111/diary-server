-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS diary_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE diary_db;

-- 创建日记表
CREATE TABLE IF NOT EXISTS diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    diary_id VARCHAR(64) NOT NULL COMMENT '日记唯一标识',
    open_id VARCHAR(64) NOT NULL COMMENT '用户OpenID',
    editor_content LONGTEXT COMMENT '日记内容（富文本）',
    create_time VARCHAR(32) COMMENT '创建时间',
    log_time VARCHAR(16) COMMENT '记录时间（日期）',
    log_week VARCHAR(16) COMMENT '记录星期',
    log_lunar VARCHAR(32) COMMENT '农历日期',
    address VARCHAR(255) COMMENT '地址位置',
    image_urls TEXT COMMENT '图片URL列表（JSON格式）',
    db_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '数据库创建时间',
    db_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '数据库更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识 0-未删除 1-已删除',
    INDEX idx_open_id (open_id),
    INDEX idx_diary_id (diary_id),
    INDEX idx_log_time (log_time),
    INDEX idx_deleted (deleted),
    INDEX idx_open_id_deleted (open_id, deleted),
    INDEX idx_open_id_log_time (open_id, log_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

-- 创建待做事项表
CREATE TABLE IF NOT EXISTS todo_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    open_id VARCHAR(64) NOT NULL COMMENT '用户OpenID',
    title VARCHAR(255) NOT NULL COMMENT '事项标题',
    description TEXT COMMENT '事项描述',
    category ENUM('PROJECT', 'HABIT') NOT NULL COMMENT '事项类别：PROJECT-项目，HABIT-习惯养成',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM' COMMENT '优先级',
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '状态',
    due_date DATE COMMENT '截止日期',
    estimated_duration INT COMMENT '预计完成时长（分钟）',
    source_diary_ids TEXT COMMENT '来源日记ID列表（JSON格式）',
    ai_confidence DECIMAL(3,2) COMMENT 'AI分析置信度(0.00-1.00)',
    ai_analysis_result TEXT COMMENT 'AI分析详细结果（JSON格式）',
    created_date DATE NOT NULL COMMENT '创建日期',
    db_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '数据库创建时间',
    db_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '数据库更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识 0-未删除 1-已删除',
    INDEX idx_open_id (open_id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_due_date (due_date),
    INDEX idx_created_date (created_date),
    INDEX idx_open_id_deleted (open_id, deleted),
    INDEX idx_open_id_status (open_id, status),
    INDEX idx_open_id_category (open_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待做事项表'; 