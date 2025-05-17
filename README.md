# diary-server
个人日记服务端，功能非常简单，只实现了token认证、写日记和查看历史日记的功能，使用Spring Boot。日记数据
存储在本地，图片存储在本地，纯个人使用已经够了。

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


# 使用方法

## 直接打包
```bash
# 打包
./mvnw package -DskipTests

# 运行
java -jar target/diary-server-0.0.1-SNAPSHOT.jar
```

## 构建docker镜像

### 构建docker镜像
```bash
docker build -t diary-server:0.1 .
```

###  创建本地映射目录
```bash
mkdir -p /data/diary-server/images
mkdir -p /data/diary-server/diary
mkdir -p /data/diary-server/backups
```

### .env文件
敏感的信息放在.env文件中，docker run时会自动加载, 文件模板如下
```bash
WX_APPID=xxxxx
WX_SECRET=xxxxxx
JWT_SECRET=xxxx
ANDROID_APPID=xxxx
```
配置说明：
如果使用微信小程序作为前端，必须需要配置WX_APPID和WX_SECRET
如果使用安卓app作为前端，必须需要配置ANDROID_APPID
JWT_SECRET是jwt的密钥，请用src/main/java/wy/diary/server/util/JwtUtil.java 代码生成

### 运行docker镜像
```bash
docker run -d --name diary-server \
--env-file .env \
-p 7080:7080 \
-v /data/diary-server/images:/data/images \
-v /data/diary-server/diary:/data/diary \
-v /data/diary-server/backups:/data/backups \
diary-server:0.1
```



# 数据存储说明

## 日记数据
日记数据存储在/data/diary-server/diary目录下, 以json文件的形式存储, 
文件名微信小程序的 {openid}-年份.json, 例如： xxxxx-xxxxx-QzN1atAd8hc-2025.json

文件内容如下：
```json
{
  "2025-05-1301": {
    "editorContent": "记录今天的心情...",
    "address": "未选择地址",
    "createTime": "Tue May 13 22:03:48 CST 2025",
    "diaryId": "4118b233-560e-4c16-8f21-0f96cfb75e99",
    "logWeek": "星期二",
    "imageUrls": [
      "diary-server/images/{openid}/2025/20250513_140340_4118b233-560e-4c16-8f21-0f96cfb75e99.jpg"
    ],
    "logLunar": "四月十六",
    "logTime": "2025-05-13"
  }
}
```
一年就365天，所以每个文件记录数不会太多，就算一天写了多篇日记也不会太大。
key是日期+序号，同一天的日记会有多个key, 例如：2025-05-1301, 2025-05-1302, 2025-05-1303
每篇日记都会有一个唯一的diaryId, 这个id是uuid生成的, 例如：4118b233-560e-4c16-8f21-0f96cfb75e99

## 图片数据
图片存储在/data/diary-server/images目录下
以openid/年份/日期+日记id 的形式存储, 例如：diary-server/images/{openid}/2025/20250513_140340_4118b233-560e-4c16-8f21-0f96cfb75e99.jpg
每篇日记都会有一个唯一的id, 这个id是uuid生成的, 例如：4118b233-560e-4c16-8f21-0f96cfb75e99





