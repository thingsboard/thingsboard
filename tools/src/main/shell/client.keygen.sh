#!/bin/bash
#
# Copyright © 2016-2020 The Thingsboard Authors
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
    echo "This script generates client public/private key pair, extracts them to a no-password pem file,"
    echo "and imports server public key to client keystore"
    echo "usage: ./client.keygen.sh [-p file]"
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

if [ -f $CLIENT_FILE_PREFIX.jks ] || [ -f $CLIENT_FILE_PREFIX.pub.pem ] || [ -f $CLIENT_FILE_PREFIX.nopass.pem ] || [ -f $CLIENT_FILE_PREFIX.pem ] || [ -f $CLIENT_FILE_PREFIX.p12 ];
then
while :
   do
       read -p "Output files from previous server.keygen.sh script run found. Overwrite? [Y/N]: " response
       case $response in
        [nN]|[nN][oO])
            echo "Skipping"
            echo "Done"
            exit 0
            ;;
        [yY]|[yY][eE]|[yY][eE][sS]|"")
            echo "Cleaning up files"
            rm -rf $CLIENT_FILE_PREFIX.jks
            rm -rf $CLIENT_FILE_PREFIX.pub.pem
            rm -rf $CLIENT_FILE_PREFIX.nopass.pem
            rm -rf $CLIENT_FILE_PREFIX.pem
            rm -rf $CLIENT_FILE_PREFIX.p12
            break;
            ;;
        *)  echo "Please reply 'yes' or 'no'"
            ;;
        esac
    done
fi

OPENSSL_CMD=""
case $CLIENT_KEY_ALG in
RSA)
	OPENSSL_CMD="rsa"
	;;
EC)
	OPENSSL_CMD="ec"
	;;
esac
if [ -z "$OPENSSL_CMD" ]; then
	echo "Unexpected CLIENT_KEY_ALG. Exiting."
	exit 0
fi

echo "Generating SSL Key Pair..."

keytool -genkeypair -v \
  -alias $CLIENT_KEY_ALIAS \
  -keystore $CLIENT_FILE_PREFIX.jks \
  -keypass $CLIENT_KEY_PASSWORD \
  -storepass $CLIENT_KEYSTORE_PASSWORD \
  -keyalg $CLIENT_KEY_ALG \
  -keysize $CLIENT_KEY_SIZE\
  -validity 9999 \
  -dname "CN=$DOMAIN_SUFFIX, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE"

echo "Converting keystore to pkcs12"
keytool -importkeystore  \
  -srckeystore $CLIENT_FILE_PREFIX.jks \
  -destkeystore $CLIENT_FILE_PREFIX.p12 \
  -srcalias $CLIENT_KEY_ALIAS \
  -srcstoretype jks \
  -deststoretype pkcs12 \
  -srcstorepass $CLIENT_KEYSTORE_PASSWORD \
  -deststorepass $CLIENT_KEY_PASSWORD \
  -srckeypass $CLIENT_KEY_PASSWORD \
  -destkeypass $CLIENT_KEY_PASSWORD

echo "Converting pkcs12 to pem"
openssl pkcs12 -in $CLIENT_FILE_PREFIX.p12 \
  -out $CLIENT_FILE_PREFIX.pem \
  -passin pass:$CLIENT_KEY_PASSWORD \
  -passout pass:$CLIENT_KEY_PASSWORD \

echo "Importing server public key to $CLIENT_FILE_PREFIX.jks"
keytool --importcert \
   -file $SERVER_FILE_PREFIX.cer \
   -keystore $CLIENT_FILE_PREFIX.jks \
   -alias $SERVER_KEY_ALIAS \
   -keypass $SERVER_KEY_PASSWORD \
   -storepass $CLIENT_KEYSTORE_PASSWORD \
   -noprompt

echo "Exporting no-password pem certificate"
openssl $OPENSSL_CMD -in $CLIENT_FILE_PREFIX.pem -out $CLIENT_FILE_PREFIX.nopass.pem -passin pass:$CLIENT_KEY_PASSWORD
tail -n +$(($(grep -m1 -n -e '-----BEGIN CERTIFICATE' $CLIENT_FILE_PREFIX.pem | cut -d: -f1) )) \
  $CLIENT_FILE_PREFIX.pem >> $CLIENT_FILE_PREFIX.nopass.pem

echo "Exporting client public key"
tail -n +$(($(grep -m1 -n -e '-----BEGIN CERTIFICATE' $CLIENT_FILE_PREFIX.pem | cut -d: -f1) )) \
  $CLIENT_FILE_PREFIX.pem >> $CLIENT_FILE_PREFIX.pub.pem

echo "Done."
