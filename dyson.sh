#!/bin/bash

##
## example shell script on how to start a DysonServer
##

# find jarjar
JAR=`find . -iname "jj*.jar"`

if [ -n "$JAR" -a -f "$JAR" ] 
then
  echo "executing dyson ($JAR)..."
else
  echo "cannot find dyson jarjar: $JAR. you may consider running: \$> sbt package jarjar" 
  exit 1
fi

#see javadoc of DysonConfig for more configuration options
java -cp $JAR \
  -Ddyson.smtp.server.port=1025
  -Ddyson.smtp.max.connections=1000 \
  -Ddyson.smpt.discard.recipients.enabled=false \
  $@ \  
  com.emarsys.dyson.DysonServer

