FROM gradle:4.0.1-jre8-alpine

COPY  src src
COPY  test test
COPY  build.gradle build.gradle
COPY  codenarc_rules.groovy codenarc_rules.groovy

CMD ["gradle", "test", "--debug", "--stacktrace"]
