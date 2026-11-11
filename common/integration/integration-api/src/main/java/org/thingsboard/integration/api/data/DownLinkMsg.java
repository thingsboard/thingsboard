// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class DownLinkMsg implements Serializable {

    private final List<TbMsg> msgs = new ArrayList<>();

    public static DownLinkMsg from(IntegrationDownlinkMsg msg) {
        return merge(new DownLinkMsg(), msg);
    }

    public static DownLinkMsg merge(DownLinkMsg result, IntegrationDownlinkMsg msg) {
        result.getMsgs().clear();
        result.getMsgs().add(msg.getTbMsg());
        return result;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return msgs.isEmpty();
    }
}
