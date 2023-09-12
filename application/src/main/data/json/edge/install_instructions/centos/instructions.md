## Install ThingsBoard Edge on CentOS/RHEL Server

Here is the list of commands, that can be used to quickly install ThingsBoard Edge on RHEL/CentOS 7/8 and connect to the cloud.

### Prerequisites
Before continue to installation execute the following commands in order to install necessary tools:

```bash
sudo yum install -y nano wget
sudo yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
```

### Install Java 11 (OpenJDK)
ThingsBoard service is running on Java 11. Follow this instructions to install OpenJDK 11:

```bash
sudo yum install java-11-openjdk
```
{: .copy-code}

Please don't forget to configure your operating system to use OpenJDK 11 by default.
You can configure which version is the default using the following command:

```bash
sudo update-alternatives --config java
```
{: .copy-code}

You can check the installation using the following command:

```bash
java -version
```
{: .copy-code}

Expected command output is:

```text
openjdk version "11.0.xx"
OpenJDK Runtime Environment (...)
OpenJDK 64-Bit Server VM (build ...)
```

### Configure PostgreSQL
ThingsBoard Edge uses PostgreSQL database as a local storage.
Instructions listed below will help you to install PostgreSQL.

```bash
# Update your system
sudo yum update
```
{: .copy-code}

**For CentOS 7:**

```bash
# Install the repository RPM (for CentOS 7):
sudo yum -y install https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
# Install packages
sudo yum -y install epel-release yum-utils
sudo yum-config-manager --enable pgdg12
sudo yum install postgresql12-server postgresql12
# Initialize your PostgreSQL DB
sudo /usr/pgsql-12/bin/postgresql-12-setup initdb
sudo systemctl start postgresql-12
# Optional: Configure PostgreSQL to start on boot
sudo systemctl enable --now postgresql-12

```
{: .copy-code}

**For CentOS 8:**

```bash
# Install the repository RPM (for CentOS 8):
sudo yum -y install https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm
# Install packages
sudo dnf -qy module disable postgresql
sudo dnf -y install postgresql12 postgresql12-server
# Initialize your PostgreSQL DB
sudo /usr/pgsql-12/bin/postgresql-12-setup initdb
sudo systemctl start postgresql-12
# Optional: Configure PostgreSQL to start on boot
sudo systemctl enable --now postgresql-12

```
{: .copy-code}

Once PostgreSQL is installed you may want to create a new user or set the password for the the main user.
The instructions below will help to set the password for main postgresql user

```text
sudo su - postgres
psql
\password
\q
```

Then, press "Ctrl+D" to return to main user console.

After configuring the password, edit the pg_hba.conf to use MD5 authentication with the postgres user.

Edit pg_hba.conf file:

```bash
sudo nano /var/lib/pgsql/12/data/pg_hba.conf
```
{: .copy-code}

Locate the following lines:

```text
# IPv4 local connections:
host    all             all             127.0.0.1/32            ident
```

Replace `ident` with `md5`:

```text
host    all             all             127.0.0.1/32            md5
```

Finally, you should restart the PostgreSQL service to initialize the new configuration:

```bash
sudo systemctl restart postgresql-12.service
```
{: .copy-code}

Connect to the database to create ThingsBoard Edge DB:

```bash
psql -U postgres -d postgres -h 127.0.0.1 -W
```
{: .copy-code}

Execute create database statement

```bash
CREATE DATABASE tb_edge;
\q
```
{: .copy-code}

### ThingsBoard Edge service installation
Download installation package.

```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/{{ site.release.edge_tag }}/tb-edge-{{ site.release.edge_ver }}.rpm
```
{: .copy-code}

Go to the download repository and install ThingsBoard Edge service

```bash
sudo rpm -Uvh tb-edge-${TB_EDGE_VERSION}.rpm
```
{: .copy-code}

### Configure ThingsBoard Edge


#### [Optional] Update bind ports
If ThingsBoard Edge is going to be running on the same machine where ThingsBoard server (cloud) is running, you'll need to update docker compose port mapping to avoid port collision between ThingsBoard server and ThingsBoard Edge.

Please update next lines of `docker-compose.yml` file:

```bash
ports:
  - "18080:8080"
  - "11883:1883"
  - "15683-15688:5683-5688/udp"
```
Make sure that ports above (18080, 11883, 15683-15688) are not used by any other application.

#### Run installation script
Once ThingsBoard Edge is installed and configured please execute the following install script:

```bash
sudo /usr/share/tb-edge/bin/install/install.sh
```
{: .copy-code}

### Restart ThingsBoard Edge service

```bash
sudo service tb-edge restart
```

#### Open ThingsBoard Edge UI

Once started, you will be able to open **ThingsBoard Edge UI** using the following link http://localhost:8080.

###### NOTE: Edge HTTP bind port update

Use next **ThingsBoard Edge UI** link **http://localhost:18080** if you updated HTTP 8080 bind port to **18080**.

