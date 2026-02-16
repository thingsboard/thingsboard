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
package org.thingsboard.server.common.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ResourceType {
    LWM2M_MODEL("application/xml", false, false),
    JKS("application/x-java-keystore", false, false),
    PKCS_12("application/x-pkcs12", false, false),
    JS_MODULE("application/javascript", true, true),
    IMAGE(null, true, true),
    DASHBOARD("application/json", true, true),
    GENERAL(null, false, true);

    @Getter
    private final String mediaType;
    @Getter
    private final boolean customerAccess;
    @Getter
    private final boolean updatable;

}
