// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

public interface SimpleKeyFilterPredicate<T> extends KeyFilterPredicate {

    FilterPredicateValue<T> getValue();

}
