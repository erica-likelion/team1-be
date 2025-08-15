# ===== 1단계: 빌드 =====
FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace

# 의존성 캐시 최적화
COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle ./gradle
RUN gradle --no-daemon build -x test || true

# 소스 복사 후 실제 빌드
COPY . .
RUN gradle --no-daemon clean bootJar -x test

# ===== 2단계: 런타임 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
EXPOSE 8080
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
