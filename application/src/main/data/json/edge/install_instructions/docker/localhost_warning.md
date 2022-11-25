#### WARNING - 'localhost (127.0.0.1)' is not supported

Please note that your ThingsBoard base URL is **'localhost (127.0.0.1)'** at the moment.  
**'localhost (127.0.0.1)'** cannot be used for docker containers - please update **CLOUD_RPC_HOST** environment variable below to the IP address of your machine (*docker **host** machine*). IP address must be next format **192.168.1.XX** or similar. In other case - Edge service, that is running in docker container, will not be able to connect to the cloud.