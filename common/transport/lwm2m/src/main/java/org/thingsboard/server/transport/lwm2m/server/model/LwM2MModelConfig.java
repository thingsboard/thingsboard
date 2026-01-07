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
package org.thingsboard.server.transport.lwm2m.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_BY_OBJECT;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;
import static org.thingsboard.server.common.data.util.CollectionsUtil.diffSets;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.areArraysStringEqual;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.deepCopyConcurrentMap;

@Data
@NoArgsConstructor
@ToString(exclude = "toCancelRead")
@Slf4j
public class LwM2MModelConfig {
    private String endpoint;
    private Map<String, ObjectAttributes> attributesToAdd;
    private Set<String> attributesToRemove;
    private Set<String> toObserve;
    private Set<String> toCancelObserve;
    private Map<Integer, String[]> toObserveByObject;
    private Map<Integer, String[]> toObserveByObjectToCancel;
    private Set<String> toRead;
    private TelemetryObserveStrategy observeStrategyOld;
    private TelemetryObserveStrategy observeStrategyNew;
    @JsonIgnore
    private Set<String> toCancelRead;

    public LwM2MModelConfig(String endpoint, Map<String, ObjectAttributes> attributesToAdd, Set<String> attributesToRemove, Set<String> toObserve,
                            Set<String> toCancelObserve, Map<Integer, String[]> toObserveByObject, Map<Integer, String[]> toObserveByObjectToCancel,
                            Set<String> toRead, TelemetryObserveStrategy observeStrategyOld, TelemetryObserveStrategy observeStrategyNew) {
        this.endpoint = endpoint;
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = attributesToRemove;
        this.toObserve = toObserve;
        this.toCancelObserve = toCancelObserve;
        this.toObserveByObject = toObserveByObject;
        this.toObserveByObjectToCancel = toObserveByObjectToCancel;
        this.toRead = toRead;
        this.toCancelRead = ConcurrentHashMap.newKeySet();
        this.observeStrategyOld = observeStrategyOld;
        this.observeStrategyNew = observeStrategyNew;
    }

    public LwM2MModelConfig(String endpoint) {
        this.endpoint = endpoint;
        this.attributesToAdd = new ConcurrentHashMap<>();
        this.attributesToRemove = ConcurrentHashMap.newKeySet();
        this.toObserve = ConcurrentHashMap.newKeySet();
        this.toCancelObserve = ConcurrentHashMap.newKeySet();
        this.toObserveByObject = new ConcurrentHashMap<>();
        this.toObserveByObjectToCancel = new ConcurrentHashMap<>();
        this.toRead = ConcurrentHashMap.newKeySet();
        this.toCancelRead = ConcurrentHashMap.newKeySet();
        this.observeStrategyOld = SINGLE;
        this.observeStrategyNew = SINGLE;
    }

    public void merge(LwM2MModelConfig modelConfig) {
        if (modelConfig.isEmpty() && modelConfig.getToCancelRead().isEmpty()) {
            return;
        }

        modelConfig.getAttributesToAdd().forEach((k, v) -> {
            if (this.attributesToRemove.contains(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToAdd.put(k, v);
            }
        });

        modelConfig.getAttributesToRemove().forEach(k -> {
            if (this.attributesToAdd.containsKey(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToRemove.add(k);
            }
        });

        this.toObserve.addAll(diffSets(this.toCancelObserve, modelConfig.getToObserve()));
        this.toCancelObserve.addAll(diffSets(this.toObserve, modelConfig.getToCancelObserve()));

        this.toObserve.removeAll(modelConfig.getToCancelObserve());
        this.toCancelObserve.removeAll(modelConfig.getToObserve());

        this.toRead.removeAll(modelConfig.getToObserve());
        this.toRead.removeAll(modelConfig.getToCancelRead());
        this.toRead.addAll(modelConfig.getToRead());

        if (COMPOSITE_BY_OBJECT.equals(this.observeStrategyNew)
                && COMPOSITE_BY_OBJECT.equals(modelConfig.getObserveStrategyNew())
                && COMPOSITE_BY_OBJECT.equals(modelConfig.getObserveStrategyOld())) {

            modelConfig.getToObserveByObjectToCancel().forEach((key, value) ->
                    this.toObserveByObjectToCancel.putIfAbsent(key, value.clone())
            );
            Map<Integer, String[]> toObserveByObjectOld = deepCopyConcurrentMap(this.toObserveByObject);
            this.toObserveByObject =  new ConcurrentHashMap<>();
            for (Map.Entry<Integer, String[]> entry : modelConfig.getToObserveByObject().entrySet()) {
                Integer key = entry.getKey();
                String[] newValue = entry.getValue();
                if (toObserveByObjectOld.containsKey(key)) {
                    String[] oldValue = toObserveByObjectOld.get(key);
                    if (!areArraysStringEqual(oldValue, newValue)) {
                        this.toObserveByObjectToCancel.putIfAbsent(key, oldValue.clone());
                        this.toObserveByObject.put(key, newValue);
                    }
                } else {
                    this.toObserveByObject.put(key, newValue);
                }
            }
        } else {
            this.toObserveByObject = new ConcurrentHashMap<>();
            this.toObserveByObjectToCancel = new ConcurrentHashMap<>();
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return attributesToAdd.isEmpty() && toObserve.isEmpty() && toCancelObserve.isEmpty() && toRead.isEmpty() && this.isEmptyByObject();
    }
    @JsonIgnore
    private boolean isEmptyByObject() {
        return !this.observeStrategyOld.equals(this.observeStrategyNew) || (COMPOSITE_BY_OBJECT.equals(this.observeStrategyOld) && toObserveByObject.isEmpty() && toObserveByObjectToCancel.isEmpty());
    }
}
