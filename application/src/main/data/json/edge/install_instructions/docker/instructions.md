## Install edge and connect to cloud instructions

Here is the list of commands, that can be used to quickly install and connect edge to the cloud using docker compose.

### Prerequisites

Install <a href="https://docs.docker.com/engine/install/" target="_blank"> Docker CE</a> and <a href="https://docs.docker.com/compose/install/" target="_blank"> Docker Compose</a>.

### Create data and logs folders

Run following commands to create directories for storing data and logs. These commands will change directories owner to *thingsboard* docker container user. 
**chown** command requires sudo permissions to be able to change owner (command will request password for a sudo access):

```bash
mkdir -p ~/.mytb-edge-data && sudo chown -R 799:799 ~/.mytb-edge-data
mkdir -p ~/.mytb-edge-logs && sudo chown -R 799:799 ~/.mytb-edge-logs
{:copy-code}
```

### Running ThingsBoard Edge as docker service

Create docker compose file for ThingsBoard Edge service:

```bash
nano docker-compose.yml
{:copy-code}
```

${LOCALHOST_WARNING}

Add the following lines to the yml file:

```bash

# Uncomment the following line if you are using docker-compose and not docker compose V2
# version: '2.2'

services:
  mytbedge:
    restart: always
    image: "thingsboard/tb-edge:${EDGE_VERSION}"
    ports:
      - "8080:8080"
      - "1883:1883"
      - "5683-5688:5683-5688/udp"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb_edge
      CLOUD_ROUTING_KEY: ${CLOUD_ROUTING_KEY}
      CLOUD_ROUTING_SECRET: ${CLOUD_ROUTING_SECRET}
      CLOUD_RPC_HOST: ${BASE_URL}
    volumes:
      - ~/.mytb-edge-data:/data
      - ~/.mytb-edge-logs:/var/log/tb-edge
  postgres:
    restart: always
    image: "postgres:12"
    ports:
      - "5432"
    environment:
      POSTGRES_DB: tb_edge
      POSTGRES_PASSWORD: postgres
    volumes:
      - ~/.mytb-edge-data/db:/var/lib/postgresql/data
{:copy-code}
```

#### [Optional] Update bind ports 
If ThingsBoard Edge is going to be running on the same machine where ThingsBoard server (cloud) is running you'll need to update docker compose port mapping.
Please update next lines of docker compose:

```bash
ports:
  - "18080:8080"
  - "11883:1883"
  - "15683-15688:5683-5688/udp"
```
Please make sure ports above are not used by any other application.

#### Start ThingsBoard Edge
Execute the following commands to pull and up using docker compose V2:

```bash
docker compose pull
docker compose up
{:copy-code}
```

*NOTE*: If you are using outdated docker-compose (we recommend to use docker compose V2), execute the following commands:

```bash
docker-compose pull
docker-compose up
{:copy-code}
```

#### Open ThingsBoard Edge UI

Once started, you will be able to open ThingsBoard Edge UI using the following link http://localhost:8080

*NOTE*: If you updated bind ports (optional step) please use updated port instead for Edge UI URL
http://localhost:YOUR_UPDATED_PORT
