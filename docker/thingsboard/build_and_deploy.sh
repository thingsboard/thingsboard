#!/bin/bash

cp ../../application/target/thingsboard.deb thingsboard.deb

docker build -t thingsboard/application:0.1 .

docker login

docker push thingsboard/application:0.1