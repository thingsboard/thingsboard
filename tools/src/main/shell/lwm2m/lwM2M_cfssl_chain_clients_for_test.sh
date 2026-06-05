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


# Change working directory
cd -- "$(
	dirname "${0}"
)" || exit 1

readonly TRUST_PATH="Trust"
readonly CA_ROOT_CERT_KEY="ca-root"
readonly CA_ROOT_ALIAS="root"
readonly CA_INTERMEDIATE_CERT_KEY_PREF="intermediate_ca"
CA_INTERMEDIATE_START=0
CA_INTERMEDIATE_FINISH=2
CA_INTERMEDIATE_NUMBER=${CA_INTERMEDIATE_START}
CA_INTERMEDIATE_CERT_SIGN=${CA_ROOT_CERT_KEY}
CA_LIST_CERT_FOR_CAT=""
readonly CA_TRUST_STORE_ALL_CHAIN="lwm2mtruststorechain"
readonly CA_TRUST_STORE_PWD="server_ks_password"
readonly CA_TRUST_CERT_ALIAS="root"
readonly CA_TRUST_CERT_CHAIN_JKS="lwm2mtruststorechain"
readonly CA_TRUST_STORE_CHAIN_ALIAS="trust_cert_chain_alias"

readonly CLIENT_PATH="Client"
readonly CLIENT_JKS_FOR_TEST="lwm2mclient"
readonly CLIENT_CERT_KEY_PREF="LwX509"
readonly CLIENT_CERT_ALIAS_PREF="client_alias_"
readonly CLIENT_STORE_PWD="client_ks_password"
readonly CLIENT_HOST_NAME="thingsboard_test.io"

readonly TRUST_NO_PATH="TrustNo"
readonly CA_ROOT_NO_ALIAS="root-no"
readonly CLIENT_CERT_TRUST_NO_KEY="LwX509TrustNo"
readonly CLIENT_CERT_ALIAS_TRUST_NO="client_alias_trust_no"

CLIENT_START=0
CLIENT_FINISH=1
CLIENT_NUMBER=${CLIENT_START}

SERVER_HOST_NAME="localhost.localdomain"
SERVER_LOCAL_HOST_NAME="localhost"
SERVER_PUBLIC_HOST_NAMES="-"

readonly CF_COMMANDS="
	cfssl
	cfssljson
"

if [ ! -z "$1" ]; then
  CA_INTERMEDIATE_START=$1
  CA_INTERMEDIATE_NUMBER=${CA_INTERMEDIATE_START}
fi

if [ ! -z "$2" ]; then
  CA_INTERMEDIATE_FINISH=$2
fi

if [ ! -z "$3" ]; then
   CLIENT_START=$1
   CLIENT_NUMBER=${CLIENT_START}
fi

if [ ! -z "$4" ]; then
  CLIENT_FINISH=$4
fi

# Change working directory
rm -rf ${TRUST_PATH}
mkdir -p ${TRUST_PATH}
rm -rf ${TRUST_NO_PATH}
mkdir -p ${TRUST_NO_PATH}
rm -rf ${CLIENT_PATH}
mkdir -p ${CLIENT_PATH}
cd -- "$(
	dirname "${0}"
)" || exit 1


rm *.csr
rm *.p12
rm *.json
rm *.pem
rm *.jks

intermediate_common_name() {
  echo "${CA_INTERMEDIATE_CERT_KEY_PREF}${CA_INTERMEDIATE_NUMBER}"
}

set_list_sert_for_cat() {
  local first="$1"
  echo "$first ${CA_LIST_CERT_FOR_CAT}"
}

client_common_name() {
  echo "${CLIENT_CERT_KEY_PREF}$(printf "%08d" ${CLIENT_NUMBER})"
}

client_alias_name() {
  echo "${CLIENT_CERT_ALIAS_PREF}$(printf "%08d" ${CLIENT_NUMBER})"
}

for COMMAND in ${CF_COMMANDS}; do
	if ! command -v ${COMMAND} &> /dev/null; then
		echo "ERROR: Missing command ${COMMAND}" >&2
		echo "Install the package from: https://pkg.cfssl.org/" >&2
		exit 1
	fi
