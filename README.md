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

    19-02-16 16:11:08 f1d1605733ee INFO [com.zaxxer.hikari.HikariDataSource:87] - HikariPool-1 - Starting...
    19-02-16 16:11:08 f1d1605733ee WARN [com.zaxxer.hikari.pool.HikariPool:282] - setMetricRegistry is not supported in native-image - use setMetricsTrackerFactory directly
    19-02-16 16:11:08 f1d1605733ee WARN [com.zaxxer.hikari.pool.HikariPool:309] - setHealthCheckRegistry is not supported in native-image - use CodahaleHealthChecker.registerHealthChecks directly
    19-02-16 16:11:08 f1d1605733ee INFO [com.zaxxer.hikari.HikariDataSource:89] - HikariPool-1 - Start completed.
    19-02-16 16:11:08 f1d1605733ee INFO [pgmig.db.store:40] - Using jdbc-uri jdbc:postgresql://172.17.0.2:5432/pgmig?user=pgmig&password=pgmig
    19-02-16 16:11:08 f1d1605733ee INFO [pgmig.main:106] - #'pgmig.config/env started
    19-02-16 16:11:08 f1d1605733ee INFO [pgmig.main:106] - #'pgmig.db.store/db-spec started
    19-02-16 16:11:08 f1d1605733ee INFO [pgmig.migration:11] - Using migration dir 'migrations'
    19-02-16 16:11:08 f1d1605733ee INFO [pgmig.migration:27] - You have 2 PENDING migration(s):
    [20180830154000 "first"]
    [20190216143455 "second"]
    19-02-16 16:11:08 f1d1605733ee INFO [com.zaxxer.hikari.HikariDataSource:357] - HikariPool-1 - Shutdown initiated...
    19-02-16 16:11:08 f1d1605733ee INFO [com.zaxxer.hikari.HikariDataSource:359] - HikariPool-1 - Shutdown completed.
    19-02-16 16:11:08 f1d1605733ee INFO [pgmig.main:87] - #'pgmig.db.store/db-spec stopped

You can safelly ignore warnings about unsupported features as they are not used by PGMig.

# Limitations

There is a number of Graal's [SubstrateVM LIMITATTIONS](https://github.com/oracle/graal/blob/master/substratevm/LIMITATIONS.md)
and they all apply to PGMig.

Because of this the only supported(*) JDBC driver is PostgreSQL JDBC DataSource which is bundled with PGMig binary.

(*) *not all features are supported but the basics work well*
               
## License

Copyright © 2019 leafclick s.r.o.

Licensed under the Apache License, Version 2.0.

