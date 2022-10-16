/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import ua_parser.Client;

public class RestAuthenticationDetailsUtils {

    public static AuthorizationDetails getRestAuthenticationDetails(RestAuthenticationDetails details) {
        String clientAddress = details.getClientAddress();
        String browser = "Unknown";
        String os = "Unknown";
        String device = "Unknown";
        if (details.getUserAgent() != null) {
            Client userAgent = details.getUserAgent();
            if (userAgent.userAgent != null) {
                browser = userAgent.userAgent.family;
                if (userAgent.userAgent.major != null) {
                    browser += " " + userAgent.userAgent.major;
                    if (userAgent.userAgent.minor != null) {
                        browser += "." + userAgent.userAgent.minor;
                        if (userAgent.userAgent.patch != null) {
                            browser += "." + userAgent.userAgent.patch;
                        }
                    }
                }
            }
            if (userAgent.os != null) {
                os = userAgent.os.family;
                if (userAgent.os.major != null) {
                    os += " " + userAgent.os.major;
                    if (userAgent.os.minor != null) {
                        os += "." + userAgent.os.minor;
                        if (userAgent.os.patch != null) {
                            os += "." + userAgent.os.patch;
                            if (userAgent.os.patchMinor != null) {
                                os += "." + userAgent.os.patchMinor;
                            }
                        }
                    }
                }
            }
            if (userAgent.device != null) {
                device = userAgent.device.family;
            }
        }
        return new AuthorizationDetails(clientAddress, browser, os, device);
    }
}
