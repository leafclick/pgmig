## Create Docker Image

To create the graal docker image

    wget 'https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.1/graalvm-ce-java11-linux-amd64-19.3.1.tar.gz'
    docker build -t graalvm-ce:19.3.1 .
