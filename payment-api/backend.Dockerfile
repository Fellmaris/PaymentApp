FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:resolve

COPY src ./src

RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ARG APP_USER=appuser
ARG APP_GROUP=appgroup
ARG UID=1001
ARG GID=1001

RUN groupadd -g ${GID} ${APP_GROUP} && \
    useradd -u ${UID} -g ${APP_GROUP} -m -s /bin/sh ${APP_USER}

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

RUN chown ${APP_USER}:${APP_GROUP} app.jar

USER ${APP_USER}

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]