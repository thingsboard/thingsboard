/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.install.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.service.install.DatabaseHelper.CSV_DUMP_FORMAT;

/**
 * Created by igor on 2/27/18.
 */
@Slf4j
public class SqlDbHelper {

    public static Path dumpTableIfExists(Connection conn, String tableName,
                                         String[] columns, String[] defaultValues, String dumpPrefix) throws Exception {
        return dumpTableIfExists(conn, tableName, columns, defaultValues, dumpPrefix, false);
    }

    public static Path dumpTableIfExists(Connection conn, String tableName,
                                         String[] columns, String[] defaultValues, String dumpPrefix, boolean printHeader) throws Exception {

        if (tableExists(conn, tableName)) {
            Path dumpFile = Files.createTempFile(dumpPrefix, null);
            Files.deleteIfExists(dumpFile);
            CSVFormat csvFormat = CSV_DUMP_FORMAT;
            if (printHeader) {
                csvFormat = csvFormat.withHeader(columns);
            }
            try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(dumpFile), csvFormat)) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + tableName)) {
                    try (ResultSet tableRes = stmt.executeQuery()) {
                        ResultSetMetaData resMetaData = tableRes.getMetaData();
                        Map<String, Integer> columnIndexMap = new HashMap<>();
                        for (int i = 1; i <= resMetaData.getColumnCount(); i++) {
                            String columnName = resMetaData.getColumnName(i);
                            columnIndexMap.put(columnName.toUpperCase(), i);
                        }
                        while(tableRes.next()) {
                            dumpRow(tableRes, columnIndexMap, columns, defaultValues, csvPrinter);
                        }
                    }
                }
            }
            return dumpFile;
        } else {
            return null;
        }
    }

    private static boolean tableExists(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("select * from " + tableName + " where 1=0");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void loadTable(Connection conn, String tableName, String[] columns, Path sourceFile) throws Exception {
        loadTable(conn, tableName, columns, sourceFile, false);
    }

    public static void loadTable(Connection conn, String tableName, String[] columns, Path sourceFile, boolean parseHeader) throws Exception {
        CSVFormat csvFormat = CSV_DUMP_FORMAT;
        if (parseHeader) {
            csvFormat = csvFormat.withFirstRecordAsHeader();
        } else {
            csvFormat = CSV_DUMP_FORMAT.withHeader(columns);
        }
        try (PreparedStatement prepared = conn.prepareStatement(createInsertStatement(tableName, columns))) {
            try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(sourceFile), csvFormat)) {
                csvParser.forEach(record -> {
                    try {
                        for (int i = 0; i < columns.length; i++) {
                            setColumnValue(i, columns[i], record, prepared);
                        }
                        prepared.execute();
                    } catch (SQLException e) {
                        log.error("Unable to load table record!", e);
                    }
                });
            }
        }
    }

    private static void dumpRow(ResultSet res, Map<String, Integer> columnIndexMap, String[] columns,
                                String[] defaultValues, CSVPrinter csvPrinter) throws Exception {
        List<String> record = new ArrayList<>();
        for (int i=0;i<columns.length;i++) {
            String column = columns[i];
            String defaultValue;
            if (defaultValues != null && i < defaultValues.length) {
                defaultValue = defaultValues[i];
            } else {
                defaultValue = "";
            }
            record.add(getColumnValue(column, defaultValue, columnIndexMap, res));
        }
        csvPrinter.printRecord(record);
    }

    private static String getColumnValue(String column, String defaultValue, Map<String, Integer> columnIndexMap, ResultSet res) {
        int index = columnIndexMap.containsKey(column.toUpperCase()) ? columnIndexMap.get(column.toUpperCase()) : -1;
        if (index > -1) {
            String str;
            try {
                Object obj = res.getObject(index);
                if (obj == null) {
                    return null;
                } else {
                    str = obj.toString();
                }
            } catch (Exception e) {
                str = "";
            }
            return str;
        } else {
            return defaultValue;
        }
    }

    private static void setColumnValue(int index, String column,
                                       CSVRecord record, PreparedStatement preparedStatement) throws SQLException {
        String value = record.get(column);
        int type = preparedStatement.getParameterMetaData().getParameterType(index + 1);
        preparedStatement.setObject(index + 1, value, type);
    }

    private static String createInsertStatement(String tableName, String[] columns) {
        StringBuilder insertStatementBuilder = new StringBuilder();
        insertStatementBuilder.append("INSERT INTO ").append(tableName).append(" (");
        for (String column : columns) {
            insertStatementBuilder.append(column).append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (String column : columns) {
            insertStatementBuilder.append("?").append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");
        return insertStatementBuilder.toString();
    }

}
