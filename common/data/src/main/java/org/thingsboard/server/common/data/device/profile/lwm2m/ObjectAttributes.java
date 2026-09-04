// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
