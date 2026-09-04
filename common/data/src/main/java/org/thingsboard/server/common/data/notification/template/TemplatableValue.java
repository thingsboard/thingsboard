// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class TemplatableValue {
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public static TemplatableValue of(Supplier<String> getter, Consumer<String> setter) {
        return new TemplatableValue(getter, setter);
    }

    public String get() {
        return getter.get();
    }

    public void set(String processed) {
        setter.accept(processed);
    }

    public boolean containsParams(Collection<String> params) {
        return StringUtils.containsAny(get(), params.toArray(String[]::new));
    }

}
