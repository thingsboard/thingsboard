package com.iot.thingmodel.config;

import com.iot.thingmodel.common.Constants;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class DatabaseGuard {

    private final DataSource dataSource;
    private final ThingModelProperties properties;

    public DatabaseGuard(DataSource dataSource, ThingModelProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    public void verify() {
        String expectedName = properties.getDatabase().getExpectedName();
        String expectedSchema = properties.getDatabase().getExpectedSchema();
        if (isBlank(expectedName) && isBlank(expectedSchema)) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(Constants.Db.CURRENT_DATABASE_SQL)) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Failed to determine thing-model database");
            }
            String actualName = resultSet.getString(Constants.Db.DATABASE_COLUMN_INDEX);
            String actualSchema = resultSet.getString(Constants.Db.SCHEMA_COLUMN_INDEX);
            if (!isBlank(expectedName) && !expectedName.equalsIgnoreCase(actualName)) {
                throw new IllegalStateException(
                        "thing-model refuses to run against database '" + actualName + "'; expected '" + expectedName + "'");
            }
            if (!isBlank(expectedSchema) && !expectedSchema.equalsIgnoreCase(actualSchema)) {
                throw new IllegalStateException(
                        "thing-model refuses to run against schema '" + actualSchema + "'; expected '" + expectedSchema + "'");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to verify thing-model database", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
