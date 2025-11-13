Modify ‘main’ docker compose (`docker-compose.yml`) a file for ThingsBoard Edge and update a version of the image:
```bash
nano docker-compose.yml
{:copy-code}
```

```text
services:
    mytbedge:
        restart: always
        image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
...
```

Make sure your image is set to **tb-edge-${TB_EDGE_VERSION}**.
Execute the following commands to up this docker compose directly:

```bash
docker compose up -d
docker compose logs -f mytbedge
{:copy-code}
```
