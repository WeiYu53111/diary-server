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


# 使用方法

## 前置要求
- Java 17+
- MySQL 8.0+
- Docker (可选)

## 直接打包运行

### 1. 启动MySQL数据库
```bash
# 启动MySQL容器，并初始化数据库
docker run -d --name diary-mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
-e MYSQL_DATABASE=diary_db \
-e MYSQL_USER=diary \
-e MYSQL_PASSWORD=diary123 \
-p 3306:3306 \
-v $(pwd)/src/main/resources/sql:/docker-entrypoint-initdb.d \
-v /data/mysql:/var/lib/mysql \
mysql:8.0 \
--character-set-server=utf8mb4 \
--collation-server=utf8mb4_unicode_ci
```

### 2. 打包运行应用
```bash
# 打包
./mvnw package -DskipTests

# 运行
java -jar target/diary-server-0.0.1-SNAPSHOT.jar
```

## Docker部署

### 1. 启动MySQL数据库
```bash
# 启动MySQL容器，并初始化数据库
docker run -d --name diary-mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
-e MYSQL_DATABASE=diary_db \
-e MYSQL_USER=diary \
-e MYSQL_PASSWORD=diary123 \
-p 3306:3306 \
-v $(pwd)/src/main/resources/sql:/docker-entrypoint-initdb.d \
-v $(pwd)/mysql:/var/lib/mysql \
mysql:8.4 \
--character-set-server=utf8mb4 \
--collation-server=utf8mb4_unicode_ci
```

### 2. 构建应用docker镜像
```bash
docker build -t diary-server:0.1 .
```

### 3. 创建本地映射目录
```bash
mkdir -p /data/diary-server/images
mkdir -p /data/diary-server/backups
```

### 4. 配置环境变量文件
敏感的信息放在.env文件中，docker run时会自动加载, 文件模板如下
```bash
# 微信小程序配置（如果使用微信小程序前端）
WX_APPID=xxxxx
WX_SECRET=xxxxxx

# 安卓app配置（如果使用安卓app前端）
ANDROID_APPID=xxxx

# JWT密钥配置（必填）
JWT_SECRET=xxxx

# 数据库配置
DB_URL=jdbc:mysql://diary-mysql:3306/diary_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8
DB_USERNAME=diary
DB_PASSWORD=diary123
```

配置说明：
- 如果使用微信小程序作为前端，必须需要配置WX_APPID和WX_SECRET
- 如果使用安卓app作为前端，必须需要配置ANDROID_APPID
- JWT_SECRET是jwt的密钥，请用src/main/java/wy/diary/server/util/JwtUtil.java 代码生成
- 数据库配置中的主机名要使用Docker容器名`diary-mysql`

### 5. 运行应用docker镜像
```bash
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

## 图片数据
图片存储在/data/diary-server/images目录下
以openid/年份/日期+日记id 的形式存储, 例如：diary-server/images/{openid}/2025/20250513_140340_4118b233-560e-4c16-8f21-0f96cfb75e99.jpg
每篇日记都会有一个唯一的id, 这个id是uuid生成的, 例如：4118b233-560e-4c16-8f21-0f96cfb75e99

## 备份数据
系统支持定时备份和手动备份：
- 定时备份：每天早上7点自动备份数据库数据到 `/data/diary-server/backups/` 目录
- 手动备份：用户可以通过接口触发个人数据备份，生成包含个人所有日记和图片的压缩文件
- 备份文件保留最近3个版本，超过数量会自动删除最早的备份





