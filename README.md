# ThingsBoard

This repository contains the ThingsBoard Community Edition. Follow the instructions below to set up your development environment.

## Cloning the Repository

```sh
git clone git@github.com:lamah-co/thingsboard.git
cd thingsboard
mvn clean install -DskipTests
```

## Setting Up PostgreSQL with Docker Compose

```sh
services:
  postgres:
    image: postgres:latest
    container_name: thingsboard-postgres
    environment:
      POSTGRES_DB: thingsboard
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
  ```
### Start the PostgreSQL container:

  ```sh
    docker-compose up -d
  ```
### Connecting to PostgreSQL

Connect to the PostgreSQL container and create the ThingsBoard database:
```sh
psql -U postgres -d postgres -h 127.0.0.1 -W
CREATE DATABASE thingsboard;
\q
```

### Creating the Database Schema 
```sh
cd thingsboard/application/target/bin/install
chmod +x install_dev_db.sh
./install_dev_db.sh # On Linux
install_dev_db.bat # Windows
```

### Fixing Logging Issues
If you encounter the following error:
```sh
/var/log/thingsboard/install.log (No such file or directory)
```

### Fix it by running:

sudo mkdir /var/log/thingsboard
sudo chmod 777 /var/log/thingsboard

### Running the Development Environment
```sh
cd thingsboard/ui-ngx
mvn clean install -P yarn-start
```

### Running the Server-Side Container
You can run the server in two ways:
### Option 1: Using the Command Line
```sh
cd thingsboard
java -jar application/target/thingsboard-${VERSION}-boot.jar
```

### Option 2: Using Visual Studio Code (Recommended)

	1.	Install the Maven for Java Extension:
	•	Extension ID: vscjava.vscode-maven
	2.	Install the Spring Boot Tools Plugin Extension:
	•	Extension ID: vmware.vscode-spring-boot
	3.	Open the `ThingsboardServerApplication main method located in org.thingsboard.server` and run the server in Visual Studio Code.
