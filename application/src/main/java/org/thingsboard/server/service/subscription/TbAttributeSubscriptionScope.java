// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import org.thingsboard.server.common.data.AttributeScope;

public enum TbAttributeSubscriptionScope {

    ANY_SCOPE(),
    CLIENT_SCOPE(AttributeScope.CLIENT_SCOPE),
    SHARED_SCOPE(AttributeScope.SHARED_SCOPE),
    SERVER_SCOPE(AttributeScope.SERVER_SCOPE);

    private final AttributeScope attributeScope;

    TbAttributeSubscriptionScope() {
        this.attributeScope = null;
    }

    TbAttributeSubscriptionScope(AttributeScope attributeScope) {
        this.attributeScope = attributeScope;
    }

    public AttributeScope getAttributeScope() {
        return attributeScope;
    }

    public static TbAttributeSubscriptionScope of(AttributeScope attributeScope) {
        for (TbAttributeSubscriptionScope scope : TbAttributeSubscriptionScope.values()) {
            if (attributeScope == scope.getAttributeScope()) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown AttributeScope: " + attributeScope.name());
    }


}
