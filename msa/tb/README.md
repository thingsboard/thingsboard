# ThingsBoard single docker images 

This project provides the build for the ThingsBoard single docker images.

* `thingsboard/tb` - single instance of ThingsBoard with embedded HSQLDB database.
* `thingsboard/tb-postgres` - single instance of ThingsBoard with PostgreSQL database.
* `thingsboard/tb-cassandra` - single instance of ThingsBoard with Hybrid PostgreSQL (entities) and Cassandra (timeseries) database.

## Running

Before starting Docker container run following command to create a directory for storing data and change its owner to docker container user.
To be able to change user, **chown** command is used, which requires sudo permissions (command will request password for a sudo access):

`
$ mkdir -p ~/.mytb-data && sudo chown -R 799:799 ~/.mytb-data
` 

**NOTE**: replace directory `~/.mytb-data` with directory you're planning to use on container creation. 

In this example `thingsboard/tb` image will be used. You can choose any other images with different databases (see above).
Execute the following command to run this docker directly:

` 
$ docker run -it -p 9090:9090 -p 1883:1883 -p 5683:5683/udp  -p 5685:5685/udp -v ~/.mytb-data:/data --name mytb thingsboard/tb
` 

Where: 
    
- `docker run`              - run this container
- `-it`                     - attach a terminal session with current ThingsBoard process output
- `-p 9090:9090`            - connect local port 9090 to exposed internal HTTP port 9090
- `-p 1883:1883`            - connect local port 1883 to exposed internal MQTT port 1883    
- `-p 5683:5683`            - connect local port 5683 to exposed internal COAP port 5683 
- `-p 5685:5685`            - connect local port 5685 to exposed internal COAP port 5685 (lwm2m) 
- `-v ~/.mytb-data:/data`   - mounts the host's dir `~/.mytb-data` to ThingsBoard DataBase data directory
- `--name mytb`             - friendly local name of this machine
- `thingsboard/tb`          - docker image, can be also `thingsboard/tb-postgres` or `thingsboard/tb-cassandra`

> **NOTE**: **Windows** users should use docker managed volume instead of host's dir. Create docker volume (for ex. `mytb-data`) before executing `docker run` command:
> ```
> $ docker volume create mytb-data
> ```
> After you can execute docker run command using `mytb-data` volume instead of `~/.mytb-data`.
> In order to get access to necessary resources from external IP/Host on **Windows** machine, please execute the following commands:
> ```
> $ VBoxManage controlvm "default" natpf1 "tcp-port9090,tcp,,9090,,9090"  
> $ VBoxManage controlvm "default" natpf1 "tcp-port1883,tcp,,1883,,1883"
> $ VBoxManage controlvm "default" natpf1 "tcp-port5683,tcp,,5683,,5683"
> $ VBoxManage controlvm "default" natpf1 "tcp-port5683,tcp,,5685,,5685"
> ```

After executing `docker run` command you can open `http://{your-host-ip}:9090` in you browser (for ex. `http://localhost:9090`). You should see ThingsBoard login page.
Use the following default credentials:

- **System Administrator**: sysadmin@thingsboard.org / sysadmin
- **Tenant Administrator**: tenant@thingsboard.org / tenant
- **Customer User**: customer@thingsboard.org / customer
    
You can always change passwords for each account in account profile page.

You can detach from session terminal with `Ctrl-p` `Ctrl-q` - the container will keep running in the background.

To reattach to the terminal (to see ThingsBoard logs) run:

```
$ docker attach mytb
```

To stop the container:

```
$ docker stop mytb
```

To start the container:

```
$ docker start mytb
```

## Upgrading

In order to update to the latest image, execute the following commands:

```
$ docker pull thingsboard/tb
$ docker stop mytb
$ docker run -it -v ~/.mytb-data:/data --rm thingsboard/tb upgrade-tb.sh
$ docker start mytb
```

**NOTE**: if you use different database change image name in all commands from `thingsboard/tb` to `thingsboard/tb-postgres` or `thingsboard/tb-cassandra` correspondingly.
 
**NOTE**: replace host's directory `~/.mytb-data` with directory used during container creation. 
