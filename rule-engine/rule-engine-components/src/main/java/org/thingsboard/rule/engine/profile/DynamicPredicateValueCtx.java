// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.profile;

@Deprecated
public interface DynamicPredicateValueCtx {

    EntityKeyValue getTenantValue(String key);

    EntityKeyValue getCustomerValue(String key);

    void resetCustomer();
}
