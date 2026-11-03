#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

firstlaunch=${DATA_FOLDER}/.firstlaunch

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