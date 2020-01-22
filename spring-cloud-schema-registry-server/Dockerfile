FROM springcloud/openjdk:latest

ARG JAR_FILE

ADD target/${JAR_FILE} spring-cloud-schema-registry.jar

ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /spring-cloud-schema-registry.jar
