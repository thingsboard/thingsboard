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
    echo "This script generates client public/private rey pair, extracts them to a no-password RSA pem file,"
    echo "and also imports server public key to client trust store"
    echo "usage: ./securemqttclient.keygen.sh [-p file]"
    echo "    -p | --props | --properties file  Properties file. default value is ./keygen.properties"
	echo "    -h | --help  | ?                  Show this message"
}

PROPERTIES_FILE=keygen.properties

while true; do
  case "$1" in
    -p | --props | --properties) PROPERTIES_FILE=$2 ;
                                shift
                                ;;
    -h | --help | ?)            usage
                                exit 0
                                ;;
    -- ) shift;
         break
         ;;
    * )  break
         ;;
  esac
  shift
done

. $PROPERTIES_FILE

echo "Generating SSL Key Pair..."

keytool -genkeypair -v \
  -alias $CLIENT_KEY_ALIAS \
  -dname "CN=$DOMAIN_SUFFIX, OU=Thingsboard, O=Thingsboard, L=Piscataway, ST=NJ, C=US" \
  -keystore $CLIENT_FILE_PREFIX.jks \
  -keypass $PASSWORD \
  -storepass $PASSWORD \
  -keyalg RSA \
  -keysize 2048 \
  -validity 9999
echo "Converting keystore to pkcs12"
keytool -importkeystore  \
  -srckeystore $CLIENT_FILE_PREFIX.jks \
  -destkeystore $CLIENT_FILE_PREFIX.p12 \
  -srcalias $CLIENT_KEY_ALIAS \
  -srcstoretype jks \
  -deststoretype pkcs12 \
  -keypass $PASSWORD \
  -srcstorepass $PASSWORD \
  -deststorepass $PASSWORD \
  -srckeypass $PASSWORD \
  -destkeypass $PASSWORD

echo "Converting pkcs12 to pem"
openssl pkcs12 -in $CLIENT_FILE_PREFIX.p12 \
  -out $CLIENT_FILE_PREFIX.pem \
  -passin pass:$PASSWORD \
  -passout pass:$PASSWORD \

echo "Importing server public key..."
keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -keystore $SERVER_KEYSTORE_DIR/$SERVER_FILE_PREFIX.jks \
  -file $CLIENT_TRUSTSTORE -rfc \
  -storepass $PASSWORD

echo "Exporting no-password pem certificate"
openssl rsa -in $CLIENT_FILE_PREFIX.pem -out $CLIENT_FILE_PREFIX.nopass.pem -passin pass:$PASSWORD
tail -n +$(($(grep -m1 -n -e '-----BEGIN CERTIFICATE' $CLIENT_FILE_PREFIX.pem | cut -d: -f1) )) \
  $CLIENT_FILE_PREFIX.pem >> $CLIENT_FILE_PREFIX.nopass.pem

echo "Done."