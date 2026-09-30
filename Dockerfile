# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
# cached layer: re-runs only when pom.xml changes
RUN mvn -B -q dependency:go-offline
COPY src ./src
# tests run locally; keeps image builds short
RUN mvn -B -q package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/target/app.jar app.jar
# Sized for a small container (512 MB RAM, 0.1 CPU): capped heap, small GC, faster startup
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"
USER nobody
ENTRYPOINT ["java", "-jar", "app.jar"]
