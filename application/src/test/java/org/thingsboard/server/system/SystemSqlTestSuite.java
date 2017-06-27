package org.thingsboard.server.system;

import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.thingsboard.server.dao.CustomPostgresUnit;

import java.util.Arrays;

/**
 * Created by Valerii Sosliuk on 6/27/2017.
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({"org.thingsboard.server.system.sql.*SqlTest"})
public class SystemSqlTestSuite {

    @ClassRule
    public static CustomPostgresUnit postgresUnit = new CustomPostgresUnit(
            Arrays.asList("postgres/schema.sql", "postgres/system-data.sql"),
            "postgres-embedded-test.properties");


}
