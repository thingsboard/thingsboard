#!/bin/sh
#
# Copyright © 2016 The Thingsboard Authors
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


. keygen.properties

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

keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -keystore $SERVER_FILE_PREFIX.jks \
  -file $CLIENT_TRUSTSTORE -rfc \
  -storepass $PASSWORD

read -p  "Do you want to copy $SERVER_FILE_PREFIX.jks to server directory? " yn
    case $yn in
        [Yy]) echo "Please, specify destination dir: "
             read -p "(Default: $SERVER_KEYSTORE_DIR): " dir
             if [[ !  -z  $dir  ]]; then
                DESTINATION=$dir;
             else
                DESTINATION=$SERVER_KEYSTORE_DIR
             fi;
             mkdir -p $SERVER_KEYSTORE_DIR
             cp $SERVER_FILE_PREFIX.jks $DESTINATION
             if [ $? -ne 0 ]; then
                echo "Failed to copy keystore file."
             else
                echo "File copied successfully."
             fi
             break;;
        * ) ;;
    esac
echo "Done."
