# PGMig

Standalone PostgreSQL Migration Runner using [Migratus](https://github.com/yogthos/migratus).

## Changelog

There are **BREAKING changes** since release `0.4.0` changing most environment variables and some command line options
to match `psql`.

See [Changelog](CHANGELOG.md) for upgrade path hints.

## Build uberjar

    lein with-profile native do clean, test, uberjar

## Build Linux binary to be run locally

If you have already [GraalVM CE](https://github.com/graalvm/graalvm-ce-builds/releases) installed and properly set `GRAAL_HOME`, `JAVA_HOME` and `PATH` you can try to build and run the native image locally. You need **Graal 20.2.0** release with [native-image](https://www.graalvm.org/docs/reference-manual/native-image/) to build and deploy `pgmig` successfully.

From the project directory create the uberjar and run the `create-image.sh` helper script.

    lein with-profile native do clean, test, uberjar
    ./create-image.sh target/uberjar/pgmig.jar

You can use both `pgmig` command line options or environment variables when running it (env variables have the priority).

    ./pgmig -h localhost -p 5432 -d pgmig -U pgmig -P pgmig -r samples/db/migrations pending

Note that you need the same `.so` libraries that the native binary is linked to
on the target machine. Also keep in mind that you might need to add
`-Djava.library.path=<path-to-shared-libs>` as a `pgmig` option if it needs to load some
libraries dynamically (including shared libraries from the JDK itself).

## Build docker image

The docker image is based on the [GraalVM Community Edition Official Image](https://hub.docker.com/r/oracle/graalvm-ce/tags).  Once you have successfully built the uberjar you can create the migration runner image itself

    lein with-profile native do clean, test, uberjar
    docker build -t leafclick/pgmig .

# Usage

## Run uberjar with java 

To try out the basic functionality you can run the uberjar itself. To get help and available commands run

    java -jar target/uberjar/pgmig.jar 

To list pending migrations (uberjar)

    java -jar target/uberjar/pgmig.jar -h localhost -p 5432 -d pgmig -u pgmig -P pgmig -r samples/db/migrations pending

## Run Linux binary within docker container

To run the native image within the docker container set the environment, bind the migrations directory and specify the action

    docker run -ti \
               --rm \
               --mount "type=bind,src=$PWD/samples/db/migrations,dst=/migrations" \
               -e PGHOST=172.17.0.2 \
               -e PGDATABASE=pgmig \
               -e PGUSER=pgmig \
               -e PGPASSWORD=pgmig \
               -e RESOURCE_DIR=migrations \
               leafclick/pgmig pending
               
For now the expected output is something like this

        20180830154000 first
        20190216143455 second

You can safely ignore warnings about unsupported features as they are not used by PGMig.

# Limitations

There is a number of Graal's [SubstrateVM LIMITATTIONS](https://github.com/oracle/graal/blob/master/substratevm/LIMITATIONS.md)
and they all apply to PGMig.

Because of this the only supported(*) JDBC driver is PostgreSQL JDBC DataSource which is bundled with PGMig binary.

(*) *not all features are supported but the basics work well*
               
## License

Copyright © 2019-2020 leafclick s.r.o.

Licensed under the Apache License, Version 2.0.
