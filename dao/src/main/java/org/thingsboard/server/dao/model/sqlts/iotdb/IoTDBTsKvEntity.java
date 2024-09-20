package org.thingsboard.server.dao.model.sqlts.iotdb;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;


@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public final class IoTDBTsKvEntity extends AbstractTsKvEntity {
    private  String device;
    private  long timestamp;
    private  String measurement;
    private  Object value;
    private  TSDataType tsDataType;

    public IoTDBTsKvEntity() {
    }


    @Override
    public boolean isNotEmpty() {
        return ts != null && (strValue != null || longValue != null || doubleValue != null || booleanValue != null);
    }
}
