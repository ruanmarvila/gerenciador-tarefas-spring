FROM maven:3.9.16-eclipse-temurin-25-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY src/ src/

RUN mvn clean package

FROM eclipse-temurin:25-jre-alpine AS jre-build

WORKDIR /app

COPY --from=builder app/target/gerenciador-tarefas-0.0.1-SNAPSHOT.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]