// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.instructions;

import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInstructions;

public interface EdgeInstallInstructionsService {

    EdgeInstructions getInstallInstructions(Edge edge, String installationMethod, HttpServletRequest request);

    void setPlatformEdgeVersion(String version);

}
