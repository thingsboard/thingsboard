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

firstlaunch=${DATA_FOLDER}/.firstlaunch

export PG_CTL=$(find /usr/lib/postgresql/ -name pg_ctl)

if [ ! -d ${PGDATA} ]; then
    mkdir -p ${PGDATA}
    chown -R postgres:postgres ${PGDATA}
    su postgres -c '${PG_CTL} initdb -U postgres'
fi

su postgres -c '${PG_CTL} -l /var/log/postgres/postgres.log -w start'

if [ ! -f ${firstlaunch} ]; then
    su postgres -c 'psql -U postgres -d postgres -c "CREATE DATABASE thingsboard"'
fi

cassandra_data_dir=${CASSANDRA_DATA}
cassandra_data_link=/var/lib/cassandra

if [ ! -L ${cassandra_data_link} ]; then
    if [ -d ${cassandra_data_link} ]; then
        rm -rf ${cassandra_data_link}
    fi
    if [ ! -d ${cassandra_data_dir} ]; then
        mkdir -p ${cassandra_data_dir}
        chown -R cassandra:cassandra ${cassandra_data_dir}
    fi
    ln -s ${cassandra_data_dir} ${cassandra_data_link}
fi

service cassandra start

until nmap $CASSANDRA_HOST -p $CASSANDRA_PORT | grep "$CASSANDRA_PORT/tcp open"
do
  echo "Wait for cassandra db to start..."
  sleep 5
done
