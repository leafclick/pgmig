# PGMig

Standalone PostgreSQL Migration Runner using [Migratus](https://github.com/yogthos/migratus).

## Build uberjar

    lein with-profile native do clean, test, uberjar

## Build docker base image

To create the graal vm base image follow [base image README](graalvm/README.md) in the `graalvm/` subdirectory.

## Build docker image

Once you have successfully built the base image you can create the migration runner image itself

    docker build -t leafclick/pgmig .

# Usage

## Uberjar 

To try out the basic functionality you can run the uberjar itself. To get help and available commands run

    java -jar target/uberjar/pgmig.jar 

To list pending migrations (uberjar)

    java -jar target/uberjar/pgmig.jar -h localhost -p 5432 -d pgmig -u pgmig -P pgmig -r samples/db/migrations pending

## Linux binary within docker container

To run the native image within the docker container set the environment, bind the migrations directory and specify the action

    docker run -ti \
               --mount "type=bind,src=$PWD/samples/db/migrations,dst=/migrations" \
               -e SERVER_HOST=172.17.0.2 \
               -e DATABASE_NAME=pgmig \
               -e DBUSER=pgmig \
               -e DBPASS=pgmig \
               -e RESOURCE_DIR=migrations \
               leafclick/pgmig pending
               
For now the expected output is something like this

    19-02-22 17:49:17 oryx INFO [com.zaxxer.hikari.HikariDataSource:80] - HikariPool-1 - Starting...
    19-02-22 17:49:17 oryx INFO [com.zaxxer.hikari.HikariDataSource:82] - HikariPool-1 - Start completed.
    19-02-22 17:49:17 oryx INFO [pgmig.main:106] - #'pgmig.config/env started
    19-02-22 17:49:17 oryx INFO [pgmig.main:106] - #'pgmig.db.store/db-spec started
    19-02-22 17:49:17 oryx INFO [pgmig.migration:11] - Using migration dir 'samples/db/migrations'
    19-02-22 17:49:17 oryx INFO [pgmig.migration:27] - You have 2 PENDING migration(s):
    [20180830154000 "first"]
    [20190216143455 "second"]
    19-02-22 17:49:17 oryx INFO [com.zaxxer.hikari.HikariDataSource:350] - HikariPool-1 - Shutdown initiated...
    19-02-22 17:49:17 oryx INFO [com.zaxxer.hikari.HikariDataSource:352] - HikariPool-1 - Shutdown completed.
    19-02-22 17:49:17 oryx INFO [pgmig.main:87] - #'pgmig.db.store/db-spec stopped

You can safelly ignore warnings about unsupported features as they are not used by PGMig.

## Linux binary running locally

If you have already [GraalVM](https://github.com/oracle/graal/releases) installed and properly set `GRAAL_HOME`, `JAVA_HOME` and `PATH` you can try to build and run the native image locally.
You need **Graal 19.3.0** long term release to build and deploy `pgmig` successfully.

From the project directory create the uberjar and run the `create-image.sh` helper script.

    lein with-profile native do clean, test, uberjar
    ./create-image.sh target/uberjar/pgmig.jar

You can use both direct `pgmig` options or environment variables when running it (env variables have the priority).

    ./pgmig -h localhost -p 5432 -d pgmig -u pgmig -P pgmig -r samples/db/migrations pending

Note that you need the same `.so` libraries that the native binary is linked to
on a target machine. Also keep in mind that you might need to add
`-Djava.library.path=<path-to-shared-libs>` as a `pgmig` option if it needs to load some
libraries dynamically (including shared libraries from the JDK itself).

# Limitations

There is a number of Graal's [SubstrateVM LIMITATTIONS](https://github.com/oracle/graal/blob/master/substratevm/LIMITATIONS.md)
and they all apply to PGMig.

Because of this the only supported(*) JDBC driver is PostgreSQL JDBC DataSource which is bundled with PGMig binary.

(*) *not all features are supported but the basics work well*
               
## License

Copyright © 2019 leafclick s.r.o.

Licensed under the Apache License, Version 2.0.