done

tee ./${TRUST_PATH}/ca-config.json 1> /dev/null <<-CONFIG
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

tee ./${TRUST_PATH}/ca-root-to-intermediate-config.json 1> /dev/null <<-CONFIG
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
echo -e "Generate the root of certificates: \n-${CA_ROOT_CERT_KEY}-key.pem (certificate key)\n-${CA_ROOT_CERT_KEY}.pem (certificate)\n-${CA_ROOT_CERT_KEY}.csr (sign request)"
echo "===================================================="
cfssl genkey \
  -initca \
  - \
  <<-CONFIG | cfssljson -bare ./${TRUST_PATH}/${CA_ROOT_CERT_KEY}
{
  "CN": "ROOT CA",
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
CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${TRUST_PATH}/${CA_ROOT_CERT_KEY}.pem)

echo "===================================================="
echo -e "Generate and Signed the intermediates of our certificates: \n-${CA_INTERMEDIATE_CERT_KEY_PREF}?-key.pem (certificate key)\n-${CA_INTERMEDIATE_CERT_KEY_PREF}?.pem (certificate)\n-${CA_INTERMEDIATE_CERT_KEY_PREF}?.csr (sign request)"
echo "===================================================="

while [[ ${CA_INTERMEDIATE_NUMBER} -lt ${CA_INTERMEDIATE_FINISH} ]];
do
  CA_INTERMEDIATE_CERT_KEY=$(intermediate_common_name)
  CA_INTERMEDIATE_NUMBER=$((${CA_INTERMEDIATE_NUMBER} + 1))

  cfssl gencert \
    -ca ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_SIGN}.pem \
    -ca-key ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_SIGN}-key.pem \
    -config ./${TRUST_PATH}/ca-root-to-intermediate-config.json \
    -hostname "${SERVER_HOST_NAME},${SERVER_LOCAL_HOST_NAME}${SERVER_PUBLIC_HOST_NAMES:+, }${SERVER_PUBLIC_HOST_NAMES}" \
    - \
    <<-CONFIG | cfssljson -bare ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}
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
  #openssl x509 -in ${CA_INTERMEDIATE_CERT_KEY}.pem -text -noout
  CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem)
  CA_INTERMEDIATE_CERT_SIGN=${CA_INTERMEDIATE_CERT_KEY}
done

echo "===================================================="
echo -e "Add the CA_certificate to keystore: ${CA_TRUST_CERT_CHAIN_JKS}.jks"
echo "===================================================="
cat ${CA_LIST_CERT_FOR_CAT} > ./${TRUST_PATH}/${CA_TRUST_STORE_ALL_CHAIN}.pem
openssl pkcs12 -export -in ./${TRUST_PATH}/${CA_TRUST_STORE_ALL_CHAIN}.pem -inkey ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}-key.pem -out ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}.p12 -name ${CA_TRUST_STORE_CHAIN_ALIAS} -CAfile ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem -caname ${CA_ROOT_ALIAS} -passin pass:${CA_TRUST_STORE_PWD} -passout pass:${CA_TRUST_STORE_PWD}
keytool -importkeystore -deststorepass ${CA_TRUST_STORE_PWD} -destkeypass ${CA_TRUST_STORE_PWD} -destkeystore ./${TRUST_PATH}/${CA_TRUST_CERT_CHAIN_JKS}.jks -srckeystore ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}.p12 -srcstoretype PKCS12 -srcstorepass ${CA_TRUST_STORE_PWD} -alias ${CA_TRUST_STORE_CHAIN_ALIAS}

keytool -list -v -keystore ./${TRUST_PATH}/lwm2mtruststorechain.jks -storepass server_ks_password -storetype PKCS12

echo "===================================================="
echo -e "Generate and Signed the clients of our certificates: \n-${CLIENT_CERT_KEY_PREF}?-key.pem (certificate key)\n-${CLIENT_CERT_KEY_PREF}?.pem (certificate)\n-${CCLIENT_CERT_KEY_PREF}?.csr (sign request)"
echo "===================================================="


