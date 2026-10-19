// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.model;

import lombok.Data;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;

@Data
public class ParametersObserveAnalyzeResult {
    Set<String> observeSingleToCancel = ConcurrentHashMap.newKeySet();
    Set<String> observeSingleToNew = ConcurrentHashMap.newKeySet();
    Map<Integer, String[]> observeByObjectToNew = new ConcurrentHashMap<>();;
    Map<Integer, String[]> observeByObjectToCancel = new ConcurrentHashMap<>();;
    TelemetryObserveStrategy observeStrategyOld = SINGLE;
    TelemetryObserveStrategy observeStrategyNew = SINGLE;

    public ParametersObserveAnalyzeResult(Set<String> observeSingleToCancel, Set<String> observeSingleToNew, TelemetryObserveStrategy observeStrategyOld, TelemetryObserveStrategy observeStrategyNew){
        this.observeSingleToCancel = observeSingleToCancel;
        this.observeSingleToNew = observeSingleToNew;
        this.observeStrategyOld = observeStrategyOld;
        this.observeStrategyNew = observeStrategyNew;
    }

    public ParametersObserveAnalyzeResult(){}
}
