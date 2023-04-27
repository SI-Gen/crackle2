FROM eclipse-temurin:11-jre-alpine

RUN apk --no-cache add --update bash openssl

COPY ./target/crackle2.jar /crackle2/crackle2.jar

WORKDIR /local

ENTRYPOINT ["java", "-jar", "/crackle2/crackle2.jar"]
CMD ["-?"]