#!/bin/sh
#
# Copyright © 2016-2017 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

usage() {
    echo "This script generates thingsboard server's ssl certificate"
    echo "and optionally copies it to the server's resource directory."
    echo "usage: ./keygen.sh [-c flag] [-d directory]"
    echo "    -c | --copy flag                  Set if copy keystore to server directory needed. Default value is true"
    echo "    -d | --dir directory              Server keystore directory, where the generated keystore file will be copied."
    echo "                                      Default value is SERVER_KEYSTORE_DIR property from properties file"
    echo "    -p | --props | --properties file  Properties file. default value is ./keygen.properties"
	echo "    -h | --help | ?                   Show this message"
}

COPY=true;
COPY_DIR="d"
PROPERTIES_FILE=keygen.properties

while true; do
  case "$1" in
    -c | --copy)                 COPY=$2 ;
                                 shift
                                 ;;
    -d | --dir | --directory)    COPY_DIR=$2 ;
                                 shift
                                 ;;
    -p | --props | --properties) PROPERTIES_FILE=$2 ;
                                shift
                                ;;
    -h | --help | ?)            usage
                                exit 0
                                ;;
    -- ) shift;
         break
         ;;
    * ) break
         ;;
  esac
  shift
done

if [[ "$COPY" != true ]] && [[ "$COPY" != false ]]; then
   usage
fi

echo "copy: $COPY; copy_dir: $COPY_DIR; PROPERTIES_FILE=$PROPERTIES_FILE";

. $PROPERTIES_FILE

echo "Generating SSL Key Pair..."

keytool -genkeypair -v \
  -alias $SERVER_KEY_ALIAS \
  -dname "CN=$HOSTNAME, OU=Thingsboard, O=Thingsboard, L=Piscataway, ST=NJ, C=US" \
  -keystore $SERVER_FILE_PREFIX.jks \
  -keypass $PASSWORD \
  -storepass $PASSWORD \
  -keyalg RSA \
  -keysize 2048 \
  -validity 9999

status=$?
if [[ $status != 0 ]]; then
    exit $status;
fi

keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -keystore $SERVER_FILE_PREFIX.jks \
  -file $CLIENT_TRUSTSTORE -rfc \
  -storepass $PASSWORD

status=$?
if [[ $status != 0 ]]; then
    exit $status;
fi


if [[ $COPY = true ]]; then
    if [[ -z "$COPY_DIR" ]]; then
        read -p  "Do you want to copy $SERVER_FILE_PREFIX.jks to server directory? " yn
            case $yn in
                [Yy]) echo "Please, specify destination dir: "
                     read -p "(Default: copy_dir): " dir
                     if [[ !  -z  $dir  ]]; then
                        DESTINATION=$dir;
                     else
                        DESTINATION=$SERVER_KEYSTORE_DIR
                     fi;
                     break;;
                * ) ;;
            esac
    else
        DESTINATION=$COPY_DIR
    fi
    mkdir -p $DESTINATION
    cp $SERVER_FILE_PREFIX.jks $DESTINATION
    if [ $? -ne 0 ]; then
        echo "Failed to copy keystore file."
    else
        echo "File copied successfully."
    fi
fi
echo "Done."