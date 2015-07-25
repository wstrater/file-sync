#!/bin/bash

sudo chown -R wstrater:users .

./gradlew clean

DATE=`date +'%Y%m%d'`

rm file-sync-$DATE.tar.gz

tar --exclude="*.tar.gz" \
    --exclude="*.cer" \
    --exclude="*.csr" \
    --exclude="*.jks" \
    --exclude="*.log" \
    --exclude="*.class" \
    --exclude="*.bak" \
    --exclude=".git" \
    -czf file-sync-$DATE.tar.gz .

tar -tzvf file-sync-$DATE.tar.gz | wc -l

scp file-sync-$DATE.tar.gz cannibis:/usr/local/downloads/ 

ls -latr
