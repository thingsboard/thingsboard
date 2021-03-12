#!/bin/bash
#
# Copyright © 2016-2021 The Thingsboard Authors
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
    echo "usage: ./server.keygen.sh [-c flag] [-d directory] [-p file]"
    echo "    -c | --copy flag                  Specifies if the keystore should be copied to the server directory. Defaults to true"
    echo "    -d | --dir directory              Server keystore directory, where the generated keystore file will be copied. If specified, overrides the value from the properties file"
    echo "                                      Default value is SERVER_KEYSTORE_DIR property from properties file"
    echo "    -p | --props | --properties file  Properties file. default value is ./keygen.properties"
    echo "    -h | --help | ?                   Show this message"
}

COPY=true;
COPY_DIR=
PROPERTIES_FILE=keygen.properties

while true; do
  case "$1" in
    -c | --copy)                  COPY=$2 ;
                                  shift
                                  ;;
    -d | --dir | --directory )    COPY_DIR=$2 ;
                                  shift
                                  ;;
    -p | --props | --properties ) PROPERTIES_FILE=$2 ;
                                  shift
                                  ;;
    -- )                          shift;
                                  break
                                  ;;
    "" )                          break
                                  ;;

    -h | --help | ? | *)          usage
                                  exit 0
                                  ;;
  esac
  shift
done

if [[ "$COPY" != true ]] && [[ "$COPY" != false ]]; then
   usage
fi

. $PROPERTIES_FILE

if [ -f $SERVER_FILE_PREFIX.jks ] || [ -f $SERVER_FILE_PREFIX.cer ] || [ -f $SERVER_FILE_PREFIX.pub.pem ] || [ -f $SERVER_FILE_PREFIX.pub.der ];
then
while :
   do
       read -p "Output files from previous server.keygen.sh script run found. Overwrite?[yes]" response
       case $response in
        [nN]|[nN][oO])
            echo "Skipping"
            echo "Done"
            exit 0
            ;;
        [yY]|[yY][eE]|[yY][eE][sS]|"")
            echo "Cleaning up files"
            rm -rf $SERVER_FILE_PREFIX.jks
            rm -rf $SERVER_FILE_PREFIX.pub.pem
            rm -rf $SERVER_FILE_PREFIX.cer
            break;
            ;;
        *)  echo "Please reply 'yes' or 'no'"
            ;;
        esac
    done
fi

echo "Generating SSL Key Pair..."

EXT=""

if [[ ! -z "$SUBJECT_ALTERNATIVE_NAMES" ]]; then
  EXT="-ext san=$SUBJECT_ALTERNATIVE_NAMES "
fi

keytool -genkeypair -v \
  -alias $SERVER_KEY_ALIAS \
  -dname "CN=$DOMAIN_SUFFIX, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $SERVER_FILE_PREFIX.jks \
  -keypass $SERVER_KEY_PASSWORD \
  -storepass $SERVER_KEYSTORE_PASSWORD \
  -keyalg $SERVER_KEY_ALG \
  -keysize $SERVER_KEY_SIZE \
  -validity 9999 \
  $EXT

status=$?
if [[ $status != 0 ]]; then
    exit $status;
fi

keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -keystore $SERVER_FILE_PREFIX.jks \
  -file $SERVER_FILE_PREFIX.pub.pem -rfc \
  -storepass $SERVER_KEYSTORE_PASSWORD

keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -file $SERVER_FILE_PREFIX.cer \
  -keystore $SERVER_FILE_PREFIX.jks \
  -storepass $SERVER_KEYSTORE_PASSWORD \
  -keypass $SERVER_KEY_PASSWORD

status=$?
if [[ $status != 0 ]]; then
    exit $status;
fi


if [[ $COPY = true ]]; then
    if [[ -z "$COPY_DIR" ]]; then
        while :
        do
            read -p  "Do you want to copy $SERVER_FILE_PREFIX.jks to server directory? [Y/N]: " yn
            case $yn in
                [nN]|[nN][oO])
                    break
                    ;;
                [yY]|[yY][eE]|[yY][eE][sS]|"")
                    read -p "(Default: $SERVER_KEYSTORE_DIR): " dir
                     if [[ !  -z  $dir  ]]; then
                        DESTINATION=$dir;
                     else
                        DESTINATION=$SERVER_KEYSTORE_DIR
                     fi;
                     break;;
                *)  echo "Please reply 'yes' or 'no'"
                    ;;
             esac
         done
    else
        DESTINATION=$COPY_DIR
    fi
    echo "*** DEST: $DESTINATION"
    if [[ -n $DESTINATION ]]; then
        mkdir -p $DESTINATION
        cp $SERVER_FILE_PREFIX.jks $DESTINATION
        if [ $? -ne 0 ]; then
            echo "Failed to copy keystore file."
        else
            echo "File copied successfully."
        fi
    fi
fi
echo "Done."
