#!/usr/bin/env bash
#
# Copyright © 2016-2026 The Thingsboard Authors
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


# REF: https://github.com/cloudflare/cfssl

# Change working directory
cd -- "$(
	dirname "${0}"
)" || exit 1

readonly CA_ROOT_CERT_KEY="ca-root"
readonly CA_ROOT_ALIAS="root"
readonly CA_INTERMEDIATE_CERT_KEY_PREF="intermediate_ca"
CA_INTERMEDIATE_NUMBER=0
CA_LIST_CERT_FOR_CAT=""

readonly CF_COMMANDS="
	cfssl
	cfssljson
"

readonly SERVER_JKS_FOR_TEST="lwm2mserver"
readonly STORE_PASS_PWD="server_ks_password"
readonly SERVER_PATH="Server"
readonly SERVER_CERT_KEY="lwm2mserver"
readonly SERVER_CERT_CHAIN="lwm2mserver_chain"
readonly SERVER_CERT_ALIAS="server"
readonly BS_SERVER_CERT_KEY="lwm2mserverbs"
readonly BS_SERVER_CERT_CHAIN="lwm2mserverbs_chain"
readonly BS_SERVER_CERT_ALIAS="bootstrap"

SERVER_HOST_NAME="localhost.localdomain"
SERVER_LOCAL_HOST_NAME="localhost"
SERVER_PUBLIC_HOST_NAMES="-"

intermediate_common_name() {
  echo "${CA_INTERMEDIATE_CERT_KEY_PREF}${CA_INTERMEDIATE_NUMBER}"
}

set_list_sert_for_cat() {
  local first="$1"
  echo "$first ${CA_LIST_CERT_FOR_CAT}"
}


# Change working directory
rm -rf ${SERVER_PATH}
mkdir -p ${SERVER_PATH}

cd -- "$(
	dirname ./${SERVER_PATH}
)" || exit 1


rm *.csr
rm *.p12
rm *.json
rm *.pem
rm *.jks

CA_INTERMEDIATE_CERT_SIGN=${CA_ROOT_CERT_KEY}
CA_INTERMEDIATE_CERT_KEY=$(intermediate_common_name)
CA_INTERMEDIATE_NUMBER=$((${CA_INTERMEDIATE_NUMBER} + 1))
CA_LIST_CERT_FOR_CAT=""

for COMMAND in ${CF_COMMANDS}; do
	if ! command -v ${COMMAND} &> /dev/null; then
		echo "ERROR: Missing command ${COMMAND}" >&2
		echo "Install the package from: https://pkg.cfssl.org/" >&2
		exit 1
	fi
done

tee ./${SERVER_PATH}/ca-config.json 1> /dev/null <<-CONFIG
{
  "signing": {
    "default": {
      "expiry": "8760h",
      "names": [
        {
          "C": "UK",
          "ST": "Kyiv city",
          "L": "Kyiv",
          "O": "Thingsboard",
          "OU": "DEVELOPER_TEST"
        }
      ]
    },
    "profiles": {
      "server": {
        "expiry": "43800h",
        "key": {
          "algo": "ecdsa",
          "size": 256
        },
        "usages": [
          "signing",
          "key encipherment",
          "server auth"
        ]
      },
      "client": {
        "expiry": "43800h",
        "key": {
          "algo": "ecdsa",
          "size": 256
        },
        "usages": [
          "signing",
          "key encipherment",
          "client auth"
        ]
      },
      "client-server": {
        "expiry": "43800h",
        "key": {
          "algo": "ecdsa",
          "size": 256
        },
        "usages": [
          "signing",
          "key encipherment",
          "server auth",
          "client auth"
        ]
      }
    }
  }
}
CONFIG

tee ./${SERVER_PATH}/ca-root-to-intermediate-config.json 1> /dev/null <<-CONFIG
{
	"signing": {
		"default": {
			"expiry": "43800h",
			"ca_constraint": {
				"is_ca": true,
				"max_path_len": 0,
				"max_path_len_zero": true
			},
      "key": {
        "algo": "ecdsa",
        "size": 256
	    },
			"usages": [
				"digital signature",
				"cert sign",
				"crl sign",
				"signing"
			]
		}
	}
}
CONFIG

