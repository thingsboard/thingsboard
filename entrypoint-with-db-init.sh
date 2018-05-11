#!/bin/bash

if [[ $1 = 'cassandra' ]]; then
  # Create default keyspace for single node cluster

  until cqlsh -f/opt/cassandra/schema.cql; do
    echo "cqlsh: Cassandra is unavailable - retring"
    sleep 2
  done &
fi

exec /docker-entrypoint.sh "$@"
