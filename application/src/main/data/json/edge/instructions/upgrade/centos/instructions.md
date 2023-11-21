#### Upgrading to ${TB_EDGE_VERSION_TITLE}
**NOTE**:These steps are applicable for ThingsBoard Edge ${CURRENT_TB_EDGE_VERSION} version.

**ThingsBoard Edge package download**
```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_TAG}/tb-edge-${TB_EDGE_TAG}.rpm
{:copy-code}
```

#### ThingsBoard Edge service upgrade

Stop ThingsBoard Edge service if it is running:

```bash
sudo service tb-edge stop
{:copy-code}
```

```bash
sudo rpm -Uvh tb-edge-${TB_EDGE_TAG}.rpm
{:copy-code}
```

${UPGRADE_DB}

Start the service
```bash
sudo service tb-edge start
{:copy-code}
```
