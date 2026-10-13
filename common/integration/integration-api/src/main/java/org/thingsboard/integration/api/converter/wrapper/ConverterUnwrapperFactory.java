// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ConverterUnwrapperFactory {

    private static final ConcurrentHashMap<IntegrationType, Optional<ConverterUnwrapper>> unwrappers = new ConcurrentHashMap<>();

    private ConverterUnwrapperFactory() {}

    public static Optional<ConverterUnwrapper> getUnwrapper(IntegrationType integrationType) {
        if (integrationType == null) {
            return Optional.empty();
        }

        return unwrappers.computeIfAbsent(integrationType, key ->
                Optional.ofNullable(switch (integrationType) {
                    case LORIOT -> new LoriotConverterUnwrapper();
                    case CHIRPSTACK -> new ChirpStackConverterUnwrapper();
                    case THINGPARK, TPE -> new ThingParkConverterUnwrapper();
                    case TTN, TTI -> new ThingsStackConverterUnwrapper();
                    default -> null;
                }));
    }
}
