// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageVO implements Serializable {

    private String data;
    private Integer protocol;
    private String pv;
    private String sign;
    private Long t;

    @Override
    public String toString() {
        return JacksonUtil.toString(this);
    }

}