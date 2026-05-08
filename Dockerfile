FROM maven:3.9.9 as builder
WORKDIR /build
COPY . .
#RUN --mount=type=secret,id=maven_settings,target=/run/secrets/maven_settings \
#    mvn --settings /run/secrets/maven_settings clean package
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn clean package

FROM eclipse-temurin:25-jre-alpine as runtime
RUN adduser -D -s /bin/sh appuser
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]

# COPY syntax: COPY <host machine> <build working dir>