#!/usr/bin/env bash

set -e
if [ "$CIFS_PARAMS" != "" ]
then
    mkdir -p $CIFS_MOUNT_PATH
    mount -t cifs -o $CIFS_PARAMS $CIFS_MOUNT_PATH
else
    echo "No cifs params"
fi

java -Djava.security.egd=file:/dev/./urandom -jar app.jar