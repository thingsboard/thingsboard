// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

@Slf4j
public class ThingsboardPostgreSQLDialect extends PostgreSQLDialect {

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        log.trace("initializeFunctionRegistry [{}]", functionContributions);
        super.initializeFunctionRegistry(functionContributions);
        BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();

        functionRegistry.registerPattern(
                "ilike",
                "(?1::text ILIKE ?2::text)",
                basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN));
    }
}
