// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class AutoVersionCreateConfig extends VersionCreateConfig {

    @Serial
    private static final long serialVersionUID = 8245450889383315551L;

    private String branch;

}
