FROM oracle/graalvm-ce:20.2.0-java11 as BASE

ENV GRAAL_HOME=/opt/graalvm-ce-java11-20.2.0/

RUN gu install native-image
RUN update-alternatives --install /usr/bin/native-image native-image $GRAAL_HOME/bin/native-image 1

RUN mkdir /target
WORKDIR /target

COPY target/uberjar/pgmig.jar .
COPY graal.json .
COPY create-image.sh .

RUN ./create-image.sh pgmig.jar

FROM scratch

COPY --from=BASE /target/pgmig /pgmig
COPY resources resources

COPY --from=BASE /lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2
COPY --from=BASE /lib64/libtinfo.so.5 /lib64/libtinfo.so.5
COPY --from=BASE /lib64/libdl.so.2 /lib64/libdl.so.2
COPY --from=BASE /lib64/libc.so.6 /lib64/libc.so.6
COPY --from=BASE /lib64/libm.so.6 /lib64/libm.so.6
COPY --from=BASE /lib64/libcrypt.so.1 /lib64/libcrypt.so.1
COPY --from=BASE /lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2
COPY --from=BASE /lib64/librt.so.1 /lib64/librt.so.1
COPY --from=BASE /lib64/libnss_compat.so.2 /lib64/libnss_compat.so.2
COPY --from=BASE /lib64/libnss_files.so.2 /lib64/libnss_files.so.2
COPY --from=BASE /lib64/libnss_nis.so.2 /lib64/libnss_nis.so.2
COPY --from=BASE /lib64/libnsl.so.1 /lib64/libnsl.so.1
COPY --from=BASE /lib64/libpthread.so.0 /lib64/libpthread.so.0
COPY --from=BASE /lib64/libselinux.so.1 /lib64/libselinux.so.1
COPY --from=BASE /lib64/libgcc_s.so.1 /lib64/libgcc_s.so.1
COPY --from=BASE /lib64/libpcre.so.1 /lib64/libpcre.so.1
COPY --from=BASE /usr/lib64/nss/libnssckbi.so /usr/lib64/nss/libnssckbi.so
COPY --from=base /lib64/libstdc++.so.6 /lib64/libstdc++.so.6
COPY --from=BASE /lib64/libz.so.1.2.7 /lib64/libz.so.1

COPY --from=BASE /bin/sh /bin/sh
COPY --from=BASE /bin/mkdir /bin/mkdir
COPY --from=BASE /bin/chmod /bin/chmod
COPY --from=BASE /bin/ls /bin/ls
COPY --from=BASE /bin/cat /bin/cat

RUN mkdir /tmp
RUN chmod 1777 /tmp

ENTRYPOINT ["/pgmig"]
CMD ["-Djava.library.path=/usr/lib"]
