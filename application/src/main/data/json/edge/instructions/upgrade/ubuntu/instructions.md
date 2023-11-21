#### Upgrading to ${TB_EDGE_VERSION}
**NOTE**:These steps are applicable for ThingsBoard Edge ${CURRENT_TB_EDGE_VERSION} version.

**ThingsBoard Edge package download**
```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_TAG}/tb-edge-${TB_EDGE_TAG}.deb
{:copy-code}
```

#### ThingsBoard Edge service upgrade

${STOP_SERVICE}

```bash
sudo dpkg -i tb-edge-${TB_EDGE_TAG}.deb
{:copy-code}
```

${UPGRADE_DB}
