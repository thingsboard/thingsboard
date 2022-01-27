#!/usr/bin/env bash

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

Help()
{
   # Display Help
   echo "Description of the script functions."
   echo
   echo "Syntax: scriptTemplate [-g|h|v|V]"
   echo "options:"
   echo "h     Print this Help."
   echo "v     Verbose mode."
   echo "V     Print software version and exit."
   echo
}

if [ "$1" == "-h" ] ; then
    echo -e "Usage 2: ./`basename $0` \"Information is not displayed\" : \"Keys for the server are generated\" : \"Keys for the clients and trusts are generated\""
    echo -e "Usage 1: ./`basename $0` true \"Information is displayed\" : \"Keys for the server are generated\" : \"Keys for the clients and trusts are generated\""
    echo -e "Usage 3: ./`basename $0` true false \"Information is displayed\" : \"Keys for the server are not generated\" : \"Keys for the clients and trusts are generated\""
    echo -e "Usage 4: ./`basename $0` true false false \"Information is displayed\" : \"Keys for the server are not generated\" : \"Keys for the clients and trusts are not generated\""
    echo -e "Usage 4: ./`basename $0` true true false \"Information is displayed\" : \"Keys for the server are generated\" : \"Keys for the clients and trusts are not generated\""
    echo "This Help File: ./`basename $0` -h"
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

if  [ "$IS_IHFO" = false ] ; then
  if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
    ./lwm2m_cfssl_chain_server_for_test.sh > /dev/null 2>&1 &
  fi
  if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
    ./lwM2M_cfssl_chain_trusts_and_clients_for_test.sh ${INTERMEDIATE_START} ${INTERMEDIATE_FINISH} ${CLIENT_START} ${CLIENT_FINISH} > /dev/null 2>&1 &
  fi
else
    if [ "$IS_SERVER_CREATED_KEY" = true ] ; then
    ./lwm2m_cfssl_chain_server_for_test.sh
  fi
  if [ "$IS_TRUST_CLIENT_CREATED_KEY" = true ] ; then
    ./lwM2M_cfssl_chain_trusts_and_clients_for_test.sh ${INTERMEDIATE_START} ${INTERMEDIATE_FINISH} ${CLIENT_START} ${CLIENT_FINISH}
  fi
fi