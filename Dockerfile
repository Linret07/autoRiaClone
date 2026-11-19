FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src src
RUN mvn -q package -DskipTests
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/autoria-clone-1.0.0.jar app.jar
ENV UPLOAD_DIR=/data/uploads
VOLUME /data/uploads
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