while [[ ${CLIENT_NUMBER} -lt ${CLIENT_FINISH} ]];
do
  CLIENT_CERT_KEY=$(client_common_name)
  CLIENT_CERT_ALIAS=$(client_alias_name)
  CLIENT_NUMBER=$((${CLIENT_NUMBER} + 1))

  cfssl gencert \
	-ca ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem \
	-ca-key ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}-key.pem \
	-config ./${TRUST_PATH}/ca-config.json \
	-profile client \
	-hostname "${CLIENT_HOST_NAME}" \
	- \
	<<-CONFIG | cfssljson -bare ./${CLIENT_PATH}/${CLIENT_CERT_KEY}
{
	"CN": "${CLIENT_CERT_KEY}"
}
CONFIG

echo "===================================================="
echo -e "Add the client certificate (${CLIENT_CERT_KEY}.pem) to keystore: ${CLIENT_JKS_FOR_TEST}.jks"
echo "===================================================="
cat ./${CLIENT_PATH}/${CLIENT_CERT_KEY}.pem ${CA_LIST_CERT_FOR_CAT} > ./${CLIENT_PATH}/${CLIENT_CERT_KEY}_chain.pem
openssl pkcs12 -export -in ./${CLIENT_PATH}/${CLIENT_CERT_KEY}_chain.pem -inkey ./${CLIENT_PATH}/${CLIENT_CERT_KEY}-key.pem -out ./${CLIENT_PATH}/${CLIENT_CERT_KEY}.p12 -name ${CLIENT_CERT_ALIAS} -CAfile ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem -caname ${CA_ROOT_ALIAS} -passin pass:${CLIENT_STORE_PWD} -passout pass:${CLIENT_STORE_PWD}
keytool -importkeystore -deststorepass ${CLIENT_STORE_PWD} -destkeypass ${CLIENT_STORE_PWD} -destkeystore ./${CLIENT_PATH}/${CLIENT_JKS_FOR_TEST}.jks -srckeystore ./${CLIENT_PATH}/${CLIENT_CERT_KEY}.p12 -srcstoretype PKCS12 -srcstorepass ${CLIENT_STORE_PWD} -alias ${CLIENT_CERT_ALIAS}

done

#keytool -list -v -keystore ./${CLIENT_PATH}/lwm2mclient.jks -storepass client_ks_password -storetype PKCS12

echo "===================================================="
echo -e "Generate the root no trust in ${TRUST_NO_PATH} of certificates: \n-${CA_ROOT_CERT_KEY}-key.pem (certificate key)\n-${CA_ROOT_CERT_KEY}.pem (certificate)\n-${CA_ROOT_CERT_KEY}.csr (sign request)"
echo "===================================================="
cfssl genkey \
  -initca \
  - \
  <<-CONFIG | cfssljson -bare ./${TRUST_NO_PATH}/${CA_ROOT_CERT_KEY}
{
  "CN": "ROOT CA NO TRUST",
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

CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${TRUST_NO_PATH}/${CA_ROOT_CERT_KEY}.pem)

echo "===================================================="
echo -e "Generate and Signed the intermediates of our no trust in ${TRUST_NO_PATH} certificate: \n-${CA_INTERMEDIATE_CERT_KEY_PREF}?-key.pem (certificate key)\n-${CA_INTERMEDIATE_CERT_KEY_PREF}?.pem (certificate)\n-${CA_INTERMEDIATE_CERT_KEY_PREF}?.csr (sign request)"
echo "===================================================="

