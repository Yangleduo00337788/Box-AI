# Multi-stage build for Box server (run from repo root: docker build -f box-server/Dockerfile .)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY box-server/pom.xml box-server/pom.xml
COPY box-server/box-common box-server/box-common
COPY box-server/box-security box-server/box-security
COPY box-server/box-infrastructure box-server/box-infrastructure
COPY box-server/box-domain box-server/box-domain
COPY box-server/box-application box-server/box-application
COPY box-server/box-modules box-server/box-modules
COPY box-server/box-bootstrap box-server/box-bootstrap
RUN mvn -f box-server/pom.xml -pl box-bootstrap -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S box && adduser -S box -G box
COPY --from=build /src/box-server/box-bootstrap/target/box-bootstrap-*.jar /app/app.jar
USER box
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
