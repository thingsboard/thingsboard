#!/bin/bash

cp ../../dao/src/main/resources/schema.cql schema.cql
cp ../../dao/src/main/resources/demo-data.cql demo-data.cql
cp ../../dao/src/main/resources/system-data.cql system-data.cql

docker build -t thingsboard/thingsboard-db-schema:1.0 .

docker login

docker push thingsboard/thingsboard-db-schema:1.0