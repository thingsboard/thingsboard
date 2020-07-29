/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Builder;
import lombok.Data;
import org.eclipse.leshan.core.request.BindingMode;

@Data
public class LwM2MBootstrapServers {
    @Builder.Default
    private Integer shortId = 123;
    @Builder.Default
    private Integer lifetime = 300;
    @Builder.Default
    private Integer defaultMinPeriod = 1;
    @Builder.Default
    private boolean notifIfDisabled = true;
    @Builder.Default
    private String binding = "U";
}
