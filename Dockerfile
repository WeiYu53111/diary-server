# 使用OpenJDK 17作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 设置构建参数
ARG WX_APPID
ARG WX_SECRET
ARG JWT_SECRET

# 设置环境变量
ENV WX_APPID=${WX_APPID}
ENV WX_SECRET=${WX_SECRET}
ENV JWT_SECRET=${JWT_SECRET}
ENV IMAGE_STORAGE_PATH=/data/images/
ENV DIARY_STORAGE_PATH=/data/diary/
ENV TEMP_IMAGES_PATH=/tmp/images/

# 复制Maven包装器和pom.xml
COPY mvnw .
COPY pom.xml .
COPY .mvn/ .mvn/

# 为mvnw脚本添加执行权限
RUN chmod +x ./mvnw

# 复制源代码
COPY src ./src

# 构建应用
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -DskipTests

# 构建应用
# RUN ./mvnw package -DskipTests

# 创建数据目录
RUN mkdir -p /data/images /data/diary /tmp/images

# 暴露端口（根据application.properties中的配置）
EXPOSE 7080

# 运行应用
ENTRYPOINT ["java", "-jar", "target/diary-server-0.0.1-SNAPSHOT.jar"]
