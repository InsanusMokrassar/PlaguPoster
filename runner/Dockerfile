FROM bellsoft/liberica-openjdk-alpine:19

USER 1000

ENTRYPOINT ["/plaguposter.runner/bin/plaguposter.runner", "/config.json"]

ADD ./build/distributions/plaguposter.runner.tar /
