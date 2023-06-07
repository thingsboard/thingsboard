/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

public enum ResourceType {
    LWM2M_MODEL("lwm2m", "application/xml"),
    JKS("jks", "application/x-java-keystore"),
    PKCS_12("pkcs12", "application/x-pkcs12"),
    JS_MODULE("js", "application/javascript");

    private final String type;
    private final String mediaType;

    ResourceType(String type, String mediaType) {
        this.type = type;
        this.mediaType = mediaType;
    }

    public static ResourceType getResourceByType(String type) {
        for(ResourceType resourceType : values()) {
            if (resourceType.getType().equalsIgnoreCase(type)) {
                return resourceType;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getType() {
        return type;
    }
}
