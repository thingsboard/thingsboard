// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface OutputStrategy {

    @JsonIgnore
    OutputStrategyType getType();

    boolean hasContextOnlyChanges(OutputStrategy other);

    boolean hasRefreshContextOnlyChanges(OutputStrategy other);

}
