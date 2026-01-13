Here is the list of commands that can be used to quickly install ThingsBoard Edge on RHEL/CentOS 7/8 and connect to the server.

#### Prerequisites
Before continuing to installation, execute the following commands to install the necessary tools:

```bash
sudo yum install -y nano wget && sudo yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
{:copy-code}
```

#### Step 1. Install Java 17 (OpenJDK)
ThingsBoard service is running on Java 17. To install OpenJDK 17, follow these instructions:

```bash
sudo dnf install java-17-openjdk
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
OpenJDK 64-Bit Server VM (build ...)
```

#### Step 2. Configure ThingsBoard Edge Database

ThingsBoard Edge supports **SQL** and **hybrid** database configurations.
In this guide, we’ll use an **SQL** database.
For more details about the hybrid setup, please refer to the official installation instructions on the <a href="https://thingsboard.io/docs/user-guide/install/edge/rhel/#step-2-configure-thingsboard-database" target="_blank">ThingsBoard documentation site</a>.

To install the PostgreSQL database, run these commands:

```bash
# Update your system
sudo dnf update
{:copy-code}
```

Install the repository RPM:

* **For CentOS/RHEL 8:**

```bash
# Install the repository RPM (For CentOS/RHEL 8):
sudo sudo dnf -y install https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm
{:copy-code}
```

* **For CentOS/RHEL 9:**

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

#### Step 3. ThingsBoard Edge Service Installation
Download installation package:

```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_TAG}/tb-edge-${TB_EDGE_TAG}.rpm
{:copy-code}
```

Go to the download repository and install ThingsBoard Edge service:

```bash
sudo rpm -Uvh tb-edge-${TB_EDGE_TAG}.rpm
{:copy-code}
```

#### Step 4. Configure ThingsBoard Edge
To configure ThingsBoard Edge, you can use the following command to automatically update the configuration file with specific values:

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

##### [Optional] Configure PostgreSQL Connection
If you changed PostgreSQL default datasource settings, use the following command:

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tb_edge
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<PUT_YOUR_POSTGRESQL_PASSWORD_HERE>
EOL'
{:copy-code}
```

* **PUT_YOUR_POSTGRESQL_PASSWORD_HERE**: Replace with your actual **PostgreSQL user password**.

##### [Optional] Update Bind Ports
If ThingsBoard Edge runs on the same machine as the ThingsBoard Server, you need to update the port configuration to avoid conflicts between the two services.

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

Make sure that ports **18080**, **11883**, and **15683–15688** are not being used by any other applications.

#### Step 5. Run Installation Script

Once ThingsBoard Edge is installed and configured, please execute the following install script:

```bash
sudo /usr/share/tb-edge/bin/install/install.sh
{:copy-code}
```

#### Step 6. Start ThingsBoard Edge Service

```bash
sudo service tb-edge start
{:copy-code}
```

#### Step 7. Open ThingsBoard Edge UI

Once the Edge service has started, open the Edge web interface at http://localhost:8080, or http://localhost:18080 if you modified the HTTP bind port configuration in the previous step.

Log in using your **tenant credentials** from either your local ThingsBoard Server or the **ThingsBoard Live Demo**.