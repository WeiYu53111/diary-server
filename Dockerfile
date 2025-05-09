# 使用OpenJDK 17作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制Maven包装器和pom.xml
COPY mvnw .
COPY pom.xml .
COPY .mvn/ .mvn/

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw package -DskipTests

# 暴露端口（根据application.properties中的配置）
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-jar", "target/diary-server-0.0.1-SNAPSHOT.jar"]
