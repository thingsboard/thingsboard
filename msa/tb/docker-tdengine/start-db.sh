#!/bin/bash

firstlaunch=${DATA_FOLDER}/.firstlaunch

ls -ahl /var/lib/taos

PG_CTL=$(find /usr/lib/postgresql/ -name pg_ctl)

if [ ! -d ${PGDATA} ]; then
    mkdir -p ${PGDATA}
    ${PG_CTL} initdb
fi

echo "Starting Postgresql..."
${PG_CTL} start

RETRIES="${PG_ISREADY_RETRIES:-300}"
until pg_isready -U ${pkg.user} -d postgres --quiet || [ $RETRIES -eq 0 ]
do
    echo "Connecting to Postgres, $((RETRIES--)) attempts left..."
    sleep 1
done

if [ ! -f ${firstlaunch} ]; then
    echo "Creating database..."
    psql -U ${pkg.user} -d postgres -c "CREATE DATABASE thingsboard"
fi
echo "Postgresql is ready"

taosd &
pgrep taosadapter || taosadapter >> /dev/null 2>&1 &

until (taos -t |grep "service ok")
do
    echo "Connecting to TDengine..."
    sleep 1
done

echo "Creating taos database..."
if [ ! -f ${firstlaunch} ]; then
    taos -s 'create database if not exists thingsboard;' 2>&1
fi

echo "taos is ready"