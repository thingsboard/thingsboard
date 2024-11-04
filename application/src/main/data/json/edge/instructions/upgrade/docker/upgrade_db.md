Create docker compose file for ThingsBoard Edge upgrade process:

```bash
> docker-compose-upgrade.yml && nano docker-compose-upgrade.yml
{:copy-code}
```

Add the following lines to the yml file:

```bash
version: '3.8'
services:
  mytbedge:
    restart: on-failure
    image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
    entrypoint: upgrade-tb-edge.sh
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
