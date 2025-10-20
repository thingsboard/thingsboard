/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.utils;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.util.Optional;

public class CalculatedFieldArgumentUtils {

    public static ListenableFuture<ArgumentEntry> transformSingleValueArgument(ListenableFuture<Optional<? extends KvEntry>> kvEntryFuture) {
        return Futures.transform(kvEntryFuture, CalculatedFieldArgumentUtils::transformSingleValueArgument, MoreExecutors.directExecutor());
    }

    public static ArgumentEntry transformSingleValueArgument(Optional<? extends KvEntry> kvEntry) {
        if (kvEntry.isPresent() && kvEntry.get().getValue() != null) {
            return ArgumentEntry.createSingleValueArgument(kvEntry.get());
        } else {
            return new SingleValueArgumentEntry();
        }
    }

    public static KvEntry createDefaultKvEntry(Argument argument) {
        String key = argument.getRefEntityKey().getKey();
        String defaultValue = argument.getDefaultValue();
        if (StringUtils.isBlank(defaultValue)) {
            return new StringDataEntry(key, null);
        }
        if (NumberUtils.isParsable(defaultValue)) {
            return new DoubleDataEntry(key, Double.parseDouble(defaultValue));
        }
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return new BooleanDataEntry(key, Boolean.parseBoolean(defaultValue));
        }
        return new StringDataEntry(key, defaultValue);
    }

    public static AttributeKvEntry createDefaultAttributeEntry(Argument argument, long ts) {
        KvEntry kvEntry = createDefaultKvEntry(argument);
        return new BaseAttributeKvEntry(kvEntry, ts, 0L);
    }

    public static CalculatedFieldState createStateByType(CalculatedFieldCtx ctx, EntityId entityId) {
        return switch (ctx.getCfType()) {
            case SIMPLE -> new SimpleCalculatedFieldState(entityId);
            case SCRIPT -> new ScriptCalculatedFieldState(entityId);
            case GEOFENCING -> new GeofencingCalculatedFieldState(entityId);
            case ALARM -> new AlarmCalculatedFieldState(entityId);
            case PROPAGATION -> new PropagationCalculatedFieldState(entityId);
        };
    }

}
