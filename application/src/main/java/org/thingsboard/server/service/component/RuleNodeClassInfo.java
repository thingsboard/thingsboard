// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.component;

import lombok.Data;
import org.thingsboard.rule.engine.api.RuleNode;

@Data
public class RuleNodeClassInfo {

    private final Class<?> clazz;
    private final RuleNode annotation;

    public String getClassName(){
        return clazz.getName();
    }

    public String getSimpleName() {
        return clazz.getSimpleName();
    }

    public int getCurrentVersion() {
        return annotation.version();
    }

    public boolean isVersioned() {
        return annotation.version() > 0;
    }

}
