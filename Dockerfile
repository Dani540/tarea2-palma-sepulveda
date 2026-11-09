# Etapa de compilación: genera un JRE reducido con jlink y el JAR de la aplicación con Maven.
# Las dependencias de Maven se descargan en una capa propia, que se reutiliza mientras pom.xml no cambie.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
RUN jlink \
      --add-modules java.base,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.security.jgss,java.sql,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs \
      --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
      --output /jre
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && cp target/*.jar app.jar

# Etapa de ejecución: JRE reducido y JAR de la aplicación, ejecutados por un usuario sin privilegios.
# El archivo de compras no forma parte de la imagen; se monta en la ruta indicada por CSV_PATH.
FROM alpine:3.24
ENV JAVA_HOME=/opt/java/jre \
    PATH=/opt/java/jre/bin:$PATH
COPY --from=build /jre /opt/java/jre
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build --chown=app:app /build/app.jar app.jar

ARG APP_PORT=3001
ENV APP_ENV=production \
    APP_PORT=${APP_PORT} \
    CSV_PATH=/data/compras.csv
EXPOSE ${APP_PORT}

USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
