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
##### Backup Database
Make a copy of the database folder before upgrading:

```bash
sudo cp -r ~/.mytb-edge-data/db ~/.mytb-edge-db-BACKUP
{:copy-code}
```
