# syntax=docker/dockerfile:1
# Obraz backendu: budowa przez Maven Wrapper (tylko Maven Central, .mvn/settings.xml), uruchomienie na JRE 21.
# Kontekst budowania: katalog backend/.

FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
 && cp target/b2b-showroom-*.jar /app.jar

FROM eclipse-temurin:21-jre-noble
# Ubuntu (glibc) zamiast Alpine: renderer PDF korzysta z Java2D/fontów, a obraz JRE ma do tego komplet bibliotek.
RUN groupadd --system showroom && useradd --system --gid showroom --home /app showroom \
 && mkdir -p /app /data/storage && chown -R showroom:showroom /data
WORKDIR /app
COPY --from=build /app.jar app.jar
USER showroom
ENV STORAGE_LOCAL_ROOT=/data/storage \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
VOLUME ["/data"]
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
