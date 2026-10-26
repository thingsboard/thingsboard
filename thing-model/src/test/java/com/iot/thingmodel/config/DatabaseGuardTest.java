package com.iot.thingmodel.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatabaseGuardTest {

    private final DataSource dataSource = mock(DataSource.class);

    @Test
    void acceptsExpectedDatabaseAndSchema() throws Exception {
        stubDatabase("thingsboard", "thingmodel");
        DatabaseGuard guard = new DatabaseGuard(dataSource, properties("thingsboard", "thingmodel"));

        assertDoesNotThrow(guard::verify);
    }

    @Test
    void acceptsExpectedDatabaseWithoutSchema() throws Exception {
        stubDatabase("thingmodel", "public");
        DatabaseGuard guard = new DatabaseGuard(dataSource, properties("thingmodel", ""));

        assertDoesNotThrow(guard::verify);
    }

    @Test
    void rejectsUnexpectedDatabase() throws Exception {
        stubDatabase("thingsboard", "public");
        DatabaseGuard guard = new DatabaseGuard(dataSource, properties("thingmodel", ""));

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::verify);

        assertEquals(
                "thing-model refuses to run against database 'thingsboard'; expected 'thingmodel'",
                ex.getMessage());
    }

    @Test
    void rejectsUnexpectedSchema() throws Exception {
        stubDatabase("thingsboard", "public");
        DatabaseGuard guard = new DatabaseGuard(dataSource, properties("thingsboard", "thingmodel"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::verify);

        assertEquals(
                "thing-model refuses to run against schema 'public'; expected 'thingmodel'",
                ex.getMessage());
    }

    @Test
    void skipsWhenExpectedNameAndSchemaAreBlank() {
        DatabaseGuard guard = new DatabaseGuard(dataSource, properties("", ""));

        assertDoesNotThrow(guard::verify);
        verifyNoInteractions(dataSource);
    }

    @Test
    void failsWhenConnectionCannotBeObtained() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));
        DatabaseGuard guard = new DatabaseGuard(dataSource, properties("thingsboard", "thingmodel"));

        assertThrows(IllegalStateException.class, guard::verify);
    }

    private void stubDatabase(String database, String schema) throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT current_database(), current_schema()")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn(database);
        when(resultSet.getString(2)).thenReturn(schema);
    }

    private ThingModelProperties properties(String expectedName, String expectedSchema) {
        ThingModelProperties properties = new ThingModelProperties();
        properties.getDatabase().setExpectedName(expectedName);
        properties.getDatabase().setExpectedSchema(expectedSchema);
        return properties;
    }
}
