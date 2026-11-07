// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EdgeProfileId;

import java.io.Serial;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EdgeProfileInfo extends EdgeProfile {

    @Serial
    private static final long serialVersionUID = 4818203640517291653L;

    public EdgeProfileInfo() {
        super();
    }

    public EdgeProfileInfo(EdgeProfileId id) {
        super(id);
    }

    public EdgeProfileInfo(EdgeProfile profile) {
        super(profile);
        setRootRuleChainId(null);
        setProfileData(new EdgeProfileData());
    }

}
