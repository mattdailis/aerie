FROM eclipse-temurin:17-focal

COPY build/distributions/*.tar /usr/src/app/server.tar
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/scheduler-server"]
