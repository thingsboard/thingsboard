// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.create;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class VersionCreateConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1223723167716612772L;

    private boolean saveRelations;
    private boolean saveAttributes;
    private boolean saveCredentials;
    private boolean saveCalculatedFields;

}
