FROM openjdk:11.0.2-jre-stretch
VOLUME /tmp

ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ADD youtube-dl /bin
ADD devops/entrypoint.sh entrypoint.sh

RUN apt update && apt install -y python ffmpeg cifs-utils rpm && youtube-dl --version

EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=2s --retries=12 --start-period=320s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
