# =============================================================================
# mobile-agent — image base sin Android SDK (TASK-I04, RFC-AGENT-01)
# =============================================================================
# Para mobile real, montar el Android SDK como volumen y exponer ANDROID_HOME,
# o derivar otra image FROM esta + apt-install android-sdk + adb. La capa que
# define la versión de Android queda fuera del default deployment para no
# inflar la imagen base de quienes sólo corren web/HTTP/DB.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

ARG JAR_FILE=build/libs/mobile-agent-*.jar
WORKDIR /opt/mobile-agent

COPY ${JAR_FILE} mobile-agent.jar

# El workspace efímero vive en /tmp/mobile-agent — montable como volumen si se
# necesita persistencia entre reinicios.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.io.tmpdir=/tmp"
ENV SERVER_PORT=8090

EXPOSE 8090

# healthcheck contra el actuator — wget viene con alpine.
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/mobile-agent/mobile-agent.jar --server.port=${SERVER_PORT}"]
