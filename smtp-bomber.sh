#!/bin/bash

## a simple shell script which runs com.emarsys.ecommon.mail.test.SmtpBomber in order to generate and send mails for testing purposes.
## you might use this script to load test dyson.

# find jarjar
JAR=`find . -iname "jj*.jar"`

if [ -n "$JAR" -a -f "$JAR" ] 
then
  echo "executing smtp-bomber ($JAR)..."
else
  echo "cannot find smtp-bomber jarjar: $JAR. you may consider running: \$> sbt package jarjar" 
  exit 1
fi

# see javadoc of SmtpBomber for more configuration options
java -cp $JAR \
  -Dsmtp.bomber.server.host=localhost \
  -Dsmtp.bomber.server.port=1025 \
  -Dsmtp.bomber.nbrOfMails=1000 \
  $@ \
  com.emarsys.ecommon.mail.test.SmtpBomber

