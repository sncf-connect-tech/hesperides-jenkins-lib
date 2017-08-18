FROM gradle:4.0.1-jre8-alpine

COPY ./src .
COPY ./test .
COPY ./build.gradle .
COPY ./codenarc_rules.groovy .

CMD ["gradle", "test", "--debug", "--stacktrace"]
