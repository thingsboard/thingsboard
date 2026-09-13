// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

public interface ExpressionBasedCalculatedFieldConfiguration extends ArgumentsBasedCalculatedFieldConfiguration {

    String getExpression();

    void setExpression(String expression);

}
