/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Request;

public final class ObservationUtil {

    /**
     * Create shallow clone of observation and the contained request.
     *
     * @param observation observation to clone
     * @return a cloned observation with a shallow clone of request, or null, if
     *         null was provided.
     * @throws IllegalArgumentException if observation didn't contain a
     *             request.
     */
    public static Observation shallowClone(Observation observation) {
        if (null == observation) {
            return null;
        }
        Request request = observation.getRequest();
        if (null == request) {
            throw new IllegalArgumentException("missing request for observation!");
        }
        Request clonedRequest = new Request(request.getCode());
        clonedRequest.setDestinationContext(request.getDestinationContext());
        clonedRequest.setType(request.getType());
        clonedRequest.setMID(request.getMID());
        clonedRequest.setToken(request.getToken());
        clonedRequest.setOptions(request.getOptions());
        if (request.isUnintendedPayload()) {
            clonedRequest.setUnintendedPayload();
        }
        clonedRequest.setPayload(request.getPayload());
        clonedRequest.setUserContext(request.getUserContext());
        clonedRequest.setMaxResourceBodySize(request.getMaxResourceBodySize());
        return new Observation(clonedRequest, observation.getContext());
    }
}

