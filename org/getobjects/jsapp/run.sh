#!/bin/sh

JARDIR="$HOME/dev/eclipse/JOPE/ThirdParty"
JOPEPATH="$HOME/dev/eclipse/JOPE/bin"

RUNNER="org.opengroupware.jope.jsapp.run"
WOPORT=5859

# ThirdParty dependencies
CLASSPATH="" # reset
CLASSPATH="`ls ${JARDIR}/commons-fileupload-1.*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/commons-logging-1.*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/javax.servlet.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/jetty-6.1.*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/jetty-util-6.1.*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/log4j-1.2.*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/ognl-2.6.*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/js*.jar`:${CLASSPATH}"

# JOPE
CLASSPATH="${JOPEPATH}:${CLASSPATH}"

echo "CLASSPATH: ${CLASSPATH}"

# run the runner ;-)
java -cp ".:${CLASSPATH}" "${RUNNER}" . -DWOPort=${WOPORT}
