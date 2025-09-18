Here is the list of commands, that can be used to quickly upgrade ThingsBoard Edge on Docker (Linux or MacOS).

#### Prepare for upgrading ThingsBoard Edge
Set the terminal in the directory which contains the `docker-compose.yml` file and execute the following command
to stop and remove currently running TB Edge container:

```bash
docker compose stop
docker compose rm mytbedge
{:copy-code}
```

**OPTIONAL:** If you still rely on Docker Compose as docker-compose (with a hyphen) here is the list of the above commands:
```text
docker-compose stop
docker-compose rm mytbedge
```

##### Migrating Data from Docker Bind Mount Folders to Docker Volumes
Starting with the **3.6.2** release, the ThingsBoard team has transitioned from using Docker bind mount folders to Docker volumes.
This change aims to enhance security and efficiency in storing data for Docker containers and to mitigate permission issues across various environments.

To migrate from Docker bind mounts to Docker volumes, please execute the following commands:

```bash
docker run --rm -v tb-edge-data:/volume -v ~/.mytb-edge-data:/backup busybox sh -c "cp -a /backup/. /volume"
docker run --rm -v tb-edge-logs:/volume -v ~/.mytb-edge-logs:/backup busybox sh -c "cp -a /backup/. /volume"
docker run --rm -v tb-edge-postgres-data:/volume -v ~/.mytb-edge-data/db:/backup busybox sh -c "cp -a /backup/. /volume"
{:copy-code}
```

After completing the data migration to the newly created Docker volumes, you'll need to update the volume mounts in your Docker Compose configuration.
Modify the `docker-compose.yml` file for ThingsBoard Edge to update the volume settings.

Update volume mounts. Locate the following snippet:
```text
    volumes:
      - ~/.mytb-edge-data:/data
      - ~/.mytb-edge-logs:/var/log/tb-edge
...
```

And replace it with:
```text
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
...
```

Apply a similar update for the PostgreSQL service. Find the section:
```text
    volumes:
      - ~/.mytb-edge-data/db:/var/lib/postgresql/data
...
```

And replace it with:
```text
    volumes:
      - tb-edge-postgres-data:/var/lib/postgresql/data
...
```

Finally, please add next volumes section at the end of the file:
```text
...
volumes:
  tb-edge-data:
    name: tb-edge-data
  tb-edge-logs:
    name: tb-edge-logs
  tb-edge-postgres-data:
    name: tb-edge-postgres-data
```

##### Backup Database
Make a copy of the database volume before upgrading:

```bash
docker run --rm -v tb-edge-postgres-data:/source -v tb-edge-postgres-data-backup:/backup busybox sh -c "cp -a /source/. /backup"
{:copy-code}
```
