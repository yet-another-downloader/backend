FROM openjdk:11-jre-slim
VOLUME /tmp

ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ADD youtube-dl /bin

RUN apt install python ffmpeg && youtube-dl --version

EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=2s --retries=12 --start-period=320s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
