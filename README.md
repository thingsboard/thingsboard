# Thingsboard


<img src="./img/logo.png?raw=true" width="100" height="100">

## Introduction

Tningsboard is an open-source IoT platform for data collection, processing, visualization, and device management.

![Dashboard](./img/dashboard.gif?raw=true "Real-time Fleet Dashboard")


## Community


## Contribute


## Support


## Docker usage

**start platform using docker:**
- install docker
- cd to 'docker' folder
- create folder for cassandra data directory on your local env (host)
  - `mkdir /home/user/data_dir`
- modify .env file to point to the directory created in previous step
- start ./deploy.sh script to run all the services


**start-up for local development** 

cassandra with thingsboard schema (9042 and 9061 ports are exposed).  
zookeper services (2181 port is exposed).  
9042, 9061 and 2181 ports must be free so 'Thingsboard' server that is running outside docker container is able to connect to services.  
you can change these ports in docker-compose.static.yml file to some others, but 'Thingsbaord' application.yml file must be updated accordingly.    
if you would like to change cassandra port, change it to "9999:9042" for example and update cassandra.node_list entry in application.yml file to localhost:9999.  

- install docker
- cd to 'docker' folder
- create folder for cassandra data directory on your local env (host)
  - `mkdir /home/user/data_dir`
- modify .env file to point to the directory created in previous step
- start ./deploy_cassandra_zookeeper.sh script to run cassandra with thingsboard schema and zookeper services
- Start boot class: _org.thingsboard.server.ThingsboardServerApplication_

## Licenses

This project is released under [Apache 2.0 license](./LICENSE).