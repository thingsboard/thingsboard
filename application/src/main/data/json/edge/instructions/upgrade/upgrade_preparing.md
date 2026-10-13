Here is the list of commands that can be used to quickly upgrade ThingsBoard Edge on ${OS}

#### Prepare for Upgrading ThingsBoard Edge

Stop ThingsBoard Edge service:

```bash
sudo systemctl stop tb-edge
{:copy-code}
```

##### Backup Database
Make a backup of the database before upgrading. **Make sure you have enough space to place a backup of the database.**

Check database size:

```bash
sudo -u postgres psql -c "SELECT pg_size_pretty( pg_database_size('tb_edge') );"
{:copy-code}
```

Check free space:

```bash
df -h /
{:copy-code}
```

If there is enough free space - make a backup:

```bash
sudo -Hiu postgres pg_dump tb_edge > tb_edge.sql.bak
{:copy-code}
```

Check the backup file created successfully.
