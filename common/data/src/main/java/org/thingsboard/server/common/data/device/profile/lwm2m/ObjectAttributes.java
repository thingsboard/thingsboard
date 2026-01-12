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
package org.thingsboard.server.common.data.device.profile.lwm2m;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.eclipse.leshan.core.LwM2m;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectAttributes implements Serializable {

    private static final long serialVersionUID = 4765123984733721312L;
    private Long dim;
    private Long ssid;
    private String uri;
    private String ver;
    private String lwm2m;
    private Long pmin;
    private Long pmax;
    private Double gt;
    private Double lt;
    private Double st;
    private Long epmin;
    private Long epmax;

    public LwM2m.Version getVer(){
        return  ver != null ? new LwM2m.Version(ver) : null;
    }

    public LwM2m.LwM2mVersion getLwm2m(){
        return lwm2m != null ?  LwM2m.LwM2mVersion.get(lwm2m) : null;
    }

}
