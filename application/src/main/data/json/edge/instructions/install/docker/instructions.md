Here is the list of commands, that can be used to quickly install ThingsBoard Edge using docker compose and connect to the server.

#### Prerequisites

Install <a href="https://docs.docker.com/engine/install/" target="_blank"> Docker CE</a> and <a href="https://docs.docker.com/compose/install/" target="_blank"> Docker Compose</a>.

#### Step 1. Running ThingsBoard Edge

Here you can find ThingsBoard Edge docker image:

<a href="https://hub.docker.com/r/thingsboard/tb-edge" target="_blank"> thingsboard/tb-edge</a>

#### Step 2. Choose Queue and/or Database Services

ThingsBoard Edge is able to use different messaging systems/brokers for storing the messages and communication between ThingsBoard services. 
How to choose the right queue implementation?

In Memory queue implementation is built-in and default. It is useful for development(PoC) environments and is not suitable for production deployments or any sort of cluster deployments.

Kafka is recommended for production deployments. This queue is used on the most of ThingsBoard production environments now.

Hybrid implementation combines PostgreSQL and Cassandra databases with Kafka queue service. It is recommended if you plan to manage 1M+ devices in production or handle high data ingestion rate (more than 5000 msg/sec).

Create a docker compose file for the ThingsBoard Edge service:

##### In Memory

```bash
nano docker-compose.yml
{:copy-code}
```

Add the following lines to the yml file:

```bash
version: '3.8'
services:
  mytbedge:
    restart: always
    image: "thingsboard/tb-edge:3.9.1EDGE"
    ports:
      - "8080:8080"
      - "1883:1883"
      - "5683-5688:5683-5688/udp"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
      CLOUD_ROUTING_KEY: PUT_YOUR_EDGE_KEY_HERE # e.g. 19ea7ee8-5e6d-e642-4f32-05440a529015
      CLOUD_ROUTING_SECRET: PUT_YOUR_EDGE_SECRET_HERE # e.g. bztvkvfqsye7omv9uxlp
      CLOUD_RPC_HOST: PUT_YOUR_CLOUD_IP # e.g. 192.168.1.1 or demo.thingsboard.io
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
  postgres:
    restart: always
    image: "postgres:15"
    ports:
      - "5432"
    environment:
      POSTGRES_DB: tb-edge
      POSTGRES_PASSWORD: postgres
    volumes:
      - tb-edge-postgres-data:/var/lib/postgresql/data

volumes:
  tb-edge-data:
    name: tb-edge-data
  tb-edge-logs:
    name: tb-edge-logs
  tb-edge-postgres-data:
    name: tb-edge-postgres-data
{:copy-code}
```

##### [Optional] Update bind ports 
If ThingsBoard Edge is set to run on the same machine where the ThingsBoard server is operating, you need to update port configuration to prevent port collision between the ThingsBoard server and ThingsBoard Edge.

Ensure that the ports 18080, 11883, 15683-15688 are not used by any other application.

Then, update the port configuration in the docker-compose.yml file:
```bash
sed -i ‘s/8080:8080/18080:8080/; s/1883:1883/11883:1883/; s/5683-5688:5683-5688\/udp/15683-15688:5683-5688\/udp/’ docker-compose.yml
{:copy-code}
```
#### Start ThingsBoard Edge
Set the terminal in the directory which contains the docker-compose.yml file and execute the following commands to up this docker compose directly:

```bash
docker compose up -d && docker compose logs -f mytbedge
{:copy-code}
```

###### NOTE: Docker Compose V2 vs docker-compose (with a hyphen)

ThingsBoard supports Docker Compose V2 (Docker Desktop or Compose plugin) starting from **3.4.2** release, because **docker-compose** as standalone setup is no longer supported by Docker.
We **strongly** recommend to update to Docker Compose V2 and use it.
If you still rely on using Docker Compose as docker-compose (with a hyphen), then please execute the following commands to start ThingsBoard Edge:

```bash
docker-compose up -d
docker-compose logs -f mytbedge
```

#### Step 3. Open ThingsBoard Edge UI

Once the Edge service is started, open the Edge UI at http://localhost:8080.

###### NOTE: Edge HTTP bind port update 

If the Edge HTTP bind port was changed to 18080 during Edge installation, access the ThingsBoard Edge instance at http://localhost:18080.

Please use your tenant credentials from local Server instance or ThingsBoard Live Demo to log in to the ThingsBoard Edge.
