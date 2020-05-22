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


# Keystore parameters
CLIENT_STORE=clientKeyStore.jks
CLIENT_STORE_PWD=client
SERVER_STORE=serverKeyStore.jks
SERVER_STORE_PWD=server

VALIDITY=36500 #days

# Color output stuff
red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
bold=`tput bold`
H1=${green}${bold} 
H2=${blue} 
RESET=`tput sgr0`

# Generation of the keystore needed for Leshan integration tests.
echo "${H1}Server Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating the trusted root CA key and certificate...${RESET}"
keytool -genkeypair -alias rootCA -keyalg EC -dname 'CN=Leshan root CA' \
        -validity $VALIDITY -keypass $SERVER_STORE_PWD -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD
echo
echo "${H2}Creating an untrusted root CA key and certificate...${RESET}"
keytool -genkeypair -alias untrustedrootCA -keyalg EC -dname 'CN=Leshan untrusted root CA' \
        -validity $VALIDITY -keypass $SERVER_STORE_PWD -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD
echo
echo "${H2}Creating server key and self-signed  certificate ...${RESET}"
keytool -genkeypair -alias server -keyalg EC -dname 'CN=Leshan server self-signed' \
        -validity $VALIDITY -keypass $SERVER_STORE_PWD -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD
keytool -exportcert -alias server -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD | \
  keytool -importcert -alias server_self_signed -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -noprompt

echo
echo "${H2}Creating server certificate signed by root CA...${RESET}"
keytool -certreq -alias server -dname 'CN=Leshan server' -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -validity $VALIDITY  | \
    keytool -importcert -alias server -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD

echo
echo "${H1}Client Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating client key and self-signed certificate with expected CN...${RESET}"
keytool -genkeypair -alias client -keyalg EC -dname 'CN=leshan_integration_test' \
        -validity $VALIDITY -keypass $CLIENT_STORE_PWD -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD
keytool -exportcert -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -importcert -alias client_self_signed -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Import root certificate just to be able to sign certificate ...${RESET}"
keytool -exportcert -alias rootCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD | \
  keytool -importcert -alias rootCA -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Creating client certificate signed by root CA with expected CN...${RESET}"
keytool -certreq -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -validity $VALIDITY  | \
    keytool -importcert -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Creating client certificate signed by root CA with bad/unexpected CN...${RESET}"
keytool -certreq -alias client -dname 'CN=leshan_client_with_bad_cn' -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -validity $VALIDITY  | \
    keytool -importcert -alias client_bad_cn -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Creating client certificate signed by untrusted root CA with expected CN...${RESET}"
keytool -certreq -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -gencert -alias untrustedrootCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -validity $VALIDITY  | \
    keytool -importcert -alias client_not_trusted -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
