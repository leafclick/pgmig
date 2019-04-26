FROM graalvm-ce:1.0.0-rc16 as BASE

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
COPY --from=BASE /lib/x86_64-linux-gnu/libtinfo.so.5 /lib/x86_64-linux-gnu/libtinfo.so.5
COPY --from=BASE /lib/x86_64-linux-gnu/libdl.so.2 /lib/x86_64-linux-gnu/libdl.so.2
COPY --from=BASE /lib/x86_64-linux-gnu/libc.so.6 /lib/x86_64-linux-gnu/libc.so.6
COPY --from=BASE /lib/x86_64-linux-gnu/libcrypt.so.1 /lib/x86_64-linux-gnu/libcrypt.so.1
COPY --from=BASE /lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 /lib/x86_64-linux-gnu/ld-linux-x86-64.so.2
COPY --from=BASE /lib/x86_64-linux-gnu/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1
COPY --from=BASE /lib/x86_64-linux-gnu/librt.so.1 /lib/x86_64-linux-gnu/librt.so.1
COPY --from=BASE /lib/x86_64-linux-gnu/libnss_compat.so.2 /lib/x86_64-linux-gnu/libnss_compat.so.2
COPY --from=BASE /lib/x86_64-linux-gnu/libnss_files.so.2 /lib/x86_64-linux-gnu/libnss_files.so.2
COPY --from=BASE /lib/x86_64-linux-gnu/libnss_nis.so.2 /lib/x86_64-linux-gnu/libnss_nis.so.2
COPY --from=BASE /lib/x86_64-linux-gnu/libnsl.so.1 /lib/x86_64-linux-gnu/libnsl.so.1
COPY --from=BASE /lib/x86_64-linux-gnu/libpthread.so.0 /lib/x86_64-linux-gnu/libpthread.so.0
COPY --from=BASE /lib/x86_64-linux-gnu/libselinux.so.1 /lib/x86_64-linux-gnu/libselinux.so.1
COPY --from=BASE /lib/x86_64-linux-gnu/libgcc_s.so.1 /lib/x86_64-linux-gnu/libgcc_s.so.1
COPY --from=BASE /lib/x86_64-linux-gnu/libpcre.so.3 /lib/x86_64-linux-gnu/libpcre.so.3
COPY --from=BASE /usr/lib/x86_64-linux-gnu/nss/libfreebl3.so /usr/lib/x86_64-linux-gnu/libfreebl3.so

COPY --from=BASE /bin/sh /bin/sh
COPY --from=BASE /bin/mkdir /bin/mkdir
COPY --from=BASE /bin/chmod /bin/chmod
COPY --from=BASE /bin/ls /bin/ls
COPY --from=BASE /bin/cat /bin/cat

RUN mkdir /tmp
RUN chmod 1777 /tmp

ENTRYPOINT ["/pgmig"]
CMD ["-Djava.library.path=/usr/lib"]
