/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.core.UpdateAttributesRequest;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.extensions.api.component.Filter;
import org.thingsboard.server.extensions.api.device.DeviceAttributes;
import org.thingsboard.server.extensions.api.rules.RuleContext;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
@Filter(name = "Device Attributes Filter", descriptor = "JsFilterDescriptor.json", configuration = JsFilterConfiguration.class)
@Slf4j
public class DeviceAttributesFilter extends BasicJsFilter {

    public static final String CLIENT_SIDE = "cs";
    public static final String SERVER_SIDE = "ss";
    public static final String SHARED = "shared";

    @Override
    protected boolean doFilter(RuleContext ctx, ToDeviceActorMsg msg) throws ScriptException {
        return evaluator.execute(toBindings(ctx.getDeviceMetaData().getDeviceAttributes(), msg != null ? msg.getPayload() : null));
    }

    private Bindings toBindings(DeviceAttributes attributes, FromDeviceMsg msg) {
        Bindings bindings = new SimpleBindings();
        convertListEntries(bindings, CLIENT_SIDE, attributes.getClientSideAttributes());
        convertListEntries(bindings, SERVER_SIDE, attributes.getServerSideAttributes());
        convertListEntries(bindings, SHARED, attributes.getServerSidePublicAttributes());

        if (msg != null) {
            switch (msg.getMsgType()) {
                case POST_ATTRIBUTES_REQUEST:
                    updateBindings(bindings, (UpdateAttributesRequest) msg);
                    break;
            }
        }

        return bindings;
    }

    private void updateBindings(Bindings bindings, UpdateAttributesRequest msg) {
        Map<String, Object> attrMap = (Map<String, Object>) bindings.get(CLIENT_SIDE);
        for (AttributeKvEntry attr : msg.getAttributes()) {
            if (!CLIENT_SIDE.equalsIgnoreCase(attr.getKey()) && !SERVER_SIDE.equalsIgnoreCase(attr.getKey())
                    && !SHARED.equalsIgnoreCase(attr.getKey())) {
                bindings.put(attr.getKey(), getValue(attr));
            }
            attrMap.put(attr.getKey(), getValue(attr));
        }
        bindings.put(CLIENT_SIDE, attrMap);
    }

    public static Bindings convertListEntries(Bindings bindings, String attributesVarName, Collection<AttributeKvEntry> attributes) {
        Map<String, Object> attrMap = new HashMap<>();
        for (AttributeKvEntry attr : attributes) {
            if (!CLIENT_SIDE.equalsIgnoreCase(attr.getKey()) && !SERVER_SIDE.equalsIgnoreCase(attr.getKey())
                    && !SHARED.equalsIgnoreCase(attr.getKey())) {
                bindings.put(attr.getKey(), getValue(attr));
            }
            attrMap.put(attr.getKey(), getValue(attr));
        }
        bindings.put(attributesVarName, attrMap);
        return bindings;
    }
}
