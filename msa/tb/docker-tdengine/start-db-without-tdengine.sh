#!/bin/bash

firstlaunch=${DATA_FOLDER}/.firstlaunch

host=${TD_HOST}

ls -ahl /var/lib/taos

PG_CTL=$(find /usr/lib/postgresql/ -name pg_ctl)

if [ ! -d ${PGDATA} ]; then
    mkdir -p ${PGDATA}
    ${PG_CTL} initdb
fi

echo "Starting Postgresql..."
${PG_CTL} start

RETRIES="${PG_ISREADY_RETRIES:-300}"
until pg_isready -U ${pkg.user} -h ${PG_HOST} --quiet || [ $RETRIES -eq 0 ]
do
    echo "Connecting to Postgres, $((RETRIES--)) attempts left..."
    sleep 1
done

if [ ! -f ${firstlaunch} ]; then
    echo "Creating database..."
    psql -U ${pkg.user} -d postgres -c "CREATE DATABASE thingsboard"
fi
echo "Postgresql is ready"