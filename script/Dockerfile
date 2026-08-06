# =============================================
# 金融 AI Agent 多阶段构建 Dockerfile
# 阶段一：Maven 构建可执行 jar
# 阶段二：JRE 运行
# =============================================

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先复制 pom 文件，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
COPY financial-agent-domain/pom.xml financial-agent-domain/
COPY financial-agent-application/pom.xml financial-agent-application/
COPY financial-agent-infrastructure/pom.xml financial-agent-infrastructure/
COPY financial-agent-interfaces/pom.xml financial-agent-interfaces/
COPY financial-agent-starter/pom.xml financial-agent-starter/

# 下载依赖（利用缓存）
RUN mvn -q dependency:go-offline || true

# 复制源码并打包
COPY financial-agent-domain financial-agent-domain
COPY financial-agent-application financial-agent-application
COPY financial-agent-infrastructure financial-agent-infrastructure
COPY financial-agent-interfaces financial-agent-interfaces
COPY financial-agent-starter financial-agent-starter

RUN mvn -q clean package -DskipTests

# ---------------- 运行阶段 ----------------
FROM eclipse-temurin:21-jre
WORKDIR /home/apps

COPY --from=build /build/financial-agent-starter/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75 -Xlog:gc*:file=/home/apps/data/gc.log:utctime,pid,tid,level,tags:filecount=3,filesize=50M"
ENV SPRING_PROFILES_ACTIVE=aliyun

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
