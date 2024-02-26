/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.service.install.DatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public abstract class SqlAbstractDatabaseSchemaService implements DatabaseSchemaService {

    protected static final String SQL_DIR = "sql";

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Autowired
    protected InstallScripts installScripts;

    private final String schemaSql;
    private final String schemaIdxSql;

    protected SqlAbstractDatabaseSchemaService(String schemaSql, String schemaIdxSql) {
        this.schemaSql = schemaSql;
        this.schemaIdxSql = schemaIdxSql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("Installing SQL DataBase schema part: " + schemaSql);
        executeQueryFromFile(schemaSql);

        if (createIndexes) {
            this.createDatabaseIndexes();
        }
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
        if (schemaIdxSql != null) {
            log.info("Installing SQL DataBase schema indexes part: " + schemaIdxSql);
            executeQueryFromFile(schemaIdxSql);
        }
    }

    void executeQueryFromFile(String schemaIdxSql) throws SQLException, IOException {
        Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaIdxSql);
        String sql = Files.readString(schemaIdxFile);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
        }
    }

    protected void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info("Successfully executed query: {}", query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.error("Failed to execute query: {} due to: {}", query, e.getMessage());
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
    }

}
