FROM debian:10-slim

ENV GRAALVM_PKG=graalvm-ce-java11-linux-amd64-19.3.0.tar.gz \
    GRAAL_HOME=/opt/graalvm-ce-java11-19.3.0 \
    JAVA_HOME=$GRAAL_HOME/jre \
    PATH=$GRAAL_HOME/bin:$PATH

ADD $GRAALVM_PKG /opt/

RUN apt-get update && apt-get install -y apt-utils gcc zlib1g-dev libnss3-dev

RUN update-alternatives --install /usr/bin/java java $GRAAL_HOME/bin/java 1 && \
    update-alternatives --install /usr/bin/javac javac $GRAAL_HOME/bin/javac 1

RUN $GRAAL_HOME/bin/gu install native-image

RUN update-alternatives --install /usr/bin/native-image native-image $GRAAL_HOME/bin/native-image 1
