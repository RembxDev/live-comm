FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -DskipTests -pl backend/chat-service -am package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN adduser -D app && chown -R app /app
COPY --from=build /src/backend/chat-service/target/chat-service-*.jar app.jar
USER app
ENV JAVA_OPTS=""
EXPOSE 8082
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
