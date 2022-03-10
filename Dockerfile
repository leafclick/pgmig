FROM ghcr.io/graalvm/graalvm-ce:21.3.1 as BASE

ENV GRAAL_HOME=/opt/graalvm-ce-java11-21.3.1

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

COPY --from=BASE /etc/alternatives/libnssckbi.so.x86_64 /lib64/libnssckbi.so
COPY --from=BASE /lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2
COPY --from=BASE /lib64/libacl.so.1 /lib64/libacl.so.1
COPY --from=BASE /lib64/libattr.so.1 /lib64/libattr.so.1
COPY --from=BASE /lib64/libblkid.so.1 /lib64/libblkid.so.1
COPY --from=BASE /lib64/libcap.so.2 /lib64/libcap.so.2
COPY --from=BASE /lib64/libcrypt.so.1 /lib64/libcrypt.so.1
COPY --from=BASE /lib64/libc.so.6 /lib64/libc.so.6
COPY --from=BASE /lib64/libdl.so.2 /lib64/libdl.so.2
COPY --from=BASE /lib64/libgcc_s.so.1 /lib64/libgcc_s.so.1
COPY --from=BASE /lib64/libm.so.6 /lib64/libm.so.6
COPY --from=BASE /lib64/libmount.so.1 /lib64/libmount.so.1
COPY --from=BASE /lib64/libnss_compat.so.2 /lib64/libnss_compat.so.2
COPY --from=BASE /lib64/libnss_dns.so.2 /lib64/libnss_dns.so.2
COPY --from=BASE /lib64/libnss_files.so.2 /lib64/libnss_files.so.2
COPY --from=BASE /lib64/libnss_myhostname.so.2 /lib64/libnss_myhostname.so.2
COPY --from=BASE /lib64/libnss_resolve.so.2 /lib64/libnss_resolve.so.2
COPY --from=BASE /lib64/libnss_systemd.so.2 /lib64/libnss_systemd.so.2
COPY --from=BASE /lib64/libpcre2-8.so.0 /lib64/libpcre2-8.so.0
COPY --from=BASE /lib64/libpcre.so.1 /lib64/libpcre.so.1
COPY --from=BASE /lib64/libpthread.so.0 /lib64/libpthread.so.0
COPY --from=BASE /lib64/libresolv.so.2 /lib64/libresolv.so.2
COPY --from=BASE /lib64/librt.so.1 /lib64/librt.so.1
COPY --from=BASE /lib64/libselinux.so.1 /lib64/libselinux.so.1
COPY --from=base /lib64/libstdc++.so.6 /lib64/libstdc++.so.6
COPY --from=BASE /lib64/libtasn1.so.6 /lib64/libtasn1.so.6
COPY --from=BASE /lib64/libtinfo.so.6 /lib64/libtinfo.so.6
COPY --from=BASE /lib64/libuuid.so.1 /lib64/libuuid.so.1
COPY --from=BASE /lib64/libz.so.1 /lib64/libz.so.1

COPY --from=BASE /bin/sh /bin/sh
COPY --from=BASE /bin/mkdir /bin/mkdir
COPY --from=BASE /bin/chmod /bin/chmod
COPY --from=BASE /bin/ls /bin/ls
COPY --from=BASE /bin/cat /bin/cat
COPY --from=BASE /usr/bin/coreutils /usr/bin/coreutils

RUN mkdir /tmp
RUN chmod 1777 /tmp

ENTRYPOINT ["/pgmig"]
CMD ["-Djava.library.path=/usr/lib"]
