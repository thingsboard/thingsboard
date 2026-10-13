Here is the list of commands that can be used to quickly upgrade ThingsBoard Edge on Docker (Linux or macOS).

#### Prepare for Upgrading ThingsBoard Edge
Set the terminal in the directory which contains the `docker-compose.yml` file and execute the following command
to stop and remove currently running TB Edge container:

```bash
docker compose stop
docker compose rm mytbedge
{:copy-code}
```

##### Backup Database
Make a copy of the database volume before upgrading:

```bash
docker run --rm -v tb-edge-postgres-data:/source -v tb-edge-postgres-data-backup:/backup busybox sh -c "cp -a /source/. /backup"
{:copy-code}
```