echo "===================================================="
echo -e "Generate the root of certificates: \n-${CA_ROOT_KEY}-key.pem (certificate key)\n-${CA_ROOT_KEY}.pem (certificate)\n-${CA_ROOT_KEY}.csr (sign request)"
echo "===================================================="
cfssl genkey \
  -initca \
  - \
  <<-CONFIG | cfssljson -bare ./${SERVER_PATH}/${CA_ROOT_CERT_KEY}
{
  "CN": "ROOT CA for servers",
  "key": {
    "algo": "ecdsa",
    "size": 256
  },
  "names": [
    {
      "C": "UK",
      "ST": "Kyiv city",
      "L": "Kyiv",
      "O": "Thingsboard",
      "OU": "DEVELOPER_TEST"
    }
  ],
  "ca": {
    "expiry": "131400h"
  }
}
CONFIG
CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${SERVER_PATH}/${CA_ROOT_CERT_KEY}.pem)

echo "===================================================="
echo -e "Generate and Signed the first intermediates of our certificates: \n-${CA_INTERMEDIATE_CERT_KEY}-key.pem (certificate key)\n-${CA_INTERMEDIATE_CERT_KEY}.pem (certificate)\n-${CA_INTERMEDIATE_CERT_KEY}.csr (sign request)"
echo "===================================================="
cfssl gencert \
	-ca ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_SIGN}.pem \
	-ca-key ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_SIGN}-key.pem \
	-config ./${SERVER_PATH}/ca-root-to-intermediate-config.json \
	-hostname "${SERVER_HOST_NAME},${SERVER_LOCAL_HOST_NAME}${SERVER_PUBLIC_HOST_NAMES:+, }${SERVER_PUBLIC_HOST_NAMES}" \
	- \
	<<-CONFIG | cfssljson -bare ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}
{
	"CN": "${CA_INTERMEDIATE_CERT_KEY}",
  "names": [
    {
      "C": "UK",
      "ST": "Kyiv city",
      "L": "Kyiv",
      "O": "Thingsboard",
      "OU": "DEVELOPER_TEST"
    }
  ]
}
CONFIG
CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem)


## Lwm2m Server certificate
echo "===================================================="
echo -e "Generate and Signed the server certificate: \n-${SERVER_CERT_KEY}-key.pem (certificate key)\n-${SERVER_CERT_KEY}.pem (certificate)\n-${SERVER_CERT_KEY}.csr (sign request)"
echo "===================================================="
cfssl gencert \
	-ca ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem \
	-ca-key ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}-key.pem \
	-config ./${SERVER_PATH}/ca-config.json \
	-profile server \
	-hostname "${SERVER_HOST_NAME},${SERVER_LOCAL_HOST_NAME}${SERVER_PUBLIC_HOST_NAMES:+, }${SERVER_PUBLIC_HOST_NAMES}" \
	- \
	<<-CONFIG | cfssljson -bare ./${SERVER_PATH}/${SERVER_CERT_KEY}
{
	"CN": "${SERVER_LOCAL_HOST_NAME}"
}
CONFIG

echo "===================================================="
echo -e "Add the server certificate (${SERVER_CERT_KEY}.pem) to keystore: ${SERVER_JKS_FOR_TEST}.jks"
echo "===================================================="
cat ./${SERVER_PATH}/${SERVER_CERT_KEY}.pem ${CA_LIST_CERT_FOR_CAT} > ./${SERVER_PATH}/${SERVER_CERT_CHAIN}.pem
openssl pkcs12 -export -in ./${SERVER_PATH}/${SERVER_CERT_CHAIN}.pem -inkey ./${SERVER_PATH}/${SERVER_CERT_KEY}-key.pem -out ./${SERVER_PATH}/${SERVER_CERT_KEY}.p12 -name ${SERVER_CERT_ALIAS} -CAfile ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem -caname ${CA_ROOT_ALIAS} -passin pass:${STORE_PASS_PWD} -passout pass:${STORE_PASS_PWD}
keytool -importkeystore -deststorepass ${STORE_PASS_PWD} -destkeypass ${STORE_PASS_PWD} -destkeystore ./${SERVER_PATH}/${SERVER_JKS_FOR_TEST}.jks -srckeystore ./${SERVER_PATH}/${SERVER_CERT_KEY}.p12 -srcstoretype PKCS12 -srcstorepass ${STORE_PASS_PWD} -alias ${SERVER_CERT_ALIAS}


