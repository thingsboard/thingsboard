/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.install.migrate;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Data
@Slf4j
public class CassandraToSqlTable {

    private String cassandraCf;
    private String sqlTableName;

    private List<CassandraToSqlColumn> columns;

    public CassandraToSqlTable(String tableName, CassandraToSqlColumn... columns) {
        this(tableName, tableName, columns);
    }

    public CassandraToSqlTable(String cassandraCf, String sqlTableName, CassandraToSqlColumn... columns) {
        this.cassandraCf = cassandraCf;
        this.sqlTableName = sqlTableName;
        this.columns = Arrays.asList(columns);
    }

    public void migrateToSql(Session session, Connection conn) throws SQLException {
        log.info("Migrating data from cassandra '{}' Column Family to '{}' SQL table...", this.cassandraCf, this.sqlTableName);
        PreparedStatement sqlInsertStatement = createSqlInsertStatement(conn);
        Statement cassandraSelectStatement = createCassandraSelectStatement();
        cassandraSelectStatement.setFetchSize(100);
        ResultSet rs = session.execute(cassandraSelectStatement);
        Iterator<Row> iter = rs.iterator();
        int rowCounter = 0;
        while (iter.hasNext()) {
            Row row = iter.next();
            if (row != null) {
                this.migrateRowToSql(row, sqlInsertStatement);
                rowCounter++;
                if (rowCounter % 100 == 0) {
                    sqlInsertStatement.executeBatch();
                    log.info("{} records migrated so far...", rowCounter);
                }
            }
        }
        if (rowCounter % 100 > 0) {
            sqlInsertStatement.executeBatch();
        }
        sqlInsertStatement.close();
        log.info("{} total records migrated.", rowCounter);
        log.info("Finished migration data from cassandra '{}' Column Family to '{}' SQL table.", this.cassandraCf, this.sqlTableName);
    }

    private void migrateRowToSql(Row row, PreparedStatement sqlInsertStatement) throws SQLException {
        for (int i=0; i<this.columns.size();i++) {
            CassandraToSqlColumn column = this.columns.get(i);
            column.prepareColumnValue(row, sqlInsertStatement, i);
        }
        sqlInsertStatement.addBatch();
    }

    private Statement createCassandraSelectStatement() {
        StringBuilder selectStatementBuilder = new StringBuilder();
        selectStatementBuilder.append("SELECT ");
        for (CassandraToSqlColumn column : columns) {
            selectStatementBuilder.append(column.getCassandraColumnName()).append(",");
        }
        selectStatementBuilder.deleteCharAt(selectStatementBuilder.length() - 1);
        selectStatementBuilder.append(" FROM ").append(cassandraCf);
        return new SimpleStatement(selectStatementBuilder.toString());
    }

    private PreparedStatement createSqlInsertStatement(Connection conn) throws SQLException {
        StringBuilder insertStatementBuilder = new StringBuilder();
        insertStatementBuilder.append("INSERT INTO ").append(this.sqlTableName).append(" (");
        for (CassandraToSqlColumn column : columns) {
            insertStatementBuilder.append(column.getSqlColumnName()).append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (CassandraToSqlColumn ignored : columns) {
            insertStatementBuilder.append("?").append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");
        return conn.prepareStatement(insertStatementBuilder.toString());
    }

}
