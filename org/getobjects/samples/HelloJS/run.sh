#!/bin/sh

JARDIR="$HOME/dev/eclipse-go/JOPE/ThirdParty"
GOPATH="$HOME/dev/eclipse-go/JOPE/bin"
OGOPATH="$HOME/dev/eclipse-go/OGoCore/bin"

RUNNER="org.getobjects.jsapp.run"
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
CLASSPATH="`ls ${JARDIR}/postgresql-8.2*.jar`:${CLASSPATH}"

# JFreeChart
CLASSPATH="`ls ${JARDIR}/jfreechart-1.0*.jar`:${CLASSPATH}"
CLASSPATH="`ls ${JARDIR}/jcommon-1.0*.jar`:${CLASSPATH}"

# iText
CLASSPATH="`ls ${JARDIR}/iText-2.0*.jar`:${CLASSPATH}"

# JOPE
CLASSPATH="${GOPATH}:${OGOPATH}:${CLASSPATH}"

#echo "CLASSPATH: ${CLASSPATH}"

# run the runner ;-)
java -Djava.awt.headless=true -cp ".:${CLASSPATH}" "${RUNNER}" \
  . -DWOPort=${WOPORT}
