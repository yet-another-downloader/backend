#!/usr/bin/env bash

if [ "$CIFS_PARAMS" != "" ]
then
    mount -t cifs -o $CIFS_PARAMS
else
    echo "No cifs params"
fi

java -Djava.security.egd=file:/dev/./urandom -jar app.jar