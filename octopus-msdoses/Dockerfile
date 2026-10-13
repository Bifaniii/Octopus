FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8085
# Memória: na EC2 o container sobe com teto de 400 MB (--memory no pipeline.yml), e a JVM se ajusta a ele.
# Heap em até metade do teto; o resto fica para metaspace, código compilado e threads. SerialGC é o
# coletor que menos memória gasta, adequado a um serviço pequeno.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=48m", "-Xss512k", "-XX:+UseSerialGC", "-jar", "app.jar"]
