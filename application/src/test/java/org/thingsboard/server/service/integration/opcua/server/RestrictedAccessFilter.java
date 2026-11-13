// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.opcua.server;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilterContext.GetAttributeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class RestrictedAccessFilter implements AttributeFilter {

    private static final Set<AccessLevel> INTERNAL_ACCESS = AccessLevel.READ_WRITE;

    private final Function<Object, Set<AccessLevel>> accessLevelsFn;

    public RestrictedAccessFilter(Function<Object, Set<AccessLevel>> accessLevelsFn) {
        this.accessLevelsFn = accessLevelsFn;
    }

    @Override
    public Object getAttribute(GetAttributeContext ctx, AttributeId attributeId) {
        if (attributeId == AttributeId.UserAccessLevel) {
            Optional<Object> identity = ctx.getSession().map(Session::getIdentityObject);

            Set<AccessLevel> accessLevels = identity.map(accessLevelsFn).orElse(INTERNAL_ACCESS);

            return AccessLevel.toValue(accessLevels);
        } else {
            return ctx.getAttribute(attributeId);
        }
    }

}
