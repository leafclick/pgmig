## Create Docker Image

To create the graal docker image

	wget 'https://github.com/oracle/graal/releases/download/vm-1.0.0-rc12/graalvm-ce-1.0.0-rc12-linux-amd64.tar.gz'
	docker build -t graalvm-ce:1.0.0-rc12 .