CA_INTERMEDIATE_CERT_SIGN=${CA_INTERMEDIATE_CERT_KEY}
CA_INTERMEDIATE_CERT_KEY=$(intermediate_common_name)
CA_INTERMEDIATE_NUMBER=$((${CA_INTERMEDIATE_NUMBER} + 1))
echo "===================================================="
echo -e "Generate and Signed the second intermediates of our certificates: \n-${CA_INTERMEDIATE_CERT_KEY}-key.pem (certificate key)\n-${CA_INTERMEDIATE_CERT_KEY}.pem (certificate)\n-${CA_INTERMEDIATE_CERT_KEY}.csr (sign request)"
echo "===================================================="
cfssl gencert \
	-ca ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_SIGN}.pem \
	-ca-key ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_SIGN}-key.pem \
	-config ./${SERVER_PATH}/ca-root-to-intermediate-config.json \
	-hostname "${SERVER_HOST_NAME},${SERVER_LOCAL_HOST_NAME}${SERVER_PUBLIC_HOST_NAMES:+, }${SERVER_PUBLIC_HOST_NAMES}" \
	- \
	<<-CONFIG | cfssljson -bare ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}
{
	"CN": "${CA_INTERMEDIATE_CERT_KEY}",
  "names": [
    {
      "C": "UK",
      "ST": "Kyiv city",
      "L": "Kyiv",
      "O": "Thingsboard",
      "OU": "DEVELOPER_TEST"
    }
  ]
}
CONFIG
CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem)

## Bootstrap server certificate
echo "===================================================="
echo -e "Generate and Signed the server certificate: \n-${BS_SERVER_CERT_KEY}-key.pem (certificate key)\n-${BS_SERVER_CERT_KEY}.pem (certificate)\n-${BS_SERVER_CERT_KEY}.csr (sign request)"
echo "===================================================="
cfssl gencert \
	-ca ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem \
	-ca-key ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}-key.pem \
	-config ./${SERVER_PATH}/ca-config.json \
	-profile server \
	-hostname "${SERVER_HOST_NAME},${SERVER_LOCAL_HOST_NAME}${SERVER_PUBLIC_HOST_NAMES:+, }${SERVER_PUBLIC_HOST_NAMES}" \
	- \
	<<-CONFIG | cfssljson -bare ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}
{
	"CN": "${SERVER_LOCAL_HOST_NAME}"
}
CONFIG

echo "===================================================="
echo -e "Add the Bootstrap server certificate (${BS_SERVER_CERT_KEY}.pem) to keystore: ${SERVER_JKS_FOR_TEST}.jks"
echo "===================================================="
cat ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}.pem ${CA_LIST_CERT_FOR_CAT} > ./${SERVER_PATH}/${BS_SERVER_CERT_CHAIN}.pem
openssl pkcs12 -export -in ./${SERVER_PATH}/${BS_SERVER_CERT_CHAIN}.pem -inkey ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}-key.pem -out ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}.p12 -name ${BS_SERVER_CERT_ALIAS} -CAfile ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem -caname ${CA_ROOT_ALIAS} -passin pass:${STORE_PASS_PWD} -passout pass:${STORE_PASS_PWD}
keytool -importkeystore -deststorepass ${STORE_PASS_PWD} -destkeypass ${STORE_PASS_PWD} -destkeystore ./${SERVER_PATH}/${SERVER_JKS_FOR_TEST}.jks -srckeystore ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}.p12 -srcstoretype PKCS12 -srcstorepass ${STORE_PASS_PWD} -alias ${BS_SERVER_CERT_ALIAS}


keytool -list -v -keystore ./${SERVER_PATH}/lwm2mserver.jks -storepass server_ks_password -storetype PKCS12

rm ./${SERVER_PATH}/*.p12 2> /dev/null
rm ./${SERVER_PATH}/*.csr 2> /dev/null
rm ./${SERVER_PATH}/*.json 2> /dev/null
rm ./${SERVER_PATH}/${CA_INTERMEDIATE_CERT_KEY_PREF}* 2> /dev/null
rm ./${SERVER_PATH}/${CA_ROOT_CERT_KEY}* 2> /dev/null
mv ./${SERVER_PATH}/${SERVER_CERT_KEY}-key.pem ./${SERVER_PATH}/${SERVER_CERT_KEY}_key.pem
mv ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}-key.pem ./${SERVER_PATH}/${BS_SERVER_CERT_KEY}_key.pem

