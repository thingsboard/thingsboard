# Docker configuration for ThingsBoard Microservices 

This folder containing scripts and Docker Compose configurations to run ThingsBoard in Microservices mode.

## Installation

Execute the following command to run DataBase installation:

` 
$ ./docker-install-tb.sh --loadDemo
` 

- `--loadDemo`              - optional argument. Whether to load additional demo data.

## Running

Execute the following command to run services:

` 
$ ./docker-start-services.sh
` 

Execute the following command to stop services:

` 
$ ./docker-stop-services.sh
` 

Execute the following command to stop and completely remove deployed docker containers:

` 
$ ./docker-remove-services.sh
` 

Execute the following command to update particular services (pull newer docker image and rebuild container):

` 
$ ./docker-update-service.sh [SERVICE...]
` 

## Upgrading 

In case when database upgrade is needed, execute the following commands:

`
$ ./docker-stop-services.sh
$ ./docker-upgrade-tb.sh --fromVersion=[FROM_VERSION]
$ ./docker-start-services.sh
` 

- `FROM_VERSION`              - from which version upgrade should be started.
