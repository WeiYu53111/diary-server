# diary-server
个人日记服务端，功能非常简单，只实现了token认证、写日记和查看历史日记的功能，使用Spring Boot。日记数据
存储在MySQL数据库中，图片存储在本地，纯个人使用已经够了。

本来是想安卓集成微信登录的，结果要求app审核，太麻烦了，所以就直接写死了token，安卓app走绿色通道。


前端地址如下：

小程序  
https://github.com/WeiYu53111/diary-wxapp  
安卓app  
https://github.com/WeiYu53111/diary-android.git  


## 功能
- [x] token认证,使用jwt(微信小程序登录)
- [x] 写日记
- [x] 历史日记
- [x] 定时备份
- [x] 个人备份、备份文件下载
- [x] MySQL数据库存储
- [x] **AI智能待做事项分析** ⭐ 新增功能


# 使用方法

## 前置要求
- Java 17+
- MySQL 8.0+
- Docker (可选)

## Docker Compose部署

### 快速开始
```bash
# 1. 复制环境变量配置文件
cp .env.example .env

# 2. 编辑.env文件，配置你的实际参数
vim .env

# 3. 一键启动所有服务（MySQL + 应用服务）
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看应用日志
docker-compose logs -f diary-server
```

### 环境变量配置
编辑`.env`文件，重要配置项如下：
```bash
# 数据库配置
MYSQL_ROOT_PASSWORD=your_strong_root_password
MYSQL_PASSWORD=your_strong_db_password

# JWT密钥配置（必填，建议64位以上随机字符串）
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here

# 微信小程序配置（如果使用）
WX_APPID=your_wechat_miniprogram_appid
WX_SECRET=your_wechat_miniprogram_secret

# 安卓app配置（如果使用）
ANDROID_APPID=your_android_app_id

# AI分析配置（可选）
AI_API_URL=                       # AI API URL（留空使用模拟数据）
AI_API_KEY=                       # AI API Key
TODO_ANALYSIS_ENABLED=true       # 是否启用AI分析功能
```

### 常用操作
```bash
# 停止所有服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs diary-server
docker-compose logs mysql

# 更新应用（重新构建）
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 数据持久化
所有重要数据都通过Docker卷进行持久化：
- MySQL数据库文件
- 用户上传的图片
- 数据备份文件
- 日记数据文件

## 传统Docker部署

### 1. 启动MySQL数据库
```bash
docker run -d --name diary-mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
-e MYSQL_DATABASE=diary_db \
-e MYSQL_USER=diary \
-e MYSQL_PASSWORD=diary123 \
-p 3306:3306 \
-v $(pwd)/src/main/resources/sql:/docker-entrypoint-initdb.d \
-v $(pwd)/mysql:/var/lib/mysql \
mysql:8.0 \
--character-set-server=utf8mb4 \
--collation-server=utf8mb4_unicode_ci
```

### 2. 构建并运行应用
```bash
# 构建镜像
docker build -t diary-server:0.1 .

# 创建数据目录
mkdir -p /data/diary-server/{images,backups}

