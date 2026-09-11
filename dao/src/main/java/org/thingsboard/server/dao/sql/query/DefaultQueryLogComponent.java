// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DefaultQueryLogComponent implements QueryLogComponent {

    @Value("${sql.log_queries:false}")
    private boolean logSqlQueries;
    @Value("${sql.log_queries_threshold:5000}")
    private long logQueriesThreshold;

    @Override
    public void logQuery(SqlQueryContext ctx, String query, long duration) {
        if (logSqlQueries && duration > logQueriesThreshold) {

            String sqlToUse = substituteParametersInSqlString(query, ctx);
            log.warn("SLOW QUERY took {} ms: {}", duration, sqlToUse);

        }
    }

    String substituteParametersInSqlString(String sql, SqlParameterSource paramSource) {

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
        List<SqlParameter> declaredParams = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);

        if (declaredParams.isEmpty()) {
            return sql;
        }

        for (SqlParameter parSQL: declaredParams) {
            String paramName = parSQL.getName();
            if (!paramSource.hasValue(paramName)) {
                continue;
            }

            Object value = paramSource.getValue(paramName);
            if (value instanceof SqlParameterValue) {
                value = ((SqlParameterValue)value).getValue();
            }

            if (!(value instanceof Iterable)) {

                String ValueForSQLQuery = getValueForSQLQuery(value);
                sql = sql.replace(":" + paramName, ValueForSQLQuery);
                continue;
            }

            //Iterable
            int count = 0;
            String valueArrayStr = "";

            for (Object valueTemp: (Iterable)value) {

                if (count > 0) {
                    valueArrayStr+=", ";
                }

                String valueForSQLQuery = getValueForSQLQuery(valueTemp);
                valueArrayStr += valueForSQLQuery;
                ++count;
            }

            sql = sql.replace(":" + paramName, valueArrayStr);

        }

        return sql;
    }

    String getValueForSQLQuery(Object valueParameter) {

        if (valueParameter instanceof String) {
            return "'" + ((String) valueParameter).replaceAll("'", "''") + "'";
        }

        if (valueParameter instanceof UUID) {
            return "'" + valueParameter + "'";
        }

        return String.valueOf(valueParameter);
    }
}
