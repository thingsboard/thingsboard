Here is the list of commands that can be used to quickly install ThingsBoard Edge using docker compose and connect to the server.

#### Prerequisites

Install <a href="https://docs.docker.com/engine/install/" target="_blank"> Docker CE</a> and <a href="https://docs.docker.com/compose/install/" target="_blank"> Docker Compose</a>.

#### Step 1. Create the ThingsBoard Edge Docker Compose file

ThingsBoard Edge supports both **in-memory** and **Kafka** queues for message storage and communication between ThingsBoard services.
It also supports **SQL** and **hybrid** database configurations.
In this guide, we’ll use the **in-memory** queue and an **SQL** database.
For more details about the hybrid setup, please refer to the official installation instructions on the <a href="https://thingsboard.io/docs/user-guide/install/edge/docker/#step-2-choose-queue-andor-database-services" target="_blank">ThingsBoard documentation site</a>.

Now, create a Docker Compose file for the ThingsBoard Edge service:

```bash
nano docker-compose.yml
{:copy-code}
```

Add the following lines to the yml file:

```bash
services:
  mytbedge:
    restart: always
    image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
    ports:
      - "8080:8080"
      - "1883:1883"
      - "5683-5688:5683-5688/udp"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
      CLOUD_ROUTING_KEY: ${CLOUD_ROUTING_KEY}
      CLOUD_ROUTING_SECRET: ${CLOUD_ROUTING_SECRET}
      CLOUD_RPC_HOST: ${BASE_URL}
      CLOUD_RPC_PORT: ${CLOUD_RPC_PORT}
      CLOUD_RPC_SSL_ENABLED: ${CLOUD_RPC_SSL_ENABLED}
    ${EXTRA_HOSTS}
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
  postgres:
    restart: always
    image: "postgres:16"
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

##### [Optional] Update Bind Ports
If ThingsBoard Edge runs on the same machine as the ThingsBoard Server, you need to update the port configuration to avoid conflicts between the two services.

Make sure that ports **18080**, **11883**, and **15683–15688** are not being used by any other applications.

Then, update the port configuration in the `docker-compose.yml` file accordingly:

```bash
sed -i 's/8080:8080/18080:8080/; s/1883:1883/11883:1883/; s/5683-5688:5683-5688\/udp/15683-15688:5683-5688\/udp/' docker-compose.yml
{:copy-code}
```

#### Step 2. Start ThingsBoard Edge
Navigate to the directory containing the `docker-compose.yml` file and run the following command to start the ThingsBoard Edge service:

```bash
docker compose up -d && docker compose logs -f mytbedge
{:copy-code}
```

#### Step 3. Open ThingsBoard Edge UI

Once the Edge service has started, open the Edge web interface at http://localhost:8080, or http://localhost:18080 if you modified the HTTP bind port configuration in the previous step.

Log in using your **tenant credentials** from either your local ThingsBoard Server or the **ThingsBoard Live Demo**.
