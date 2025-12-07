FROM gradle:8.5-jdk21-alpine AS build
COPY --chown=gradle:gradle . /home/app
WORKDIR /home/app
RUN gradle build --no-daemon

FROM amazoncorretto:21-alpine
EXPOSE 8080
COPY --from=build /home/app/build/libs/lab4-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Xmx1024M", "-XX:MaxMetaspaceSize=1024M", "-jar", "app.jar"]
