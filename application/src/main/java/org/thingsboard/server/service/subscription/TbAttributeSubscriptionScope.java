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
