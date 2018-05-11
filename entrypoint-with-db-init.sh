#!/bin/bash

if [[ $1 = 'cassandra' ]]; then

  until cqlsh -f/opt/cassandra/schema.cql; do
    echo "cqlsh: Cassandra is unavailable - retrying"
    sleep 2
  done &

fi

exec /docker-entrypoint.sh "$@"
