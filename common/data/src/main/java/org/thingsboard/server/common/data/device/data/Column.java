/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.device.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.List;
import java.util.Set;

/**
 * for tdengine
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Column {

    public static Set<String> types = Set.of("TIMESTAMP", "INT", "INT UNSIGNED", "BIGINT", "BIGINT UNSIGNED", "FLOAT", "DOUBLE", "BINARY", "SMALLINT", "SMALLINT UNSIGNED", "TINYINT", "TINYINT UNSIGNED", "BOOL", "NCHAR", "JSON", "VARCHAR");

    private String name;
    private String type;
    private int len;

    public static void validColumn(Set<String> names, Column column) throws ThingsboardException {
        if (column == null) {
            return;
        }
        if (column.getName() == null || column.getName().length() < 1) {
            throw new ThingsboardException("Super table column name can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (names.contains(column.getName())) {
            throw new ThingsboardException(String.format("Super table column name:%s duplication!", column.getName()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        names.add(column.getName());

        if (column.getType() == null) {
            throw new ThingsboardException(String.format("Super table column:%s type can't be empty!", column.getName()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!Column.types.contains(column.getType().toUpperCase())) {
            throw new ThingsboardException(String.format("Super table column(%s)'s type: %s is not valid", column.getName(), column.getType()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (("BINARY".equalsIgnoreCase(column.getType()) || "VARCHAR".equalsIgnoreCase(column.getType()) || "NCHAR".equalsIgnoreCase(column.getType())) && column.getLen() < 1) {
            throw new ThingsboardException(String.format("Super table column:%s length must greater than 1!", column.getName()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    public static void validTag(Set<String> tags, Column column) throws ThingsboardException {
        if (column == null) {
            return;
        }
        if (column.getName() == null || column.getName().length() < 1) {
            throw new ThingsboardException("Super table tag name can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (tags.contains(column.getName())) {
            throw new ThingsboardException(String.format("Super table tag name:%s duplication!", column.getName()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        tags.add(column.getName());

        if (column.getType() == null) {
            throw new ThingsboardException(String.format("Super table tag:%s type can't be empty!", column.getName()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!Column.types.contains(column.getType().toUpperCase())) {
            throw new ThingsboardException(String.format("Super table tag(%s)'s type:%s is not valid", column.getName(), column.getType()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (("BINARY".equalsIgnoreCase(column.getType()) || "VARCHAR".equalsIgnoreCase(column.getType()) || "NCHAR".equalsIgnoreCase(column.getType())) && column.getLen() < 1) {
            throw new ThingsboardException(String.format("Super table tag:%s length must greater than 1!", column.getName()), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * add additional column
     *
     * @param columnList
     */
    public static void addAdditionalColumn(List<Column> columnList) {
        Column activeState = new Column("active", "BOOL", 0);
        Column lastConnectTime = new Column("lastConnectTime", "TIMESTAMP", 0);
        Column lastDisconnectTime = new Column("lastDisconnectTime", "TIMESTAMP", 0);
        Column lastActivityTime = new Column("lastActivityTime", "TIMESTAMP", 0);
        Column inactivityAlarmTime = new Column("inactivityAlarmTime", "TIMESTAMP", 0);
        Column inactivityTimeout = new Column("inactivityTimeout", "INT", 0);
        columnList.add(activeState);
        columnList.add(lastConnectTime);
        columnList.add(lastDisconnectTime);
        columnList.add(lastActivityTime);
        columnList.add(inactivityAlarmTime);
        columnList.add(inactivityTimeout);
    }
}