# Render bu dosyayla backend'i derler ve çalıştırır (render.yaml).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Ücretsiz katman 512 MB RAM ve düşük CPU: küçük heap, tek thread'li GC, hızlı açılış için C1 derleyicisi
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
