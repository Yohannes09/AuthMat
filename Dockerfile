FROM maven:3.9.9 as builder
WORKDIR /build
ARG REPO_USERNAME
ARG REPO_TOKEN
RUN mkdir -p /root/.m2 && \
    echo "<settings><servers><server><id>github</id><username>${REPO_USERNAME}</username><password>${REPO_TOKEN}</password></server></servers></settings>" > /root/.m2/settings.xml

# COPY <local machine> <docker working dir>
COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jre-alpine as runtime
RUN adduser -D -s /bin/sh appuser
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]