## ELiT

Here at ELiT, we use this as part of elit-cli-2.

To build:

    lein with-profile native do clean, test, uberjar && cp target/uberjar/pgmig.jar ../elit-cli-2/lib

## Documentation below here is from the original project:
---

# PGMig

Standalone PostgreSQL Migration Runner using [Migratus](https://github.com/yogthos/migratus) runnable as a native-image binary.

## Changelog

There are **BREAKING changes** since release `0.4.0` changing most environment variables and some command line options
to match `psql`.

See [Changelog](CHANGELOG.md) for upgrade path hints.

# Installation

## Github Releases
There are prebuild binaries that you can download from [Github Releases](https://github.com/leafclick/pgmig/releases).

## Docker Image Releases
The latest PGMig build can be pulled from the [dockerhub](https://hub.docker.com/r/leafclick/pgmig).

## Installing From Sources

Follow the [build instructions](#build-linux-binary-to-be-run-locally) below and place the resulting binary in your path,
 e.g. to `/usr/local/bin`.

# Building PGMig

## Build uberjar

    lein with-profile native do clean, test, uberjar

## Build Linux binary to be run locally

If you have already [GraalVM CE](https://github.com/graalvm/graalvm-ce-builds/releases) installed and properly set `GRAAL_HOME`, `JAVA_HOME` and `PATH` you can try to build and run the native image locally. You need **Graal 21.2** release with [native-image](https://www.graalvm.org/docs/reference-manual/native-image/) to build and deploy `pgmig` successfully.

From the project directory create the uberjar and run the `create-image.sh` helper script.

    lein with-profile native do clean, test, uberjar
    ./create-image.sh target/uberjar/pgmig.jar

You can use both `pgmig` command line options or environment variables when running it (command line options have the priority).

    ./pgmig -h localhost -p 5432 -d pgmig -U pgmig -P pgmig -r samples/db/migrations pending

Status code 0 is returned on success, non-zero return code signals an error.

Note that you need the same `.so` libraries that the native binary is linked to
on the target machine. Also keep in mind that you might need to add
`-Djava.library.path=<path-to-shared-libs>` as a `pgmig` option if it needs to load some
libraries dynamically (including shared libraries from the JDK itself).
When building the docker image the `.so` library addition is done automatically.

## Build docker image

The docker image is based on the [GraalVM Community Edition Official Image](https://hub.docker.com/r/oracle/graalvm-ce/tags).  Once you have successfully built the uberjar you can create the migration runner image itself

    lein with-profile native do clean, test, uberjar
    docker build -t leafclick/pgmig .

# Usage

PGMig supports following commands:

   | Function | Description                                                                               |
   |----------|-------------------------------------------------------------------------------------------|
   | `init`   | Runs a script to initialize the database, e.g: create a new schema.                       |
   | `list`   | List migrations already applied.                                                          |
   | `pending`| List all pending migrations.                                                              |
   | `migrate`| Run 'up' for any migrations that have not been run.                                       |
   | `up`     | Run 'up' for the specified migration ids. Will skip any migration that is already up.     |
   | `down`   | Run 'down' for the specified migration ids. Will skip any migration that is already down. |
   | `reset`  | Run 'down' all migrations and run 'up' all migrations again.                              |
   | `create` | Create a new migration using the given name with the current date and time.               |

The following options skip all other actions:

   | Function    | Description                                                                            |
   |-------------|----------------------------------------------------------------------------------------|
   | `--help`    | Print the usage summary.                                                               |
   | `--version` | Print the current 'pgmig' version.                                                     |

## Run the native image directly

For example to apply all pending migrations run the `migrate` command

    pgmig -h localhost -p 5432 -d pgmig -U pgmig -P pgmig -r samples/db/migrations --classpath samples/db/clj migrate

Note that if you use clj programmatic migrations (using [sci](https://github.com/borkdude/sci)) you need to list
all directories that contain migration support code files (if there are any). You can usually create a classpath
list by running `lein classpath` or `clj -Spath` in the project.

## Run uberjar with java

To try out the basic functionality you can run the uberjar itself. To get help and available commands run

    java -jar target/uberjar/pgmig.jar

To list pending migrations (uberjar)

    java -jar target/uberjar/pgmig.jar -h localhost -p 5432 -d pgmig -U pgmig -P pgmig -r samples/db/migrations pending

## Run Linux binary within docker container

To run the native image within the docker container set the environment, bind the migrations directory and specify the action

    docker run -ti \
               --rm \
               --mount "type=bind,src=$PWD/samples/db/migrations,dst=/migrations" \
               --mount "type=bind,src=$PWD/samples/db/clj,dst=/clj" \
               -e PGHOST=172.17.0.2 \
               -e PGDATABASE=pgmig \
               -e PGUSER=pgmig \
               -e PGPASSWORD=pgmig \
               -e RESOURCE_DIR=migrations \
               -e CLASSPATH=clj \
               leafclick/pgmig migrate

For now the expected output is something like this

    20180830154000 first
    20190216143455 second
    20201106155531 programmatic-third

You can safely ignore warnings about unsupported features as they are not used by PGMig.

# Limitations

There is a number of Graal's [SubstrateVM LIMITATTIONS](https://github.com/oracle/graal/blob/master/substratevm/Limitations.md)
and they all apply to PGMig.

Because of this the only supported(*) JDBC driver is PostgreSQL JDBC DataSource which is bundled with PGMig binary.

(*) *not all features are supported but the basics work well*

# License

Copyright © 2019-2021 leafclick s.r.o.

Licensed under the Apache License, Version 2.0.
