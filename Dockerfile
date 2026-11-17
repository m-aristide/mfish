# Etape 1 : build avec le JDK 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests -Djacoco.skip=true package \
    && java -Djarmode=tools -jar target/erp-0.0.1-SNAPSHOT.jar extract --layers --destination target/extracted

# Etape 2 : image d'execution minimale, utilisateur non root
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home mfish
WORKDIR /app
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
USER mfish
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "erp-0.0.1-SNAPSHOT.jar"]
