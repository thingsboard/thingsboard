## Install ThingsBoard Edge on Ubuntu Server

Here is the list of commands, that can be used to quickly install ThingsBoard Edge on Ubuntu Server and connect to the cloud.

### Install Java 11 (OpenJDK)
ThingsBoard service is running on Java 11. Follow this instructions to install OpenJDK 11:

```bash
sudo apt update
sudo apt install openjdk-11-jdk
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
# install **wget** if not already installed:
sudo apt install -y wget

# import the repository signing key:
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# add repository contents to your system:
RELEASE=$(lsb_release -cs)
echo "deb http://apt.postgresql.org/pub/repos/apt/ ${RELEASE}"-pgdg main | sudo tee  /etc/apt/sources.list.d/pgdg.list

# install and launch the postgresql service:
sudo apt update
sudo apt -y install postgresql-12
sudo service postgresql start
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

Then, press “Ctrl+D” to return to main user console and connect to the database to create ThingsBoard Edge DB:

```text
psql -U postgres -d postgres -h 127.0.0.1 -W
CREATE DATABASE tb_edge;
\q
```

### Thingsboard Edge service installation
Download installation package.

```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_VERSION}/tb-edge-${TB_EDGE_VERSION}.deb
```
{: .copy-code}

Go to the download repository and install ThingsBoard Edge service

```bash
sudo dpkg -i tb-edge-{TB_EDGE_VERSION}.deb
{:copy-code}
```

Edit ThingsBoard Edge configuration file

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export HTTP_BIND_PORT=18080
export MQTT_BIND_PORT=11883
export COAP_BIND_PORT=15683
export LWM2M_ENABLED=false
EOL'
{:copy-code}
```

#### Run installation script

Once ThingsBoard Edge is installed and configured please execute the following install script:

```bash
sudo /usr/share/tb-edge/bin/install/install.sh
```

#### Restart ThingsBoard Edge service

```bash
sudo service tb-edge restart
```

#### Open ThingsBoard Edge UI

Once started, you will be able to open **ThingsBoard Edge UI** using the following link http://localhost:8080.

###### NOTE: Edge HTTP bind port update

Use next **ThingsBoard Edge UI** link **http://localhost:18080** if you updated HTTP 8080 bind port to **18080**.