# 运行应用
docker run -d --name diary-server \
--env-file .env \
--link diary-mysql:mysql \
-p 7080:7080 \
-v /data/diary-server/images:/data/images \
-v /data/diary-server/backups:/data/backups \
diary-server:0.1
```

# 数据存储说明

## 日记数据
日记数据存储在MySQL数据库中的`diary`表中，包含以下字段：

- `id`: 主键ID（自增）
- `diary_id`: 日记唯一标识（UUID）
- `open_id`: 用户OpenID
- `editor_content`: 日记内容（富文本）
- `create_time`: 创建时间
- `log_time`: 记录时间（日期格式：YYYY-MM-DD）
- `log_week`: 记录星期
- `log_lunar`: 农历日期
- `address`: 地址位置
- `image_urls`: 图片URL列表（JSON格式）
- `db_create_time`: 数据库创建时间
- `db_update_time`: 数据库更新时间
- `deleted`: 逻辑删除标识（0-未删除，1-已删除）

数据库表结构定义在 `src/main/resources/sql/diary_schema.sql` 中，容器启动时会自动执行初始化脚本。

## 待做事项数据 ⭐ 新增
待做事项数据存储在MySQL数据库中的`todo_item`表中，包含以下字段：

- `id`: 主键ID（自增）
- `open_id`: 用户OpenID
- `title`: 事项标题
- `description`: 事项描述
- `category`: 事项类别（PROJECT-项目，HABIT-习惯养成）
- `priority`: 优先级（LOW/MEDIUM/HIGH/URGENT）
- `status`: 状态（PENDING/IN_PROGRESS/COMPLETED/CANCELLED）
- `due_date`: 截止日期
- `estimated_duration`: 预计完成时长（分钟）
- `source_diary_ids`: 来源日记ID列表（JSON格式）
- `ai_confidence`: AI分析置信度(0.00-1.00)
- `ai_analysis_result`: AI分析详细结果（JSON格式）
- `created_date`: 创建日期
- `db_create_time`: 数据库创建时间
- `db_update_time`: 数据库更新时间
- `deleted`: 逻辑删除标识（0-未删除，1-已删除）

## 图片数据
图片存储在/data/diary-server/images目录下
以openid/年份/日期+日记id 的形式存储, 例如：diary-server/images/{openid}/2025/20250513_140340_4118b233-560e-4c16-8f21-0f96cfb75e99.jpg
每篇日记都会有一个唯一的id, 这个id是uuid生成的, 例如：4118b233-560e-4c16-8f21-0f96cfb75e99

## 备份数据
系统支持定时备份和手动备份：
- 定时备份：每天早上7点自动备份数据库数据到 `/data/diary-server/backups/` 目录
- 手动备份：用户可以通过接口触发个人数据备份，生成包含个人所有日记和图片的压缩文件
- 备份文件保留最近3个版本，超过数量会自动删除最早的备份

---

# AI智能待做事项分析功能 ⭐ 新增

基于用户日记内容，智能分析并生成个性化的待做事项列表，帮助用户将想法转化为行动。

## 功能特性

- **智能分析**：基于用户日记内容，通过AI分析提取潜在的待做事项
- **事项分类**：自动将事项分为项目（PROJECT）和习惯养成（HABIT）两大类
- **优先级管理**：智能设置事项优先级（LOW/MEDIUM/HIGH/URGENT）
- **定时分析**：每日凌晨1点自动分析活跃用户的日记，生成待做事项
- **手动触发**：支持手动触发分析功能

## API接口

### 待做事项管理

- `GET /api/todo/list?openId={openId}` - 查询用户待做事项列表
- `GET /api/todo/list/category?openId={openId}&category={category}` - 根据类别查询
- `GET /api/todo/list/status?openId={openId}&status={status}` - 根据状态查询
- `GET /api/todo/list/dateRange?openId={openId}&startDate={date}&endDate={date}` - 根据日期范围查询
- `GET /api/todo/{id}` - 查询待做事项详情
- `POST /api/todo/create` - 创建待做事项
- `PUT /api/todo/update` - 更新待做事项
- `PUT /api/todo/updateStatus?id={id}&status={status}` - 更新状态
- `DELETE /api/todo/{id}` - 删除待做事项

### AI分析功能

- `POST /api/todo/analyze?openId={openId}` - 手动触发AI分析
- `POST /api/todo/analyze/all` - 触发全局分析任务

## 定时任务

系统会在每日凌晨1点自动执行分析任务：
- 获取最近30天内有日记的活跃用户
- 分析每个用户最近7天的日记内容
- 生成相应的待做事项并保存到数据库

## AI分析实现说明

系统支持通过HTTP请求调用外部AI服务（如Dify、n8n等工作流平台）进行日记内容分析。当外部AI服务不可用时，会自动降级使用模拟数据。

### 外部AI服务集成
- **HTTP调用**：支持POST请求调用外部AI分析服务
- **数据格式**：发送JSON格式的日记内容给外部服务
- **错误处理**：网络异常或服务不可用时自动使用模拟数据
- **超时控制**：设置合理的请求超时时间

### 请求格式
发送给外部AI服务的数据格式：
```json
{
  "diaries": [
    {
      "date": "2025-01-15",
      "content": "日记内容..."
    }
  ],
  "analysisType": "todo_extraction",
  "categories": ["PROJECT", "HABIT"]
}
```

### 响应格式
期望外部AI服务返回的数据格式：
```json
{
  "todos": [
    {
      "title": "待做事项标题",
      "description": "详细描述",
      "category": "PROJECT或HABIT",
      "priority": "优先级",
      "estimatedDuration": 120,
      "dueDate": "2025-01-20",
      "confidence": 0.85
    }
  ]
}
```

## 配置选项

在`application.properties`中可配置以下参数：

```properties
# AI分析配置
ai.api.url=                    # AI API URL（留空使用模拟数据）
ai.api.key=                    # AI API Key
ai.model=gpt-3.5-turbo        # AI模型

# 待做事项分析配置
todo.analysis.enabled=true                 # 是否启用分析功能
todo.analysis.cron=0 0 1 * * ?             # 定时任务执行时间
```

## 使用示例

```bash
# 查询用户待做事项
curl "http://localhost:7080/api/todo/list?openId=user123"

# 手动触发AI分析
curl -X POST "http://localhost:7080/api/todo/analyze?openId=user123"

# 创建待做事项
curl -X POST "http://localhost:7080/api/todo/create" \
  -H "Content-Type: application/json" \
  -d '{
    "openId": "user123",
    "title": "完成项目报告",
    "description": "整理本周工作内容并提交报告",
    "category": "PROJECT",
    "priority": "HIGH"
  }'
```





