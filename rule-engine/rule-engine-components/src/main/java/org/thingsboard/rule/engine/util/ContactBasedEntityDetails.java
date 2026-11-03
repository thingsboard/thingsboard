// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.util;

import lombok.Getter;

public enum ContactBasedEntityDetails {

    ID("id"),
    TITLE("title"),
    COUNTRY("country"),
    CITY("city"),
    STATE("state"),
    ZIP("zip"),
    ADDRESS("address"),
    ADDRESS2("address2"),
    PHONE("phone"),
    EMAIL("email"),
    ADDITIONAL_INFO("additionalInfo");

    @Getter
    private final String ruleEngineName;

    ContactBasedEntityDetails(String ruleEngineName) {
        this.ruleEngineName = ruleEngineName;
    }

}
