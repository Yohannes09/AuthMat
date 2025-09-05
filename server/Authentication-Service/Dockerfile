FROM maven:3.9.9 as builder
WORKDIR /build
# COPY <local machine> <docker working dir>
COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jre-alpine as runtime
RUN useradd -ms /bin/bash appuser
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]