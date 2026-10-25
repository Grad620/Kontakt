#!/bin/bash
# kotlinc из Gradle (Kotlin 2.0.21): kc.sh -d out.jar -cp deps files...
L=/opt/gradle-8.14.3/lib
CP="$L/kotlin-compiler-embeddable-2.0.21.jar:$L/kotlin-stdlib-2.0.21.jar:$L/kotlin-script-runtime-2.0.21.jar:$L/kotlin-reflect-2.0.21.jar:$L/kotlin-daemon-embeddable-2.0.21.jar:$L/kotlinx-coroutines-core-jvm-1.6.4.jar"
for j in $L/trove4j*.jar $L/annotations-*.jar $L/jna*.jar; do [ -f "$j" ] && CP="$CP:$j"; done
exec java -Xmx1g -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect "$@"
