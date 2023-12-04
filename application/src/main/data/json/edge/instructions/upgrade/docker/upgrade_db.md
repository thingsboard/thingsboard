${CLEAR_DOCKER_UPGRADE}

Create docker compose file for ThingsBoard Edge upgrade process:

```bash
nano docker-compose-upgrade.yml
{:copy-code}
```

Add the following lines to the yml file:

```bash
version: '3.0'
services:
  mytbedge:
    restart: on-failure
    image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
    volumes:
      - ~/.mytb-edge-data:/data
      - ~/.mytb-edge-logs:/var/log/tb-edge
    entrypoint: upgrade-tb-edge.sh
  postgres:
    restart: always
    image: "postgres:15"
    ports:
      - "5432"
    environment:
      POSTGRES_DB: tb-edge
      POSTGRES_PASSWORD: postgres
    volumes:
      - ~/.mytb-edge-data/db:/var/lib/postgresql/data
{:copy-code}
```

Execute the following command to start upgrade process:

```bash
docker compose -f docker-compose-upgrade.yml up
{:copy-code}
```

Once upgrade process successfully completed, exit from the docker-compose shell by this combination:

```text
Ctrl + C
```

Execute the following command to stop TB Edge upgrade container:

```bash
docker compose -f docker-compose-upgrade.yml stop
{:copy-code}
```
