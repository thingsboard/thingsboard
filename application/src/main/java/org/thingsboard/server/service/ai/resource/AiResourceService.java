// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

public interface AiResourceService {

    JsonNode getConfig(SecurityUser user);

    JsonNode upload(MultipartFile file, SecurityUser user) throws ThingsboardException;

    AiResourceContent download(UUID resourceId, SecurityUser user);

    void delete(UUID resourceId, SecurityUser user);

    record AiResourceContent(String name, String mediaType, byte[] data) {}

}
