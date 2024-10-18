# Config
* change the TDengine host location in `DockerfileWithoutTDengine.sh`.

```bash
ENV TD_HOST=localhost
```

* create `thingsboard` database in your TDengine Cluster. or be consistent with the database in the configuration

```bash
ENV TDENGINE_URL=jdbc:TAOS-RS://$TD_HOST:6041/thingsboard
```