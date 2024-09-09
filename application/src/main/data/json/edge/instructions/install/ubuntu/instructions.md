Here is the list of commands, that can be used to quickly install ThingsBoard Edge on Ubuntu Server and connect to the server.

#### Install Java 17 (OpenJDK)
ThingsBoard service is running on Java 17. Follow these instructions to install OpenJDK 17:

```bash
sudo apt update
sudo apt install openjdk-17-jdk
{:copy-code}
```

Please don't forget to configure your operating system to use OpenJDK 17 by default.
You can configure which version is the default using the following command:

```bash
sudo update-alternatives --config java
{:copy-code}
```

You can check the installation using the following command:

```bash
java -version
{:copy-code}
```

Expected command output is:

```text
openjdk version "17.x.xx"
OpenJDK Runtime Environment (...)
OpenJDK 64-Bit Server VM (build ...)
```

#### Configure PostgreSQL
ThingsBoard Edge uses PostgreSQL database as a local storage.
Instructions listed below will help you to install PostgreSQL.

```bash
# install **wget** if not already installed:
sudo apt install -y wget

# import the repository signing key:
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# add repository contents to your system:
RELEASE=$(lsb_release -cs)
echo "deb http://apt.postgresql.org/pub/repos/apt/ ${RELEASE}"-pgdg main | sudo tee  /etc/apt/sources.list.d/pgdg.list

# install and launch the postgresql service:
sudo apt update
sudo apt -y install postgresql-15
sudo service postgresql start
{:copy-code}
```

Once PostgreSQL is installed you may want to create a new user or set the password for the main user.
The instructions below will help to set the password for main PostgreSQL user:

```text
sudo su - postgres
psql
\password
\q
```

Then, press “Ctrl+D” to return to main user console and connect to the database to create ThingsBoard Edge DB:

```text
psql -U postgres -d postgres -h 127.0.0.1 -W
CREATE DATABASE tb_edge;
\q
```

#### Thingsboard Edge service installation
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

#### Configure ThingsBoard Edge
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

##### [Optional] Database Configuration
In case you changed default PostgreSQL datasource settings (**postgres**/**postgres**) please update the configuration file (**/etc/tb-edge/conf/tb-edge.conf**) with your actual values:

```bash
sudo nano /etc/tb-edge/conf/tb-edge.conf
{:copy-code}
```

Please update the following lines in your configuration file. Make sure **to replace**:
- Replace 'postgres' with your actual PostgreSQL username;
- Replace 'PUT_YOUR_POSTGRESQL_PASSWORD_HERE' with your actual PostgreSQL password.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tb_edge
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=PUT_YOUR_POSTGRESQL_PASSWORD_HERE
{:copy-code}
```

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

#### Run installation script

Once ThingsBoard Edge is installed and configured please execute the following install script:

```bash
sudo /usr/share/tb-edge/bin/install/install.sh
{:copy-code}
```

#### Restart ThingsBoard Edge service

```bash
sudo service tb-edge restart
{:copy-code}
```

#### Open ThingsBoard Edge UI

Once started, you will be able to open **ThingsBoard Edge UI** using the following link http://localhost:8080.

###### NOTE: Edge HTTP bind port update

Use next **ThingsBoard Edge UI** link **http://localhost:18080** if you updated HTTP 8080 bind port to **18080**.

