// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

public record DockerComposeParams(boolean includeVersion, String containerName, boolean includePortBindings,
                                  boolean includeExtraHosts, boolean includeVolumesBind,
                                  boolean includeVolumesDeclaration) {
}
