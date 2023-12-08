Modify ‘main’ docker compose (`docker-compose.yml`) file for ThingsBoard Edge and update version of the image:
```bash
nano docker-compose.yml
{:copy-code}
```

```text
version: '3.8'
services:
    mytbedge:
        restart: always
        image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
...
```

Make sure your image is the set to **tb-edge-${TB_EDGE_VERSION}**.
Execute the following commands to up this docker compose directly:

```bash
docker compose up -d
docker compose logs -f mytbedge
{:copy-code}
```
