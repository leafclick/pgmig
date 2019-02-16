FROM debian:9-slim

ENV GRAALVM_PKG=graalvm-ce-1.0.0-rc12-linux-amd64.tar.gz \
    GRAAL_HOME=/opt/graalvm-ce-1.0.0-rc12 \
    JAVA_HOME=$GRAAL_HOME/jre \
    PATH=$GRAAL_HOME/bin:$PATH

ADD $GRAALVM_PKG /opt/

RUN apt-get update && apt-get install -y apt-utils gcc zlib1g-dev libnss3-dev

RUN update-alternatives --install /usr/bin/java java $GRAAL_HOME/bin/java 1 && \
    update-alternatives --install /usr/bin/javac javac $GRAAL_HOME/bin/javac 1 && \
    update-alternatives --install /usr/bin/native-image native-image $GRAAL_HOME/bin/native-image 1
