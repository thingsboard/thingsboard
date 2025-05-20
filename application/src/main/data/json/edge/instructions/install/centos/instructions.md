Here is the list of commands, that can be used to quickly install ThingsBoard Edge on RHEL/CentOS 7/8 and connect to the server.

#### Prerequisites
Before continue to installation execute the following commands in order to install necessary tools:

```bash
sudo yum install -y nano wget && sudo yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
{:copy-code}
```

#### Step 1. Install Java 17 (OpenJDK)
ThingsBoard service is running on Java 17. Follow these instructions to install OpenJDK 17:

```bash
sudo dnf install java-17-openjdk
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

#### Step 2. Configure ThingsBoard Database
ThingsBoard Edge supports SQL and hybrid database approaches.
In this guide we will use SQL only.
For hybrid details please follow official installation instructions from the ThingsBoard documentation site.

### PostgresSql
ThingsBoard Edge uses PostgreSQL database as a local storage.
To install PostgreSQL, follow the instructions below.

```bash
# Update your system
sudo dnf update
{:copy-code}
```

Install the repository RPM:
**For CentOS/RHEL 8:**

```bash
# Install the repository RPM (For CentOS/RHEL 8):
sudo sudo dnf -y install https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm
{:copy-code}
```

**For CentOS/RHEL 9:**

```bash
# Install the repository RPM (for CentOS 9):
sudo dnf -y install https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm
{:copy-code}
```

Install packages and initialize PostgreSQL. The PostgreSQL service will automatically start every time the system boots up.

```bash
sudo dnf -qy module disable postgresql && \
sudo dnf -y install postgresql16 postgresql16-server postgresql16-contrib && \
sudo /usr/pgsql-16/bin/postgresql-16-setup initdb && \
sudo systemctl enable --now postgresql-16
{:copy-code}
```

Once PostgreSQL is installed, it is recommended to set the password for the PostgreSQL main user.

The following command will switch the current user to the PostgreSQL user and set the password directly in PostgreSQL.

```bash
sudo -u postgres psql -c "\password"
{:copy-code}
```

Then, enter and confirm the password.

Since ThingsBoard Edge uses the PostgreSQL database for local storage, configuring MD5 authentication ensures that only authenticated users or 
applications can access the database, thus protecting your data. After configuring the password, 
edit the pg_hba.conf file to use MD5 hashing for authentication instead of the default method (ident) for local IPv4 connections.

To replace ident with md5, run the following command:

```bash
sudo sed -i 's/^host\s\+all\s\+all\s\+127\.0\.0\.1\/32\s\+ident/host    all             all             127.0.0.1\/32            md5/' /var/lib/pgsql/16/data/pg_hba.conf
{:copy-code}
```

Then run the command that will restart the PostgreSQL service to apply configuration changes, connect to the database as a postgres user, 
and create the ThingsBoard Edge database (tb_edge). To connect to the PostgreSQL database, enter the PostgreSQL password.

```bash
sudo systemctl restart postgresql-16.service && psql -U postgres -d postgres -h 127.0.0.1 -W -c "CREATE DATABASE tb_edge;"
{:copy-code}
```

#### Step 3. Choose Queue Service

ThingsBoard Edge supports only Kafka or in-memory queue (since v4.0) for message storage and communication between ThingsBoard services.
How to choose the right queue implementation?

In Memory queue implementation is built-in and default. It is useful for development(PoC) environments and is not suitable for production deployments or any sort of cluster deployments.

Kafka is recommended for production deployments. This queue is used on the most of ThingsBoard production environments now.

In Memory queue is built in and enabled by default. No additional configuration is required.

#### Step 4. ThingsBoard Edge Service Installation
Download installation package:

```bash
wget wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_TAG}/tb-edge-${TB_EDGE_TAG}.rpm
{:copy-code}
```

Go to the download repository and install ThingsBoard Edge service:

```bash
sudo rpm -Uvh tb-edge-${TB_EDGE_TAG}.rpm
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

##### Configure PostgreSQL (Optional)
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

If the Edge HTTP bind port was changed to 18080 during Edge installation, access the ThingsBoard Edge instance at http://localhost:18080.

