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


readonly INTERMEDIATE_START=0
readonly INTERMEDIATE_FINISH=2
readonly CLIENT_START=0
readonly CLIENT_FINISH=5

IS_IHFO=false
IS_SERVER_CREATED_KEY=true
IS_TRUST_CLIENT_CREATED_KEY=true

cd -- "$(
	dirname "${0}"
)" || exit 1

ResultInfo()
{
#   # Display Help
#   echo "Description of the script functions."
#   echo
#   echo "Syntax: scriptTemplate [-g|h|v|V]"
#   echo "options:"
#   echo "h     Print this Help."
#   echo "v     Verbose mode."
#   echo "V     Print software version and exit."
#   echo
if  [ "$IS_IHFO" = false ] ; then
  if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
    ./lwm2m_cfssl_chain_server_for_test.sh > /dev/null 2>&1 &
  fi
  if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
    ./lwM2M_cfssl_chain_clients_for_test.sh ${INTERMEDIATE_START} ${INTERMEDIATE_FINISH} ${CLIENT_START} ${CLIENT_FINISH} > /dev/null 2>&1 &
  fi
else
    if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
    ./lwm2m_cfssl_chain_server_for_test.sh
  fi
  if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
    ./lwM2M_cfssl_chain_clients_for_test.sh ${INTERMEDIATE_START} ${INTERMEDIATE_FINISH} ${CLIENT_START} ${CLIENT_FINISH}
  fi
fi
}

if [ "$1" == "-h" ] ||[ "$1" == "-?" ] || [ "$1" == "-help" ] ; then
    echo -e "Usage 1:  \"Information is not displayed\" : \"Keys for the server are generated\" : \"Keys for the clients and trusts are generated\"\n./`basename $0`"
    echo -e "Usage 2:  \"Information is displayed\" : \"Keys for the server are generated\" : \"Keys for the clients and trusts are generated\"\n./`basename $0` true \n./`basename $0` true true true "
    echo -e "Usage 3:  \"Information is displayed\" : \"Keys for the server are not generated\" : \"Keys for the clients and trusts are generated\"\n./`basename $0` true false \n./`basename $0` true false true"
    echo -e "Usage 4:  \"Information is displayed\" : \"Keys for the server are not generated\" : \"Keys for the clients and trusts are not generated\"\n./`basename $0` true false false"
    echo -e "Usage 5:  \"Information is displayed\" : \"Keys for the server are generated\" : \"Keys for the clients and trusts are not generated\"\n./`basename $0` true true false"
    echo -e "This Help File: \n./`basename $0` -h | -? | -help"
    exit 0
fi

if [ -n "$1" ]; then
  IS_IHFO=$1
fi

if [ -n "$2" ]; then
  IS_SERVER_CREATED_KEY=$2
fi

if [ -n "$3" ]; then
  IS_TRUST_CLIENT_CREATED_KEY=$3
fi

if  [ "$IS_SERVER_CREATED_KEY" = false ] && [ "$IS_TRUST_CLIENT_CREATED_KEY" = false ] ; then
  echo -e "Result is null"
  echo -e "This Help File: \n./`basename $0` -h | -? | -help"
  exit 0
fi



if  [ "$IS_IHFO" = false ] ; then
  if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
    ./lwm2m_cfssl_chain_server_for_test.sh > /dev/null 2>&1 &
  fi
  if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
    ./lwM2M_cfssl_chain_clients_for_test.sh ${INTERMEDIATE_START} ${INTERMEDIATE_FINISH} ${CLIENT_START} ${CLIENT_FINISH} > /dev/null 2>&1 &
  fi
else
    if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
    ./lwm2m_cfssl_chain_server_for_test.sh
  fi
  if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
    ./lwM2M_cfssl_chain_clients_for_test.sh ${INTERMEDIATE_START} ${INTERMEDIATE_FINISH} ${CLIENT_START} ${CLIENT_FINISH}
  fi
fi

echo -e "Result into:"
if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
   echo -e "./Server"
fi
if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
  echo -e "./Client"
  echo -e "./Trust"
fi

echo -e "Finish"