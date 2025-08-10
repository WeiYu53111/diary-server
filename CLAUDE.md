# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于 Spring Boot 的个人日记服务端应用，主要功能包括JWT认证、日记管理、定时备份和AI智能待做事项分析。项目使用MySQL数据库存储日记和待做事项数据，支持图片上传和存储。

## 常用开发命令

### 构建和运行
```bash
# 编译项目
./mvnw clean compile

# 打包（跳过测试）
./mvnw package -DskipTests

# 运行应用
java -jar target/diary-server-0.0.1-SNAPSHOT.jar

# 或者直接使用 Maven 运行
./mvnw spring-boot:run
```

### 数据库操作
```bash
# 一次性数据迁移（从JSON文件到MySQL）
./migrate-once.sh

# 试运行迁移（不实际执行）
./migrate-once.sh dry-run
```

### Docker操作

#### 方式1：使用Docker Compose（推荐）
```bash
# 1. 复制环境变量配置文件
cp .env.example .env

# 2. 编辑.env文件，填写你的实际配置
vim .env

# 3. 启动所有服务（MySQL + 应用服务）
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看应用日志
docker-compose logs -f diary-server

# 6. 停止所有服务
docker-compose down

# 7. 停止并删除数据卷（慎用！会清除所有数据）
docker-compose down -v
```

#### 方式2：传统Docker命令
```bash
# 构建Docker镜像
docker build -t diary-server:0.1 .

# 启动MySQL数据库容器
docker run -d --name diary-mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
-e MYSQL_DATABASE=diary_db \
-e MYSQL_USER=diary \
-e MYSQL_PASSWORD=diary123 \
-p 3306:3306 \
-v $(pwd)/src/main/resources/sql:/docker-entrypoint-initdb.d \
mysql:8.0

# 运行应用容器
docker run -d --name diary-server \
--env-file .env \
--link diary-mysql:mysql \
-p 7080:7080 \
-v /data/diary-server/images:/data/images \
-v /data/diary-server/backups:/data/backups \
diary-server:0.1
```

## 代码架构

### 核心模块结构
- **Controller层**: REST API接口控制器
  - `DiaryController`: 日记CRUD操作（基于JSON文件存储）
  - `DatabaseDiaryController`: 数据库版本的日记操作
  - `TodoController`: AI待做事项管理
  - `BackupController`: 备份功能
  - `ImageController`: 图片上传和查看

- **Service层**: 业务逻辑服务
  - `DiaryDatabaseService`: 日记数据库操作服务
  - `TodoAnalysisService`: AI分析和待做事项生成服务
  - `BackupService`: 备份服务
  - `BackupQueueService`: 备份队列管理

- **DAO层**: 数据访问对象
  - `DiaryDao/DiaryDaoImpl`: 日记数据访问
  - `TodoItemDao/TodoItemDaoImpl`: 待做事项数据访问
  - 使用MyBatis进行ORM映射

- **Entity层**: 实体类
  - `Diary`: 日记实体
  - `TodoItem`: 待做事项实体

### 关键配置
- **认证机制**: 使用JWT token认证，通过`TokenInterceptor`拦截器处理
- **文件存储**: 图片存储在本地文件系统，路径可配置
- **数据存储**: 支持双存储模式 - JSON文件存储（原有）和MySQL数据库存储（新增）
- **定时任务**: 
  - 每日7点自动备份数据库
  - 每日凌晨1点AI分析用户日记生成待做事项
  - 每日3点清理临时备份文件

### AI分析功能架构
- **外部API集成**: 支持通过HTTP调用外部AI服务（如Dify、n8n等工作流平台）
- **降级策略**: 当外部AI服务不可用时，自动使用模拟数据
- **数据流向**: 日记内容 → AI分析 → 待做事项生成 → 数据库存储

## 环境配置

### 必需的环境变量
```properties
# JWT配置（必填）
JWT_SECRET=your-jwt-secret-here

# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/diary_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8
DB_USERNAME=diary
DB_PASSWORD=diary123

# 微信小程序配置（如果使用）
WX_APPID=your-wx-appid
WX_SECRET=your-wx-secret

# 安卓app配置（如果使用）
ANDROID_APPID=your-android-appid

# AI分析配置（可选）
AI_API_URL=http://your-ai-service-url
AI_API_KEY=your-ai-api-key
AI_MODEL=gpt-3.5-turbo
TODO_ANALYSIS_ENABLED=true
TODO_ANALYSIS_CRON=0 0 1 * * ?
```

### 数据库初始化
- 数据库表结构定义在 `src/main/resources/sql/diary_schema.sql`
- Docker启动MySQL时会自动执行初始化脚本
- 支持从旧的JSON文件格式一次性迁移到数据库

## 开发注意事项

### 数据库操作
- 使用MyBatis作为ORM框架，XML映射文件位于 `src/main/resources/mapper/`
- 实体类使用Lombok简化getter/setter
- 所有数据表都有逻辑删除字段`deleted`

### API接口规范
- 所有API返回格式统一使用 `ApiResponse` 封装
- 认证接口路径: `/api/**` （需要token）
- 排除认证的路径: `/wx/login`, `/api/images/view/**`

### 文件存储规则
- 图片存储路径格式: `{storage-path}/{openid}/{年份}/{日期}_{时间}_{diaryId}.{扩展名}`
- 备份文件存储在单独的目录，保留最近3个版本

### 定时任务配置
- 使用`@EnableScheduling`启用定时任务
- Cron表达式可通过配置文件调整
- 重要定时任务有: 数据备份、AI分析、文件清理