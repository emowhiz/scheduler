FROM alpine/java:21-jdk AS runtime

WORKDIR /app

COPY target/scheduler-0.0.1-SNAPSHOT.jar /app/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/scheduler-0.0.1-SNAPSHOT.jar"]
