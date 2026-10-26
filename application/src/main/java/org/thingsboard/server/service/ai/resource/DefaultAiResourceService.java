// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.resource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ResourceOperationRequest;
import org.thingsboard.ai.common.channel.ResourceUploadRequest;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiResourceService implements AiResourceService {

    static final long MAX_UPLOAD_BYTES = 7L * 1024 * 1024;

    private final TbAiService tbAiService;
    private final TbAiOperations operations;

    @Override
    public JsonNode getConfig(SecurityUser user) {
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_CONFIG, null), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode upload(MultipartFile file, SecurityUser user) throws ThingsboardException {
        if (file == null || file.isEmpty()) {
            throw new ThingsboardException("The file is empty", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ThingsboardException("The file is too large: " + file.getOriginalFilename(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        String data;
        try {
            data = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new ThingsboardException("Failed to read the file", e, ThingsboardErrorCode.GENERAL);
        }
        var request = new ResourceUploadRequest(file.getOriginalFilename(), file.getContentType(), data);
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_UPLOAD, request), user, tokenProvider), user, false);
    }

    @Override
    public AiResourceContent download(UUID resourceId, SecurityUser user) {
        JsonNode content = tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_GET, new ResourceOperationRequest(resourceId)), user, tokenProvider), user, false);
        return new AiResourceContent(content.path("name").asText(), content.path("mediaType").asText(),
                Base64.getDecoder().decode(content.path("data").asText()));
    }

    @Override
    public void delete(UUID resourceId, SecurityUser user) {
        tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_DELETE, new ResourceOperationRequest(resourceId)), user, tokenProvider), user, false);
    }

}
