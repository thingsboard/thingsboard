/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
