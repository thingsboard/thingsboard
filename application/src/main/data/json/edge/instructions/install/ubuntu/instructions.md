Here is the list of commands, that can be used to quickly install ThingsBoard Edge on Ubuntu Server and connect to the server.

#### Step 1. Install Java 17 (OpenJDK)
ThingsBoard service is running on Java 17. To install OpenJDK 17, follow these instructions:

```bash
sudo apt update && sudo apt install openjdk-17-jdk
{:copy-code}
```

Configure your operating system to use OpenJDK 17 by default. You can configure the default version by running the following command:

```bash
sudo update-alternatives --config java
{:copy-code}
```

To check the installed Java version on your system, use the following command:

```bash
java -version
{:copy-code}
```

The expected result is:

```text
openjdk version "17.x.xx" 
OpenJDK Runtime Environment (...)
OpenJDK 64-Bit Server VM (...)
```

#### Step 2. Configure ThingsBoard Edge Database

ThingsBoard Edge supports SQL and hybrid database approaches.
In this guide we will use SQL only.
For hybrid details please follow official installation instructions from the ThingsBoard documentation site.

### Configure PostgreSQL
ThingsBoard Edge uses PostgreSQL database as a local storage.

To install the PostgreSQL database, run these commands:

```bash
# Automated repository configuration:
sudo apt install -y postgresql-common
sudo /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh

# install and launch the postgresql service:
sudo apt update
sudo apt -y install postgresql-16
sudo service postgresql start
{:copy-code}
```

Once PostgreSQL is installed, it is recommended to set the password for the PostgreSQL main user.

The following command will switch the current user to the PostgreSQL user and set the password directly in PostgreSQL.

```bash
sudo -u postgres psql -c "\password"
{:copy-code}
```

Then, enter and confirm the password.

Finally, create a new PostgreSQL database named tb_edge by running the following command:

```bash
echo "CREATE DATABASE tb_edge;" | psql -U postgres -d postgres -h 127.0.0.1 -W
{:copy-code}
```

#### Step 3. Choose Queue Service

ThingsBoard Edge supports only Kafka or in-memory queue (since v4.0) for message storage and communication between ThingsBoard services. Choose the appropriate queue implementation based on your specific business needs:

In Memory: The built-in and default queue implementation. It is useful for development or proof-of-concept (PoC) environments, but is not recommended for production or any type of clustered deployments due to limited scalability.

Kafka: Recommended for production deployments. This queue is used in the most of ThingsBoard production environments now.

In Memory queue is built in and enabled by default. No additional configuration is required.

#### Step 4. ThingsBoard Edge Service Installation
Download installation package:

```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_TAG}/tb-edge-${TB_EDGE_TAG}.deb
{:copy-code}
```

Go to the download repository and install ThingsBoard Edge service:

```bash
sudo dpkg -i tb-edge-${TB_EDGE_TAG}.deb
{:copy-code}
```

#### Step 5. Configure ThingsBoard Edge
To configure ThingsBoard Edge, you  can use the following command to automatically update the configuration file with specific values:

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export CLOUD_ROUTING_KEY=${CLOUD_ROUTING_KEY}
export CLOUD_ROUTING_SECRET=${CLOUD_ROUTING_SECRET}
export CLOUD_RPC_HOST=${BASE_URL}
export CLOUD_RPC_PORT=${CLOUD_RPC_PORT}
export CLOUD_RPC_SSL_ENABLED=${CLOUD_RPC_SSL_ENABLED}
EOL'
{:copy-code}
```

##### [Optional] Configure PostgreSQL
If you changed PostgreSQL default datasource settings, use the following command:

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tb_edge
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<PUT_YOUR_POSTGRESQL_PASSWORD_HERE>
EOL'
{:copy-code}
```

PUT_YOUR_POSTGRESQL_PASSWORD_HERE: Replace with your actual PostgreSQL user password.

##### [Optional] Update bind ports
If ThingsBoard Edge is going to be running on the same machine where ThingsBoard server (cloud) is running, you'll need to update configuration parameters to avoid port collision between ThingsBoard server and ThingsBoard Edge.

Please execute the following command to update ThingsBoard Edge configuration file (**/etc/tb-edge/conf/tb-edge.conf**):

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export HTTP_BIND_PORT=18080
export MQTT_BIND_PORT=11883
export COAP_BIND_PORT=15683
export LWM2M_ENABLED=false
export SNMP_ENABLED=false
EOL'
{:copy-code}
```

Make sure that ports above (18080, 11883, 15683) are not used by any other application.

#### Step 6. Run installation Script

Once ThingsBoard Edge is installed and configured please execute the following install script:

```bash
sudo /usr/share/tb-edge/bin/install/install.sh
{:copy-code}
```

#### Step 7. Restart ThingsBoard Edge Service

```bash
sudo service tb-edge restart
{:copy-code}
```

#### Step 8. Open ThingsBoard Edge UI

Once started, you will be able to open **ThingsBoard Edge UI** using the following link http://localhost:8080.

###### NOTE: Edge HTTP bind port update

Use next **ThingsBoard Edge UI** link **http://localhost:18080** if you updated HTTP 8080 bind port to **18080**.

