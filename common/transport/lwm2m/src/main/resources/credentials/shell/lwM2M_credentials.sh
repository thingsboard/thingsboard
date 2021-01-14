#!/bin/sh
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

# source the properties:
script_dir=$(dirname $0)
echo "script_dir: $script_dir"
cd $script_dir
. ./lwM2M_keygen.properties

# Generation of the keystore.
echo "${H0}====START========${RESET}"
echo "${H1}Server Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating the trusted root CA key and certificate...${RESET}"
# -keysize
#    1024 (when using -genkeypair)
keytool \
  -genkeypair \
  -alias $ROOT_KEY_ALIAS \
  -keyalg EC \
  -dname "CN=$ROOT_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $SERVER_STORE_PWD \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD

echo
echo "${H2}Creating server key and self-signed  certificate ...${RESET}"
keytool \
  -genkeypair \
  -alias $SERVER_ALIAS \
  -keyalg EC \
  -dname "CN=$SERVER_SELF_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $SERVER_STORE_PWD \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD
keytool \
  -exportcert \
  -alias $SERVER_ALIAS \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -importcert \
    -alias $SERVER_SELF_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -noprompt

echo
echo "${H2}Creating server certificate signed by root CA...${RESET}"
keytool \
  -certreq \
  -alias $SERVER_ALIAS \
  -dname "CN=$SERVER_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -gencert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -storetype $STORETYPE \
    -validity $VALIDITY  | \
    keytool \
      -importcert \
      -alias $SERVER_ALIAS \
      -keystore $SERVER_STORE \
      -storepass $SERVER_STORE_PWD

echo
echo "${H2}Creating server key and self-signed  certificate ...${RESET}"
keytool \
  -genkeypair \
  -alias $BOOTSTRAP_ALIAS \
  -keyalg EC \
  -dname "CN=$BOOTSTRAP_SELF_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $SERVER_STORE_PWD \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD
keytool \
  -exportcert \
  -alias $BOOTSTRAP_ALIAS \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -importcert \
    -alias $BOOTSTRAP_SELF_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -noprompt

echo
echo "${H2}Creating bootstrap certificate signed by root CA...${RESET}"
keytool \
  -certreq \
  -alias $BOOTSTRAP_ALIAS \
  -dname "CN=$BOOTSTRAP_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -gencert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -storetype $STORETYPE \
    -validity $VALIDITY  | \
    keytool \
      -importcert \
      -alias $BOOTSTRAP_ALIAS \
      -keystore $SERVER_STORE \
      -storepass $SERVER_STORE_PWD


echo
echo "${H1}Client Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating client key and self-signed certificate with expected CN...${RESET}"
keytool \
  -genkeypair \
  -alias $CLIENT_ALIAS \
  -keyalg EC \
  -dname "CN=$CLIENT_SELF_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $CLIENT_STORE_PWD \
  -keystore $CLIENT_STORE \
  -storepass $CLIENT_STORE_PWD
keytool \
  -exportcert \
  -alias $CLIENT_ALIAS \
  -keystore $CLIENT_STORE \
  -storepass $CLIENT_STORE_PWD | \
  keytool \
    -importcert \
    -alias $CLIENT_SELF_ALIAS \
    -keystore $CLIENT_STORE \
    -storepass $CLIENT_STORE_PWD \
    -noprompt

echo
echo "${H2}Import root certificate just to be able to import  ned by root CA with expected CN...${RESET}"
keytool \
  -exportcert \
  -alias $ROOT_KEY_ALIAS \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -importcert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $CLIENT_STORE \
    -storepass $CLIENT_STORE_PWD \
    -noprompt

echo
echo "${H2}Creating client certificate signed by root CA with expected CN...${RESET}"
keytool \
  -certreq \
  -alias $CLIENT_ALIAS \
  -dname "CN=$CLIENT_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $CLIENT_STORE \
  -storepass $CLIENT_STORE_PWD | \
  keytool \
    -gencert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -storetype $STORETYPE \
    -validity $VALIDITY  | \
    keytool \
      -importcert \
      -alias $CLIENT_ALIAS \
      -keystore $CLIENT_STORE \
      -storepass $CLIENT_STORE_PWD \
      -noprompt

echo
echo "${H0}!!! Warning ${H2}Migrate ${H1}${SERVER_STORE} ${H2}to ${H1}PKCS12 ${H2}which is an industry standard format..${RESET}"
keytool \
  -importkeystore \
  -srckeystore $SERVER_STORE \
  -destkeystore $SERVER_STORE \
  -deststoretype pkcs12 \
  -srcstorepass $SERVER_STORE_PWD

echo
echo "${H0}!!! Warning ${H2}Migrate ${H1}${CLIENT_STORE} ${H2}to ${H1}PKCS12 ${H2}which is an industry standard format..${RESET}"
keytool \
  -importkeystore \
  -srckeystore $CLIENT_STORE \
  -destkeystore $CLIENT_STORE \
  -deststoretype pkcs12 \
  -srcstorepass $CLIENT_STORE_PWD
