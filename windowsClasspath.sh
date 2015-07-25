#!/bin/bash

#
# Gradle is on Linux and Eclipse is on Windows so I need to rewrite the 
# class path generated on Linux for Windows.
#

./gradlew cleanEclipse eclipse

FILES=`find . -name '\.classpath'`

for FILE in $FILES; do
  echo $FILE
  rm $FILE.bak
  mv $FILE $FILE.bak
  cat $FILE.bak | sed 's/\/home\/wstrater/T:/g' > $FILE
  ls -l $FILE*
done
