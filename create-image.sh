#!/bin/sh

uberjar="$1"

native-image --verbose \
    --no-server -Djava.library.path=$GRAAL_HOME/jre/lib/amd64/ \
    -H:ReflectionConfigurationFiles=graal.json \
    -H:IncludeResources='.*\.(properties|edn)' \
    -H:IncludeResources='com/ibm/icu/impl/data/icudt58b/.*' \
    -H:IncludeResources='com/ibm/icu/impl/duration/impl/data/icudt58b/.*' \
    -H:IncludeResources='com/ibm/icu/.*\.properties' \
    -H:IncludeResources='com/github/fge/.*\.properties' \
    --report-unsupported-elements-at-runtime \
    --allow-incomplete-classpath \
    --delay-class-initialization-to-runtime=org.postgresql.sspi.NTDSAPI \
    --delay-class-initialization-to-runtime=com.sun.jna.platform.win32.Secur32 \
    --delay-class-initialization-to-runtime=com.sun.jna.platform.win32.Kernel32 \
    -jar "$uberjar"

