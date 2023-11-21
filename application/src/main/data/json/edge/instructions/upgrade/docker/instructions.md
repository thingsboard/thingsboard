#### Upgrading to ${TB_EDGE_VERSION}

**NOTE**:These steps are applicable for ThingsBoard Edge ${CURRENT_TB_EDGE_VERSION} version.
Execute the following command to pull **${TB_EDGE_VERSION}** image:

```bash
docker pull thingsboard/tb-edge:${TB_EDGE_VERSION}
{:copy-code}
```

${STOP_SERVICE}

${UPGRADE_DB}

Make sure your image is the set to tb-edge-${TB_EDGE_VERSION}.
