# One image for both app and worker; profile chosen at runtime via env.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test \
	&& cp $(ls build/libs/weather-insights-*.jar | grep -v plain | head -1) /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Corp TLS inspection CA (Flipkart) — required for outbound HTTPS from this network.
COPY certs/FlipkartRootCA.cer /tmp/FlipkartRootCA.cer
RUN keytool -importcert -noprompt \
	-alias flipkart-root-ca \
	-file /tmp/FlipkartRootCA.cer \
	-keystore "$JAVA_HOME/lib/security/cacerts" \
	-storepass changeit \
	&& rm /tmp/FlipkartRootCA.cer

COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
