FROM openjdk:11-jdk
ARG JAR_FILE=build/libs/*.jar
#COPY ${JAR_FILE} ./app.jar
COPY ${JAR_FILE} areastory-location.jar
#EXPOSE 8099
ENTRYPOINT ["java", "-jar", "areastory-location.jar"]