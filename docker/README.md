# Docker configuration for ThingsBoard Microservices

This folder containing scripts and Docker Compose configurations to run ThingsBoard in Microservices mode.

## Prerequisites

ThingsBoard Microservices are running in dockerized environment.
Before starting please make sure [Docker CE](https://docs.docker.com/install/) and [Docker Compose](https://docs.docker.com/compose/install/) are installed in your system.

## Installation

Before performing initial installation you can configure the type of database to be used with ThingsBoard.
In order to set database type change the value of `DATABASE` variable in `.env` file to one of the following:

- `postgres` - use PostgreSQL database;
- `hybrid` - use PostgreSQL for entities database and Cassandra for timeseries database;

**NOTE**: According to the database type corresponding docker service will be deployed (see `docker-compose.postgres.yml`, `docker-compose.hybrid.yml` for details).

In order to set cache type change the value of `CACHE` variable in `.env` file to one of the following:

- `valkey` - use Valkey standalone cache (1 node - 1 primary);
- `valkey-cluster` - use Valkey cluster cache (6 nodes - 3 primaries, 3 replicas);
- `valkey-sentinel` - use Valkey sentinel cache (3 nodes - 1 primary, 1 replica, 1 sentinel)

**NOTE**: According to the cache type corresponding docker service will be deployed (see `docker-compose.valkey.yml`, `docker-compose.valkey-cluster.yml`, `docker-compose.valkey-sentinel.yml` for details).

Execute the following command to create log folders for the services and chown of these folders to the docker container users. 
To be able to change user, **chown** command is used, which requires sudo permissions (script will request password for a sudo access): 

`
$ ./docker-create-log-folders.sh
`

Execute the following command to run installation:

`
$ ./docker-install-tb.sh --loadDemo
`

Where:

- `--loadDemo` - optional argument. Whether to load additional demo data.

## Running

Execute the following command to start services:

`
$ ./docker-start-services.sh
`

After a while when all services will be successfully started you can open `http://{your-host-ip}` in you browser (for ex. `http://localhost`).
You should see ThingsBoard login page.

Use the following default credentials:

- **System Administrator**: sysadmin@thingsboard.org / sysadmin

If you installed DataBase with demo data (using `--loadDemo` flag) you can also use the following credentials:

- **Tenant Administrator**: tenant@thingsboard.org / tenant
- **Customer User**: customer@thingsboard.org / customer

In case of any issues you can examine service logs for errors.
For example to see ThingsBoard node logs execute the following command:

`
$ docker-compose logs -f tb-core1 tb-core2 tb-rule-engine1 tb-rule-engine2 tb-mqtt-transport1 tb-mqtt-transport2
`

Or use `docker-compose ps` to see the state of all the containers.
Use `docker-compose logs --f` to inspect the logs of all running services.
See [docker-compose logs](https://docs.docker.com/compose/reference/logs/) command reference for details.

Execute the following command to stop services:

`
$ ./docker-stop-services.sh
`

Execute the following command to stop and completely remove deployed docker containers:

`
$ ./docker-remove-services.sh
`

Execute the following command to update particular or all services (pull newer docker image and rebuild container):

`
$ ./docker-update-service.sh [SERVICE...]
`

Where:

- `[SERVICE...]` - list of services to update (defined in docker-compose configurations). If not specified all services will be updated.

## Upgrading

In case when database upgrade is needed, execute the following commands:

```
$ ./docker-stop-services.sh
$ ./docker-upgrade-tb.sh --fromVersion=[FROM_VERSION]
$ ./docker-start-services.sh
```

Where:

- `FROM_VERSION` - from which version upgrade should be started. See [Upgrade Instructions](https://thingsboard.io/docs/user-guide/install/upgrade-instructions) for valid `fromVersion` values.


## Monitoring

If you want to enable monitoring with Prometheus and Grafana you need to set <b>MONITORING_ENABLED</b> environment variable to <b>true</b>.
After this Prometheus and Grafana containers will be deployed. You can reach Prometheus at `http://localhost:9090` and Grafana at `http://localhost:3000` (default login is `admin` and password `foobar`).
To change Grafana password you need to update `GF_SECURITY_ADMIN_PASSWORD` environment variable at `./monitoring/grafana/config.monitoring` file.
Dashboards are loaded from `./monitoring/grafana/provisioning/dashboards` directory.

If you want to add new monitoring jobs for Prometheus update `./monitoring/prometheus/prometheus.yml` file.