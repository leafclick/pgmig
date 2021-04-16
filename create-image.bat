@echo off

SET uberjar="%~1"

native-image --verbose  --no-server -Djava.library.path=%%GRAAL_HOME%%/jre/lib/amd64/ -H:+ReportExceptionStackTraces -H:ReflectionConfigurationFiles=graal.json -H:IncludeResources=".*\.(properties|edn)" -H:IncludeResources="com/ibm/icu/impl/data/icudt58b/.*" -H:IncludeResources="com/ibm/icu/impl/duration/impl/data/icudt58b/.*" -H:IncludeResources="com/ibm/icu/.*\.properties" -H:IncludeResources="com/github/fge/.*\.properties" -H:IncludeResources="PGMIG_VERSION" --no-fallback --allow-incomplete-classpath --report-unsupported-elements-at-runtime --enable-url-protocols=http,https --initialize-at-build-time --initialize-at-run-time=org.postgresql.sspi.NTDSAPI --initialize-at-run-time=com.sun.jna.platform.win32.Secur32 --initialize-at-run-time=com.sun.jna.platform.win32.Kernel32 --initialize-at-run-time=org.postgresql.sspi.SSPIClient -jar "%uberjar%"
