FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "target/academic-platform-0.0.1-SNAPSHOT.jar"]
