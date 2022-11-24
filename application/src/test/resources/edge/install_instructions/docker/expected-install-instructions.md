## Install edge and connect to cloud instructions

### Localhost warning

Localhost cannot be used for docker install - please update baseUrl to the IP address of your machine!

Here is the list of commands, that can be used to quickly install and connect edge to the cloud using docker-compose.

### Prerequisites

Install <a href="https://docs.docker.com/engine/install/" target="_blank"> Docker CE</a> and <a href="https://docs.docker.com/compose/install/" target="_blank"> Docker Compose</a>.

### Create data and logs folders

Run following commands to create a directory for storing data and logs and then change its owner to docker container user, to be able to change user, chown command is used, which requires sudo permissions (command will request password for a sudo access):

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

Add the following lines to the yml file:

```
version: '2.2'
services:
mytbedge:
restart: always
image: "thingsboard/tb-edge:3.4.1EDGE"
ports:
- "8080:8080"
- "1883:1883"
- "5683-5688:5683-5688/udp"
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
CLOUD_ROUTING_KEY: 7390c3a6-69b0-9910-d155-b90aca4b772e
CLOUD_ROUTING_SECRET: l7q4zsjplzwhk16geqxy
CLOUD_RPC_HOST: localhost
volumes:
- ~/.mytb-edge-data:/data
- ~/.mytb-edge-logs:/var/log/tb-edge
postgres:
restart: always
image: "postgres:12"
ports:
- "5432"
environment:
POSTGRES_DB: tb-edge
POSTGRES_PASSWORD: postgres
volumes:
- ~/.mytb-edge-data/db:/var/lib/postgresql/data
{:copy-code}
```

### Ports warning [Optional]
If ThingsBoard Edge is going to be running on the same machine where ThingsBoard server is running youâ€™ll need to update docker compose port mapping.
Please update next lines of docker compose:
ports:
- â€œ18080:8080â€
- â€œ11883:1883â€
- â€œ15683-15688:5683-5688/udpâ€
  Please make sure ports above are not used by any other application.


Execute the following commands to up this docker compose directly:

```bash
docker-compose pull
docker-compose up
{:copy-code}
```

### Open ThingsBoard Edge UI

Once started, you will be able to open ThingsBoard Edge UI using the following link http://localhost:8080.

If during installation process you have changed edge HTTP_BIND_PORT please use that port instead for Edge UI URL:
http://localhost:HTTP_BIND_PORT
