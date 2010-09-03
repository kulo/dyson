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

java -cp $JAR \
  -Ddyson.smtp.max.connections=50 \
  com.emarsys.dyson.DysonServer