CA_INTERMEDIATE_CERT_SIGN=${CA_ROOT_CERT_KEY}
CA_LIST_CERT_FOR_CAT=""
CA_INTERMEDIATE_NUMBER=0
while [[ ${CA_INTERMEDIATE_NUMBER} -lt ${CA_INTERMEDIATE_FINISH} ]];
do
  CA_INTERMEDIATE_CERT_KEY=$(intermediate_common_name)
  CA_INTERMEDIATE_NUMBER=$((${CA_INTERMEDIATE_NUMBER} + 1))

  cfssl gencert \
    -ca ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_SIGN}.pem \
    -ca-key ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_SIGN}-key.pem \
    -config ./${TRUST_PATH}/ca-root-to-intermediate-config.json \
    -hostname "${SERVER_HOST_NAME},${SERVER_LOCAL_HOST_NAME}${SERVER_PUBLIC_HOST_NAMES:+, }${SERVER_PUBLIC_HOST_NAMES}" \
    - \
    <<-CONFIG | cfssljson -bare ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_KEY}
  {
    "CN": "${CA_INTERMEDIATE_CERT_KEY}_TRUST_NO",
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
  #openssl x509 -in ${CA_INTERMEDIATE_CERT_KEY}.pem -text -noout
  CA_LIST_CERT_FOR_CAT=$(set_list_sert_for_cat ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem)
  CA_INTERMEDIATE_CERT_SIGN=${CA_INTERMEDIATE_CERT_KEY}
done

echo "===================================================="
echo -e "Generate and Signed the client no trust of our certificate: \n-${CLIENT_CERT_TRUST_NO_KEY}-key.pem (certificate key)\n-${CLIENT_CERT_TRUST_NO_KEY}.pem (certificate)\n-${CLIENT_CERT_TRUST_NO_KEY}.csr (sign request)"
echo "===================================================="

  CLIENT_CERT_ALIAS=$(client_alias_name)
  CLIENT_NUMBER=$((${CLIENT_NUMBER} + 1))

  cfssl gencert \
	-ca ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem \
	-ca-key ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_KEY}-key.pem \
	-config ./${TRUST_PATH}/ca-config.json \
	-profile client \
	-hostname "${CLIENT_HOST_NAME}" \
	- \
	<<-CONFIG | cfssljson -bare ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}
{
	"CN": "${CLIENT_CERT_TRUST_NO_KEY}"
}
CONFIG

echo "===================================================="
echo -e "Add the client certificate no trust (${CLIENT_CERT_TRUST_NO_KEY}.pem) to keystore: ${CLIENT_JKS_FOR_TEST}.jks"
echo "===================================================="
cat ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}.pem ${CA_LIST_CERT_FOR_CAT} > ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}_chain.pem
openssl pkcs12 -export -in ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}_chain.pem -inkey ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}-key.pem -out ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}.p12 -name ${CLIENT_CERT_ALIAS_TRUST_NO} -CAfile ./${TRUST_NO_PATH}/${CA_INTERMEDIATE_CERT_KEY}.pem -caname ${CA_ROOT_NO_ALIAS} -passin pass:${CLIENT_STORE_PWD} -passout pass:${CLIENT_STORE_PWD}
keytool -importkeystore -deststorepass ${CLIENT_STORE_PWD} -destkeypass ${CLIENT_STORE_PWD} -destkeystore ./${CLIENT_PATH}/${CLIENT_JKS_FOR_TEST}.jks -srckeystore ./${CLIENT_PATH}/${CLIENT_CERT_TRUST_NO_KEY}.p12 -srcstoretype PKCS12 -srcstorepass ${CLIENT_STORE_PWD} -alias ${CLIENT_CERT_ALIAS_TRUST_NO}



keytool -list -v -keystore ./${CLIENT_PATH}/lwm2mclient.jks -storepass client_ks_password -storetype PKCS12


rm ./${TRUST_PATH}/*.p12 2> /dev/null
rm ./${TRUST_PATH}/*.csr 2> /dev/null
rm ./${TRUST_PATH}/*.json 2> /dev/null
rm ./${TRUST_PATH}/${CA_ROOT_CERT_KEY}* 2> /dev/null
rm ./${TRUST_PATH}/${CA_INTERMEDIATE_CERT_KEY_PREF}* 2> /dev/null

rm -rf ${TRUST_NO_PATH} 2> /dev/null

rm ./${CLIENT_PATH}/*.p12 2> /dev/null
rm ./${CLIENT_PATH}/*.csr 2> /dev/null
