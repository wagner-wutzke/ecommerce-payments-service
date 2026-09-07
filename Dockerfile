FROM bellsoft/liberica-openjre-alpine:21 AS builder

WORKDIR /app

ARG JAR_FILE=target/payments-service-*.jar
COPY ${JAR_FILE} app.jar

RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-alpine:21 AS runtime

WORKDIR /app

RUN addgroup -S spring \
    && adduser -S -D -H -G spring spring \
    && mkdir -p /app/data \
    && chown -R spring:spring /app

COPY --from=builder --chown=spring:spring /app/extracted/dependencies/lib/ /app/lib/
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/lib/ /app/lib/
COPY --from=builder --chown=spring:spring /app/extracted/application/*.jar /app/app.jar

USER spring

EXPOSE 9030

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
