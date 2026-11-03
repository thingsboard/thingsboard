// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class VersionCreationResult implements Serializable {
    private static final long serialVersionUID = 8032189124530267838L;

    private EntityVersion version;
    private int added;
    private int modified;
    private int removed;

    private String error;
    private boolean done;


    public VersionCreationResult(EntityVersion version, int added, int modified, int removed) {
        this.version = version;
        this.added = added;
        this.modified = modified;
        this.removed = removed;
        this.done = true;
    }

    public VersionCreationResult(String error) {
        this.error = error;
        this.done = true;
    }
}
