// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceTest {

    @Test
    public void everyResourceIsRegisteredInOperationsByResource() {
        Set<Resource> missingEntries = new HashSet<>();

        for (Resource resource : Resource.values()) {
            if (resource == Resource.ALL) {
                continue;
            }
            if (!Resource.operationsByResource.containsKey(resource)) {
                missingEntries.add(resource);
            }
        }

        assertThat(missingEntries)
                .as("Resources with no entry in Resource.operationsByResource " +
                        "(missing resources are silently skipped by role permission expansion)")
                .isEmpty();
    }

}
